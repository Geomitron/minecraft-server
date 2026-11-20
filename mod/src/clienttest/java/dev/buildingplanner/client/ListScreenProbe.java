package dev.buildingplanner.client;

import dev.buildingplanner.sync.ListData;
import dev.buildingplanner.sync.Payloads;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.Items;

/**
 * The driver's view of the material list screens (step9.sh): the buttons of Litematica's list screen, which MaLiLib
 * keeps in a list of its own rather than as the screen's children, the widgets of Building Planner's two list screens,
 * the index of the player's lists the game keeps, and list messages sent as a game that broke the server's limits
 * would send them.
 */
final class ListScreenProbe {
	private ListScreenProbe() {
	}

	/**
	 * Each MaLiLib button on the open screen as its label, without formatting codes, and its window box (x y width
	 * height), separated by " | ".
	 */
	static String malilibButtons(Minecraft mc) throws Exception {
		List<String> out = new ArrayList<>();
		for (ButtonBase button : buttons(mc)) {
			out.add(label(button) + ": " + box(mc, button.getX(), button.getY(), button.getWidth(), button.getHeight()));
		}
		return String.join(" | ", out);
	}

	/** The window position of the middle of the MaLiLib button whose label starts with the text, or none. */
	static String malilibButton(Minecraft mc, String prefix) throws Exception {
		double scale = mc.getWindow().getGuiScale();
		for (ButtonBase button : buttons(mc)) {
			if (label(button).startsWith(prefix)) {
				return (int) ((button.getX() + button.getWidth() / 2.0) * scale) + " "
					+ (int) ((button.getY() + button.getHeight() / 2.0) * scale);
			}
		}
		return "none";
	}

	/**
	 * Each shown widget on the open screen as its text, with its value for a text field and "(greyed out)" when it isn't
	 * active, then its window box, separated by " | ".
	 */
	static String widgets(Minecraft mc) {
		List<String> out = new ArrayList<>();
		for (GuiEventListener child : mc.gui.screen().children()) {
			if (child instanceof AbstractWidget w && w.visible) {
				String text = w instanceof EditBox field ? "field=" + field.getValue() : w.getMessage().getString();
				out.add(text + (w.active ? "" : " (greyed out)") + ": "
					+ box(mc, w.getX(), w.getY(), w.getWidth(), w.getHeight()));
			}
		}
		return String.join(" | ", out);
	}

	/** The rows the open "Saved lists" screen shows, each as its name and details, separated by " | ". */
	static String savedLists(Minecraft mc) {
		return mc.gui.screen() instanceof SavedListsScreen screen ? String.join(" | ", screen.rowTexts()) : "not open";
	}

	/** The index of the player's lists the game keeps: "Last counted" and each saved list, with their sizes. */
	static String index() throws Exception {
		Payloads.Lists index = Driver.session().lists().index();
		List<String> out = new ArrayList<>();
		out.add("last counted " + index.lastCounted().map(summary -> size(summary.size())).orElse("none"));
		for (Payloads.ListSummary summary : index.saved()) {
			out.add(summary.name() + " " + size(summary.size()));
		}
		return String.join(" | ", out);
	}

	/**
	 * Sends list messages past the server's limits, as the game never does: {@code longname}, a {@code save_list} of the
	 * current list named with 65 characters; {@code overtotal <name>}, one whose row has more missing than its total;
	 * {@code burst <name>}, 20 {@code delete_list}s of a name never kept, then a {@code save_list} of the current list,
	 * the 21st list message in 10 seconds.
	 */
	static String sendPastLimits(String[] a) {
		ListData list = ((GhostMaterialList) DataManager.getMaterialList()).data();
		switch (a[1]) {
			case "longname" -> ClientPlayNetworking.send(new Payloads.SaveList("L".repeat(65), list));
			case "overtotal" -> ClientPlayNetworking.send(new Payloads.SaveList(a[2], new ListData(list.name(),
				list.time(), list.dimension(), list.min(), list.max(), list.multiplier(), list.sort(), list.reverse(),
				list.hideAvailable(), List.of(new ListData.Row(Items.OAK_PLANKS, 4, 10, 0)))));
			case "burst" -> {
				for (int i = 0; i < 20; i++) {
					ClientPlayNetworking.send(new Payloads.DeleteList("Never kept"));
				}
				ClientPlayNetworking.send(new Payloads.SaveList(a[2], list));
			}
			default -> {
				return "unknown " + a[1];
			}
		}
		return "sent";
	}

	@SuppressWarnings("unchecked")
	private static List<ButtonBase> buttons(Minecraft mc) throws Exception {
		if (!(mc.gui.screen() instanceof GuiBase screen)) {
			return List.of();
		}
		Field f = GuiBase.class.getDeclaredField("buttons");
		f.setAccessible(true);
		return (List<ButtonBase>) f.get(screen);
	}

	private static String label(ButtonBase button) throws Exception {
		Field f = ButtonBase.class.getDeclaredField("displayString");
		f.setAccessible(true);
		// Without its formatting codes ("§a" before ON, say).
		return String.valueOf(f.get(button)).replaceAll("§.", "");
	}

	private static String box(Minecraft mc, int x, int y, int width, int height) {
		double scale = mc.getWindow().getGuiScale();
		return List.of(x, y, width, height).stream().map(v -> String.valueOf((int) (v * scale)))
			.collect(Collectors.joining(" "));
	}

	private static String size(Vec3i size) {
		return size.getX() + "x" + size.getY() + "x" + size.getZ();
	}
}
