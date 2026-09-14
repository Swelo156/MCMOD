package com.unstablecompanions.entity;

import java.util.UUID;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Uuids;
import net.minecraft.world.World;

import com.unstablecompanions.ai.dialogue.DialogueProvider;
import com.unstablecompanions.config.CharacterTraits;
import com.unstablecompanions.faction.FactionRank;
import com.unstablecompanions.relationship.RelationshipManager;
import com.unstablecompanions.relationship.RelationshipType;

/**
 * Shared base for every Unstable Companions character.
 *
 * Responsibilities kept here (character-agnostic):
 *  - Identity: stable character id + persistent UUID-based recruiter/owner link.
 *  - Relationship graph (via {@link RelationshipManager}).
 *  - Faction membership pointer (the actual {@code Faction} object lives in
 *    {@code FactionManager}; this entity only stores the id + rank cache).
 *  - Common NBT read/write so custom data survives chunk unload/reload.
 *  - A slot for character-specific goals, added by subclasses in
 *    {@link #initCustomGoals()} -- keeps this class from becoming a god-class
 *    of "if (characterId.equals(...))" branches.
 *  - A dialogue hook for personality-driven chat responses (LLM-backed).
 *
 * Performance notes:
 *  - Distant companions should be excluded from full goal ticking by the
 *    server tick handler based on {@code ModConfig.activeSimulationRadius};
 *    see {@code OfflineSimulationManager}. This class does not self-throttle,
 *    to keep it a simple/dumb data+behavior holder.
 */
public abstract class CompanionEntity extends PathAwareEntity {

	private final RelationshipManager relationshipManager = new RelationshipManager();
	private UUID recruitedBy; // player who recruited this companion into their faction, if any
	private UUID factionId;   // Faction.getFactionId(), or null if unaffiliated
	private FactionRank factionRank = FactionRank.MEMBER;
	private boolean goalsInitialized = false;

	protected CompanionEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
	}

	/** Stable, lowercase_snake_case id matching CharacterTraits registry keys, e.g. "flame_frags". */
	public abstract String getCharacterId();

	public CharacterTraits.Traits getTraits() {
		return CharacterTraits.get(getCharacterId());
	}

	public RelationshipManager getRelationshipManager() {
		return relationshipManager;
	}

	public RelationshipType getRelationshipWith(UUID uuid) {
		return relationshipManager.classify(uuid);
	}

	public UUID getRecruitedBy() {
		return recruitedBy;
	}

	public void setRecruitedBy(UUID recruitedBy) {
		this.recruitedBy = recruitedBy;
	}

	public UUID getFactionId() {
		return factionId;
	}

	public void setFactionId(UUID factionId) {
		this.factionId = factionId;
	}

	public FactionRank getFactionRank() {
		return factionRank;
	}

	public void setFactionRank(FactionRank factionRank) {
		this.factionRank = factionRank;
	}

	// -----------------------------------------------------------------
	// Attributes
	// -----------------------------------------------------------------

	/** Call from each subclass's static initializer / ModEntities registration via FabricDefaultAttributeRegistry. */
	public static DefaultAttributeContainer.Builder createCompanionAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.MAX_HEALTH, 20.0)
				.add(EntityAttributes.MOVEMENT_SPEED, 0.3)
				.add(EntityAttributes.ATTACK_DAMAGE, 3.0)
				.add(EntityAttributes.FOLLOW_RANGE, 32.0);
	}

	// -----------------------------------------------------------------
	// Goals
	// -----------------------------------------------------------------

	@Override
	protected void initGoals() {
		// Deliberately NOT calling super.initGoals() equivalent here since PathAwareEntity
		// has no default goals of its own; base movement/look goals are added explicitly
		// so the ordering (priority) is obvious and consistent across all characters.
		this.goalSelector.add(0, new SwimGoal(this));
		this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
		this.goalSelector.add(8, new LookAroundGoal(this));
		this.goalSelector.add(9, new WanderAroundFarGoal(this, 0.8));

		if (!goalsInitialized) {
			initCustomGoals();
			goalsInitialized = true;
		}
	}

	/**
	 * Subclasses register their unique {@link Goal}s here (confrontation,
	 * revenge hunts, rescues, prison-building, spying, betrayal, faction
	 * recruitment, etc). Keeping this abstract forces every new character
	 * to make a deliberate decision about its goal set instead of inheriting
	 * unrelated behavior by accident.
	 */
	protected abstract void initCustomGoals();

	public GoalSelector getGoalSelectorAccess() {
		return this.goalSelector;
	}

	// -----------------------------------------------------------------
	// Dialogue hook
	// -----------------------------------------------------------------

	/**
	 * Called when the player interacts (right-click) or when a goal wants to
	 * surface a line of dialogue (confrontation, betrayal, rescue banter...).
	 * Delegates to {@link DialogueProvider}, which is where a local/cloud LLM
	 * integration should be hooked in -- see that class for the exact seam.
	 */
	public String requestDialogue(PlayerEntity player, DialogueProvider.Trigger trigger) {
		return DialogueProvider.getInstance().generateLine(this, player, trigger);
	}

	// -----------------------------------------------------------------
	// Persistence
	// -----------------------------------------------------------------

	/**
	 * Minecraft 1.21.6+ replaced the old writeCustomDataToNbt(NbtCompound) /
	 * readCustomDataFromNbt(NbtCompound) hooks with a View-based API
	 * (writeCustomData(WriteView) / readCustomData(ReadView)). We bridge our
	 * existing hand-rolled NbtCompound serialization for the relationship
	 * graph through the NbtCompound codec rather than teaching
	 * RelationshipManager to speak WriteView directly.
	 */
	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putString("CharacterId", getCharacterId());
		view.putNullable("RecruitedBy", Uuids.CODEC, recruitedBy);
		view.putNullable("FactionId", Uuids.CODEC, factionId);
		view.putString("FactionRank", factionRank.name());
		view.put("Relationships", NbtCompound.CODEC, relationshipManager.writeNbt());
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		recruitedBy = view.read("RecruitedBy", Uuids.CODEC).orElse(null);
		factionId = view.read("FactionId", Uuids.CODEC).orElse(null);
		String rankName = view.getString("FactionRank", FactionRank.MEMBER.name());
		try {
			factionRank = FactionRank.valueOf(rankName);
		} catch (IllegalArgumentException ignored) {
			factionRank = FactionRank.MEMBER;
		}
		view.read("Relationships", NbtCompound.CODEC).ifPresent(relationshipManager::readNbt);
	}

	/**
	 * Persistent companions should survive natural despawning. Combined with
	 * {@code ModConfig.maxActiveCompanions}, admins/players still have a way
	 * to bound world entity count -- see OfflineSimulationManager for the
	 * "demote distant companions to lightweight simulation" strategy instead
	 * of despawning them outright.
	 */
	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	/**
	 * Central point for awarding/penalizing relationship score in response to
	 * gameplay events (gifting, fighting alongside, betrayal, etc). Kept here
	 * so goals don't reach into RelationshipManager with ad-hoc numbers.
	 */
	public void adjustRelationship(UUID target, int delta) {
		if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
			relationshipManager.adjustScore(target, delta, serverWorld.getServer().getTicks());
		}
	}
}
