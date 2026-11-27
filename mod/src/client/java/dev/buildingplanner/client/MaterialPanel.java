package dev.buildingplanner.client;

import com.mojang.blaze3d.platform.Window;
import dev.buildingplanner.look.MaterialPanelRows;
import dev.buildingplanner.look.PanelRoom;
import dev.buildingplanner.look.SmallText;
import fi.dy.masa.litematica.data.DataManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The material list panel in the screen's top-left corner, 4 from each edge: while the panel is on, it pins the current
 * list to the screen as the mix panel pins the mix, drawn as a {@link CountPanel}. It shows the heading "Material
 * list", with the multiplier after it when it isn't 1, the column headings "All", "Hold" and "Need", then a row for
 * each of the list's rows the screen shows ({@link MaterialPanelRows}): the block's icon and its three counts, with no
 * name, "Need" in the screen's colours. It shows at most 10 rows and only the rows that fit above the hotbar
 * ({@link #rows}), and "+N more", which fits above it too, counts the rest; a list with no rows to show has "No blocks
 * to list." under the heading. It hides while a screen is open, while the HUD is hidden (F1), without a session, and
 * while the current list isn't Building Planner's.
 */
final class MaterialPanel implements HudElement {
	/** From the screen's top and left edges to the box, in GUI pixels. */
	static final int EDGE = 4;
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

	private static CountPanel.Texts texts(int multiplier) {
		Component[] headings = new Component[COLUMNS.length];
		for (int i = 0; i < COLUMNS.length; i++) {
			headings[i] = Component.translatable(COLUMNS[i]);
		}
		return new CountPanel.Texts(multiplier == 1 ? Component.translatable("buildingplanner.ui.material_panel.title")
			: Component.translatable("buildingplanner.ui.material_panel.title_multiplied", multiplier), headings,
			Component.translatable("buildingplanner.ui.material_panel.empty"), MaterialPanel::more);
	}

	/** "+N more", which the click's panel shows too. */
	static Component more(int more) {
		return Component.translatable("buildingplanner.ui.material_panel.more", more);
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
		return list.panelRows().within(room - CountPanel.fixedHeight(font), CountPanel.ROW, CountPanel.moreHeight(font));
	}

	private static CountPanel.Layout layout(GhostMaterialList list) {
		Font font = Minecraft.getInstance().font;
		return CountPanel.Layout.of(font, texts(list.getMultiplier()), rows(font, list));
	}

	/**
	 * The panel's lines as the player reads them, separated by " | ", or {@code hidden}; for the in-game checks. A row
	 * reads as its block's item id, then its "All", "Hold" and "Need", with "Need"'s colour.
	 */
	static String text() {
		GhostMaterialList list = shown();
		return list == null ? "hidden" : CountPanel.text(layout(list));
	}

	/**
	 * The panel's box in the window's pixels (x y width height), or {@code hidden}; for the in-game checks and for
	 * outlining it in screenshots.
	 */
	static String box() {
		GhostMaterialList list = shown();
		return list == null ? "hidden" : CountPanel.windowBox(layout(list), EDGE, EDGE);
	}

	/** The panel's box in GUI pixels while it shows, or null; the click's panel keeps out of it. */
	static PanelRoom.Box guiBox() {
		GhostMaterialList list = shown();
		if (list == null) {
			return null;
		}
		CountPanel.Layout layout = layout(list);
		return new PanelRoom.Box(EDGE, EDGE, EDGE + (int) Math.ceil(layout.guiWidth()),
			EDGE + (int) Math.ceil(layout.guiHeight()));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		GhostMaterialList list = shown();
		if (list != null) {
			CountPanel.draw(graphics, Minecraft.getInstance().font, layout(list), EDGE, EDGE);
		}
	}
}
