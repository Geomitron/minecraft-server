package dev.buildingplanner.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/**
 * A key mapping of the driver's own, bound to Page Up as the copy's "Nudge the copy up" is, which counts the Page Up
 * presses that reach Minecraft's key mappings (step10.sh); Minecraft gives a press to every key mapping bound to its
 * key. It is in a class of its own, with no reference to Litematica, so it also counts on a client without it, and under
 * a heading of its own in Controls, so the "Building Planner" heading shows only the add-on's keys.
 */
final class PageProbe {
	private static final KeyMapping KEY = new KeyMapping("key.buildingplanner_clienttest.page_up", InputConstants.KEY_PAGEUP,
		KeyMapping.Category.register(Identifier.fromNamespaceAndPath("buildingplanner_clienttest", "driver")));

	private PageProbe() {
	}

	/** Registers the key mapping; call while the client starts, before the game reads its options. */
	static void register() {
		KeyMappingHelper.registerKeyMapping(KEY);
	}

	/** Takes the key mapping's key away, as a player would in Controls. */
	static void unbind() {
		KEY.setKey(InputConstants.UNKNOWN);
		KeyMapping.resetMapping();
	}

	/** How many presses of the key reached the key mapping since the last call. */
	static int take() {
		int presses = 0;
		while (KEY.consumeClick()) {
			presses++;
		}
		return presses;
	}
}
