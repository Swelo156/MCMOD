package com.unstablecompanions.relationship;

/**
 * Coarse relationship state between two entities (companion-player or
 * companion-companion). Fine-grained standing is tracked as a numeric
 * score in {@link RelationshipData}; this enum is the derived "bucket"
 * used for AI decision-making.
 */
public enum RelationshipType {
	NEUTRAL,
	FRIEND,
	ENEMY,
	RIVAL, // stronger than ENEMY: an enemy specifically tied to drama/tension mechanics
	RIGHT_HAND // promoted friend, see FactionRank.RIGHT_HAND
}
