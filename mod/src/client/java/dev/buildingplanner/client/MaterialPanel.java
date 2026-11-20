package dev.buildingplanner.client;

import com.mojang.blaze3d.platform.Window;
import dev.buildingplanner.look.MaterialPanelRows;
import dev.buildingplanner.look.SmallText;
import fi.dy.masa.litematica.data.DataManager;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The material list panel in the screen's top-left corner, 4 from each edge: while the panel is on, it pins the current
 * list to the screen as the mix panel pins the mix, on a dark see-through box at {@link SmallText}'s size. It shows the
 * heading "Material list", with the multiplier after it when it isn't 1, the column headings "Total", "Available" and
 * "Missing", then a row for each of the list's rows the screen shows ({@link MaterialPanelRows}): the block's icon and
 * its three counts, with no name, "Missing" in the screen's colours. It shows at most 10 rows and only the rows that fit
 * above the hotbar ({@link #rows}), and "+N more", which fits above it too, counts the rest; a list with no rows to
 * show has "No blocks to list." under the heading. It hides while a screen is open, while the HUD is
 * hidden (F1), without a session, and while the current list isn't Building Planner's.
 */
final class MaterialPanel implements HudElement {
	/** From the screen's top and left edges to the box, in GUI pixels. */
	static final int EDGE = 4;
	private static final int ROW = 18;
	private static final int PAD = 4;
	private static final int GAP = 6;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int HEADING = 0xFFAAAAAA;
	private static final int BACKGROUND = 0xA0000000;
	private static final String[] COLUMNS = {"buildingplanner.ui.material_panel.total",
		"buildingplanner.ui.material_panel.available", "buildingplanner.ui.material_panel.missing"};

	/** The list the panel shows now, or null while it hides. */
	private static GhostMaterialList shown() {
		Minecraft mc = Minecraft.getInstance();
		ClientSession session = BuildingPlannerClient.session();
		if (session == null || !session.lists().panel() || mc.gui.screen() != null || mc.gui.hud.isHidden()) {
			return null;
		}
		return DataManager.getMaterialList() instanceof GhostMaterialList list ? list : null;
	}

	private static Component title(int multiplier) {
		return multiplier == 1 ? Component.translatable("buildingplanner.ui.material_panel.title")
			: Component.translatable("buildingplanner.ui.material_panel.title_multiplied", multiplier);
	}

	private static Component more(int more) {
		return Component.translatable("buildingplanner.ui.material_panel.more", more);
	}

	private static Component empty() {
		return Component.translatable("buildingplanner.ui.material_panel.empty");
	}

	/**
	 * The list's rows the panel shows: at most {@link MaterialPanelRows#MAX_ROWS}, and only the ones that fit above the
	 * hotbar, with "+N more" counting the rest. The game keeps the bottom {@link ContextualBar#MARGIN_BOTTOM} GUI pixels
	 * of the screen for the hotbar (the bars above it, like the experience bar, are laid out from there), so the panel's
	 * bottom, with its "+N more" line, stays above that at every GUI scale and window size.
	 */
	private static MaterialPanelRows<ItemStack> rows(Font font, GhostMaterialList list) {
		Window window = Minecraft.getInstance().getWindow();
		// The panel is drawn at SmallText's scale from EDGE down, so its room in its own units is the GUI pixels above
		// the hotbar's strip, less EDGE, over that scale.
		int room = (int) ((window.getGuiScaledHeight() - ContextualBar.MARGIN_BOTTOM - EDGE)
			/ SmallText.scale(window.getGuiScale()));
		int fixed = 2 * PAD + font.lineHeight + 2 + font.lineHeight + 2;
		return list.panelRows().within(room - fixed, ROW, 2 + font.lineHeight);
	}

	/** Whether the list has no rows to show at all ("No blocks to list."), as opposed to none that fit. */
	private static boolean nothing(MaterialPanelRows<ItemStack> panel) {
		return panel.rows().isEmpty() && panel.more() == 0;
	}

	/**
	 * The panel's lines as the player reads them, separated by " | ", or {@code hidden}; for the in-game checks. A row
	 * reads as its block's item id, then its "Total", "Available" and "Missing", with "Missing"'s colour.
	 */
	static String text() {
		GhostMaterialList list = shown();
		if (list == null) {
			return "hidden";
		}
		MaterialPanelRows<ItemStack> panel = rows(Minecraft.getInstance().font, list);
		List<String> lines = new ArrayList<>();
		lines.add(title(list.getMultiplier()).getString());
		if (nothing(panel)) {
			lines.add(empty().getString());
			return String.join(" | ", lines);
		}
		List<String> headings = new ArrayList<>();
		for (String key : COLUMNS) {
			headings.add(Component.translatable(key).getString());
		}
		lines.add(String.join(" ", headings));
		for (MaterialPanelRows.Row<ItemStack> row : panel.rows()) {
			lines.add(BuiltInRegistries.ITEM.getKey(row.item().getItem()) + " " + row.total() + " " + row.available()
				+ " " + row.missing() + " " + row.missingColour().name().toLowerCase());
		}
		if (panel.more() > 0) {
			lines.add(more(panel.more()).getString());
		}
		return String.join(" | ", lines);
	}

	/**
	 * The panel's box in the window's pixels (x y width height), or {@code hidden}; for the in-game checks and for
	 * outlining it in screenshots.
	 */
	static String box() {
		GhostMaterialList list = shown();
		if (list == null) {
			return "hidden";
		}
		Minecraft mc = Minecraft.getInstance();
		Layout layout = Layout.of(mc.font, list);
		int guiScale = mc.getWindow().getGuiScale();
		float scale = SmallText.scale(guiScale);
		return EDGE * guiScale + " " + EDGE * guiScale + " " + Math.round(layout.width() * scale * guiScale) + " "
			+ Math.round(layout.height() * scale * guiScale);
	}

	/** Where the panel's parts go, in the panel's own units (GUI pixels before {@link SmallText}'s scale). */
	private record Layout(MaterialPanelRows<ItemStack> panel, Component title, Component[] headings, int[] columns,
		int width, int height) {
		static Layout of(Font font, GhostMaterialList list) {
			MaterialPanelRows<ItemStack> panel = rows(font, list);
			Component title = MaterialPanel.title(list.getMultiplier());
			Component[] headings = new Component[COLUMNS.length];
			int[] columns = new int[COLUMNS.length];
			for (int i = 0; i < COLUMNS.length; i++) {
				headings[i] = Component.translatable(COLUMNS[i]);
				columns[i] = font.width(headings[i]);
			}
			for (MaterialPanelRows.Row<ItemStack> row : panel.rows()) {
				columns[0] = Math.max(columns[0], font.width(String.valueOf(row.total())));
				columns[1] = Math.max(columns[1], font.width(String.valueOf(row.available())));
				columns[2] = Math.max(columns[2], font.width(String.valueOf(row.missing())));
			}
			int width = font.width(title);
			int height = font.lineHeight;
			if (nothing(panel)) {
				width = Math.max(width, font.width(empty()));
				height += 2 + font.lineHeight;
			} else {
				width = Math.max(width, ROW + columns[0] + GAP + columns[1] + GAP + columns[2]);
				height += 2 + font.lineHeight + 2 + panel.rows().size() * ROW;
				if (panel.more() > 0) {
					width = Math.max(width, font.width(more(panel.more())));
					height += 2 + font.lineHeight;
				}
			}
			return new Layout(panel, title, headings, columns, width + 2 * PAD, height + 2 * PAD);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		GhostMaterialList list = shown();
		if (list == null) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		Layout layout = Layout.of(font, list);
		graphics.pose().pushMatrix();
		graphics.pose().translate(EDGE, EDGE);
		graphics.pose().scale(SmallText.scale(Minecraft.getInstance().getWindow().getGuiScale()));
		graphics.fill(0, 0, layout.width(), layout.height(), BACKGROUND);

		int x = PAD;
		int y = PAD;
		graphics.text(font, layout.title(), x, y, TEXT);
		y += font.lineHeight + 2;
		if (nothing(layout.panel())) {
			graphics.text(font, empty(), x, y, TEXT);
			graphics.pose().popMatrix();
			return;
		}
		// Each column's counts end at its right edge, under its heading.
		int[] rights = new int[layout.columns().length];
		int right = x + ROW;
		for (int i = 0; i < rights.length; i++) {
			right += layout.columns()[i];
			rights[i] = right;
			right += GAP;
		}
		for (int i = 0; i < rights.length; i++) {
			graphics.text(font, layout.headings()[i], rights[i] - font.width(layout.headings()[i]), y, HEADING);
		}
		y += font.lineHeight + 2;
		for (MaterialPanelRows.Row<ItemStack> row : layout.panel().rows()) {
			graphics.item(row.item(), x, y + 1);
			int textY = y + (ROW - font.lineHeight) / 2 + 1;
			String[] counts = {String.valueOf(row.total()), String.valueOf(row.available()), String.valueOf(row.missing())};
			int[] colours = {TEXT, TEXT, row.missingColour().argb};
			for (int i = 0; i < counts.length; i++) {
				graphics.text(font, counts[i], rights[i] - font.width(counts[i]), textY, colours[i]);
			}
			y += ROW;
		}
		if (layout.panel().more() > 0) {
			graphics.text(font, more(layout.panel().more()), x, y + 2, TEXT);
		}
		graphics.pose().popMatrix();
	}
}
