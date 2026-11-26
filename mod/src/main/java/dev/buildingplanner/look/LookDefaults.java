package dev.buildingplanner.look;

import dev.buildingplanner.ClientConfig;
import java.io.IOException;

/**
 * How Building Planner sets Litematica up on a player's computer, once for each group of settings: the look of ghost
 * blocks and the tool item (ghost blocks translucent, at 0.8 opacity, and Litematica's tool item off, since the stick
 * and Litematica's tool modes play no part in planning), the drawing of area selections (off, since a selection left
 * from before has no tool to change or clear it), and the status panel's showing itself (off, since Building Planner
 * adds Litematica placements often, each of which would show the panel for 10 seconds). A setting the player has changed from Litematica's default keeps
 * the player's value, and once a group's settings are set, the player's {@link ClientConfig} holds that group's own
 * record, so a later change of the player's own is never overwritten, and a group added in a later version still
 * reaches a player whose file holds only the earlier groups' records.
 */
public final class LookDefaults {
	/** What Building Planner sets {@code renderBlocksAsTranslucent} to. */
	public static final boolean TRANSLUCENT = true;
	/** What Building Planner sets {@code ghostBlockAlpha} to. */
	public static final double GHOST_ALPHA = 0.8;
	/** What Building Planner sets {@code toolItemEnabled} to. */
	public static final boolean TOOL_ITEM = false;
	/** What Building Planner sets {@code enableAreaSelectionBoxesRendering} to. */
	public static final boolean AREA_SELECTION_RENDERING = false;
	/** What Building Planner sets {@code statusInfoHudAuto} to. */
	public static final boolean STATUS_PANEL_AUTO = false;

	/** The record of the look group (ghost blocks translucent at 0.8 opacity, tool item off); 0.3.0 and 0.4.0 wrote it too. */
	static final String LOOK_APPLIED = "look-defaults-applied";
	/** The record of the area selection group. */
	static final String AREA_SELECTION_APPLIED = "area-selection-off-applied";
	/** The record of the status panel group. */
	static final String STATUS_PANEL_APPLIED = "status-panel-off-applied";

	private LookDefaults() {
	}

	/** The value an option takes: Building Planner's when the player left it at Litematica's default, else the player's. */
	public static boolean choose(boolean current, boolean litematicaDefault, boolean ours) {
		return current == litematicaDefault ? ours : current;
	}

	/** The value an option takes: Building Planner's when the player left it at Litematica's default, else the player's. */
	public static double choose(double current, double litematicaDefault, double ours) {
		return Double.compare(current, litematicaDefault) == 0 ? ours : current;
	}

	/** Whether the player's config records that the look group was set. */
	public static boolean lookApplied(ClientConfig config) {
		return Boolean.parseBoolean(config.get(LOOK_APPLIED));
	}

	/** Records in the player's config that the look group was set. */
	public static void markLookApplied(ClientConfig config) throws IOException {
		config.set(LOOK_APPLIED, "true");
	}

	/** Whether the player's config records that the area selection group was set. */
	public static boolean areaSelectionApplied(ClientConfig config) {
		return Boolean.parseBoolean(config.get(AREA_SELECTION_APPLIED));
	}

	/** Records in the player's config that the area selection group was set. */
	public static void markAreaSelectionApplied(ClientConfig config) throws IOException {
		config.set(AREA_SELECTION_APPLIED, "true");
	}

	/** Whether the player's config records that the status panel group was set. */
	public static boolean statusPanelApplied(ClientConfig config) {
		return Boolean.parseBoolean(config.get(STATUS_PANEL_APPLIED));
	}

	/**
	 * Sets the status panel group once: unless the player's config holds its record, runs {@code setAndSave}, which
	 * turns {@code statusInfoHudAuto} off while it is at Litematica's default and saves Litematica's config, then
	 * writes the record. When {@code setAndSave} throws, the exception passes on with no record written, so the next
	 * start tries again.
	 */
	public static void setStatusPanelOnce(ClientConfig config, Runnable setAndSave) throws IOException {
		if (statusPanelApplied(config)) {
			return;
		}
		setAndSave.run();
		config.set(STATUS_PANEL_APPLIED, "true");
	}
}
