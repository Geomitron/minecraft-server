package dev.buildingplanner.client;

import dev.buildingplanner.input.BoxCorners;
import dev.buildingplanner.input.MouseMode;
import dev.buildingplanner.look.BoxListLayout;
import dev.buildingplanner.look.ControlsLine;
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
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;

/**
 * The line of a mouse mode's controls, centred where Effortless Building draws its own line during a shape
 * ({@code RenderHandler.renderSubtitle}: 58 above the screen's bottom, with a shadow), in its colours: white, with each
 * action in dark aqua, each wrapped line on a dark see-through background as wide as its text; the lines themselves
 * are worked out by {@link ControlsLine}. A box's line ends with its size, a
 * box past a limit has a red line above saying which, and the copy's line names in red why it can't be pasted. A
 * line too wide for the screen wraps onto lines above it, keeping the size whole. While a box is drawn, the blocks in it
 * show under the crosshair ({@link BoxList}, laid out by {@link BoxListLayout}), in rows that end above the controls,
 * and in Delete by aim, the marked ghost blocks.
 * They show while a mode is on, with no screen open and the HUD shown (not hidden with F1).
 */
final class ModeHud implements HudElement {
	private static final int BOTTOM = 58;
	private static final int MARGIN = 4;
	private static final int WHITE = -1;
	/** The black behind each line of text: the opacity of the game's chat background by default (0.5). */
	private static final int BACKGROUND = ARGB.black(0.5F);
	/** How far the background reaches past the text at each side, as {@code GuiGraphicsExtractor.textWithBackdrop}'s. */
	private static final int PADDING = 2;

	/** A line of the controls, and the box's size at its end, which never breaks across lines, or null. */
	private record Line(Component text, Component size) {
		String read() {
			return size == null ? text.getString() : text.getString() + " " + size.getString();
		}
	}

	private final Supplier<MouseMode> mode;
	private final Supplier<BoxDraw.Shown> box;
	private final Supplier<ControlsLine.Line> copy;
	private final Supplier<List<BoxList.Entry>> list;

	/**
	 * {@code mode} is the mouse mode now, or null, {@code box} the box drawn now, or null, {@code copy} the copy at the
	 * aim's line ({@link ControlsLine#copy}), or null, and {@code list} the kinds of block in the box.
	 */
	ModeHud(Supplier<MouseMode> mode, Supplier<BoxDraw.Shown> box, Supplier<ControlsLine.Line> copy,
		Supplier<List<BoxList.Entry>> list) {
		this.mode = mode;
		this.box = box;
		this.copy = copy;
		this.list = list;
	}

	/** The lines to show now, from the top, or none. */
	private List<Line> lines() {
		MouseMode now = mode.get();
		if (!shown()) {
			return List.of();
		}
		List<ControlsLine.Line> lines = switch (now) {
			case BOX -> {
				BoxDraw.Shown shown = box.get();
				yield shown == null ? List.of() : ControlsLine.box(shown.purpose().name(), shown.corners(),
					shown.corners() > 0 && shown.min() != null ? BoxCorners.size(shown.min(), shown.max()) : null,
					shown.limit());
			}
			case COPY_AT_AIM -> {
				ControlsLine.Line line = copy.get();
				yield line == null ? List.of() : List.of(line);
			}
			case DELETE_AIM -> List.of();
		};
		return lines.stream().map(line -> new Line(component(line.text()).withStyle(ChatFormatting.WHITE),
			line.size() == null ? null : component(line.size()).withStyle(ChatFormatting.WHITE))).toList();
	}

	/** Whether the mode's controls and its list show: while a mode is on, with no screen open and the HUD shown. */
	private boolean shown() {
		Minecraft mc = Minecraft.getInstance();
		return mode.get() != null && mc.gui.screen() == null && !mc.gui.hud.isHidden();
	}

	/** Whether the list under the crosshair goes with the mode now: a box's, or Delete by aim's marks. */
	private boolean listed() {
		MouseMode now = mode.get();
		return now == MouseMode.BOX || now == MouseMode.DELETE_AIM;
	}

	/** The game's component for a part of a line, in its look: an action in dark aqua and a warning in red. */
	private static MutableComponent component(ControlsLine.Text text) {
		return switch (text) {
			case ControlsLine.Translated translated -> {
				Object[] args = translated.args().stream().map(ModeHud::component).toArray();
				MutableComponent made = Component.translatable(translated.key(), args);
				yield switch (translated.look()) {
					case PLAIN -> made;
					case ACTION -> made.withStyle(ChatFormatting.DARK_AQUA);
					case WARNING -> made.withStyle(ChatFormatting.RED);
				};
			}
			case ControlsLine.Literal literal -> Component.literal(literal.text());
			case ControlsLine.Joined joined -> {
				MutableComponent made = Component.empty();
				for (int i = 0; i < joined.parts().size(); i++) {
					if (i > 0) {
						made.append(", ");
					}
					made.append(component(joined.parts().get(i)));
				}
				yield made;
			}
		};
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
	 * The window box of the lines as they're drawn now: left, top, right and bottom, in the window's pixels, or
	 * {@code hidden}; for the outlines on the in-game checks' shots.
	 */
	String windowBox() {
		List<Line> lines = lines();
		if (lines.isEmpty()) {
			return "hidden";
		}
		Minecraft mc = Minecraft.getInstance();
		int width = mc.getWindow().getGuiScaledWidth();
		List<FormattedCharSequence> wrapped = wrapped(lines, mc.font, width - 2 * MARGIN);
		int top = top(wrapped.size(), mc.font, mc.getWindow().getGuiScaledHeight());
		int left = width;
		int right = 0;
		for (FormattedCharSequence line : wrapped) {
			int lineWidth = mc.font.width(line);
			left = Math.min(left, (width - lineWidth) / 2);
			right = Math.max(right, (width - lineWidth) / 2 + lineWidth);
		}
		int scale = (int) mc.getWindow().getGuiScale();
		return left * scale + " " + top * scale + " " + right * scale + " "
			+ (top + wrapped.size() * mc.font.lineHeight) * scale;
	}

	/**
	 * The list under the crosshair as it shows now, for the in-game checks: the right edge of its cells and the screen's
	 * width, the bottom of its rows and the top of the controls, all in GUI pixels, then its rows separated by " / ",
	 * each cell an item's id and its count, or "+N"; or {@code hidden}.
	 */
	String listText() {
		List<Line> lines = lines();
		List<BoxList.Entry> entries = list.get();
		if (!listed() || !shown() || entries.isEmpty()) {
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

	/** The top of the controls, {@code count} lines ending 58 above the screen's bottom; with none, of one line there. */
	private static int top(int count, Font font, int height) {
		return height - BOTTOM - (Math.max(count, 1) - 1) * font.lineHeight;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		List<Line> lines = lines();
		if (lines.isEmpty() && !(listed() && shown())) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		List<FormattedCharSequence> wrapped = wrapped(lines, font, graphics.guiWidth() - 2 * MARGIN);
		int top = top(wrapped.size(), font, graphics.guiHeight());
		// The backgrounds first, each as wide as its line's text, so no box covers the line above's shadow.
		int y = top;
		for (FormattedCharSequence text : wrapped) {
			int x = (graphics.guiWidth() - font.width(text)) / 2;
			graphics.fill(x - PADDING, y, x + font.width(text) + PADDING, y + font.lineHeight, BACKGROUND);
			y += font.lineHeight;
		}
		y = top;
		for (FormattedCharSequence text : wrapped) {
			graphics.text(font, text, (graphics.guiWidth() - font.width(text)) / 2, y, WHITE, true);
			y += font.lineHeight;
		}
		if (listed()) {
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
