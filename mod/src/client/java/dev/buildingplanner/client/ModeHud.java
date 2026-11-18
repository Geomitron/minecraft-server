package dev.buildingplanner.client;

import dev.buildingplanner.input.BoxCorners;
import dev.buildingplanner.input.MouseMode;
import dev.buildingplanner.look.BoxListLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

/**
 * The line of a mouse mode's controls, centred where Effortless Building draws its own line during a shape
 * ({@code RenderHandler.renderSubtitle}: 58 above the screen's bottom, with a shadow), in its colours: white, with each
 * action in dark aqua. A box's line ends with its size, and a box past a limit has a red line above saying which. A
 * line too wide for the screen wraps onto lines above it, keeping the size whole. While a box is drawn, the blocks in it
 * show under the crosshair ({@link BoxList}, laid out by {@link BoxListLayout}), in rows that end above the controls.
 * They show while a mode is on, with no screen open and the HUD shown (not hidden with F1).
 */
final class ModeHud implements HudElement {
	private static final int BOTTOM = 58;
	private static final int MARGIN = 4;
	private static final int WHITE = -1;
	private static final String COPY = "buildingplanner.ui.mode.copy";
	private static final String BOX = "buildingplanner.ui.mode.box";

	/** A line of the controls, and the box's size at its end, which never breaks across lines, or null. */
	private record Line(Component text, Component size) {
		Line(Component text) {
			this(text, null);
		}

		String read() {
			return size == null ? text.getString() : text.getString() + " " + size.getString();
		}
	}

	private final Supplier<MouseMode> mode;
	private final Supplier<BoxDraw.Shown> box;
	private final Supplier<List<BoxList.Entry>> list;

	/**
	 * {@code mode} is the mouse mode now, or null, {@code box} the box drawn now, or null, and {@code list} the kinds of
	 * block in it.
	 */
	ModeHud(Supplier<MouseMode> mode, Supplier<BoxDraw.Shown> box, Supplier<List<BoxList.Entry>> list) {
		this.mode = mode;
		this.box = box;
		this.list = list;
	}

	/** The lines to show now, from the top, or none. */
	private List<Line> lines() {
		Minecraft mc = Minecraft.getInstance();
		MouseMode now = mode.get();
		if (now == null || mc.gui.screen() != null || mc.gui.hud.isHidden()) {
			return List.of();
		}
		return switch (now) {
			case BOX -> boxLines(box.get());
			case COPY_AT_AIM -> List.of(new Line(Component.translatable(COPY, action(COPY + ".paste"),
				action(COPY + ".turn"), action(COPY + ".mirror"), action(COPY + ".put_away"))
				.withStyle(ChatFormatting.WHITE)));
		};
	}

	private static List<Line> boxLines(BoxDraw.Shown shown) {
		if (shown == null) {
			return List.of();
		}
		String purpose = switch (shown.purpose()) {
			case COPY -> BOX + ".copy";
			case MATERIAL_LIST -> BOX + ".material_list";
		};
		String next = switch (shown.corners()) {
			case 0 -> BOX + ".first";
			case 1 -> BOX + ".second";
			default -> BOX + ".height";
		};
		MutableComponent controls = Component.translatable(BOX, Component.translatable(purpose), action(next),
			action(BOX + ".cancel")).withStyle(ChatFormatting.WHITE);
		Component size = null;
		if (shown.corners() > 0 && shown.min() != null) {
			int[] sides = BoxCorners.size(shown.min(), shown.max());
			size = Component.translatable(BOX + ".size", sides[0], sides[1], sides[2]).withStyle(ChatFormatting.WHITE);
		}
		List<Line> lines = new ArrayList<>(2);
		if (shown.limit() != null) {
			MutableComponent limit = switch (shown.limit()) {
				case SIDE -> Component.translatable(BOX + ".too_large", BoxCorners.MAX_SIDE);
				case NOT_LOADED -> Component.translatable(BOX + ".not_loaded");
			};
			lines.add(new Line(limit.withStyle(ChatFormatting.RED)));
		}
		lines.add(new Line(controls, size));
		return lines;
	}

	/**
	 * The lines as they show on a screen {@code width} wide: each wrapped at its spaces, with the size after the last
	 * word that leaves it room, or alone on a line below.
	 */
	private static List<FormattedCharSequence> wrapped(List<Line> lines, Font font, int width) {
		List<FormattedCharSequence> wrapped = new ArrayList<>();
		for (Line line : lines) {
			List<FormattedCharSequence> parts = font.split(line.text(), width);
			wrapped.addAll(parts);
			if (line.size() == null) {
				continue;
			}
			FormattedCharSequence size = line.size().getVisualOrderText();
			FormattedCharSequence last = wrapped.getLast();
			FormattedCharSequence joined = FormattedCharSequence.composite(last,
				Component.literal(" ").getVisualOrderText(), size);
			if (font.width(joined) <= width) {
				wrapped.set(wrapped.size() - 1, joined);
			} else {
				wrapped.add(size);
			}
		}
		return wrapped;
	}

	/** The lines' text as the player reads them, separated by " | ", or {@code hidden}; for the in-game checks. */
	String text() {
		List<Line> lines = lines();
		return lines.isEmpty() ? "hidden" : lines.stream().map(Line::read).collect(Collectors.joining(" | "));
	}

	/**
	 * The lines as they wrap on the screen now, separated by " / ", or {@code hidden}; for the in-game checks, which see
	 * where the size goes.
	 */
	String wrappedText() {
		List<Line> lines = lines();
		if (lines.isEmpty()) {
			return "hidden";
		}
		Minecraft mc = Minecraft.getInstance();
		List<String> texts = new ArrayList<>();
		for (FormattedCharSequence line : wrapped(lines, mc.font, mc.getWindow().getGuiScaledWidth() - 2 * MARGIN)) {
			StringBuilder text = new StringBuilder();
			line.accept((index, style, codePoint) -> {
				text.appendCodePoint(codePoint);
				return true;
			});
			texts.add(text.toString());
		}
		return String.join(" / ", texts);
	}

	/**
	 * The list under the crosshair as it shows now, for the in-game checks: the right edge of its cells and the screen's
	 * width, the bottom of its rows and the top of the controls, all in GUI pixels, then its rows separated by " / ",
	 * each cell an item's id and its count, or "+N"; or {@code hidden}.
	 */
	String listText() {
		List<Line> lines = lines();
		List<BoxList.Entry> entries = list.get();
		if (mode.get() != MouseMode.BOX || lines.isEmpty() || entries.isEmpty()) {
			return "hidden";
		}
		Minecraft mc = Minecraft.getInstance();
		int width = mc.getWindow().getGuiScaledWidth();
		int height = mc.getWindow().getGuiScaledHeight();
		int top = top(wrapped(lines, mc.font, width - 2 * MARGIN).size(), mc.font, height);
		BoxListLayout layout = BoxListLayout.of(width, height, entries.size(), top);
		int right = 0;
		int bottom = 0;
		StringBuilder rows = new StringBuilder();
		for (int i = 0; i < layout.cells().size(); i++) {
			BoxListLayout.Cell cell = layout.cells().get(i);
			right = Math.max(right, cell.x() + BoxListLayout.CELL);
			bottom = Math.max(bottom, cell.y() + BoxListLayout.CELL);
			if (i > 0) {
				rows.append(cell.y() == layout.cells().get(i - 1).y() ? ", " : " / ");
			}
			if (layout.more() > 0 && i == layout.cells().size() - 1) {
				rows.append('+').append(layout.more());
			} else {
				BoxList.Entry entry = entries.get(i);
				rows.append(BuiltInRegistries.ITEM.getKey(entry.item().getItem())).append(' ').append(entry.count());
			}
		}
		return right + " " + width + " " + bottom + " " + top + " | " + rows;
	}

	private static Component action(String key) {
		return Component.translatable(key).withStyle(ChatFormatting.DARK_AQUA);
	}

	/** The top of the controls, {@code count} lines ending 58 above the screen's bottom. */
	private static int top(int count, Font font, int height) {
		return height - BOTTOM - (count - 1) * font.lineHeight;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		List<Line> lines = lines();
		if (lines.isEmpty()) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		List<FormattedCharSequence> wrapped = wrapped(lines, font, graphics.guiWidth() - 2 * MARGIN);
		int top = top(wrapped.size(), font, graphics.guiHeight());
		int y = top;
		for (FormattedCharSequence text : wrapped) {
			graphics.text(font, text, (graphics.guiWidth() - font.width(text)) / 2, y, WHITE, true);
			y += font.lineHeight;
		}
		if (mode.get() == MouseMode.BOX) {
			drawList(graphics, font, top);
		}
	}

	/**
	 * The list under the crosshair, drawn as Effortless Building draws its own ({@code RenderHandler.drawItemStack}):
	 * each item, then its count over it, right-aligned to the item's right edge and starting 9 below its top; the "+N"
	 * cell's text sits in the middle of its cell.
	 */
	private void drawList(GuiGraphicsExtractor graphics, Font font, int top) {
		List<BoxList.Entry> entries = list.get();
		BoxListLayout layout = BoxListLayout.of(graphics.guiWidth(), graphics.guiHeight(), entries.size(), top);
		int items = layout.more() > 0 ? layout.cells().size() - 1 : layout.cells().size();
		for (int i = 0; i < items; i++) {
			BoxListLayout.Cell cell = layout.cells().get(i);
			graphics.item(entries.get(i).item(), cell.x(), cell.y());
		}
		graphics.nextStratum();
		for (int i = 0; i < items; i++) {
			BoxListLayout.Cell cell = layout.cells().get(i);
			String count = BoxListLayout.count(entries.get(i).count());
			graphics.text(font, count, cell.x() + 17 - font.width(count), cell.y() + 9, WHITE, true);
		}
		if (layout.more() > 0) {
			BoxListLayout.Cell cell = layout.cells().getLast();
			String more = "+" + layout.more();
			graphics.text(font, more, cell.x() + (16 - font.width(more)) / 2, cell.y() + (16 - font.lineHeight) / 2 + 1,
				WHITE, true);
		}
	}
}
