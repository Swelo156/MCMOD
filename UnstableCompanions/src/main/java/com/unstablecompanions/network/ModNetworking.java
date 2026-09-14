package com.unstablecompanions.network;

import java.util.Map;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import com.unstablecompanions.config.ModConfig;
import com.unstablecompanions.entity.CompanionEntity;
import com.unstablecompanions.entity.ModEntities;
import com.unstablecompanions.persistence.CompanionPersistentState;
import com.unstablecompanions.util.ModConstants;

/**
 * Registers the {@link SpawnCompanionPayload} channel and performs the
 * actual (server-authoritative) spawn when it arrives. Called once from
 * {@code UnstableCompanionsMod#onInitialize}.
 */
public final class ModNetworking {

	private ModNetworking() {}

	/** characterId -> EntityType lookup, built once from ModEntities. */
	private static final Map<String, EntityType<? extends CompanionEntity>> CHARACTER_TYPES = Map.of(
			"flame_frags", ModEntities.FLAME_FRAGS,
			"wemmbu", ModEntities.WEMMBU,
			"spoke_is_here", ModEntities.SPOKE_IS_HERE,
			"wifies", ModEntities.WIFIES,
			"jumper_who", ModEntities.JUMPER_WHO,
			"jaden_man", ModEntities.JADEN_MAN,
			"lettuce_k", ModEntities.LETTUCE_K,
			"ashswagg", ModEntities.ASHSWAGG,
			"jamato_p", ModEntities.JAMATO_P
	);

	public static void register() {
		PayloadTypeRegistry.playC2S().register(SpawnCompanionPayload.ID, SpawnCompanionPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SpawnCompanionPayload.ID, (payload, context) -> {
			// Networking callbacks run off the server thread in some Fabric API
			// versions -- always hop back onto it before touching world state.
			context.server().execute(() -> spawnRequested(context.player(), payload.characterId()));
		});
	}

	private static void spawnRequested(ServerPlayerEntity player, String characterId) {
		EntityType<? extends CompanionEntity> type = CHARACTER_TYPES.get(characterId);
		if (type == null) {
			ModConstants.LOGGER.warn("Received spawn request for unknown character id '{}'", characterId);
			return;
		}

		ServerWorld world = player.getServerWorld();

		if (countActiveCompanions(world) >= ModConfig.INSTANCE.maxActiveCompanions) {
			player.sendMessage(
					net.minecraft.text.Text.literal(
							"The world already has the maximum number of active companions (see config)."),
					false);
			return;
		}

		// Spawn a few blocks in front of the player, facing them.
		Vec3d spawnPos = player.getPos().add(player.getRotationVector().normalize().multiply(3.0));
		BlockPos blockPos = BlockPos.ofFloored(spawnPos);

		CompanionEntity companion = (CompanionEntity) type.spawn(world, blockPos, SpawnReason.SPAWN_ITEM_USE);
		if (companion == null) {
			ModConstants.LOGGER.warn("Failed to spawn companion '{}' for player {}", characterId, player.getName());
			return;
		}

		// Face the entity back toward the player and mark who recruited it.
		double dx = player.getX() - companion.getX();
		double dz = player.getZ() - companion.getZ();
		float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		companion.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, yaw, 0f);
		companion.setRecruitedBy(player.getUuid());

		// New spawns start as a friendly acquaintance of their summoner rather
		// than a stranger -- small positive nudge, well short of "Friend".
		companion.adjustRelationship(player.getUuid(), 10);

		CompanionPersistentState.get(world).markDirtyAndLog("companion spawned via UI");

		ModConstants.LOGGER.info("Spawned companion '{}' for player {}", characterId, player.getName().getString());
	}

	/**
	 * Cheap approximation of "active companion count" for the config cap:
	 * counts loaded companions in the same world. Distant/unloaded
	 * companions aren't ticking anyway (see OfflineSimulationManager) so
	 * they don't count against the active budget.
	 */
	private static int countActiveCompanions(ServerWorld world) {
		int count = 0;
		for (var entity : world.iterateEntities()) {
			if (entity instanceof CompanionEntity) count++;
		}
		return count;
	}
}
