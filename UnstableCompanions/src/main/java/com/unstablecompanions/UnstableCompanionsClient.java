package com.unstablecompanions;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.TypedActionResult;

import com.unstablecompanions.client.gui.CompanionSpawnScreen;
import com.unstablecompanions.item.ModItems;
import com.unstablecompanions.util.ModConstants;

/**
 * Client-only setup: the spawn-menu item interaction, entity renderers,
 * particle registration, HUD overlays (e.g. a future worthiness-meter or
 * faction-standing HUD), keybinds.
 *
 * Rendering is stubbed with a placeholder humanoid renderer per character;
 * swap in real models/textures under
 * assets/unstablecompanions/textures/entity/<character>.png and a proper
 * EntityModel once art exists.
 */
public class UnstableCompanionsClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ModConstants.LOGGER.info("Initializing UnstableCompanions client");

		// Registered only here (client entrypoint), so this listener never
		// exists on a dedicated server -- keeps every client-only class
		// (MinecraftClient, Screen) out of code paths the server could ever
		// execute. UseItemCallback still fires on the server for the same
		// interaction, but since the listener itself isn't registered there,
		// nothing client-specific ever runs server-side.
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (!world.isClient()) {
				return TypedActionResult.pass(player.getStackInHand(hand));
			}
			if (!player.getStackInHand(hand).isOf(ModItems.COMPANION_SPAWNER)) {
				return TypedActionResult.pass(player.getStackInHand(hand));
			}
			MinecraftClient.getInstance().setScreen(new CompanionSpawnScreen());
			return TypedActionResult.success(player.getStackInHand(hand));
		});

		// TODO: replace with real per-character models/textures. Left as a single
		// TODO marker rather than 9 near-identical renderer registrations to avoid
		// implying these are final -- see assets/unstablecompanions/textures/entity/.
		//
		// Example once a texture exists:
		// EntityRendererRegistry.register(ModEntities.FLAME_FRAGS,
		//     context -> new PlayerLikeCompanionRenderer(context, "flame_frags"));
	}
}
