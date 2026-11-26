package dev.buildingplanner.client;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/**
 * What a screen shows, for the driver's reading of the radial menu's corner text, of Controls and of Effortless
 * Building's line during a shape (step9.sh): the plain text and the components drawn in the last frame (recorded by
 * {@code GuiTextMixin}), and the headings and keys of the key binds screen, with where they are in the window.
 */
public final class ScreenProbe {
	/** The height of a line of the game's text, in GUI pixels. */
	private static final int FONT_HEIGHT = 9;

	/** A string drawn as plain text at {@code x}, {@code y}, in GUI pixels, with its width. */
	private record Drawn(String text, int x, int y, int width) {
	}

	/** A component drawn at {@code x}, {@code y}, in GUI pixels, with its width. */
	private record DrawnComponent(Component text, int x, int y, int width) {
	}

	/** The strings of the frame being drawn, and of the last frame drawn. Only the game's render thread touches them. */
	private static List<Drawn> current = new ArrayList<>();
	private static List<Drawn> last = List.of();
	/** The same for the components. */
	private static List<DrawnComponent> currentComponents = new ArrayList<>();
	private static List<DrawnComponent> lastComponents = List.of();
	/** The same for the tooltips set for the next frame, each its lines joined by spaces. */
	private static List<String> currentTooltips = new ArrayList<>();
	private static List<String> lastTooltips = List.of();
	/** The same for each tooltip background drawn, as its visible frame: left, top, right and bottom in GUI pixels. */
	private static List<int[]> currentTooltipBackgrounds = new ArrayList<>();
	private static List<int[]> lastTooltipBackgrounds = List.of();

	private ScreenProbe() {
	}

	/** A string is being drawn as plain text. */
	public static void drawn(Font font, String text, int x, int y) {
		current.add(new Drawn(text, x, y, font.width(text)));
	}

	/** A component is being drawn. */
	public static void drawn(Font font, Component text, int x, int y) {
		currentComponents.add(new DrawnComponent(text, x, y, font.width(text)));
	}

	/** A tooltip of these lines is being set for the next frame. */
	public static void tooltip(List<Component> lines) {
		currentTooltips.add(lines.stream().map(Component::getString).collect(Collectors.joining(" ")));
	}

	/**
	 * A tooltip background is being drawn around content {@code width} by {@code height} at {@code x}, {@code y}; its
	 * frame shows 3 outside that ({@code TooltipRenderUtil.extractTooltipBackground}).
	 */
	public static void tooltipBackground(int x, int y, int width, int height) {
		currentTooltipBackgrounds.add(new int[] {x - 3, y - 3, x + width + 3, y + height + 3});
	}

	/** A frame has been drawn. */
	public static void frameDrawn() {
		last = current;
		current = new ArrayList<>();
		lastComponents = currentComponents;
		currentComponents = new ArrayList<>();
		lastTooltips = currentTooltips;
		currentTooltips = new ArrayList<>();
		lastTooltipBackgrounds = currentTooltipBackgrounds;
		currentTooltipBackgrounds = new ArrayList<>();
	}

	/**
	 * Every text and tooltip background the last frame drew, one a line, for outlining them in a screenshot: "text",
	 * "component" or "tooltip", then the window box (left top right bottom), then the text.
	 */
	static List<String> drawnAll(Minecraft mc) {
		double scale = mc.getWindow().getGuiScale();
		List<String> out = new ArrayList<>();
		for (Drawn d : last) {
			out.add("text " + box(scale, d.x(), d.y(), d.x() + d.width(), d.y() + FONT_HEIGHT) + " " + d.text());
		}
		for (DrawnComponent d : lastComponents) {
			out.add("component " + box(scale, d.x(), d.y(), d.x() + d.width(), d.y() + FONT_HEIGHT) + " "
				+ d.text().getString());
		}
		for (int[] b : lastTooltipBackgrounds) {
			out.add("tooltip " + box(scale, b[0], b[1], b[2], b[3]));
		}
		return out;
	}

	/**
	 * The components the last frame drew with their top 58 GUI pixels above the screen's bottom, where Effortless
	 * Building draws its own line during a shape (hook E31 draws it there in rectangles), left first.
	 */
	private static List<DrawnComponent> shapeRow(Minecraft mc) {
		int top = mc.getWindow().getGuiScaledHeight() - 58;
		return lastComponents.stream().filter(d -> d.y() == top).sorted(Comparator.comparingInt(DrawnComponent::x))
			.toList();
	}

	/** The components of Effortless Building's line as the last frame drew them, each in [ ], or none. */
	static String shapeLine(Minecraft mc) {
		List<DrawnComponent> row = shapeRow(mc);
		return row.isEmpty() ? "none"
			: row.stream().map(d -> "[" + d.text().getString() + "]").collect(Collectors.joining());
	}

	/**
	 * The colours of each component of Effortless Building's line as the last frame drew them, its runs of one colour
	 * joined by "+" ("white+dark_aqua"), the components separated by spaces, or none.
	 */
	static String shapeLineColours(Minecraft mc) {
		List<DrawnComponent> row = shapeRow(mc);
		return row.isEmpty() ? "none" : row.stream().map(d -> {
			List<String> runs = new ArrayList<>();
			d.text().visit((style, string) -> {
				String colour = style.getColor() == null ? "none" : style.getColor().serialize();
				if (!string.isEmpty() && (runs.isEmpty() || !runs.getLast().equals(colour))) {
					runs.add(colour);
				}
				return Optional.empty();
			}, Style.EMPTY);
			return String.join("+", runs);
		}).collect(Collectors.joining(" "));
	}

	/**
	 * Each rectangle of Effortless Building's line as the last frame drew it, as the driver's moderects gives the
	 * controls': its window box (its text with 2 GUI pixels more at each side, one line high) and its text in [ ],
	 * separated by " | ", or none.
	 */
	static String shapeLineRects(Minecraft mc) {
		List<DrawnComponent> row = shapeRow(mc);
		double scale = mc.getWindow().getGuiScale();
		return row.isEmpty() ? "none" : row.stream().map(d -> box(scale, d.x() - 2, d.y(), d.x() + d.width() + 2,
			d.y() + FONT_HEIGHT) + " [" + d.text().getString() + "]").collect(Collectors.joining(" | "));
	}

	/** Each tooltip the last frame set, in the order set, separated by " | ", or none. */
	static String tooltips() {
		return lastTooltips.isEmpty() ? "none" : String.join(" | ", lastTooltips);
	}

	/**
	 * The window box (left top right bottom: its text's own, one line high) of each component the last frame drew whose
	 * text is {@code text}, separated by " | ", or none; as the held item's name and the mode's name beside the hotbar.
	 */
	static String drawnBoxes(Minecraft mc, String text) {
		double scale = mc.getWindow().getGuiScale();
		List<String> boxes = lastComponents.stream().filter(d -> d.text().getString().equals(text))
			.map(d -> box(scale, d.x(), d.y(), d.x() + d.width(), d.y() + FONT_HEIGHT)).toList();
		return boxes.isEmpty() ? "none" : String.join(" | ", boxes);
	}

	/**
	 * The window box (left top right bottom: its text's own, one line high) of each string the last frame drew as plain
	 * text that is {@code text}, separated by " | ", or none; as the radial menu's options' headings.
	 */
	static String drawnTextBoxes(Minecraft mc, String text) {
		double scale = mc.getWindow().getGuiScale();
		List<String> boxes = last.stream().filter(d -> d.text().equals(text))
			.map(d -> box(scale, d.x(), d.y(), d.x() + d.width(), d.y() + FONT_HEIGHT)).toList();
		return boxes.isEmpty() ? "none" : String.join(" | ", boxes);
	}

	/** Each string drawn in the last frame, separated by " | ". */
	static String drawnTexts() {
		return last.stream().map(Drawn::text).collect(Collectors.joining(" | "));
	}

	/**
	 * The string the last frame drew at the screen's right, bottom corner, as Effortless Building's radial menu draws its
	 * name (right-aligned, 4 from the edge, 10 above the bottom), then its window box (left top right bottom), or none.
	 */
	static String cornerText(Minecraft mc) {
		Screen screen = mc.gui.screen();
		double scale = mc.getWindow().getGuiScale();
		for (Drawn d : last) {
			if (d.y() == screen.height - 10 && d.x() + d.width() == screen.width - 4) {
				return d.text() + " | " + box(scale, d.x(), d.y(), d.x() + d.width(), d.y() + FONT_HEIGHT);
			}
		}
		return "none";
	}

	/**
	 * The text of the widget on the open screen to the right of the label the last frame drew as {@code label}, on its
	 * row (the label's top within the widget's height), then the widget's centre in window pixels; or none.
	 */
	static String settingRow(Minecraft mc, String label) {
		double scale = mc.getWindow().getGuiScale();
		for (DrawnComponent d : lastComponents) {
			if (!d.text().getString().equals(label)) {
				continue;
			}
			for (GuiEventListener child : mc.gui.screen().children()) {
				if (child instanceof AbstractWidget w && w.visible && w.getX() > d.x() + d.width() && w.getY() <= d.y()
					&& d.y() < w.getY() + w.getHeight()) {
					return w.getMessage().getString() + " " + (int) ((w.getX() + w.getWidth() / 2.0) * scale) + " "
						+ (int) ((w.getY() + w.getHeight() / 2.0) * scale);
				}
			}
		}
		return "none";
	}

	/** Opens the key binds screen, as Options and then Controls do. */
	static void openKeyBinds(Minecraft mc) {
		mc.setScreenAndShow(new KeyBindsScreen(mc.gui.screen(), mc.options));
	}

	/** The key binds screen's headings, each with its keys in the order shown: "Heading: key, key ; Heading: key". */
	static String keyBinds(Minecraft mc) throws Exception {
		List<String> groups = new ArrayList<>();
		List<String> keys = new ArrayList<>();
		String heading = null;
		for (KeyBindsList.Entry entry : list(mc).children()) {
			if (entry instanceof KeyBindsList.CategoryEntry) {
				if (heading != null) {
					groups.add(heading + ": " + String.join(", ", keys));
				}
				heading = headingOf(entry);
				keys.clear();
			} else {
				keys.add(((Component) field(entry, "name")).getString());
			}
		}
		if (heading != null) {
			groups.add(heading + ": " + String.join(", ", keys));
		}
		return String.join(" ; ", groups);
	}

	/**
	 * Scrolls the key binds screen so the heading is at the top of its list, as far as the list allows, and gives the
	 * window boxes (left top right bottom) of the heading's row and of the heading with its keys.
	 */
	static String scrollKeyBinds(Minecraft mc, String heading) throws Exception {
		KeyBindsList list = list(mc);
		List<KeyBindsList.Entry> entries = list.children();
		int first = -1;
		int end = entries.size();
		for (int i = 0; i < entries.size(); i++) {
			if (entries.get(i) instanceof KeyBindsList.CategoryEntry) {
				if (first >= 0) {
					end = i;
					break;
				}
				if (headingOf(entries.get(i)).equals(heading)) {
					first = i;
				}
			}
		}
		if (first < 0) {
			return "no heading " + heading;
		}
		list.setScrollAmount(first * (list.getRowBottom(0) - list.getRowTop(0)));
		double scale = mc.getWindow().getGuiScale();
		int left = list.getRowLeft();
		int right = list.getRowRight();
		return "heading " + box(scale, left, list.getRowTop(first), right, list.getRowBottom(first))
			+ " ; with its keys " + box(scale, left, list.getRowTop(first), right, list.getRowBottom(end - 1));
	}

	/**
	 * The window boxes (left top right bottom) of the key binds list's visible area, then of each key's row under the
	 * heading, as the list is scrolled now: "list l t r b ; Name: l t r b ; ...". A row out of view has a box outside
	 * the list's.
	 */
	static String keyRows(Minecraft mc, String heading) throws Exception {
		KeyBindsList list = list(mc);
		double scale = mc.getWindow().getGuiScale();
		List<String> rows = new ArrayList<>();
		rows.add("list " + box(scale, list.getRowLeft(), list.getY(), list.getRowRight(), list.getBottom()));
		boolean under = false;
		List<KeyBindsList.Entry> entries = list.children();
		for (int i = 0; i < entries.size(); i++) {
			KeyBindsList.Entry entry = entries.get(i);
			if (entry instanceof KeyBindsList.CategoryEntry) {
				under = headingOf(entry).equals(heading);
			} else if (under) {
				rows.add(((Component) field(entry, "name")).getString() + ": "
					+ box(scale, list.getRowLeft(), list.getRowTop(i), list.getRowRight(), list.getRowBottom(i)));
			}
		}
		return String.join(" ; ", rows);
	}

	private static KeyBindsList list(Minecraft mc) throws Exception {
		return (KeyBindsList) field(mc.gui.screen(), "keyBindsList");
	}

	private static String headingOf(Object categoryEntry) throws Exception {
		return ((MultiLineTextWidget) field(categoryEntry, "categoryName")).getMessage().getString();
	}

	private static Object field(Object owner, String name) throws Exception {
		Field f = owner.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(owner);
	}

	/** A box in GUI pixels as its window box: left top right bottom. */
	private static String box(double scale, int left, int top, int right, int bottom) {
		return String.join(" ", String.valueOf((int) (left * scale)), String.valueOf((int) (top * scale)),
			String.valueOf((int) (right * scale)), String.valueOf((int) (bottom * scale)));
	}
}
