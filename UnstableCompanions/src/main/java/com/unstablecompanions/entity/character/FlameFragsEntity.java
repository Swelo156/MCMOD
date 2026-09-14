package com.unstablecompanions.entity.character;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

import com.unstablecompanions.ai.goal.WorthinessConfrontGoal;
import com.unstablecompanions.entity.CompanionEntity;

/**
 * FlameFrags: tracks a worthiness meter toward the player. Confronts the
 * player if they're friends with one of FlameFrags' enemies; escalates to
 * hostility if the player then sides with that enemy. Never leeches
 * resources (enforced via CharacterTraits#leechesResources = false, checked
 * wherever loot-sharing/storage-access logic lives).
 */
public class FlameFragsEntity extends CompanionEntity {

	public FlameFragsEntity(EntityType<? extends FlameFragsEntity> type, World world) {
		super(type, world);
	}

	@Override
	public String getCharacterId() {
		return "flame_frags";
	}

	@Override
	protected void initCustomGoals() {
		this.goalSelector.add(2, new WorthinessConfrontGoal(this));
	}
}
