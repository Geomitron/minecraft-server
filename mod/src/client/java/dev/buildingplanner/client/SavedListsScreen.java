package dev.buildingplanner.client;

import dev.buildingplanner.sync.Payloads;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * "Saved lists": the player's kept lists as the server last sent them, newest first, with "Last counted" always at the
 * top. Each row shows its name, when it was saved and its box's size, with "Open" and, except for "Last counted",
 * "Delete", which asks again first. "Open" asks the server for the list and opens its screen when it arrives. The rows
 * follow each index the server sends, which it sends again after each save and delete. The mouse wheel scrolls them.
 */
final class SavedListsScreen extends Screen {
	private static final int TEXT = 0xFFFFFFFF;
	private static final int GREY = 0xFFA0A0A0;
	private static final int ROW = 26;
	private static final int TOP = 24;
	private static final int BUTTON = 50;

	/** One row: the name to ask the server for ("" for "Last counted"), the name shown, and its summary. */
	private record Row(String name, Component shown, Payloads.ListSummary summary) {
	}

	private final Screen parent;
	/** The index the rows show, to rebuild them when the server sends another. */
	private Payloads.Lists shownIndex;
	private List<Row> rows = List.of();
	/** The first row shown. */
	private int scroll;

	private SavedListsScreen(Screen parent) {
		super(Component.translatable("buildingplanner.ui.saved_lists.title"));
		this.parent = parent;
	}

	/** Opens the screen over {@code parent}, to which Escape returns; does nothing while Building Planner is off. */
	static void open(Screen parent) {
		if (BuildingPlannerClient.session() != null) {
			Minecraft.getInstance().setScreenAndShow(new SavedListsScreen(parent));
		}
	}

	private int contentWidth() {
		return Math.min(width - 16, 360);
	}

	private int left() {
		return (width - contentWidth()) / 2;
	}

	/** How many rows fit on the screen. */
	private int shownRows() {
		return Math.max(1, (height - TOP - 8) / ROW);
	}

	@Override
	protected void init() {
		ClientSession session = BuildingPlannerClient.session();
		if (session == null) {
			return;
		}
		shownIndex = session.lists().index();
		List<Row> all = new ArrayList<>();
		shownIndex.lastCounted().ifPresent(summary -> all.add(new Row("",
			Component.translatable("buildingplanner.ui.saved_lists.last_counted"), summary)));
		for (Payloads.ListSummary summary : shownIndex.saved()) {
			all.add(new Row(summary.name(), Component.literal(summary.name()), summary));
		}
		rows = all;
		scroll = Math.max(0, Math.min(scroll, rows.size() - shownRows()));
		int right = left() + contentWidth();
		for (int i = scroll; i < Math.min(rows.size(), scroll + shownRows()); i++) {
			Row row = rows.get(i);
			int y = TOP + (i - scroll) * ROW + 2;
			addRenderableWidget(Button.builder(Component.translatable("buildingplanner.ui.saved_lists.open"),
				button -> open(row)).bounds(right - 2 * BUTTON - 4, y, BUTTON, 20).build());
			if (!row.name().isEmpty()) {
				addRenderableWidget(Button.builder(Component.translatable("buildingplanner.ui.saved_lists.delete"),
					button -> confirmDelete(row)).bounds(right - BUTTON, y, BUTTON, 20).build());
			}
		}
	}

	private void open(Row row) {
		ClientSession session = BuildingPlannerClient.session();
		if (session != null) {
			session.lists().open(row.name());
		}
	}

	/** "Delete" asks again; deleting, or not, comes back to this screen. */
	private void confirmDelete(Row row) {
		minecraft.setScreenAndShow(new ConfirmScreen(delete -> {
			ClientSession session = BuildingPlannerClient.session();
			if (delete && session != null) {
				session.lists().delete(row.name());
			}
			minecraft.setScreenAndShow(this);
		}, Component.translatable("buildingplanner.ui.saved_lists.delete_question", row.name()), CommonComponents.EMPTY,
			Component.translatable("buildingplanner.ui.saved_lists.delete"),
			Component.translatable("buildingplanner.ui.saved_lists.cancel")));
	}

	@Override
	public void tick() {
		ClientSession session = BuildingPlannerClient.session();
		if (session != null && session.lists().index() != shownIndex) {
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
		graphics.centeredText(font, title, width / 2, 8, TEXT);
		int left = left();
		if (rows.isEmpty()) {
			graphics.centeredText(font, Component.translatable("buildingplanner.ui.saved_lists.none"), width / 2, TOP + 8,
				GREY);
			return;
		}
		int textWidth = contentWidth() - 2 * BUTTON - 8;
		for (int i = scroll; i < Math.min(rows.size(), scroll + shownRows()); i++) {
			Row row = rows.get(i);
			int y = TOP + (i - scroll) * ROW;
			graphics.text(font, font.substrByWidth(row.shown(), textWidth).getString(), left, y + 3, TEXT);
			graphics.text(font, font.substrByWidth(details(row), textWidth).getString(), left, y + 14, GREY);
		}
	}

	/** The row's second line: when it was saved and its box's size. */
	private static Component details(Row row) {
		return Component.translatable("buildingplanner.ui.saved_lists.details",
			ListTexts.dateTime(row.summary().time()), ListTexts.size(row.summary().size()));
	}

	/** Each row, scrolled into view or not, as its name and its second line, for the in-game checks. */
	List<String> rowTexts() {
		return rows.stream().map(row -> row.shown().getString() + " / " + details(row).getString()).toList();
	}

	@Override
	public void onClose() {
		minecraft.setScreenAndShow(parent);
	}
}
