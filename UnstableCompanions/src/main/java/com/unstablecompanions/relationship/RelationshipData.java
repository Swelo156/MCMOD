package com.unstablecompanions.relationship;

import net.minecraft.nbt.NbtCompound;

/**
 * Tracks the relationship from ONE entity's perspective toward another
 * entity (identified by UUID, stored on the owning {@code RelationshipManager}).
 * Relationships are directional at the data level (Wemmbu might view the
 * player very differently than the player's other companions do) but most
 * gameplay treats them as effectively mutual via helper methods.
 */
public class RelationshipData {

	private int score; // -100..100 roughly, but not hard clamped so drama can run hot
	private float tension; // 0..100, see ModConfig#tensionConfrontThreshold
	private long lastInteractionTick;

	public RelationshipData() {
		this(0, 0f, 0L);
	}

	public RelationshipData(int score, float tension, long lastInteractionTick) {
		this.score = score;
		this.tension = tension;
		this.lastInteractionTick = lastInteractionTick;
	}

	public int getScore() {
		return score;
	}

	public void addScore(int delta, long currentTick) {
		this.score += delta;
		this.lastInteractionTick = currentTick;
	}

	public float getTension() {
		return tension;
	}

	public void addTension(float delta) {
		this.tension = Math.max(0f, Math.min(100f, this.tension + delta));
	}

	public void resetTension() {
		this.tension = 0f;
	}

	public long getLastInteractionTick() {
		return lastInteractionTick;
	}

	public RelationshipType classify(int friendThreshold, int enemyThreshold) {
		if (score >= friendThreshold) return RelationshipType.FRIEND;
		if (score <= enemyThreshold) return RelationshipType.ENEMY;
		return RelationshipType.NEUTRAL;
	}

	public NbtCompound writeNbt() {
		NbtCompound nbt = new NbtCompound();
		nbt.putInt("Score", score);
		nbt.putFloat("Tension", tension);
		nbt.putLong("LastInteractionTick", lastInteractionTick);
		return nbt;
	}

	public static RelationshipData readNbt(NbtCompound nbt) {
		return new RelationshipData(
				nbt.getInt("Score", 0),
				nbt.getFloat("Tension", 0f),
				nbt.getLong("LastInteractionTick", 0L)
		);
	}
}
