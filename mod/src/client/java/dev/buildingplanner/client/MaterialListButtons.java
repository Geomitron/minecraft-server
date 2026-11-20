package dev.buildingplanner.client;

import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Hook L10's buttons on Litematica's material list screen for Building Planner's list: "Save list" and "Saved lists"
 * in place of "Export" and its export type button. Litematica names a button's type only in the listener it gives it,
 * a record of its own, whose {@code type} this reads.
 */
public final class MaterialListButtons {
	/** The gap Litematica leaves between the buttons of a row. */
	private static final int GAP = 1;

	/** The right edge of the "Save list" button just added, where "Saved lists" goes next. */
	private static int saveRight;

	private MaterialListButtons() {
	}

	/** Whether the screen shows Building Planner's list. */
	public static boolean ours(GuiMaterialList gui) {
		return gui.getMaterialList() instanceof GhostMaterialList;
	}

	/**
	 * For Building Planner's list, takes the place of an export button the screen is about to add: "Save list" where
	 * "Export" goes, and "Saved lists" after it, where the export type button goes. Returns whether the export button is
	 * left out.
	 */
	public static boolean replaceExport(GuiMaterialList gui, ButtonBase button, IButtonActionListener listener) {
		if (!ours(gui)) {
			return false;
		}
		String type = type(listener);
		if (type.equals("EXPORT")) {
			saveRight = add(gui, button.getX(), button.getY(), "buildingplanner.ui.material_list.save_list",
				() -> SaveListScreen.open(gui));
			return true;
		}
		if (type.equals("EXPORT_TYPE")) {
			add(gui, saveRight + GAP, button.getY(), "buildingplanner.ui.material_list.saved_lists",
				() -> SavedListsScreen.open(gui));
			return true;
		}
		return false;
	}

	/** Adds a button as Litematica sizes its own (its label's width and 10), and returns its right edge. */
	private static int add(GuiMaterialList gui, int x, int y, String key, Runnable action) {
		String label = Component.translatable(key).getString();
		ButtonGeneric button = new ButtonGeneric(x, y, Minecraft.getInstance().font.width(label) + 10, 20, label);
		gui.addButton(button, (pressed, mouseButton) -> action.run());
		return x + button.getWidth();
	}

	/** The name of the button type in Litematica's listener, or "" for another listener. */
	private static String type(IButtonActionListener listener) {
		if (!listener.getClass().isRecord()) {
			return "";
		}
		try {
			for (RecordComponent component : listener.getClass().getRecordComponents()) {
				if (component.getName().equals("type")) {
					Method accessor = component.getAccessor();
					accessor.setAccessible(true);
					return accessor.invoke(listener) instanceof Enum<?> value ? value.name() : "";
				}
			}
		} catch (ReflectiveOperationException e) {
			return "";
		}
		return "";
	}
}
