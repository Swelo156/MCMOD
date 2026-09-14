package com.unstablecompanions.entity.character;

import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import com.unstablecompanions.ai.goal.RecruitGoal;
import com.unstablecompanions.config.CharacterTraits;
import com.unstablecompanions.entity.CompanionEntity;
import com.unstablecompanions.faction.Faction;
import com.unstablecompanions.faction.FactionManager;

/**
 * LettuceK: founds and grows "The Law", a faction built around order and
 * recruiting like-minded (or simply nearby, willing) companions.
 */
public class LettuceKEntity extends CompanionEntity {

	public LettuceKEntity(EntityType<? extends LettuceKEntity> type, World world) {
		super(type, world);
	}

	@Override
	public String getCharacterId() {
		return "lettuce_k";
	}

	@Override
	protected void initCustomGoals() {
		// The Law faction + RecruitGoal are wired up lazily on first server tick
		// once we have access to the world's FactionManager -- see initFactionGoal().
	}

	/**
	 * Call once after spawn, when a FactionManager instance is available
	 * (e.g. from a world-attached persistent state accessor). Left as an
	 * explicit init step rather than done in initCustomGoals() because goal
	 * construction happens before the entity is necessarily in a ServerWorld
	 * with faction data loaded.
	 */
	public void initFactionGoal(FactionManager factionManager) {
		CharacterTraits.Traits traits = getTraits();
		Faction theLaw = factionManager.getOrCreateVillainFaction(
				this.getUuid(), Faction.Archetype.THE_LAW, traits.factionName());
		this.setFactionId(theLaw.getFactionId());
		this.goalSelector.add(5, new RecruitGoal(this, theLaw, 0.5f));
	}
}
