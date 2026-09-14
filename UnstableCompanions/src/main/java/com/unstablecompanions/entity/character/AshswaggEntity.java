package com.unstablecompanions.entity.character;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

import com.unstablecompanions.ai.goal.RecruitGoal;
import com.unstablecompanions.config.CharacterTraits;
import com.unstablecompanions.entity.CompanionEntity;
import com.unstablecompanions.faction.Faction;
import com.unstablecompanions.faction.FactionManager;

/**
 * Ashswagg: founds the secretive "Invisible Mafia" -- a faction whose
 * involvement in a hit is meant to stay hidden from the target until it's
 * too late. Recruitment is quieter/slower than LettuceK's; hits are dealt
 * with in FactionManager#tickMafiaFaction.
 */
public class AshswaggEntity extends CompanionEntity {

	public AshswaggEntity(EntityType<? extends AshswaggEntity> type, World world) {
		super(type, world);
	}

	@Override
	public String getCharacterId() {
		return "ashswagg";
	}

	@Override
	protected void initCustomGoals() {
		// See initFactionGoal(); mirrors LettuceKEntity's lazy-init pattern.
	}

	public void initFactionGoal(FactionManager factionManager) {
		CharacterTraits.Traits traits = getTraits();
		Faction mafia = factionManager.getOrCreateVillainFaction(
				this.getUuid(), Faction.Archetype.INVISIBLE_MAFIA, traits.factionName());
		this.setFactionId(mafia.getFactionId());
		// Lower aggressiveness than LettuceK -- the mafia recruits quietly, by invitation.
		this.goalSelector.add(5, new RecruitGoal(this, mafia, 0.2f));
	}
}
