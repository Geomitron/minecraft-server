package dev.buildingplanner.look;

/**
 * The row of the radial menu's line above "Building Planner" during a local session (see "Servers without Building
 * Planner": "What works there"), in the GUI's units: one row (10) above the corner's name, which the menu draws 10 above
 * the screen's bottom. The menu draws a hovered mode's description 100 below its middle, so on a screen up to 256 high,
 * where that description would cross the line's row, the line goes a row above the description's instead.
 */
public final class RadialLine {
	/** The tallest screen, in the GUI's units, whose hovered mode's description would cross the line's row. */
	static final int SHORT_SCREEN = 256;

	private RadialLine() {
	}

	/** The line's row for a screen {@code height} high, in the GUI's units. */
	public static int row(int height) {
		return height <= SHORT_SCREEN ? height / 2 + 90 : height - 20;
	}
}
