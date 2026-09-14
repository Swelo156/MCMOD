package com.unstablecompanions.faction;

/**
 * Ranks available inside any faction, player-founded or villain-led.
 * Ordinal order matters: higher ordinal = more authority.
 */
public enum FactionRank {
	RECRUIT,
	MEMBER,
	TRUSTED,
	RIGHT_HAND, // offered by leaders at high friendship, see design doc "General" section
	LEADER
}
