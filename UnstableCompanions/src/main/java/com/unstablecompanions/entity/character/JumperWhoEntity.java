package com.unstablecompanions.entity.character;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import com.unstablecompanions.ai.goal.SpyNetworkGoal;
import com.unstablecompanions.entity.CompanionEntity;

/**
 * JumperWho: logical support backed by a spy network. Leaves the player's
 * faction if they repeatedly choose reckless revenge over smart plays.
 */
public class JumperWhoEntity extends CompanionEntity {

	private SpyNetworkGoal spyNetworkGoal;

	public JumperWhoEntity(EntityType<? extends JumperWhoEntity> type, World world) {
		super(type, world);
	}

	@Override
	public String getCharacterId() {
		return "jumper_who";
	}

	@Override
	protected void initCustomGoals() {
		this.spyNetworkGoal = new SpyNetworkGoal(this);
		this.goalSelector.add(4, spyNetworkGoal);
	}

	/** Wire this into whatever "choice" system tracks player decisions (quest/dialogue UI, etc). */
	public void notifyRecklessChoice(PlayerEntity player) {
		spyNetworkGoal.notifyRecklessChoice(player);
	}
}
