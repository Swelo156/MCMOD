package com.unstablecompanions.ai.goal;

import java.util.EnumSet;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;

import com.unstablecompanions.ai.dialogue.DialogueProvider;
import com.unstablecompanions.config.ModConfig;
import com.unstablecompanions.entity.CompanionEntity;
import com.unstablecompanions.relationship.RelationshipType;

/**
 * FlameFrags-specific: tracks a "worthiness" meter (stored as relationship
 * score toward the player, reusing the existing scale rather than adding a
 * parallel stat) and confronts the player if they are friends with one of
 * FlameFrags' enemies. If the player then sides with that enemy, FlameFrags
 * escalates to open hostility (see {@link #escalateToHostile}).
 *
 * Does NOT touch resources -- FlameFrags is explicitly configured
 * (leechesResources=false in CharacterTraits) to never pull from shared
 * storage, per the design doc.
 */
public class WorthinessConfrontGoal extends Goal {

	private final CompanionEntity flameFrags;
	private int cooldownTicks;

	public WorthinessConfrontGoal(CompanionEntity flameFrags) {
		this.flameFrags = flameFrags;
		this.setControls(EnumSet.of(Goal.Control.LOOK, Goal.Control.MOVE));
	}

	@Override
	public boolean canStart() {
		if (!ModConfig.INSTANCE.enableFlameFragsWorthinessMeter) return false;
		if (cooldownTicks > 0) {
			cooldownTicks--;
			return false;
		}
		PlayerEntity nearestPlayer = flameFrags.getEntityWorld().getClosestPlayer(flameFrags, 16.0);
		if (nearestPlayer == null) return false;

		return flameFrags.getRelationshipManager().isPastConfrontThreshold(nearestPlayer.getUuid());
	}

	@Override
	public void start() {
		PlayerEntity player = flameFrags.getEntityWorld().getClosestPlayer(flameFrags, 16.0);
		if (player == null) return;

		String line = flameFrags.requestDialogue(player, DialogueProvider.Trigger.WORTHINESS_CONFRONTATION);
		// TODO: send `line` as an actual chat/name-tag message once client-facing
		// messaging plumbing exists; left as a hook point here.

		// If the player has *already* sided with the enemy (tracked elsewhere,
		// e.g. player fought alongside that enemy against FlameFrags), escalate.
		if (hasPlayerSidedWithEnemy(player)) {
			escalateToHostile(player);
		}

		flameFrags.getRelationshipManager().getOrCreate(player.getUuid()).resetTension();
		cooldownTicks = 20 * 60 * 5; // don't re-confront for 5 minutes
	}

	private boolean hasPlayerSidedWithEnemy(PlayerEntity player) {
		// TODO: real implementation should check a tracked "last combat ally" event
		// or similar rather than relationship score alone. Placeholder heuristic:
		return flameFrags.getRelationshipWith(player.getUuid()) == RelationshipType.ENEMY;
	}

	private void escalateToHostile(PlayerEntity player) {
		if (flameFrags.getEntityWorld() instanceof ServerWorld) {
			flameFrags.setTarget(player);
			flameFrags.adjustRelationship(player.getUuid(), -50);
		}
	}

	@Override
	public boolean shouldContinue() {
		return false; // single-shot confrontation, not a sustained goal
	}
}
