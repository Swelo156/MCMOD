package com.unstablecompanions.faction;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.server.world.ServerWorld;

import com.unstablecompanions.config.ModConfig;
import com.unstablecompanions.util.ModConstants;

/**
 * Server-side, world-scoped registry of every faction (player-founded and
 * villain-led). This is a plain manager object; wrap it in a
 * {@code PersistentState} (see persistence.CompanionPersistentState for the
 * pattern) to save/load with the world.
 *
 * Kept independent of any single companion so cross-faction queries
 * (standing, recruitment eligibility, "who are the enemies of my rivals")
 * don't require walking every loaded entity.
 */
public class FactionManager {

	private final Map<UUID, Faction> factions = new HashMap<>();

	public Faction createPlayerFaction(UUID player, String name) {
		Faction faction = new Faction(UUID.randomUUID(), name, player, Faction.Archetype.PLAYER_FOUNDED);
		factions.put(faction.getFactionId(), faction);
		ModConstants.LOGGER.info("Player {} founded faction '{}'", player, name);
		return faction;
	}

	/**
	 * Villain factions are typically created once during world/companion
	 * first-spawn rather than on demand -- call this from the relevant
	 * leader entity's initial goal setup (LettuceKEntity, AshswaggEntity,
	 * JamatoPEntity) the first time they're spawned in a world.
	 */
	public Faction getOrCreateVillainFaction(UUID leaderUuid, Faction.Archetype archetype, String name) {
		return findByLeader(leaderUuid).orElseGet(() -> {
			Faction faction = new Faction(UUID.randomUUID(), name, leaderUuid, archetype);
			factions.put(faction.getFactionId(), faction);
			return faction;
		});
	}

	public Optional<Faction> findByLeader(UUID leaderUuid) {
		return factions.values().stream().filter(f -> f.getLeaderUuid().equals(leaderUuid)).findFirst();
	}

	public Optional<Faction> get(UUID factionId) {
		return Optional.ofNullable(factions.get(factionId));
	}

	public Optional<Faction> findFactionOf(UUID memberUuid) {
		return factions.values().stream().filter(f -> f.isMember(memberUuid)).findFirst();
	}

	public Map<UUID, Faction> all() {
		return factions;
	}

	/**
	 * Runs faction-wide bookkeeping: recruitment attempts, standing decay,
	 * hit-squad/hunter-group formation for villain factions, etc. Intended
	 * to be invoked from the offline-simulation heartbeat
	 * (see OfflineSimulationManager) rather than every server tick.
	 */
	public void tick(ServerWorld world) {
		if (factions.size() > ModConfig.INSTANCE.factionScanSoftCap) {
			ModConstants.LOGGER.warn(
					"Faction count ({}) exceeds factionScanSoftCap ({}); skipping full scan this cycle",
					factions.size(), ModConfig.INSTANCE.factionScanSoftCap);
			return;
		}

		for (Faction faction : factions.values()) {
			switch (faction.getArchetype()) {
				case THE_LAW -> tickLawFaction(world, faction);
				case INVISIBLE_MAFIA -> tickMafiaFaction(world, faction);
				case NULL_PURGATORY -> tickPurgatoryFaction(world, faction);
				case PLAYER_FOUNDED -> tickPlayerFaction(world, faction);
			}
		}
	}

	private void tickLawFaction(ServerWorld world, Faction faction) {
		// TODO: LettuceK recruits nearby neutral/friendly companions toward "order";
		// higher recruit success against companions with low current faction loyalty.
	}

	private void tickMafiaFaction(ServerWorld world, Faction faction) {
		// TODO: Ashswagg periodically selects a target (rival faction member or
		// high-tension enemy of a member) and dispatches a small hit squad in secret.
	}

	private void tickPurgatoryFaction(ServerWorld world, Faction faction) {
		// TODO: JamatoP's hunter groups patrol toward known enemy locations (from
		// JumperWho's spy network, if the player has one!) and attempt captures,
		// routing captives toward a NULL/Purgatory prison structure.
	}

	private void tickPlayerFaction(ServerWorld world, Faction faction) {
		// TODO: shared base upkeep, rank-based permissions, recruit offers surfaced
		// to the player when a companion's friendship crosses the recruit threshold.
	}
}
