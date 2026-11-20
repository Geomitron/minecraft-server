package dev.buildingplanner.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.buildingplanner.look.MenuKey;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.event.InputEventHandler;
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

	/** Litematica's two layer hotkeys as they show, or none. */
	static String layerKeys() {
		return "next=" + display(Hotkeys.LAYER_NEXT.getKeybind()) + " previous=" + display(Hotkeys.LAYER_PREVIOUS.getKeybind());
	}

	private static String display(IKeybind hotkey) {
		String shown = hotkey.getKeysDisplayString();
		return shown.isEmpty() ? "none" : shown;
	}

	/**
	 * The player's own change, saved as Litematica's screen saves it: each layer hotkey becomes the key named, one of
	 * PAGE_UP, PAGE_DOWN, K or none.
	 */
	static String setLayerKeys(String next, String previous) throws IOException {
		set(Hotkeys.LAYER_NEXT.getKeybind(), next);
		set(Hotkeys.LAYER_PREVIOUS.getKeybind(), previous);
		// MaLiLib looks a press up in a table of which hotkeys each key can trigger, which its config screen rebuilds
		// after a change; without that, a key added to a hotkey wouldn't act.
		InputEventHandler.getKeybindManager().updateUsedKeys();
		Configs.saveToFile();
		return layerKeys();
	}

	private static void set(IKeybind hotkey, String key) {
		hotkey.clearKeys();
		switch (key) {
			case "PAGE_UP" -> hotkey.addKey(InputConstants.KEY_PAGEUP);
			case "PAGE_DOWN" -> hotkey.addKey(InputConstants.KEY_PAGEDOWN);
			case "K" -> hotkey.addKey(InputConstants.KEY_K);
			case "none" -> {
			}
			default -> throw new IllegalArgumentException(key);
		}
	}
}
