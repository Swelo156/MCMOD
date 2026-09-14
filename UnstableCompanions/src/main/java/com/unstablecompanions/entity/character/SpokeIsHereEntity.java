package com.unstablecompanions.entity.character;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

import com.unstablecompanions.ai.goal.RideOrDieRescueGoal;
import com.unstablecompanions.entity.CompanionEntity;

/**
 * SpokeIsHere: ride-or-die. Goes all-in (boosted damage, no restraint) to
 * rescue the player or an ally the moment either is captured.
 */
public class SpokeIsHereEntity extends CompanionEntity {

	public SpokeIsHereEntity(EntityType<? extends SpokeIsHereEntity> type, World world) {
		super(type, world);
	}

	@Override
	public String getCharacterId() {
		return "spoke_is_here";
	}

	@Override
	protected void initCustomGoals() {
		this.goalSelector.add(0, new RideOrDieRescueGoal(this));
	}
}
