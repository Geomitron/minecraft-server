package dev.buildingplanner.client;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.input.GhostMatch;
import dev.buildingplanner.input.RealPlace;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;

/**
 * Building Planner's rows in Effortless Building's settings screen, "Settings" (hook E36, see "Settings"): "Match
 * ghost blocks", then "Place real blocks" (see "Place real blocks"), their labels after the screen's own rows, their
 * switches where the screen puts its own, and the settings the screen's "Save" button saves. The screen lays its rows
 * out from the number of its labels, by the sizes below, which it keeps in constants of its own
 * ({@code ClientConfigScreen}'s {@code PANEL_W}, {@code ROW_H}, {@code FIELD_W} and {@code FIELD_H}).
 */
public enum VisualSettingsRow {
	MATCH_GHOST("buildingplanner.ui.visual_settings.match_ghost_turn", GhostMatch.SETTING, "Match ghost blocks"),
	PLACE_REAL("buildingplanner.ui.visual_settings.place_real_blocks", RealPlace.SETTING, "Place real blocks");

	private static final List<VisualSettingsRow> ROWS = List.of(values());
	private static final int PANEL_WIDTH = 320;
	private static final int ROW_HEIGHT = 24;
	/** The space the screen leaves below its rows, before its "Save" and "Cancel" buttons. */
	private static final int BELOW_ROWS = 40;
	public static final int SWITCH_WIDTH = 100;
	public static final int SWITCH_HEIGHT = 20;
	/** How far the screen's switches end from the panel's right edge. */
	private static final int SWITCH_RIGHT = 10;

	/** The row's label; the screen shows its tooltip from the same key with {@code .tooltip} added. */
	public final String label;
	private final String setting;
	private final String name;

	VisualSettingsRow(String label, String setting, String name) {
		this.label = label;
		this.setting = setting;
		this.name = name;
	}

	/** The rows, in the order they show. */
	public static List<VisualSettingsRow> rows() {
		return ROWS;
	}

	/** The screen's labels with the rows' after them, so the screen lays out, labels and explains the rows too. */
	public static String[] withRows(String[] labels) {
		String[] all = new String[labels.length + ROWS.size()];
		System.arraycopy(labels, 0, all, 0, labels.length);
		for (VisualSettingsRow row : ROWS) {
			all[labels.length + row.ordinal()] = row.label;
		}
		return all;
	}

	/** The switch's left edge on a screen {@code width} wide: where the screen puts its own switches. */
	public static int switchX(int width) {
		return (width - PANEL_WIDTH) / 2 + PANEL_WIDTH - SWITCH_WIDTH - SWITCH_RIGHT;
	}

	/**
	 * This row's top on a screen {@code height} high with {@code own} rows of the screen's own before Building
	 * Planner's.
	 */
	public int rowY(int height, int own) {
		int rows = own + ROWS.size();
		return (height - (rows * ROW_HEIGHT + BELOW_ROWS)) / 2 + (own + ordinal()) * ROW_HEIGHT;
	}

	/**
	 * Moves the screen's widgets below its rows ("Save" and "Cancel"), which it placed for its own rows alone, down
	 * by Building Planner's rows, below the first of them at {@code firstY}.
	 */
	public static void makeRoom(List<? extends GuiEventListener> children, int firstY) {
		for (GuiEventListener child : children) {
			if (child instanceof AbstractWidget widget && widget.getY() >= firstY) {
				widget.setY(widget.getY() + ROWS.size() * ROW_HEIGHT);
			}
		}
	}

	/** Whether the row's setting is on, as saved; it starts off. */
	public boolean saved() {
		return this == MATCH_GHOST ? BuildingPlannerClient.matchGhost() : BuildingPlannerClient.placeReal();
	}

	/** The screen's "Save" button: saves the row's setting with the screen's own. */
	public void save(boolean on) {
		try {
			BuildingPlannerClient.config().set(setting, this == MATCH_GHOST ? GhostMatch.saved(on) : RealPlace.saved(on));
		} catch (IOException e) {
			BuildingPlanner.LOGGER.warn("Couldn't save \"{}\"", name, e);
		}
	}
}
