package com.unstablecompanions.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.util.Identifier;

/**
 * Shared constants used across the mod. Keeping these in one place avoids
 * typo'd mod-id strings scattered through registries.
 */
public final class ModConstants {

	private ModConstants() {}

	public static final String MOD_ID = "unstablecompanions";
	public static final Logger LOGGER = LoggerFactory.getLogger("UnstableCompanions");

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
