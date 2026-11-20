package dev.buildingplanner.look;

import dev.buildingplanner.ClientConfig;
import java.io.IOException;
import java.util.List;

/**
 * The rules for the keys Building Planner frees once on a player's computer: Page Up and Page Down no longer move
 * Litematica's layer ({@code layerNext} and {@code layerPrevious}), so they reach Minecraft's own key mappings, the
 * copy's nudge keys among them. Each hotkey takes no key while it is still Page Up or Page Down alone. A hotkey the
 * player moved stays as it is, and the player's {@link ClientConfig} holds the group's record once both are decided, so
 * a later change of the player's own is never overwritten.
 */
public final class LayerKeys {
	/** The record of the layer keys group. */
	static final String APPLIED = "layer-keys-applied";

	private LayerKeys() {
	}

	/** Whether the group applies on this computer: it needs Litematica loaded. */
	public static boolean applies(boolean litematicaLoaded) {
		return litematicaLoaded;
	}

	/**
	 * Whether a layer hotkey, with these key codes, is cleared: only while it is {@code defaultKey} alone, Page Up for
	 * {@code layerNext} and Page Down for {@code layerPrevious}. {@code defaultKey} is Minecraft's code for that key
	 * ({@code InputConstants.KEY_PAGEUP} or {@code KEY_PAGEDOWN}, which are not GLFW's in Minecraft 26.3), the code
	 * MaLiLib stores for the key.
	 */
	public static boolean clear(List<Integer> hotkeyKeys, int defaultKey) {
		return hotkeyKeys.equals(List.of(defaultKey));
	}

	/** Whether the player's config records that the layer keys group was set. */
	public static boolean applied(ClientConfig config) {
		return Boolean.parseBoolean(config.get(APPLIED));
	}

	/** Records in the player's config that the layer keys group was set. */
	public static void markApplied(ClientConfig config) throws IOException {
		config.set(APPLIED, "true");
	}
}
