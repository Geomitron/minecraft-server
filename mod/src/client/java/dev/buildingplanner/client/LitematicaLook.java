package dev.buildingplanner.client;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.ClientConfig;
import com.mojang.blaze3d.platform.InputConstants;
import dev.buildingplanner.look.LayerKeys;
import dev.buildingplanner.look.LookDefaults;
import dev.buildingplanner.look.MenuKey;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import java.io.IOException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Sets how Litematica draws ghost blocks and its tool item, turns its drawing of area selections off, and gives M to
 * Freecam's menu instead of Litematica's, frees Page Up and Page Down from its layer hotkeys, and stops its status
 * panel showing itself, once each on a player's computer, whether or not they join a server with Building Planner:
 * translucent blocks at 0.8 opacity and no tool item, then no area selections drawn, then M, then Page Up and Page
 * Down, then no status panel, for each option the player hasn't changed from its default. Each group saves the config it changed, then records that in
 * the player's {@link ClientConfig} under a record of its own, so a later change of the player's own stays and a
 * player who ran an earlier version gets each group it lacks.
 */
final class LitematicaLook {
	private LitematicaLook() {
	}

	/** Call once Litematica has loaded its config, once a game run. */
	static void setOnce(ClientConfig config) {
		if (!LookDefaults.lookApplied(config)) {
			try {
				setLook();
				Configs.saveToFile();
				LookDefaults.markLookApplied(config);
			} catch (IOException | RuntimeException e) {
				BuildingPlanner.LOGGER.warn("Couldn't set Litematica's ghost block look and tool item", e);
			}
		}
		if (!LookDefaults.areaSelectionApplied(config)) {
			try {
				setAreaSelection();
				Configs.saveToFile();
				LookDefaults.markAreaSelectionApplied(config);
			} catch (IOException | RuntimeException e) {
				BuildingPlanner.LOGGER.warn("Couldn't turn Litematica's drawing of area selections off", e);
			}
		}
		setMenuKey(config);
		setLayerKeys(config);
		try {
			LookDefaults.setStatusPanelOnce(config, () -> {
				setStatusPanel();
				Configs.saveToFile();
			});
		} catch (IOException | RuntimeException e) {
			BuildingPlanner.LOGGER.warn("Couldn't turn off Litematica's status panel showing itself", e);
		}
	}

	/**
	 * Takes Page Up from Litematica's {@code layerNext} and Page Down from its {@code layerPrevious}, each while it is
	 * still that key alone, then saves Litematica's config and records the group, once Litematica is loaded.
	 */
	private static void setLayerKeys(ClientConfig config) {
		if (LayerKeys.applied(config) || !LayerKeys.applies(FabricLoader.getInstance().isModLoaded("litematica"))) {
			return;
		}
		try {
			IKeybind next = Hotkeys.LAYER_NEXT.getKeybind();
			if (LayerKeys.clear(next.getKeys(), InputConstants.KEY_PAGEUP)) {
				next.clearKeys();
			}
			IKeybind previous = Hotkeys.LAYER_PREVIOUS.getKeybind();
			if (LayerKeys.clear(previous.getKeys(), InputConstants.KEY_PAGEDOWN)) {
				previous.clearKeys();
			}
			Configs.saveToFile();
			LayerKeys.markApplied(config);
		} catch (IOException | RuntimeException e) {
			BuildingPlanner.LOGGER.warn("Couldn't free Page Up and Page Down from Litematica's layer hotkeys", e);
		}
	}

	/**
	 * Gives Freecam's menu key to M while it has no key, and takes M from Litematica's menu while its hotkey is still
	 * "M", then saves Litematica's config and Minecraft's options and records the group, once Freecam and Litematica
	 * are both loaded.
	 */
	private static void setMenuKey(ClientConfig config) {
		FabricLoader loader = FabricLoader.getInstance();
		if (MenuKey.applied(config)
			|| !MenuKey.applies(loader.isModLoaded(MenuKey.FREECAM_MOD), loader.isModLoaded("litematica"))) {
			return;
		}
		try {
			KeyMapping freecam = KeyMapping.get(MenuKey.FREECAM_KEY);
			if (freecam == null) {
				BuildingPlanner.LOGGER.warn("Freecam's key {} wasn't found, so M isn't set to open its menu",
					MenuKey.FREECAM_KEY);
				return;
			}
			if (MenuKey.bindFreecam(freecam.isUnbound())) {
				freecam.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_M));
				KeyMapping.resetMapping();
				Minecraft.getInstance().options.save();
			}
			IKeybind menu = Hotkeys.OPEN_GUI_MAIN_MENU.getKeybind();
			if (MenuKey.clearLitematica(menu.getKeys(), InputConstants.KEY_M)) {
				menu.clearKeys();
			}
			Configs.saveToFile();
			MenuKey.markApplied(config);
		} catch (IOException | RuntimeException e) {
			BuildingPlanner.LOGGER.warn("Couldn't set M to open Freecam's menu instead of Litematica's", e);
		}
	}

	private static void setLook() {
		ConfigBoolean translucent = Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT;
		boolean translucentNow = LookDefaults.choose(translucent.getBooleanValue(), translucent.getDefaultBooleanValue(),
			LookDefaults.TRANSLUCENT);
		if (translucentNow != translucent.getBooleanValue()) {
			translucent.setBooleanValue(translucentNow);
		}
		ConfigDouble alpha = Configs.Visuals.GHOST_BLOCK_ALPHA;
		double alphaNow = LookDefaults.choose(alpha.getDoubleValue(), alpha.getDefaultDoubleValue(),
			LookDefaults.GHOST_ALPHA);
		if (Double.compare(alphaNow, alpha.getDoubleValue()) != 0) {
			alpha.setDoubleValue(alphaNow);
		}
		ConfigBoolean toolItem = Configs.Generic.TOOL_ITEM_ENABLED;
		boolean toolItemNow = LookDefaults.choose(toolItem.getBooleanValue(), toolItem.getDefaultBooleanValue(),
			LookDefaults.TOOL_ITEM);
		if (toolItemNow != toolItem.getBooleanValue()) {
			toolItem.setBooleanValue(toolItemNow);
		}
	}

	private static void setAreaSelection() {
		ConfigBoolean areaSelection = Configs.Visuals.ENABLE_AREA_SELECTION_RENDERING;
		boolean areaSelectionNow = LookDefaults.choose(areaSelection.getBooleanValue(),
			areaSelection.getDefaultBooleanValue(), LookDefaults.AREA_SELECTION_RENDERING);
		if (areaSelectionNow != areaSelection.getBooleanValue()) {
			areaSelection.setBooleanValue(areaSelectionNow);
		}
	}

	private static void setStatusPanel() {
		ConfigBoolean auto = Configs.InfoOverlays.STATUS_INFO_HUD_AUTO;
		boolean autoNow = LookDefaults.choose(auto.getBooleanValue(), auto.getDefaultBooleanValue(),
			LookDefaults.STATUS_PANEL_AUTO);
		if (autoNow != auto.getBooleanValue()) {
			auto.setBooleanValue(autoNow);
		}
	}
}
