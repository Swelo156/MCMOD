package com.unstablecompanions.relationship;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

import com.unstablecompanions.config.ModConfig;

/**
 * Owns the relationship graph for a single companion: how it feels about
 * every other UUID it has met (players and other companions alike).
 *
 * One instance lives on each {@code CompanionEntity}. World-level/faction-wide
 * queries (e.g. "who are FlameFrags' rivals' friends") are composed by
 * {@code FactionManager} pulling from each companion's manager rather than
 * duplicating a second global graph -- keeps a single source of truth.
 */
public class RelationshipManager {

	private final Map<UUID, RelationshipData> relationships = new HashMap<>();

	public RelationshipData getOrCreate(UUID target) {
		return relationships.computeIfAbsent(target, id -> new RelationshipData());
	}

	public RelationshipType classify(UUID target) {
		RelationshipData data = relationships.get(target);
		if (data == null) return RelationshipType.NEUTRAL;
		return data.classify(ModConfig.INSTANCE.friendThreshold, ModConfig.INSTANCE.enemyThreshold);
	}

	public void adjustScore(UUID target, int delta, long currentTick) {
		getOrCreate(target).addScore(delta, currentTick);
	}

	/**
	 * Call periodically (e.g. once per second of game time) for each known
	 * relationship. If {@code target} is a FRIEND and {@code target} is
	 * simultaneously an ENEMY/RIVAL of someone this companion also considers
	 * a FRIEND, tension accrues -- this is the "friends with my rival" drama
	 * hook described in the design doc. The caller supplies the rival set
	 * because computing it requires cross-referencing other companions'
	 * graphs, which this class intentionally does not reach into directly.
	 */
	public void tickTension(UUID target, boolean isConflictingFriendship) {
		RelationshipData data = relationships.get(target);
		if (data == null) return;
		if (isConflictingFriendship) {
			data.addTension(ModConfig.INSTANCE.tensionAccrualRate);
		} else {
			// Slow natural cooldown when nothing is actively stoking the drama.
			data.addTension(-ModConfig.INSTANCE.tensionAccrualRate * 0.5f);
		}
	}

	public boolean isPastConfrontThreshold(UUID target) {
		RelationshipData data = relationships.get(target);
		return data != null && data.getTension() >= ModConfig.INSTANCE.tensionConfrontThreshold;
	}

	public Map<UUID, RelationshipData> asMap() {
		return relationships;
	}

	// -----------------------------------------------------------------
	// Persistence
	// -----------------------------------------------------------------

	public NbtCompound writeNbt() {
		NbtCompound root = new NbtCompound();
		NbtList list = new NbtList();
		for (Map.Entry<UUID, RelationshipData> entry : relationships.entrySet()) {
			NbtCompound entryNbt = entry.getValue().writeNbt();
			entryNbt.putUuid("Target", entry.getKey());
			list.add(entryNbt);
		}
		root.put("Entries", list);
		return root;
	}

	public void readNbt(NbtCompound root) {
		relationships.clear();
		NbtList list = root.getListOrEmpty("Entries");
		for (int i = 0; i < list.size(); i++) {
			NbtCompound entryNbt = list.getCompound(i);
			UUID target = entryNbt.getUuid("Target").orElse(null);
			if (target != null) {
				relationships.put(target, RelationshipData.readNbt(entryNbt));
			}
		}
	}
}
