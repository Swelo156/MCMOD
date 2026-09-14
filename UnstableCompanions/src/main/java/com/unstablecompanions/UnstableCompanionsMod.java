package com.unstablecompanions;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.unstablecompanions.config.ModConfig;
import com.unstablecompanions.entity.CompanionEntity;
import com.unstablecompanions.entity.ModEntities;
import com.unstablecompanions.faction.FactionManager;
import com.unstablecompanions.item.ModItems;
import com.unstablecompanions.network.ModNetworking;
import com.unstablecompanions.persistence.CompanionPersistentState;
import com.unstablecompanions.persistence.OfflineSimulationManager;
import com.unstablecompanions.util.ModConstants;

/**
 * Common (client+server) entrypoint. All registration that must happen
 * identically on both sides lives here; anything render/GUI-only belongs in
 * {@code UnstableCompanionsClient}.
 */
public class UnstableCompanionsMod implements ModInitializer {

	private final OfflineSimulationManager offlineSimulationManager = new OfflineSimulationManager();

	@Override
	public void onInitialize() {
		ModConstants.LOGGER.info("Initializing UnstableCompanions");

		ModConfig.load();

		ModEntities.init();
		registerDefaultAttributes();

		ModItems.init();
		ModNetworking.register();

		// Drives the "world feels alive offline" simulation for companions far
		// from any player. See OfflineSimulationManager for the batching strategy.
		ServerTickEvents.END_SERVER_TICK.register(offlineSimulationManager::onServerTick);

		// Per-world FactionManager access pattern: pull it from
		// CompanionPersistentState.get(serverWorld) wherever a leader entity or
		// player-faction-founding command needs one, e.g.:
		//   FactionManager factions = CompanionPersistentState.get(serverWorld).getFactionManager();
		// Also drive FactionManager#tick from a slower heartbeat (e.g. once every
		// few offline-simulation intervals) rather than every server tick.
		ServerTickEvents.END_SERVER_TICK.register(this::tickFactions);

		ModConstants.LOGGER.info("UnstableCompanions initialized");
	}

	private int factionTickCooldown = 0;

	private void tickFactions(net.minecraft.server.MinecraftServer server) {
		if (factionTickCooldown-- > 0) return;
		factionTickCooldown = 20 * 30; // every 30s, independent of offline-sim interval

		for (var world : server.getWorlds()) {
			FactionManager factions = CompanionPersistentState.get(world).getFactionManager();
			factions.tick(world);
		}
	}

	private void registerDefaultAttributes() {
		FabricDefaultAttributeRegistry.register(ModEntities.FLAME_FRAGS, CompanionEntity.createCompanionAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.WEMMBU, CompanionEntity.createCompanionAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.SPOKE_IS_HERE, CompanionEntity.createCompanionAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.WIFIES, CompanionEntity.createCompanionAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.JUMPER_WHO, CompanionEntity.createCompanionAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.JADEN_MAN, CompanionEntity.createCompanionAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.LETTUCE_K, CompanionEntity.createCompanionAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.ASHSWAGG, CompanionEntity.createCompanionAttributes());
		FabricDefaultAttributeRegistry.register(ModEntities.JAMATO_P, CompanionEntity.createCompanionAttributes());
	}
}
