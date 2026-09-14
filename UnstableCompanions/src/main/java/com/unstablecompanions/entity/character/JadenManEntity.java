package com.unstablecompanions.entity.character;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import com.unstablecompanions.ai.goal.OpportunisticBetrayalGoal;
import com.unstablecompanions.entity.CompanionEntity;

/**
 * Jaden_MAN: opportunistic. Betrays if the player is undergeared (below
 * iron armor by default) or he otherwise decides he doesn't need them
 * anymore. Responds well to receiving high-tier gear.
 */
public class JadenManEntity extends CompanionEntity {

	private OpportunisticBetrayalGoal betrayalGoal;

	public JadenManEntity(EntityType<? extends JadenManEntity> type, World world) {
		super(type, world);
	}

	@Override
	public String getCharacterId() {
		return "jaden_man";
	}

	@Override
	protected void initCustomGoals() {
		this.betrayalGoal = new OpportunisticBetrayalGoal(this);
		this.goalSelector.add(2, betrayalGoal);
	}

	/** Wire into a gift/trade interaction handler. */
	public void notifyGearGift(PlayerEntity player, int tier) {
		betrayalGoal.notifyGearGift(player, tier);
	}
}
