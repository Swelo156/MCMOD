package com.unstablecompanions.persistence;

import com.mojang.serialization.Codec;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import com.unstablecompanions.faction.FactionManager;
import com.unstablecompanions.util.ModConstants;

/**
 * World-attached save data for anything that shouldn't live purely on an
 * entity (which can be lost if a chunk misbehaves, and can't easily answer
 * "list all factions" without iterating every loaded entity).
 *
 * Holds the {@link FactionManager}. Individual companions still persist
 * their own relationship graphs and identity in entity NBT (see
 * CompanionEntity#writeCustomData) -- this class is specifically for data
 * that must survive independent of any single entity's chunk being loaded.
 *
 * Minecraft 1.21.5+ moved PersistentState to a Codec-based save format
 * (PersistentStateType) instead of the older writeNbt(NbtCompound) override
 * and the removed PersistentState.Type helper. We bridge to that by keeping
 * our own manual NBT read/write (same shape as before) and wrapping it with
 * NbtCompound.CODEC.xmap(...), so the rest of the mod doesn't need to learn
 * full Codec composition just for this.
 */
public class CompanionPersistentState extends PersistentState {

	private static final String KEY = "unstablecompanions_state";

	private final FactionManager factionManager = new FactionManager();

	public FactionManager getFactionManager() {
		return factionManager;
	}

	private NbtCompound toNbt() {
		NbtCompound nbt = new NbtCompound();
		NbtCompound factionsNbt = new NbtCompound();
		// TODO: serialize factionManager.all() fully (members, standings, home base)
		// once Faction's NBT round-trip is fleshed out beyond the partial writeNbt()
		// stub currently on that class. Left as a deliberate no-op for now rather
		// than a half-correct serialization that would silently lose data.
		nbt.put("Factions", factionsNbt);
		return nbt;
	}

	private static CompanionPersistentState fromNbt(NbtCompound nbt) {
		CompanionPersistentState state = new CompanionPersistentState();
		// TODO: deserialize factions back into state.factionManager once the
		// corresponding write side above is implemented.
		return state;
	}

	private static final Codec<CompanionPersistentState> CODEC =
			NbtCompound.CODEC.xmap(CompanionPersistentState::fromNbt, CompanionPersistentState::toNbt);

	private static final PersistentStateType<CompanionPersistentState> TYPE =
			new PersistentStateType<>(KEY, CompanionPersistentState::new, CODEC, null);

	public static CompanionPersistentState get(ServerWorld world) {
		return world.getPersistentStateManager().getOrCreate(TYPE);
	}

	public void markDirtyAndLog(String reason) {
		this.markDirty();
		ModConstants.LOGGER.debug("CompanionPersistentState marked dirty: {}", reason);
	}
}
