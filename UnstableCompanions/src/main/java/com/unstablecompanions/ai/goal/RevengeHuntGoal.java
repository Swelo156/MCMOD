package com.unstablecompanions.ai.goal;

import java.util.EnumSet;
import java.util.UUID;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;

import com.unstablecompanions.ai.dialogue.DialogueProvider;
import com.unstablecompanions.config.ModConfig;
import com.unstablecompanions.entity.CompanionEntity;

/**
 * Wemmbu-specific: if Eggchan (tracked by UUID, see {@link #eggchanUuid})
 * dies as a direct or indirect result of the player's actions, Wemmbu enters
 * a sustained hunt state -- actively pathing toward the player's last known
 * position, using "stab shots" and "nuke shots" (see items/goal-side combat
 * hooks below) and refusing to de-aggro until the configured revenge
 * duration elapses.
 *
 * The actual "was Eggchan's death the player's fault" judgement should be
 * captured at the moment of death (e.g. via a LivingEntity death event
 * listener checking the damage source) and passed in via
 * {@link #triggerRevenge}, rather than re-derived here.
 */
public class RevengeHuntGoal extends Goal {

	private final CompanionEntity wemmbu;
	private UUID revengeTargetUuid;
	private long revengeExpiresAtTick = -1;
	private boolean active = false;

	public RevengeHuntGoal(CompanionEntity wemmbu) {
		this.wemmbu = wemmbu;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK, Goal.Control.TARGET));
	}

	/** Call from the death-event listener the moment Eggchan dies to player-caused damage. */
	public void triggerRevenge(PlayerEntity culprit, long currentTick) {
		if (!ModConfig.INSTANCE.enableWemmbuRevengeArc) return;
		this.revengeTargetUuid = culprit.getUuid();
		int days = wemmbu.getTraits().revengeDurationDays();
		this.revengeExpiresAtTick = currentTick + (20L * 60 * 20 * days); // ~20 min/day placeholder daylength
		this.active = true;

		String line = wemmbu.requestDialogue(culprit, DialogueProvider.Trigger.REVENGE_HUNT_START);
		// TODO: broadcast `line`; hook into chat/notification system.
	}

	@Override
	public boolean canStart() {
		if (!active || revengeTargetUuid == null) return false;
		if (!(wemmbu.getEntityWorld() instanceof ServerWorld serverWorld)) return false;
		if (serverWorld.getServer().getTicks() >= revengeExpiresAtTick) {
			active = false;
			return false;
		}
		PlayerEntity target = serverWorld.getServer().getPlayerManager().getPlayer(revengeTargetUuid);
		return target != null;
	}

	@Override
	public void start() {
		if (!(wemmbu.getEntityWorld() instanceof ServerWorld serverWorld)) return;
		PlayerEntity target = serverWorld.getServer().getPlayerManager().getPlayer(revengeTargetUuid);
		if (target != null) {
			wemmbu.setTarget(target);
		}
	}

	@Override
	public void tick() {
		// TODO: preferential use of "stab shot" at close range / "nuke shot" when
		// the target clusters with allies -- wire up once the ammo items and a
		// ranged-attack goal/projectile entity exist (see item.ModItems and a
		// future WemmbuStabShotItem / WemmbuNukeShotItem + projectile entity).
	}

	@Override
	public boolean shouldContinue() {
		return active && wemmbu.getTarget() != null;
	}

	@Override
	public void stop() {
		wemmbu.setTarget(null);
	}
}
