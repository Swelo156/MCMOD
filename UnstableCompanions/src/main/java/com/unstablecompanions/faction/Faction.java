package com.unstablecompanions.faction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

/**
 * A group of members (players and/or companions) sharing a rank structure,
 * a home base, and standing toward other factions. Both player-founded
 * factions (cult/gang/whatever the player names it) and villain-led
 * factions (The Law, Invisible Mafia, NULL/Purgatory) are represented by
 * this same class -- the only difference is who the leader UUID resolves to
 * and which {@code FactionArchetype} drives its recruitment/behavior goals.
 */
public class Faction {

	public enum Archetype {
		PLAYER_FOUNDED,
		THE_LAW,
		INVISIBLE_MAFIA,
		NULL_PURGATORY
	}

	private final UUID factionId;
	private String name;
	private UUID leaderUuid;
	private Archetype archetype;
	private BlockPos homeBase;

	private final Map<UUID, FactionRank> members = new HashMap<>();
	/** Standing toward other factions: positive = allied, negative = at war. */
	private final Map<UUID, Integer> factionStanding = new HashMap<>();

	public Faction(UUID factionId, String name, UUID leaderUuid, Archetype archetype) {
		this.factionId = factionId;
		this.name = name;
		this.leaderUuid = leaderUuid;
		this.archetype = archetype;
		this.members.put(leaderUuid, FactionRank.LEADER);
	}

	public UUID getFactionId() {
		return factionId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public UUID getLeaderUuid() {
		return leaderUuid;
	}

	public Archetype getArchetype() {
		return archetype;
	}

	public BlockPos getHomeBase() {
		return homeBase;
	}

	public void setHomeBase(BlockPos homeBase) {
		this.homeBase = homeBase;
	}

	public void addMember(UUID member, FactionRank rank) {
		members.put(member, rank);
	}

	public void removeMember(UUID member) {
		members.remove(member);
	}

	public FactionRank getRank(UUID member) {
		return members.getOrDefault(member, null);
	}

	public boolean isMember(UUID uuid) {
		return members.containsKey(uuid);
	}

	public Map<UUID, FactionRank> getMembers() {
		return members;
	}

	/**
	 * Promote a companion to right-hand. Per the design doc, leaders offer
	 * this at high friendship -- call from a goal/dialogue trigger once the
	 * relevant RelationshipManager score crosses a "very high friendship"
	 * threshold (e.g. 2x friendThreshold).
	 */
	public void promoteToRightHand(UUID member) {
		if (isMember(member)) {
			members.put(member, FactionRank.RIGHT_HAND);
		}
	}

	public int getStandingWith(UUID otherFactionId) {
		return factionStanding.getOrDefault(otherFactionId, 0);
	}

	public void adjustStandingWith(UUID otherFactionId, int delta) {
		factionStanding.merge(otherFactionId, delta, Integer::sum);
	}

	public NbtCompound writeNbt() {
		NbtCompound nbt = new NbtCompound();
		nbt.putUuid("FactionId", factionId);
		nbt.putString("Name", name);
		nbt.putUuid("Leader", leaderUuid);
		nbt.putString("Archetype", archetype.name());
		// Member/standing maps and home base position would be serialized here
		// via NbtList entries -- omitted for brevity in this skeleton.
		return nbt;
	}
}
