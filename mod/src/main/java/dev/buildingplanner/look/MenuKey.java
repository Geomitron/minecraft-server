package dev.buildingplanner.look;

import dev.buildingplanner.ClientConfig;
import java.io.IOException;
import java.util.List;

/**
 * The rules for the key Building Planner moves once on a player's computer: M opens Freecam's menu, and no longer
 * Litematica's. Freecam's key "Config GUI" takes M while it has no key, and Litematica's hotkey for its menu takes no
 * key while it is still "M". A key the player bound or moved stays as it is, and the player's {@link ClientConfig}
 * holds the group's record once both are done, so a later change of the player's own is never overwritten.
 */
public final class MenuKey {
	/** The name of Freecam's key "Config GUI", which has no key by default. */
	public static final String FREECAM_KEY = "key.freecam.config.open";
	/** The id of Freecam's mod, whose key this group needs. */
	public static final String FREECAM_MOD = "freecam";

	/** The record of the menu key group. */
	static final String APPLIED = "menu-key-applied";

	private MenuKey() {
	}

	/** Whether the group applies on this computer: it needs both Freecam and Litematica loaded. */
	public static boolean applies(boolean freecamLoaded, boolean litematicaLoaded) {
		return freecamLoaded && litematicaLoaded;
	}

	/** Whether Freecam's key takes M: only while the player has given it no key. */
	public static boolean bindFreecam(boolean freecamKeyUnbound) {
		return freecamKeyUnbound;
	}

	/**
	 * Whether Litematica's menu hotkey, with these key codes, is cleared: only while it is "M" alone. {@code keyM} is
	 * Minecraft's code for M ({@code InputConstants.KEY_M}, which is not GLFW's 77 in Minecraft 26.3), the code MaLiLib
	 * stores for the key.
	 */
	public static boolean clearLitematica(List<Integer> hotkeyKeys, int keyM) {
		return hotkeyKeys.equals(List.of(keyM));
	}

	/** Whether the player's config records that the menu key group was set. */
	public static boolean applied(ClientConfig config) {
		return Boolean.parseBoolean(config.get(APPLIED));
	}

	/** Records in the player's config that the menu key group was set. */
	public static void markApplied(ClientConfig config) throws IOException {
		config.set(APPLIED, "true");
	}
}
