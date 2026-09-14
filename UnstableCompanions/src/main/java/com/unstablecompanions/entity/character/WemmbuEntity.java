package com.unstablecompanions.entity.character;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import com.unstablecompanions.ai.goal.RevengeHuntGoal;
import com.unstablecompanions.entity.CompanionEntity;

/**
 * Wemmbu: launches a sustained revenge hunt if Eggchan dies due to the
 * player. Can use "stab shots" and "nuke shots" (see item.ModItems and the
 * TODO markers in RevengeHuntGoal for wiring those into combat).
 */
public class WemmbuEntity extends CompanionEntity {

	private RevengeHuntGoal revengeHuntGoal;

	public WemmbuEntity(EntityType<? extends WemmbuEntity> type, World world) {
		super(type, world);
	}

	@Override
	public String getCharacterId() {
		return "wemmbu";
	}

	@Override
	protected void initCustomGoals() {
		this.revengeHuntGoal = new RevengeHuntGoal(this);
		this.goalSelector.add(1, revengeHuntGoal);
	}

	/**
	 * Wire this call into an Eggchan-death event listener
	 * (e.g. ServerLivingEntityEvents.AFTER_DEATH from Fabric API, checking the
	 * dying entity's character id == "eggchan" and damage source traceable to
	 * a player) once an Eggchan entity/character exists in the roster.
	 */
    public void notifyEggchanDeath(PlayerEntity culprit, long currentTick) {
		revengeHuntGoal.triggerRevenge(culprit, currentTick);
	}
}
