package com.unstablecompanions.ai.goal;

import java.util.EnumSet;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;

import com.unstablecompanions.ai.dialogue.DialogueProvider;
import com.unstablecompanions.config.ModConfig;
import com.unstablecompanions.entity.CompanionEntity;

/**
 * JumperWho-specific: passively gathers "intel" about entities/structures
 * within {@code spyNetworkRadius} and periodically reports useful findings
 * to the player (e.g. a rival faction's base location, an enemy's gear).
 *
 * Also tracks whether the player has recently chosen "reckless revenge"
 * over a smarter play (a flag set externally -- e.g. by a quest/decision
 * system or simply by the player attacking a much stronger enemy faction
 * head-on despite JumperWho's advice). Exceeding {@code recklessTolerance}
 * causes JumperWho to leave the player's faction.
 */
public class SpyNetworkGoal extends Goal {

	private final CompanionEntity jumper;
	private int intelCooldown;
	private int recklessStrikes = 0;

	public SpyNetworkGoal(CompanionEntity jumper) {
		this.jumper = jumper;
		this.setControls(EnumSet.noneOf(Goal.Control.class)); // passive; doesn't hijack movement
	}

	@Override
	public boolean canStart() {
		if (!ModConfig.INSTANCE.enableJumperSpyNetwork) return false;
		if (intelCooldown > 0) {
			intelCooldown--;
			return false;
		}
		return true;
	}

	@Override
	public void start() {
		if (!(jumper.getEntityWorld() instanceof ServerWorld serverWorld)) return;
		PlayerEntity player = serverWorld.getClosestPlayer(jumper, jumper.getTraits().spyNetworkRadius());
		if (player != null) {
			String line = jumper.requestDialogue(player, DialogueProvider.Trigger.SPY_INTEL_REPORT);
			// TODO: replace generic line with a real intel payload once faction/base
			// tracking exists (e.g. "Ashswagg's mafia base is near X,Y,Z").
		}
		intelCooldown = 20 * 60 * 10; // report at most every ~10 minutes
	}

	/** Call this when the player picks a reckless option JumperWho advised against. */
	public void notifyRecklessChoice(PlayerEntity player) {
		recklessStrikes++;
		if (recklessStrikes > jumper.getTraits().recklessTolerance()) {
			leaveInDisappointment(player);
		}
	}

	private void leaveInDisappointment(PlayerEntity player) {
		String line = jumper.requestDialogue(player, DialogueProvider.Trigger.LEAVING_IN_DISAPPOINTMENT);
		// TODO: broadcast `line`, then remove from faction and either despawn
		// gracefully or relocate to an independent "retired spy" state.
		jumper.setFactionId(null);
	}

	@Override
	public boolean shouldContinue() {
		return false;
	}
}
