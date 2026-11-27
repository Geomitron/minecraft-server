package dev.buildingplanner.client;

import dev.buildingplanner.look.MaterialPanelRows;
import dev.buildingplanner.look.SmallText;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * A panel of items and counts as the material list panel draws it, which the click's panel draws too: a dark
 * see-through box at {@link SmallText}'s size, its title, then three column headings in grey, then a row for each item,
 * its icon and its three counts, each right-aligned under its heading, the third in its row's colour and the others
 * white, then "+N more" for the rows left out; with no rows to show, its empty line under the title instead.
 */
final class CountPanel {
	static final int ROW = 18;
	private static final int PAD = 4;
	private static final int GAP = 6;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int HEADING = 0xFFAAAAAA;
	private static final int BACKGROUND = 0xA0000000;

	private CountPanel() {
	}

	/** The panel's texts: its title, its three column headings, its empty line, and its "+N more" by count. */
	record Texts(Component title, Component[] headings, Component empty, IntFunction<Component> more) {
	}

	/** The height, in the panel's own units, of everything but the rows and "+N more": the title and the headings. */
	static int fixedHeight(Font font) {
		return 2 * PAD + font.lineHeight + 2 + font.lineHeight + 2;
	}

	/** The height, in the panel's own units, of the "+N more" line. */
	static int moreHeight(Font font) {
		return 2 + font.lineHeight;
	}

	/** Whether the panel has no rows to show at all (its empty line), as opposed to none that fit. */
	static boolean nothing(MaterialPanelRows<ItemStack> rows) {
		return rows.rows().isEmpty() && rows.more() == 0;
	}

	/** Where the panel's parts go, in the panel's own units (GUI pixels before {@link SmallText}'s scale). */
	record Layout(Texts texts, MaterialPanelRows<ItemStack> rows, int[] columns, int width, int height) {
		static Layout of(Font font, Texts texts, MaterialPanelRows<ItemStack> rows) {
			int[] columns = new int[texts.headings().length];
			for (int i = 0; i < columns.length; i++) {
				columns[i] = font.width(texts.headings()[i]);
			}
			for (MaterialPanelRows.Row<ItemStack> row : rows.rows()) {
				columns[0] = Math.max(columns[0], font.width(String.valueOf(row.total())));
				columns[1] = Math.max(columns[1], font.width(String.valueOf(row.available())));
				columns[2] = Math.max(columns[2], font.width(String.valueOf(row.missing())));
			}
			int width = font.width(texts.title());
			int height = font.lineHeight;
			if (nothing(rows)) {
				width = Math.max(width, font.width(texts.empty()));
				height += 2 + font.lineHeight;
			} else {
				width = Math.max(width, ROW + columns[0] + GAP + columns[1] + GAP + columns[2]);
				height += 2 + font.lineHeight + 2 + rows.rows().size() * ROW;
				if (rows.more() > 0) {
					width = Math.max(width, font.width(texts.more().apply(rows.more())));
					height += moreHeight(font);
				}
			}
			return new Layout(texts, rows, columns, width + 2 * PAD, height + 2 * PAD);
		}

		/** The panel's width and height in GUI pixels, at {@link SmallText}'s scale. */
		float guiWidth() {
			return width * scale();
		}

		float guiHeight() {
			return height * scale();
		}
	}

	/** {@link SmallText}'s scale at the window's GUI scale now. */
	static float scale() {
		return SmallText.scale(Minecraft.getInstance().getWindow().getGuiScale());
	}

	/**
	 * The panel's lines as the player reads them, separated by " | ": its title, then its empty line, or its headings,
	 * each row as its item's id and its three counts and the third's colour, and "+N more"; for the in-game checks.
	 */
	static String text(Layout layout) {
		List<String> lines = new ArrayList<>();
		lines.add(layout.texts().title().getString());
		if (nothing(layout.rows())) {
			lines.add(layout.texts().empty().getString());
			return String.join(" | ", lines);
		}
		List<String> headings = new ArrayList<>();
		for (Component heading : layout.texts().headings()) {
			headings.add(heading.getString());
		}
		lines.add(String.join(" ", headings));
		for (MaterialPanelRows.Row<ItemStack> row : layout.rows().rows()) {
			lines.add(BuiltInRegistries.ITEM.getKey(row.item().getItem()) + " " + row.total() + " " + row.available()
				+ " " + row.missing() + " " + row.missingColour().name().toLowerCase());
		}
		if (layout.rows().more() > 0) {
			lines.add(layout.texts().more().apply(layout.rows().more()).getString());
		}
		return String.join(" | ", lines);
	}

	/**
	 * The panel's box in the window's pixels (x y width height), drawn with its top-left corner at {@code x} and
	 * {@code y} in GUI pixels; for the in-game checks and for outlining it in screenshots.
	 */
	static String windowBox(Layout layout, float x, float y) {
		int guiScale = Minecraft.getInstance().getWindow().getGuiScale();
		return Math.round(x * guiScale) + " " + Math.round(y * guiScale) + " "
			+ Math.round(layout.guiWidth() * guiScale) + " " + Math.round(layout.guiHeight() * guiScale);
	}

	/** Draws the panel with its top-left corner at {@code x} and {@code y}, in GUI pixels. */
	static void draw(GuiGraphicsExtractor graphics, Font font, Layout layout, float x, float y) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale());
		graphics.fill(0, 0, layout.width(), layout.height(), BACKGROUND);

		int left = PAD;
		int top = PAD;
		graphics.text(font, layout.texts().title(), left, top, TEXT);
		top += font.lineHeight + 2;
		if (nothing(layout.rows())) {
			graphics.text(font, layout.texts().empty(), left, top, TEXT);
			graphics.pose().popMatrix();
			return;
		}
		// Each column's counts end at its right edge, under its heading.
		int[] rights = new int[layout.columns().length];
		int right = left + ROW;
		for (int i = 0; i < rights.length; i++) {
			right += layout.columns()[i];
			rights[i] = right;
			right += GAP;
		}
		Component[] headings = layout.texts().headings();
		for (int i = 0; i < rights.length; i++) {
			graphics.text(font, headings[i], rights[i] - font.width(headings[i]), top, HEADING);
		}
		top += font.lineHeight + 2;
		for (MaterialPanelRows.Row<ItemStack> row : layout.rows().rows()) {
			graphics.item(row.item(), left, top + 1);
			int textY = top + (ROW - font.lineHeight) / 2 + 1;
			String[] counts = {String.valueOf(row.total()), String.valueOf(row.available()), String.valueOf(row.missing())};
			int[] colours = {TEXT, TEXT, row.missingColour().argb};
			for (int i = 0; i < counts.length; i++) {
				graphics.text(font, counts[i], rights[i] - font.width(counts[i]), textY, colours[i]);
			}
			top += ROW;
		}
		if (layout.rows().more() > 0) {
			graphics.text(font, layout.texts().more().apply(layout.rows().more()), left, top + 2, TEXT);
		}
		graphics.pose().popMatrix();
	}
}
