package dev.buildingplanner.client;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.input.GhostMatch;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;

/**
 * The "Match ghost blocks" row in Effortless Building's settings screen, "Settings" (hook E36, see "Settings"): its
 * label after the screen's own rows, its switch where the screen puts its own, and the setting the screen's "Save"
 * button saves. The screen lays its rows out from the number of its labels, by the sizes below, which it keeps in
 * constants of its own ({@code ClientConfigScreen}'s {@code PANEL_W}, {@code ROW_H}, {@code FIELD_W} and
 * {@code FIELD_H}).
 */
public final class VisualSettingsRow {
	/** The row's label; the screen shows its tooltip from the same key with {@code .tooltip} added. */
	public static final String LABEL = "buildingplanner.ui.visual_settings.match_ghost_turn";
	private static final int PANEL_WIDTH = 320;
	private static final int ROW_HEIGHT = 24;
	/** The space the screen leaves below its rows, before its "Save" and "Cancel" buttons. */
	private static final int BELOW_ROWS = 40;
	public static final int SWITCH_WIDTH = 100;
	public static final int SWITCH_HEIGHT = 20;
	/** How far the screen's switches end from the panel's right edge. */
	private static final int SWITCH_RIGHT = 10;

	private VisualSettingsRow() {
	}

	/** The screen's labels with the row's after them, so the screen lays out, labels and explains one row more. */
	public static String[] withRow(String[] labels) {
		String[] all = Arrays.copyOf(labels, labels.length + 1);
		all[labels.length] = LABEL;
		return all;
	}

	/** The switch's left edge on a screen {@code width} wide: where the screen puts its own switches. */
	public static int switchX(int width) {
		return (width - PANEL_WIDTH) / 2 + PANEL_WIDTH - SWITCH_WIDTH - SWITCH_RIGHT;
	}

	/** The row's top on a screen {@code height} high with {@code rows} rows, the last of which is this row. */
	public static int rowY(int height, int rows) {
		return (height - (rows * ROW_HEIGHT + BELOW_ROWS)) / 2 + (rows - 1) * ROW_HEIGHT;
	}

	/**
	 * Moves the screen's widgets below its rows ("Save" and "Cancel"), which it placed for one row fewer, a row down,
	 * below this row at {@code rowY}.
	 */
	public static void makeRoom(List<? extends GuiEventListener> children, int rowY) {
		for (GuiEventListener child : children) {
			if (child instanceof AbstractWidget widget && widget.getY() >= rowY) {
				widget.setY(widget.getY() + ROW_HEIGHT);
			}
		}
	}

	/** Whether the setting is on, as saved. */
	public static boolean saved() {
		return BuildingPlannerClient.matchGhost();
	}

	/** The screen's "Save" button: saves the setting with the screen's own. */
	public static void save(boolean on) {
		try {
			BuildingPlannerClient.config().set(GhostMatch.SETTING, GhostMatch.saved(on));
		} catch (IOException e) {
			BuildingPlanner.LOGGER.warn("Couldn't save \"Match ghost blocks\"", e);
		}
	}
}
