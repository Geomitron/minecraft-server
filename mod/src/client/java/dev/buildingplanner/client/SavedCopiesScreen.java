package dev.buildingplanner.client;

import dev.buildingplanner.plan.CopyHistory;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * "Saved copies" (see "Saved copies"), laid out as "Saved lists": under the title a line saying which copies stay, then
 * a row for each saved copy, newest first, with its name, its size and number of blocks, and "Paste", "Rename" and
 * "Delete", which asks again first. "Paste" closes the screen and brings the copy to the aim. The rows follow the list
 * as the background thread leaves it after each change. The mouse wheel scrolls them. "Close" at the bottom right goes
 * back to the game as Escape does, and the rows stop above it.
 */
final class SavedCopiesScreen extends Screen {
	private static final int TEXT = 0xFFFFFFFF;
	private static final int GREY = 0xFFA0A0A0;
	private static final int ROW = 26;
	/** Where the line under the title starts, and the space between its lines when it wraps. */
	private static final int LINE_TOP = 20;
	private static final int LINE_HEIGHT = 10;
	private static final int BUTTON = 50;
	/** The "Close" button's distance from the screen's right edge and bottom, as on "Saved lists". */
	private static final int CLOSE_RIGHT = 10;
	private static final int CLOSE_BOTTOM = 26;

	/** The list the rows show, to rebuild them when the background thread leaves another. */
	private CopyHistory shown;
	private List<CopyHistory.Saved> rows = List.of();
	/** The line under the title, wrapped to the rows' width. */
	private List<String> line = List.of();
	/** The first row shown. */
	private int scroll;

	private SavedCopiesScreen() {
		super(Component.translatable("buildingplanner.ui.saved_copies.title"));
	}

	/** The key: opens the screen over the game; does nothing while Building Planner is off or a screen is open. */
	static void open() {
		Minecraft mc = Minecraft.getInstance();
		if (BuildingPlannerClient.session() != null && SavedCopies.get() != null && mc.gui.screen() == null) {
			mc.setScreenAndShow(new SavedCopiesScreen());
		}
	}

	private int contentWidth() {
		return Math.min(width - 16, 400);
	}

	private int left() {
		return (width - contentWidth()) / 2;
	}

	/** Where the rows start, below the line under the title. */
	private int top() {
		return LINE_TOP + line.size() * LINE_HEIGHT + 4;
	}

	/** How many rows fit on the screen above "Close". */
	private int shownRows() {
		return Math.max(1, (height - CLOSE_BOTTOM - 4 - top()) / ROW);
	}

	@Override
	protected void init() {
		line = wrap(font, Component.translatable("buildingplanner.ui.saved_copies.line").getString(), contentWidth());
		Component close = Component.translatable("buildingplanner.ui.saved_copies.close");
		int closeWidth = font.width(close) + 20;
		addRenderableWidget(Button.builder(close, button -> onClose())
			.bounds(width - closeWidth - CLOSE_RIGHT, height - CLOSE_BOTTOM, closeWidth, 20).build());
		SavedCopies copies = SavedCopies.get();
		if (copies == null) {
			return;
		}
		shown = copies.list();
		rows = shown.copies();
		scroll = Math.max(0, Math.min(scroll, rows.size() - shownRows()));
		int right = left() + contentWidth();
		for (int i = scroll; i < Math.min(rows.size(), scroll + shownRows()); i++) {
			CopyHistory.Saved row = rows.get(i);
			int y = top() + (i - scroll) * ROW + 2;
			addRenderableWidget(Button.builder(Component.translatable("buildingplanner.ui.saved_copies.paste"),
				button -> paste(row)).bounds(right - 3 * BUTTON - 8, y, BUTTON, 20).build());
			addRenderableWidget(Button.builder(Component.translatable("buildingplanner.ui.saved_copies.rename"),
				button -> RenameCopyScreen.open(this, row)).bounds(right - 2 * BUTTON - 4, y, BUTTON, 20).build());
			addRenderableWidget(Button.builder(Component.translatable("buildingplanner.ui.saved_copies.delete"),
				button -> confirmDelete(row)).bounds(right - BUTTON, y, BUTTON, 20).build());
		}
	}

	/** The text in lines no wider than {@code width}, broken at spaces where it can be. */
	private static List<String> wrap(Font font, String text, int width) {
		List<String> lines = new ArrayList<>();
		String rest = text;
		while (font.width(rest) > width) {
			int fits = font.substrByWidth(Component.literal(rest), width).getString().length();
			int space = rest.lastIndexOf(' ', fits);
			int end = space > 0 ? space : Math.max(1, fits);
			lines.add(rest.substring(0, end));
			rest = rest.substring(end).stripLeading();
		}
		lines.add(rest);
		return lines;
	}

	/** "Paste" closes the screen, then the copy comes to the aim once its file is read. */
	private void paste(CopyHistory.Saved row) {
		onClose();
		SavedCopies copies = SavedCopies.get();
		if (copies != null) {
			copies.pick(row.file());
		}
	}

	/** "Delete" asks again; deleting, or not, comes back to this screen. */
	private void confirmDelete(CopyHistory.Saved row) {
		minecraft.setScreenAndShow(new ConfirmScreen(delete -> {
			SavedCopies copies = SavedCopies.get();
			if (delete && copies != null) {
				copies.delete(row.file());
			}
			minecraft.setScreenAndShow(this);
		}, Component.translatable("buildingplanner.ui.saved_copies.delete_question", row.name()), CommonComponents.EMPTY,
			Component.translatable("buildingplanner.ui.saved_copies.delete"),
			Component.translatable("buildingplanner.ui.saved_copies.cancel")));
	}

	@Override
	public void tick() {
		SavedCopies copies = SavedCopies.get();
		if (copies != null && copies.list() != shown) {
			rebuildWidgets();
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		int next = Math.max(0, Math.min(scroll - (int) Math.signum(vertical), rows.size() - shownRows()));
		if (next != scroll) {
			scroll = next;
			rebuildWidgets();
		}
		return true;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(font, title.getString(), width / 2, 8, TEXT);
		for (int i = 0; i < line.size(); i++) {
			graphics.centeredText(font, line.get(i), width / 2, LINE_TOP + i * LINE_HEIGHT, GREY);
		}
		int top = top();
		if (rows.isEmpty()) {
			graphics.centeredText(font, Component.translatable("buildingplanner.ui.saved_copies.none").getString(),
				width / 2, top + 8, GREY);
			return;
		}
		int left = left();
		int textWidth = contentWidth() - 3 * BUTTON - 12;
		for (int i = scroll; i < Math.min(rows.size(), scroll + shownRows()); i++) {
			CopyHistory.Saved row = rows.get(i);
			int y = top + (i - scroll) * ROW;
			graphics.text(font, font.substrByWidth(Component.literal(row.name()), textWidth).getString(), left, y + 3,
				TEXT);
			graphics.text(font, font.substrByWidth(details(row), textWidth).getString(), left, y + 14, GREY);
		}
	}

	/** The row's second line: the copy's size and its number of blocks. */
	private static Component details(CopyHistory.Saved row) {
		Component size = ListTexts.size(row.size());
		return row.blocks() == 1 ? Component.translatable("buildingplanner.ui.saved_copies.details_one", size)
			: Component.translatable("buildingplanner.ui.saved_copies.details", size, row.blocks());
	}

	/** Each row, scrolled into view or not, as its name and its second line, for the in-game checks. */
	List<String> rowTexts() {
		return rows.stream().map(row -> row.name() + " / " + details(row).getString()).toList();
	}

	/** The first row shown and how many show, for the in-game checks. */
	String scrolled() {
		return scroll + " " + Math.min(rows.size() - scroll, shownRows());
	}
}
