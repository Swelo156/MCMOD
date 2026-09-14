package com.unstablecompanions.persistence;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import com.unstablecompanions.config.ModConfig;
import com.unstablecompanions.entity.CompanionEntity;
import com.unstablecompanions.util.ModConstants;

/**
 * The core performance mechanism for "the world feels alive even when
 * you're not looking": companions far from any player don't run their full
 * goal AI every tick. Instead, this manager periodically pulls a small,
 * round-robin batch of distant companions and applies a coarse simulation
 * step (gear-up rolls, faction bookkeeping nudges, relationship decay) far
 * less frequently and far more cheaply than full AI ticking.
 *
 * Companions within {@code activeSimulationRadius} of any player are left
 * alone entirely -- normal vanilla-style entity ticking and this mod's
 * regular Goals handle them; this manager only ever touches the distant set.
 *
 * Register {@link #onServerTick} with
 * {@code ServerTickEvents.END_SERVER_TICK} from Fabric API in your mod
 * initializer.
 */
public class OfflineSimulationManager {

	private final Deque<CompanionEntity> pendingBatch = new ArrayDeque<>();
	private int ticksUntilNextInterval = 0;

	public void onServerTick(MinecraftServer server) {
		if (!ModConfig.INSTANCE.enableOfflineProgression) return;

		if (ticksUntilNextInterval > 0) {
			ticksUntilNextInterval--;
			return;
		}
		ticksUntilNextInterval = ModConfig.INSTANCE.offlineSimulationIntervalTicks;

		for (ServerWorld world : server.getWorlds()) {
			refillBatchIfNeeded(world);
			processBatch(world);
		}
	}

	private void refillBatchIfNeeded(ServerWorld world) {
		if (!pendingBatch.isEmpty()) return;

		List<CompanionEntity> distant = new ArrayList<>();
		int radiusSq = ModConfig.INSTANCE.activeSimulationRadius * ModConfig.INSTANCE.activeSimulationRadius;

		// NOTE: iterating world.iterateEntities() touches only currently-loaded
		// entities. Companions in unloaded chunks are, by definition, not ticking
		// at all right now (standard MC chunk behavior) -- true "always simulate
		// even when the chunk is unloaded" progression would require a separate
		// lightweight data model (e.g. a serialized "ghost companion" record kept
		// in CompanionPersistentState) rather than relying on the live entity.
		// That's the recommended next step beyond this skeleton if fully unloaded
		// offline progression is required; left as a clearly-marked extension point.
		for (var entity : world.iterateEntities()) {
			if (!(entity instanceof CompanionEntity companion)) continue;
			if (isNearAnyPlayer(world, companion, radiusSq)) continue;
			distant.add(companion);
		}

		pendingBatch.addAll(distant);
	}

	private boolean isNearAnyPlayer(ServerWorld world, CompanionEntity companion, int radiusSq) {
		Vec3d pos = companion.getPos();
		for (PlayerEntity player : world.getPlayers()) {
			if (player.squaredDistanceTo(pos.x, pos.y, pos.z) <= radiusSq) {
				return true;
			}
		}
		return false;
	}

	private void processBatch(ServerWorld world) {
		int budget = ModConfig.INSTANCE.offlineSimulationBatchSize;
		while (budget-- > 0 && !pendingBatch.isEmpty()) {
			CompanionEntity companion = pendingBatch.poll();
			if (companion == null || !companion.isAlive()) continue;
			simulateStep(world, companion);
		}
	}

	/**
	 * A single coarse "turn" for a distant companion. Intentionally shallow:
	 * no pathfinding, no combat resolution tick-by-tick -- just discrete state
	 * transitions appropriate for something happening "off-screen".
	 */
	private void simulateStep(ServerWorld world, CompanionEntity companion) {
		// TODO: gear-up roll -- chance to equip better gear found from a simple
		// loot-table roll appropriate to the companion's archetype.
		// TODO: faction bookkeeping nudge -- e.g. contribute to a recruitment or
		// hit-squad counter tracked on Faction, resolved fully in FactionManager#tick.
		// TODO: relationship decay -- long-untouched relationships drift toward
		// neutral; call companion.getRelationshipManager() and decay stale entries
		// (see RelationshipData#getLastInteractionTick).
		// TODO: build progress -- Wifies/villain leaders make incremental progress
		// on structures without needing to be near a player.

		ModConstants.LOGGER.debug(
				"Offline-simulated a step for {} ({})", companion.getCharacterId(), companion.getUuid());
	}
}
