package com.unstablecompanions.ai.goal;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.Box;

import com.unstablecompanions.ai.dialogue.DialogueProvider;
import com.unstablecompanions.entity.CompanionEntity;
import com.unstablecompanions.faction.Faction;
import com.unstablecompanions.faction.FactionRank;
import com.unstablecompanions.relationship.RelationshipType;

/**
 * Shared recruitment behavior for any faction-leader companion (LettuceK,
 * Ashswagg, JamatoP, or a promoted player-faction right-hand). Scans for
 * nearby unaffiliated companions and, based on current relationship
 * standing, attempts to pull them into the leader's faction.
 *
 * Villain leaders use this to grow The Law / Invisible Mafia / NULL /
 * Purgatory over time; the same goal class backs "right-hand offers" the
 * design doc calls for at high friendship (see {@link #offerRightHand}).
 */
public class RecruitGoal extends Goal {

	private final CompanionEntity leader;
	private final Faction faction;
	private final float aggressiveness; // 0..1, chance-per-attempt modifier
	private int cooldownTicks;

	public RecruitGoal(CompanionEntity leader, Faction faction, float aggressiveness) {
		this.leader = leader;
		this.faction = faction;
		this.aggressiveness = aggressiveness;
		this.setControls(EnumSet.of(Goal.Control.MOVE));
	}

	@Override
	public boolean canStart() {
		if (cooldownTicks > 0) {
			cooldownTicks--;
			return false;
		}
		return true;
	}

	@Override
	public void start() {
		cooldownTicks = 20 * 60 * 5;
		Box box = leader.getBoundingBox().expand(24);
		List<CompanionEntity> nearby = leader.getEntityWorld().getEntitiesByClass(
				CompanionEntity.class, box, c -> c.getFactionId() == null && c != leader);

		for (CompanionEntity candidate : nearby) {
			RelationshipType relation = candidate.getRelationshipWith(leader.getUuid());
			if (relation == RelationshipType.ENEMY) continue; // won't recruit active enemies
			// Simple stub roll; a fuller implementation would weigh candidate
			// personality/traits (e.g. Jaden_MAN cares about gear, not ideology).
			if (Math.random() < aggressiveness) {
				recruit(candidate);
			}
		}
	}

	private void recruit(CompanionEntity candidate) {
		candidate.setFactionId(faction.getFactionId());
		candidate.setFactionRank(FactionRank.RECRUIT);
		faction.addMember(candidate.getUuid(), FactionRank.RECRUIT);
		// TODO: surface a recruitment pitch dialogue line to any nearby player via
		// DialogueProvider.Trigger.RECRUITMENT_PITCH once a target player context exists.
	}

	/**
	 * Call when a companion's friendship with {@code leader} crosses the
	 * "very high" threshold -- per design doc, leaders offer right-hand
	 * status at high friendship.
	 */
	public void offerRightHand(CompanionEntity candidate) {
		if (faction.isMember(candidate.getUuid())) {
			faction.promoteToRightHand(candidate.getUuid());
			candidate.setFactionRank(FactionRank.RIGHT_HAND);
			// TODO: fire DialogueProvider.Trigger.RIGHT_HAND_OFFER toward the relevant player.
		}
	}

	@Override
	public boolean shouldContinue() {
		return false;
	}
}
