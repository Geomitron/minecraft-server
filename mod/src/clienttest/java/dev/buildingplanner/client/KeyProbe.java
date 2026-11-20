package dev.buildingplanner.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.buildingplanner.look.MenuKey;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import java.io.IOException;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * The driver's commands for Freecam's "Config GUI" key and Litematica's menu hotkey (step10.sh), in a class of their
 * own so the driver's other commands still load without Litematica.
 */
final class KeyProbe {
	private KeyProbe() {
	}

	/** Freecam's key as Minecraft saves it in options.txt, then Litematica's menu hotkey as it shows, or none. */
	static String menuKeys() {
		KeyMapping freecam = KeyMapping.get(MenuKey.FREECAM_KEY);
		String hotkey = Hotkeys.OPEN_GUI_MAIN_MENU.getKeybind().getKeysDisplayString();
		return "freecam=" + (freecam == null ? "missing" : freecam.isUnbound() ? "unbound" : freecam.saveString())
			+ " litematica=" + (hotkey.isEmpty() ? "none" : hotkey);
	}

	/**
	 * The player's own change: Freecam's key becomes K or M, and Litematica's menu hotkey is "M" again, both saved as
	 * their screens save them.
	 */
	static String setMenuKeys(String freecamKey) throws IOException {
		KeyMapping freecam = KeyMapping.get(MenuKey.FREECAM_KEY);
		int code = switch (freecamKey) {
			case "K" -> InputConstants.KEY_K;
			case "M" -> InputConstants.KEY_M;
			default -> throw new IllegalArgumentException(freecamKey);
		};
		freecam.setKey(InputConstants.Type.KEYBOARD.getOrCreate(code));
		KeyMapping.resetMapping();
		Minecraft.getInstance().options.save();
		IKeybind menu = Hotkeys.OPEN_GUI_MAIN_MENU.getKeybind();
		menu.clearKeys();
		menu.addKey(InputConstants.KEY_M);
		Configs.saveToFile();
		return menuKeys();
	}
}
