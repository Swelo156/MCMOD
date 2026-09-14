package com.unstablecompanions.item;

import java.util.function.Function;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import com.unstablecompanions.util.ModConstants;

/**
 * Registers mod items. "Stab shots" and "nuke shots" are Wemmbu's signature
 * ammo per the design doc -- modeled here as plain Items for now. If they
 * should be player-obtainable/craftable, add recipes under
 * data/unstablecompanions/recipe/; if they're Wemmbu-exclusive ammo never
 * meant to enter player inventories, these can instead become internal
 * markers consumed directly by RevengeHuntGoal without ever being a real
 * Item -- left as an Item for now since "obtain" is called out in the design
 * doc, implying the player can find/use them too.
 *
 * Also registers the Companion Spawner item -- the in-game entry point for
 * the spawn UI (see client/gui/CompanionSpawnScreen).
 *
 * Since Minecraft 1.21.2, every Item must carry a RegistryKey set on its
 * Item.Settings *before* construction (Registry.register alone is no longer
 * sufficient -- omitting this throws "Item id not set" at startup).
 */
public final class ModItems {

	private ModItems() {}

	public static final Item COMPANION_SPAWNER = register(
			"companion_spawner", Item::new, new Item.Settings().maxCount(1));

	public static final Item STAB_SHOT = register(
			"stab_shot", Item::new, new Item.Settings().maxCount(16));

	public static final Item NUKE_SHOT = register(
			"nuke_shot", Item::new, new Item.Settings().maxCount(4));

	private static Item register(String path, Function<Item.Settings, Item> factory, Item.Settings settings) {
		RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ModConstants.MOD_ID, path));
		Item item = factory.apply(settings.registryKey(key));
		return Registry.register(Registries.ITEM, key, item);
	}

	public static void init() {
		ModConstants.LOGGER.info("Registered UnstableCompanions items");
	}
}
