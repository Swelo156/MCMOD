package com.unstablecompanions.entity.character;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

import com.unstablecompanions.ai.goal.RecruitGoal;
import com.unstablecompanions.config.CharacterTraits;
import com.unstablecompanions.entity.CompanionEntity;
import com.unstablecompanions.faction.Faction;
import com.unstablecompanions.faction.FactionManager;

/**
 * JamatoP: builds NULL/Purgatory -- prisons plus dedicated hunter groups
 * that actively patrol and attempt captures, distinct from Wifies' more
 * passive, individually-built prisons.
 */
public class JamatoPEntity extends CompanionEntity {

	public JamatoPEntity(EntityType<? extends JamatoPEntity> type, World world) {
		super(type, world);
	}

	@Override
	public String getCharacterId() {
		return "jamato_p";
	}

	@Override
	protected void initCustomGoals() {
		// See initFactionGoal(); mirrors LettuceKEntity's lazy-init pattern.
	}

	public void initFactionGoal(FactionManager factionManager) {
		CharacterTraits.Traits traits = getTraits();
		Faction purgatory = factionManager.getOrCreateVillainFaction(
				this.getUuid(), Faction.Archetype.NULL_PURGATORY, traits.factionName());
		this.setFactionId(purgatory.getFactionId());
		this.goalSelector.add(5, new RecruitGoal(this, purgatory, 0.35f));
		// TODO: add a dedicated HunterPatrolGoal here once capture/prison routing exists;
		// hunter group sizing should read CharacterTraits (hunterGroupSize concept --
		// currently folded into leaderStrength, split out if it needs independent tuning).
	}
}
