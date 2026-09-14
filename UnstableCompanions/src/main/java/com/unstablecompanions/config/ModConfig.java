package com.unstablecompanions.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

import com.unstablecompanions.util.ModConstants;

/**
 * Central performance & gameplay tuning knobs.
 *
 * Kept as a plain POJO + Gson so it has zero extra dependencies. Swap this
 * for Cloth Config / YetAnotherConfigLib if you want an in-game GUI later;
 * the field layout here is deliberately screen-friendly (grouped, primitive
 * types, sane defaults) to make that migration easy.
 */
public class ModConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("unstablecompanions.json");

	public static ModConfig INSTANCE = new ModConfig();

	// ---------------------------------------------------------------
	// Performance
	// ---------------------------------------------------------------

	/** Hard cap on companions with FULL AI/goal ticking loaded at once, per world. */
	public int maxActiveCompanions = 40;

	/** Companions further than this (blocks, chunk-based) from any player fall back to lightweight simulation. */
	public int activeSimulationRadius = 128;

	/** Distant companions are simulated on a coarse timer instead of every tick. Value = ticks between updates. */
    public int offlineSimulationIntervalTicks = 200; // 10s

	/** How many companions get an offline-simulation "turn" per interval tick, spread across ticks to avoid spikes. */
	public int offlineSimulationBatchSize = 10;

	/** Disable spy-network / faction-wide bookkeeping scans above this companion count (perf safety valve). */
	public int factionScanSoftCap = 300;

	/** Master switch: fully disables offline progression simulation (companions freeze when unloaded). */
	public boolean enableOfflineProgression = true;

	// ---------------------------------------------------------------
	// Relationships / Drama
	// ---------------------------------------------------------------

	/** Friendship points required to shift Neutral -> Friend. */
	public int friendThreshold = 100;

	/** Enemy points required to shift Neutral -> Enemy. */
	public int enemyThreshold = -100;

	/** How much "tension" a companion accumulates per tick while friends with one of their rivals' enemies. */
	public float tensionAccrualRate = 0.05f;

	/** Tension level at which a companion may confront the player / take drastic action. */
	public float tensionConfrontThreshold = 75.0f;

	// ---------------------------------------------------------------
	// Character-specific toggles (fine-grained on/off, not full trait tuning
	// -- see CharacterTraits for numeric per-character tuning)
	// ---------------------------------------------------------------

	public boolean enableFlameFragsWorthinessMeter = true;
	public boolean enableWemmbuRevengeArc = true;
	public boolean enableSpokeRideOrDie = true;
	public boolean enableWifiesPrisons = true;
	public boolean enableJumperSpyNetwork = true;
	public boolean enableJadenBetrayal = true;
	public boolean enableVillainFactions = true;

	// ---------------------------------------------------------------
	// Load / Save
	// ---------------------------------------------------------------

	public static void load() {
		if (!Files.exists(PATH)) {
			save();
			return;
		}
		try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
			ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
			if (loaded != null) {
				INSTANCE = loaded;
			}
		} catch (IOException e) {
			ModConstants.LOGGER.error("Failed to load UnstableCompanions config, using defaults", e);
		}
	}

	public static void save() {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(INSTANCE, writer);
			}
		} catch (IOException e) {
			ModConstants.LOGGER.error("Failed to save UnstableCompanions config", e);
		}
	}
}
