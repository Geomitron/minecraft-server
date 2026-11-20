package dev.buildingplanner.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

/**
 * A key mapping of the driver's own, bound to Page Up as the copy's "Nudge the copy up" will be, which counts the Page
 * Up presses that reach Minecraft's key mappings (step10.sh). It is in a class of its own, with no reference to
 * Litematica, so it also counts on a client without it.
 */
final class PageProbe {
	private static final KeyMapping KEY = new KeyMapping("key.buildingplanner_clienttest.page_up", InputConstants.KEY_PAGEUP,
		BuildingPlannerClient.category());

	private PageProbe() {
	}

	/** Registers the key mapping; call while the client starts, before the game reads its options. */
	static void register() {
		KeyMappingHelper.registerKeyMapping(KEY);
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
