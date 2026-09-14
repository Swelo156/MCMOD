package com.unstablecompanions.entity.character;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

import com.unstablecompanions.ai.goal.BuildPrisonGoal;
import com.unstablecompanions.entity.CompanionEntity;

/**
 * Wifies: builds near-impossible prisons. Friends are relatively safe, but
 * per the design doc there's always a baseline betrayal/imprisonment risk
 * even for friends -- see BuildPrisonGoal#rollBetrayalCheck.
 */
public class WifiesEntity extends CompanionEntity {

	private BuildPrisonGoal buildPrisonGoal;

	public WifiesEntity(EntityType<? extends WifiesEntity> type, World world) {
		super(type, world);
	}

	@Override
	public String getCharacterId() {
		return "wifies";
	}

	@Override
	protected void initCustomGoals() {
		this.buildPrisonGoal = new BuildPrisonGoal(this);
		this.goalSelector.add(3, buildPrisonGoal);
	}

	public BuildPrisonGoal getBuildPrisonGoal() {
		return buildPrisonGoal;
	}
}
