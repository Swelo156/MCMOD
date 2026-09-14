package com.unstablecompanions.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Data-only "personality dials" for each starting character.
 *
 * This is deliberately a plain record keyed in a map rather than baked into
 * enum constants, so it can later be replaced by a JSON loader
 * (data/unstablecompanions/traits/<id>.json) without touching entity code.
 * Entities pull their tuning via {@link #get(String)} using their registry id.
 */
public final class CharacterTraits {

	private CharacterTraits() {}

	/**
	 * @param baseWorthiness         FlameFrags: starting worthiness meter value (0-100).
	 * @param worthinessDecayPerDay  FlameFrags: passive decay if player neglects the relationship.
	 * @param leechesResources       Whether this companion is allowed to take resources from shared storage.
	 * @param stabShotDamage         Wemmbu: damage dealt by a "stab shot".
	 * @param nukeShotRadius         Wemmbu: explosion radius of a "nuke shot".
	 * @param revengeDurationDays    Wemmbu: in-game days a revenge hunt stays active after Eggchan's death.
	 * @param rescueAggroRadius      SpokeIsHere: blocks within which a captured ally/player triggers a rescue.
	 * @param allInDamageBoost       SpokeIsHere: damage multiplier while in "all out" rescue mode.
	 * @param prisonEscapeDifficulty Wifies: 0-1, chance an escape attempt fails.
	 * @param betrayalChancePerMonth Wifies: baseline chance of betrayal even while "friend", per in-game month.
	 * @param spyNetworkRadius       JumperWho: blocks of intel-gathering range for the spy network.
	 * @param recklessTolerance      JumperWho: number of reckless-revenge choices tolerated before leaving.
	 * @param minAcceptableArmorTier Jaden_MAN: 0=none,1=leather,2=iron,3=diamond,4=netherite. Below this = risk.
	 * @param betrayalChanceWhenUndergeared Jaden_MAN: chance/day of betrayal while below minAcceptableArmorTier.
	 * @param factionName            Villain leaders: the faction this character founds.
	 * @param leaderStrength         Villain leaders: rough power scalar used for recruitment/hit-squad sizing.
	 */
	public record Traits(
			int baseWorthiness,
			int worthinessDecayPerDay,
			boolean leechesResources,
			float stabShotDamage,
			float nukeShotRadius,
			int revengeDurationDays,
			int rescueAggroRadius,
			float allInDamageBoost,
			float prisonEscapeDifficulty,
			float betrayalChancePerMonth,
			int spyNetworkRadius,
			int recklessTolerance,
			int minAcceptableArmorTier,
			float betrayalChanceWhenUndergeared,
			String factionName,
			float leaderStrength
	) {}

	private static final Map<String, Traits> REGISTRY = new HashMap<>();

	private static void register(String id, Traits traits) {
		REGISTRY.put(id, traits);
	}

	public static Traits get(String characterId) {
		Traits t = REGISTRY.get(characterId);
		return t != null ? t : DEFAULT;
	}

	public static final Traits DEFAULT =
			new Traits(50, 1, false, 0f, 0f, 0, 0, 1f, 0.5f, 0f, 0, 0, 0, 0f, null, 1f);

	static {
		register("flame_frags", new Traits(
				50, 2, false,
				0f, 0f, 0,
				0, 1f,
				0f, 0f,
				0, 0,
				0, 0f,
				null, 1f));

		register("wemmbu", new Traits(
				50, 1, false,
				8.0f, 4.0f, 30,
				0, 1f,
				0f, 0f,
				0, 0,
				0, 0f,
				null, 1.2f));

		register("spoke_is_here", new Traits(
				50, 1, false,
				0f, 0f, 0,
				32, 1.5f,
				0f, 0f,
				0, 0,
				0, 0f,
				null, 1.1f));

		register("wifies", new Traits(
				50, 1, false,
				0f, 0f, 0,
				0, 1f,
				0.9f, 0.05f,
				0, 0,
				0, 0f,
				null, 1.3f));

		register("jumper_who", new Traits(
				50, 1, false,
				0f, 0f, 0,
				0, 1f,
				0f, 0f,
				256, 3,
				0, 0f,
				null, 1.0f));

		register("jaden_man", new Traits(
				50, 1, true, // Jaden is the one character allowed to "want" resources/gear
				0f, 0f, 0,
				0, 1f,
				0f, 0f,
				0, 0,
				2, 0.15f,
				null, 0.9f));

		register("lettuce_k", new Traits(
				50, 1, false,
				0f, 0f, 0,
				0, 1f,
				0f, 0f,
				0, 0,
				0, 0f,
				"The Law", 1.5f));

		register("ashswagg", new Traits(
				50, 1, false,
				0f, 0f, 0,
				0, 1f,
				0f, 0f,
				0, 0,
				0, 0f,
				"Invisible Mafia", 1.4f));

		register("jamato_p", new Traits(
				50, 1, false,
				0f, 0f, 0,
				0, 1f,
				0f, 0f,
				0, 0,
				0, 0f,
				"NULL / Purgatory", 1.6f));
	}
}
