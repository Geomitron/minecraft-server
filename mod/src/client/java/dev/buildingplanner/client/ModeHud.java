package dev.buildingplanner.client;

import dev.buildingplanner.input.BoxCorners;
import dev.buildingplanner.input.MouseMode;
import dev.buildingplanner.look.BoxListLayout;
import dev.buildingplanner.look.ControlsLine;
import dev.buildingplanner.look.ControlsRows;
import dev.buildingplanner.look.ModeName;
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
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.HumanoidArm;

/**
 * The lines of a mouse mode's controls, centred where Effortless Building draws its own line during a shape
 * ({@code RenderHandler.renderSubtitle}: 58 above the screen's bottom, with a shadow), in its colours: each line is a
 * row of rectangles ({@link ControlsLine.Line}), each on its own dark see-through background with a gap between them,
 * laid out by {@link ControlsRows}; white, with each action in dark aqua and each reason in red. A box's line ends with
 * its size, and a box past a limit has a red line above saying which. A line too wide for the screen goes on in a row
 * below. While a box is drawn, the blocks in it show under the crosshair ({@link BoxList}, laid out by
 * {@link BoxListLayout}), in rows that end above the controls, each row on the controls' dark background, and in
 * Delete by aim, the marked ghost blocks.
 * They show while a mode is on, with no screen open and the HUD shown (not hidden with F1). With no mode on, the build
 * modes' line shows in the same place while the lock's controls are active ({@link ControlsLine#shape}), or in Disable, Disable's line
 * ({@link ControlsLine#disable}); during a shape, while Effortless Building draws its own line there and the shape's
 * block count in the action bar above it, it shows above the action bar, its last row ending 2 pixels above it. While the HUD shows, Effortless Building's mode's name shows
 * just left of the hotbar, in a rectangle of the same look ({@link ModeName}), but in Disable.
 */
final class ModeHud implements HudElement {
	private static final int BOTTOM = 58;
	/**
	 * Where the build modes' line ends during a shape: 2 above the top of the action bar's text, which the game draws
	 * from 72 to 63 above the screen's bottom ({@code Hud.extractOverlayMessage}).
	 */
	private static final int ABOVE_ACTION_BAR = 74;
	private static final int WHITE = -1;
	/** The black behind each rectangle: the opacity of the game's chat background by default (0.5). */
	private static final int BACKGROUND = ARGB.black(0.5F);

	/** A line of the controls: its rectangles, its label first, and whether it starts with a label (a warning above the
	 * controls is a line of its label alone). */
	private record Line(List<Component> rects, boolean labelled) {
		/** The line as the checks read it: its label, then each other rectangle in [ ]. */
		String read() {
			StringBuilder text = new StringBuilder();
			for (int i = 0; i < rects.size(); i++) {
				if (i == 0 && labelled) {
					text.append(rects.get(i).getString()).append(rects.size() > 1 ? " " : "");
				} else {
					text.append('[').append(rects.get(i).getString()).append(']');
				}
			}
			return text.toString();
		}
	}

	/** A rectangle as it's drawn: its text and its background's box, in GUI pixels. */
	private record Placed(Component text, int left, int top, int right, int bottom) {
		String box(int scale) {
			return left * scale + " " + top * scale + " " + right * scale + " " + bottom * scale;
		}
	}

	private final Supplier<MouseMode> mode;
	private final Supplier<BoxDraw.Shown> box;
	private final Supplier<ControlsLine.Line> copy;
	private final Supplier<List<ControlsLine.Line>> aim;
	private final Supplier<ControlsLine.Line> modeless;
	private final Supplier<Boolean> shape;
	private final Supplier<String> modeKey;
	private final Supplier<List<BoxList.Entry>> list;
	private final Supplier<Component> modeName;

	/**
	 * {@code mode} is the mouse mode now, or null, {@code box} the box drawn now, or null, {@code copy} the copy at the
	 * aim's line ({@link ControlsLine#copy}), or null, {@code aim} Delete by aim's lines ({@link ControlsLine#deleteAim}),
	 * {@code modeless} the line with no mouse mode on, the build modes' ({@link ControlsLine#shape}) or Disable's
	 * ({@link ControlsLine#disable}), or null, {@code shape} whether Effortless
	 * Building's shape is under way, {@code modeKey} the name of the key that switches a Delete box to Aim, or null
	 * while it has none, {@code list} the kinds of block in the box, or the marks, and {@code modeName} the name of
	 * Effortless Building's mode during a session, or null in Disable or without one.
	 */
	ModeHud(Supplier<MouseMode> mode, Supplier<BoxDraw.Shown> box, Supplier<ControlsLine.Line> copy,
		Supplier<List<ControlsLine.Line>> aim, Supplier<ControlsLine.Line> modeless, Supplier<Boolean> shape,
		Supplier<String> modeKey, Supplier<List<BoxList.Entry>> list, Supplier<Component> modeName) {
		this.mode = mode;
		this.box = box;
		this.copy = copy;
		this.aim = aim;
		this.modeless = modeless;
		this.shape = shape;
		this.modeKey = modeKey;
		this.list = list;
		this.modeName = modeName;
	}

	/** The lines to show now, from the top, or none. */
	private List<Line> lines() {
		MouseMode now = mode.get();
		if (!shown()) {
			return List.of();
		}
		List<ControlsLine.Line> lines = now == null ? modelessLines() : switch (now) {
			case BOX -> {
				BoxDraw.Shown shown = box.get();
				yield shown == null ? List.of() : ControlsLine.box(shown.purpose().name(), shown.corners(),
					shown.corners() > 0 && shown.min() != null ? BoxCorners.size(shown.min(), shown.max()) : null,
					shown.limit(), modeKey.get());
			}
			case COPY_AT_AIM -> {
				ControlsLine.Line line = copy.get();
				yield line == null ? List.of() : List.of(line);
			}
			case DELETE_AIM -> aim.get();
		};
		return lines.stream().map(line -> new Line(components(line), line.label() != null)).toList();
	}

	/** The line with no mouse mode on, the build modes' or Disable's, while it shows, or none. */
	private List<ControlsLine.Line> modelessLines() {
		ControlsLine.Line line = modeless.get();
		return line == null ? List.of() : List.of(line);
	}

	/** Whether the controls and the list show: with no screen open and the HUD shown. */
	private boolean shown() {
		Minecraft mc = Minecraft.getInstance();
		return mc.gui.screen() == null && !mc.gui.hud.isHidden();
	}

	/**
	 * Whether the lines end above the action bar: the build modes' line during a shape, while Effortless Building draws
	 * its own line where the lines otherwise end.
	 */
	private boolean raised() {
		return mode.get() == null && shape.get();
	}

	/** Whether the list under the crosshair goes with the mode now: a box's, or Delete by aim's marks. */
	private boolean listed() {
		MouseMode now = mode.get();
		if (now == null) {
			return false;
		}
		return now == MouseMode.BOX || now == MouseMode.DELETE_AIM;
	}

	/** The style white where it has no colour of its own, so a warning stays red and a rectangle's text is white. */
	private static Style white(Style style) {
		return style.getColor() == null ? style.withColor(ChatFormatting.WHITE) : style;
	}

	/** The game's component for a part of a line, in its look: an action in dark aqua and a warning in red. */
	private static MutableComponent component(ControlsLine.Text text) {
		return switch (text) {
			case ControlsLine.Translated translated -> looked(Component.translatable(translated.key(),
				translated.args().stream().map(ModeHud::component).toArray()), translated.look());
			case ControlsLine.Literal literal -> looked(Component.literal(literal.text()), literal.look());
		};
	}

	private static MutableComponent looked(MutableComponent made, ControlsLine.Look look) {
		return switch (look) {
			case PLAIN -> made;
			case ACTION -> made.withStyle(ChatFormatting.DARK_AQUA);
			case WARNING -> made.withStyle(ChatFormatting.RED);
		};
	}

	/** A line's rectangles as the game's components, its label first, white where they have no colour of their own. */
	private static List<Component> components(ControlsLine.Line line) {
		return line.all().stream().map(rect -> (Component) component(rect).withStyle(ModeHud::white)).toList();
	}

	/** The rectangles as they're drawn on a screen {@code width} by {@code height}, row by row, top first. */
	private List<List<Placed>> placed(List<Line> lines, Font font, int width, int height) {
		return placed(lines.stream().map(Line::rects).toList(), font, width, height, raised());
	}

	/**
	 * The rectangles of lines whose rectangles are {@code lines} as they're drawn on a screen {@code width} by
	 * {@code height}, row by row, top first, ending above the action bar while {@code raised}.
	 */
	private static List<List<Placed>> placed(List<List<Component>> lines, Font font, int width, int height,
		boolean raised) {
		List<List<Integer>> widths = lines.stream().map(line -> line.stream().map(font::width).toList()).toList();
		List<List<ControlsRows.Rect>> rows = ControlsRows.of(widths, width);
		int y = top(rows.size(), font, height, raised);
		List<List<Placed>> placed = new ArrayList<>(rows.size());
		for (List<ControlsRows.Rect> row : rows) {
			List<Placed> rects = new ArrayList<>(row.size());
			for (ControlsRows.Rect rect : row) {
				rects.add(new Placed(lines.get(rect.line()).get(rect.index()), rect.left(), y, rect.left() + rect.width(),
					y + font.lineHeight));
			}
			placed.add(rects);
			y += ControlsRows.pitch(font.lineHeight);
		}
		return placed;
	}

	/**
	 * Draws Effortless Building's own line during a shape in Building Planner's rectangles (hook E31,
	 * {@link ControlsLine#effortless}), where Effortless Building draws it, as the controls lines are drawn; false,
	 * drawing nothing, for a text of another form, which Effortless Building then draws itself.
	 */
	static boolean drawEffortless(GuiGraphicsExtractor graphics, Font font, List<ControlsLine.Part> parts) {
		ControlsLine.Line line = ControlsLine.effortless(parts);
		if (line == null) {
			return false;
		}
		draw(graphics, font, placed(List.of(components(line)), font, graphics.guiWidth(), graphics.guiHeight(), false));
		return true;
	}

	/** Draws the rectangles: every background first, so no rectangle covers the shadow of the text in another. */
	private static void draw(GuiGraphicsExtractor graphics, Font font, List<List<Placed>> rows) {
		for (List<Placed> row : rows) {
			for (Placed rect : row) {
				graphics.fill(rect.left(), rect.top(), rect.right(), rect.bottom(), BACKGROUND);
			}
		}
		for (List<Placed> row : rows) {
			for (Placed rect : row) {
				graphics.text(font, rect.text(), rect.left() + ControlsRows.PADDING, rect.top(), WHITE, true);
			}
		}
	}

	/** The rectangles as they're drawn now, row by row, top first. */
	private List<List<Placed>> placedNow() {
		Minecraft mc = Minecraft.getInstance();
		return placed(lines(), mc.font, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
	}

	private static int scale() {
		return (int) Minecraft.getInstance().getWindow().getGuiScale();
	}

	/**
	 * The lines' text as the player reads them, separated by " | ", each its label, then each other rectangle in [ ]; or
	 * {@code hidden}; for the in-game checks.
	 */
	String text() {
		List<Line> lines = lines();
		return lines.isEmpty() ? "hidden" : lines.stream().map(Line::read).collect(Collectors.joining(" | "));
	}

	/**
	 * The rows of rectangles as they're laid out on the screen now, separated by " / ", each rectangle in [ ]; or
	 * {@code hidden}; for the in-game checks, which see where each row breaks.
	 */
	String wrappedText() {
		List<List<Placed>> rows = placedNow();
		return rows.isEmpty() ? "hidden" : rows.stream().map(row -> row.stream()
			.map(rect -> "[" + rect.text().getString() + "]").collect(Collectors.joining()))
			.collect(Collectors.joining(" / "));
	}

	/**
	 * The window box of the rectangles as they're drawn now: left, top, right and bottom, in the window's pixels, or
	 * {@code hidden}; for the outlines on the in-game checks' shots.
	 */
	String windowBox() {
		List<List<Placed>> rows = placedNow();
		if (rows.isEmpty()) {
			return "hidden";
		}
		int left = Integer.MAX_VALUE;
		int right = Integer.MIN_VALUE;
		for (List<Placed> row : rows) {
			left = Math.min(left, row.getFirst().left());
			right = Math.max(right, row.getLast().right());
		}
		return new Placed(null, left, rows.getFirst().getFirst().top(), right, rows.getLast().getFirst().bottom())
			.box(scale());
	}

	/**
	 * Each rectangle as it's drawn now: its window box (left top right bottom, in the window's pixels) and its text in
	 * [ ], separated by " | ", and the rows by " / "; or {@code hidden}; for the in-game checks.
	 */
	String rectBoxes() {
		List<List<Placed>> rows = placedNow();
		int scale = scale();
		return rows.isEmpty() ? "hidden" : rows.stream().map(row -> row.stream()
			.map(rect -> rect.box(scale) + " [" + rect.text().getString() + "]").collect(Collectors.joining(" | ")))
			.collect(Collectors.joining(" / "));
	}

	/**
	 * The colour of each rectangle's text as drawn now, row after row, separated by spaces ("white", "red"), or
	 * {@code hidden}; an action's own colour inside a rectangle doesn't count.
	 */
	String rectColours() {
		List<List<Placed>> rows = placedNow();
		return rows.isEmpty() ? "hidden" : rows.stream().flatMap(List::stream)
			.map(rect -> rect.text().getStyle().getColor().serialize()).collect(Collectors.joining(" "));
	}

	/**
	 * The window box (left top right bottom, in the window's pixels) of the first rectangle drawn now whose text holds
	 * {@code text}, or {@code none}; for the outlines on the in-game checks' shots.
	 */
	String partBox(String text) {
		for (List<Placed> row : placedNow()) {
			for (Placed rect : row) {
				if (rect.text().getString().contains(text)) {
					return rect.box(scale());
				}
			}
		}
		return "none";
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
		int top = top(placed(lines, mc.font, width, height).size(), mc.font, height, false);
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

	/**
	 * The dark background under each row of the list under the crosshair as it shows now, its window box (left top
	 * right bottom, in the window's pixels), top row first, separated by " | "; or {@code hidden}.
	 */
	String listBackgrounds() {
		List<BoxList.Entry> entries = list.get();
		if (!listed() || !shown() || entries.isEmpty()) {
			return "hidden";
		}
		Minecraft mc = Minecraft.getInstance();
		int width = mc.getWindow().getGuiScaledWidth();
		int height = mc.getWindow().getGuiScaledHeight();
		int top = top(placed(lines(), mc.font, width, height).size(), mc.font, height, false);
		int scale = scale();
		return BoxListLayout.of(width, height, entries.size(), top).backgrounds().stream()
			.map(r -> r.left() * scale + " " + r.top() * scale + " " + r.right() * scale + " " + r.bottom() * scale)
			.collect(Collectors.joining(" | "));
	}

	/**
	 * The top of the controls: {@code count} rows, the last of them starting 58 above the screen's bottom, or while
	 * {@code raised}, ending 74 above it, over the action bar; with none, of one row there.
	 */
	private static int top(int count, Font font, int height, boolean raised) {
		int rows = Math.max(count, 1);
		return raised ? height - ABOVE_ACTION_BAR - ControlsRows.height(rows, font.lineHeight)
			: height - BOTTOM - (rows - 1) * ControlsRows.pitch(font.lineHeight);
	}

	/**
	 * Hook M8: the top of the held item's name, which the game gives as {@code drawnAt}: 2 above the top of the highest
	 * row of the lines while they show, or {@code drawnAt}.
	 */
	int itemNameTop(int drawnAt) {
		List<Line> lines = lines();
		if (lines.isEmpty()) {
			return drawnAt;
		}
		Minecraft mc = Minecraft.getInstance();
		int height = mc.getWindow().getGuiScaledHeight();
		int rows = placed(lines, mc.font, mc.getWindow().getGuiScaledWidth(), height).size();
		return drawnAt + ModeName.itemNameShift(drawnAt, mc.font.lineHeight, top(rows, mc.font, height, raised()));
	}

	/** The mode's name in its rectangle beside the hotbar while it shows, or null. */
	private Placed modeNamePlaced(Font font, int width, int height) {
		Minecraft mc = Minecraft.getInstance();
		Component name = mc.gui.hud.isHidden() || mc.player == null ? null : modeName.get();
		if (name == null) {
			return null;
		}
		Component text = Component.empty().append(name).withStyle(ChatFormatting.WHITE);
		boolean offHandLeft = !mc.player.getOffhandItem().isEmpty()
			&& mc.player.getMainArm().getOpposite() == HumanoidArm.LEFT;
		ModeName.Box box = ModeName.box(font.width(text), font.lineHeight, width, height, offHandLeft);
		return new Placed(text, box.left(), box.top(), box.right(), box.bottom());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Font font = Minecraft.getInstance().font;
		Placed name = modeNamePlaced(font, graphics.guiWidth(), graphics.guiHeight());
		if (name != null) {
			draw(graphics, font, List.of(List.of(name)));
		}
		List<Line> lines = lines();
		if (lines.isEmpty() && !(listed() && shown())) {
			return;
		}
		List<List<Placed>> rows = placed(lines, font, graphics.guiWidth(), graphics.guiHeight());
		draw(graphics, font, rows);
		if (listed()) {
			drawList(graphics, font, top(rows.size(), font, graphics.guiHeight(), raised()));
		}
	}

	/**
	 * The list under the crosshair, drawn as Effortless Building draws its own ({@code RenderHandler.drawItemStack}):
	 * each item, then its count over it, right-aligned to the item's right edge and starting 9 below its top; the "+N"
	 * cell's text sits in the middle of its cell. Each row sits on the controls' dark background.
	 */
	private void drawList(GuiGraphicsExtractor graphics, Font font, int top) {
		List<BoxList.Entry> entries = list.get();
		BoxListLayout layout = BoxListLayout.of(graphics.guiWidth(), graphics.guiHeight(), entries.size(), top);
		for (BoxListLayout.Rect rect : layout.backgrounds()) {
			graphics.fill(rect.left(), rect.top(), rect.right(), rect.bottom(), BACKGROUND);
		}
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
