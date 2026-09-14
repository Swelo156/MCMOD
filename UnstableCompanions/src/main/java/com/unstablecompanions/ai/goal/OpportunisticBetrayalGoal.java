package com.unstablecompanions.ai.goal;

import java.util.EnumSet;
import java.util.Random;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;

import com.unstablecompanions.ai.dialogue.DialogueProvider;
import com.unstablecompanions.config.ModConfig;
import com.unstablecompanions.entity.CompanionEntity;

/**
 * Jaden_MAN-specific: evaluates the player's gear tier and general "do I
 * still need this player" standing. If the player's armor falls below
 * {@code minAcceptableArmorTier} (default: iron), Jaden has an escalating
 * chance per day of betraying them outright. Conversely, receiving
 * high-tier gear as a gift should award relationship score elsewhere
 * (see a future GiftItemGoal/interaction handler) to keep him loyal.
 */
public class OpportunisticBetrayalGoal extends Goal {

	private final CompanionEntity jaden;
	private final Random random = new Random();
	private int checkCooldown;

	public OpportunisticBetrayalGoal(CompanionEntity jaden) {
		this.jaden = jaden;
		this.setControls(EnumSet.of(Goal.Control.TARGET));
	}

	@Override
	public boolean canStart() {
		if (!ModConfig.INSTANCE.enableJadenBetrayal) return false;
		if (checkCooldown > 0) {
			checkCooldown--;
			return false;
		}
		checkCooldown = 20 * 60 * 20; // roughly once per in-game day at 20min days
		return jaden.getEntityWorld() instanceof ServerWorld;
	}

	@Override
	public void start() {
		ServerWorld world = (ServerWorld) jaden.getEntityWorld();
		PlayerEntity player = world.getClosestPlayer(jaden, 32.0);
		if (player == null) return;

		int armorTier = estimateArmorTier(player);
		int minTier = jaden.getTraits().minAcceptableArmorTier();

		if (armorTier < minTier) {
			float chance = jaden.getTraits().betrayalChanceWhenUndergeared();
			if (random.nextFloat() < chance) {
				betray(player);
			}
		}
	}

	/** 0=none,1=leather,2=iron,3=diamond,4=netherite -- matches CharacterTraits doc. */
	private int estimateArmorTier(PlayerEntity player) {
		int minTier = 4;
		boolean anyArmor = false;
		for (ItemStack stack : player.getArmorItems()) {
			if (stack.isEmpty()) continue;
			anyArmor = true;
			int tier = tierOf(stack);
			minTier = Math.min(minTier, tier);
		}
		return anyArmor ? minTier : 0;
	}

	private int tierOf(ItemStack stack) {
		String path = stack.getItem().toString().toLowerCase();
		// NOTE: string-matching item ids is a placeholder; a proper implementation
		// should check the ArmorMaterial registry key directly once available.
		if (path.contains("netherite")) return 4;
		if (path.contains("diamond")) return 3;
		if (path.contains("iron")) return 2;
		if (path.contains("leather") || path.contains("chainmail") || path.contains("golden")) return 1;
		return 0;
	}

	private void betray(PlayerEntity player) {
		String line = jaden.requestDialogue(player, DialogueProvider.Trigger.BETRAYAL);
		// TODO: broadcast `line`; then flip faction allegiance and set hostility.
		jaden.setFactionId(null);
		jaden.adjustRelationship(player.getUuid(), -200);
		jaden.setTarget(player);
	}

	/** Call from a gift/trade interaction handler when the player hands Jaden high-tier gear. */
	public void notifyGearGift(PlayerEntity player, int tier) {
		if (tier >= 3) {
			String line = jaden.requestDialogue(player, DialogueProvider.Trigger.GEAR_APPRECIATION);
			// TODO: broadcast `line`.
			jaden.adjustRelationship(player.getUuid(), 15 * tier);
		}
	}

	@Override
	public boolean shouldContinue() {
		return false;
	}
}
