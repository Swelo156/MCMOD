package com.unstablecompanions.entity;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import com.unstablecompanions.entity.character.AshswaggEntity;
import com.unstablecompanions.entity.character.FlameFragsEntity;
import com.unstablecompanions.entity.character.JadenManEntity;
import com.unstablecompanions.entity.character.JamatoPEntity;
import com.unstablecompanions.entity.character.JumperWhoEntity;
import com.unstablecompanions.entity.character.LettuceKEntity;
import com.unstablecompanions.entity.character.SpokeIsHereEntity;
import com.unstablecompanions.entity.character.WemmbuEntity;
import com.unstablecompanions.entity.character.WifiesEntity;
import com.unstablecompanions.util.ModConstants;

/**
 * Registers one {@link EntityType} per starting character. Each character is
 * its own entity type (rather than one generic "companion" type + a data
 * field) so that they can have distinct textures/models via standard
 * resource-pack conventions, distinct default attributes, and distinct
 * spawn eggs -- while still sharing all behavior through {@link CompanionEntity}.
 *
 * Since Minecraft 1.21.2, EntityType.Builder#build requires the RegistryKey
 * to be passed directly (the old no-arg build() was removed) -- otherwise
 * registration silently produces an entity type with no id association.
 */
public final class ModEntities {

	private ModEntities() {}

	public static final EntityType<FlameFragsEntity> FLAME_FRAGS = register(
			"flame_frags", FlameFragsEntity::new, EntityDimensions.fixed(0.6f, 1.95f));

	public static final EntityType<WemmbuEntity> WEMMBU = register(
			"wemmbu", WemmbuEntity::new, EntityDimensions.fixed(0.6f, 1.95f));

	public static final EntityType<SpokeIsHereEntity> SPOKE_IS_HERE = register(
			"spoke_is_here", SpokeIsHereEntity::new, EntityDimensions.fixed(0.6f, 1.95f));

	public static final EntityType<WifiesEntity> WIFIES = register(
			"wifies", WifiesEntity::new, EntityDimensions.fixed(0.6f, 1.95f));

	public static final EntityType<JumperWhoEntity> JUMPER_WHO = register(
			"jumper_who", JumperWhoEntity::new, EntityDimensions.fixed(0.6f, 1.95f));

	public static final EntityType<JadenManEntity> JADEN_MAN = register(
			"jaden_man", JadenManEntity::new, EntityDimensions.fixed(0.6f, 1.95f));

	public static final EntityType<LettuceKEntity> LETTUCE_K = register(
			"lettuce_k", LettuceKEntity::new, EntityDimensions.fixed(0.6f, 1.95f));

	public static final EntityType<AshswaggEntity> ASHSWAGG = register(
			"ashswagg", AshswaggEntity::new, EntityDimensions.fixed(0.6f, 1.95f));

	public static final EntityType<JamatoPEntity> JAMATO_P = register(
			"jamato_p", JamatoPEntity::new, EntityDimensions.fixed(0.6f, 1.95f));

	private static <T extends CompanionEntity> EntityType<T> register(
			String path, EntityType.EntityFactory<T> factory, EntityDimensions dimensions) {
		RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(ModConstants.MOD_ID, path));
		EntityType<T> type = EntityType.Builder.create(factory, SpawnGroup.CREATURE)
				.dimensions(dimensions)
				.maxTrackingRange(48)
				.trackingTickInterval(2)
				.build(key);
		return Registry.register(Registries.ENTITY_TYPE, key, type);
	}

	public static void init() {
		// Referencing the class triggers static init of all the fields above.
		ModConstants.LOGGER.info("Registered {} companion entity types", 9);
	}
}
