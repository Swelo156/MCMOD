package com.unstablecompanions.ai.goal;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

import com.unstablecompanions.ai.dialogue.DialogueProvider;
import com.unstablecompanions.config.ModConfig;
import com.unstablecompanions.entity.CompanionEntity;

/**
 * SpokeIsHere-specific: if the player or a faction ally is "captured"
 * (see the capture-state hook this mod's prison system, e.g. WifiesEntity's
 * prisons, should set on captives), Spoke drops whatever they're doing,
 * rushes toward the captor(s), and fights at a boosted damage output until
 * the ally is freed or Spoke is downed.
 *
 * The exact "captured" flag is intentionally left as a simple marker
 * (see {@code isCaptured}) so any prison/capture system -- Wifies' or a
 * future one -- can flip it without SpokeIsHere needing direct knowledge of
 * how captivity is implemented.
 */
public class RideOrDieRescueGoal extends Goal {

	private static final Identifier ALL_IN_MODIFIER_ID = Identifier.of("unstablecompanions", "all_in_damage_boost");

	private final CompanionEntity spoke;
	private LivingEntity rescueTarget;
	private boolean boosted = false;
	private EntityAttributeModifier activeModifier;

	public RideOrDieRescueGoal(CompanionEntity spoke) {
		this.spoke = spoke;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK, Goal.Control.TARGET));
	}

	@Override
	public boolean canStart() {
		if (!ModConfig.INSTANCE.enableSpokeRideOrDie) return false;
		rescueTarget = findCapturedAlly();
		return rescueTarget != null;
	}

	private LivingEntity findCapturedAlly() {
		int radius = spoke.getTraits().rescueAggroRadius();
		Box box = spoke.getBoundingBox().expand(radius);
		List<PlayerEntity> nearbyPlayers = spoke.getEntityWorld().getEntitiesByClass(
				PlayerEntity.class, box, PlayerEntity::isAlive);
		for (PlayerEntity player : nearbyPlayers) {
			if (isCaptured(player)) {
				return player;
			}
		}
		// TODO: also scan nearby CompanionEntity allies for a captured flag once
		// the capture/prison system exposes one uniformly.
		return null;
	}

	private boolean isCaptured(LivingEntity entity) {
		// TODO: replace with a real check against the prison/capture system
		// (e.g. entity.getAttached(CAPTURED_STATE) or a capability-style component).
		return false;
	}

	@Override
	public void start() {
		if (rescueTarget == null) return;
		applyAllInBoost();
		if (rescueTarget instanceof PlayerEntity player) {
			String line = spoke.requestDialogue(player, DialogueProvider.Trigger.RESCUE_CALLOUT);
			// TODO: broadcast `line` to nearby faction members to summon a group rescue.
		}
		// TODO: set target to the nearest captor entity once capture system tracks captors.
	}

	private void applyAllInBoost() {
		if (boosted) return;
		var attributeInstance = spoke.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
		if (attributeInstance != null) {
			activeModifier = new EntityAttributeModifier(
					ALL_IN_MODIFIER_ID,
					spoke.getTraits().allInDamageBoost() - 1.0,
					EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
			attributeInstance.addTemporaryModifier(activeModifier);
			boosted = true;
		}
	}

	private void clearAllInBoost() {
		if (activeModifier == null) return;
		var attributeInstance = spoke.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
		if (attributeInstance != null) {
			attributeInstance.removeModifier(activeModifier);
		}
		activeModifier = null;
		boosted = false;
	}

	@Override
	public boolean shouldContinue() {
		return rescueTarget != null && isCaptured(rescueTarget);
	}

	@Override
	public void stop() {
		clearAllInBoost();
		rescueTarget = null;
	}
}
