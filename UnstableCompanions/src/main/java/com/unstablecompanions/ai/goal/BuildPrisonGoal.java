package com.unstablecompanions.ai.goal;

import java.util.EnumSet;
import java.util.Random;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;

import com.unstablecompanions.config.ModConfig;
import com.unstablecompanions.entity.CompanionEntity;

/**
 * Wifies-specific: periodically constructs a "near-impossible" prison
 * structure near her base. Friends are safe from it by default, but per the
 * design doc there is always a nonzero {@code betrayalChancePerMonth} even
 * for friends -- see {@link #rollBetrayalCheck()}.
 *
 * Structure generation itself should use a structure-template / NBT
 * structure piece (data/unstablecompanions/structure/wifies_prison.nbt) via
 * {@code StructureTemplateManager} rather than manual block-by-block
 * placement -- left as a TODO since it's asset-dependent.
 */
public class BuildPrisonGoal extends Goal {

	private final CompanionEntity wifies;
	private final Random random = new Random();
	private int cooldownTicks;

	public BuildPrisonGoal(CompanionEntity wifies) {
		this.wifies = wifies;
		this.setControls(EnumSet.of(Goal.Control.MOVE));
	}

	@Override
	public boolean canStart() {
		if (!ModConfig.INSTANCE.enableWifiesPrisons) return false;
		if (cooldownTicks > 0) {
			cooldownTicks--;
			return false;
		}
		return wifies.getFactionId() != null; // only builds once she has a base/faction context
	}

	@Override
	public void start() {
		BlockPos origin = wifies.getBlockPos();
		// TODO: place a structure template centered near `origin`, offset to avoid
		// overlapping the existing base. Track placed prison bounds on the Faction
		// (or a dedicated PrisonRegistry) so capture goals know where to route captives.
		cooldownTicks = 20 * 60 * 30; // one attempt per ~30 minutes of activity
	}

	/**
	 * Call when deciding whether a currently-imprisoned FRIEND companion gets
	 * quietly released vs left to rot -- per design doc, friendship reduces
	 * but does not eliminate betrayal/imprisonment risk.
	 */
	public boolean rollBetrayalCheck() {
		float monthlyChance = wifies.getTraits().betrayalChancePerMonth();
		// Convert a "per month" probability into a per-check roll; the caller
		// determines how often this method is invoked (e.g. once per in-game day).
		float perDayChance = monthlyChance / 30f;
		return random.nextFloat() < perDayChance;
	}

	@Override
	public boolean shouldContinue() {
		return false; // one-shot construction attempt per activation
	}
}
