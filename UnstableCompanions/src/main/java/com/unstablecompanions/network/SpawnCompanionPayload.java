package com.unstablecompanions.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import com.unstablecompanions.util.ModConstants;

/**
 * Sent client -> server when the person picks a character in
 * {@code CompanionSpawnScreen}. Carries just the character id
 * (e.g. "flame_frags") -- the server looks up the matching EntityType and
 * does the actual spawning, since spawning must never be trusted to the
 * client (position, permissions, world state all need server-side checks).
 */
public record SpawnCompanionPayload(String characterId) implements CustomPayload {

	public static final CustomPayload.Id<SpawnCompanionPayload> ID =
			new CustomPayload.Id<>(Identifier.of(ModConstants.MOD_ID, "spawn_companion"));

	public static final PacketCodec<RegistryByteBuf, SpawnCompanionPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, SpawnCompanionPayload::characterId,
			SpawnCompanionPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
