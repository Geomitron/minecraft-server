package dev.buildingplanner.look;

/**
 * Where the build mode's name and the held item's name go, in GUI pixels (see "The build mode's name and the held
 * item's name"). The game draws the hotbar 182 wide, centred, along the screen's bottom, 22 high, and the off hand's
 * slot 29 wide beside it, on the left while the player's main arm is the right one ({@code Hud.extractItemHotbar}).
 */
public final class ModeName {
	/** The distance between the name's rectangle and the hotbar, or the off hand's slot. */
	public static final int GAP = 4;
	/** The space between the held item's name and the top of the highest row of the lines above the hotbar. */
	public static final int ABOVE_LINES = 2;
	private static final int HOTBAR_HALF = 91;
	private static final int HOTBAR_HEIGHT = 22;
	private static final int OFF_HAND_SLOT = 29;

	private ModeName() {
	}

	/** The name's rectangle: its background's edges, the text starting {@link ControlsRows#PADDING} right of its left. */
	public record Box(int left, int top, int right, int bottom) {
	}

	/**
	 * The rectangle of a name {@code textWidth} wide, in text {@code lineHeight} high, on a screen {@code width} by
	 * {@code height}: with the controls' padding at each side, ending 4 left of the hotbar, or 4 left of the off hand's
	 * slot while {@code offHandLeft} (the off hand holds an item and its slot shows on the hotbar's left), and centred on
	 * the hotbar's height.
	 */
	public static Box box(int textWidth, int lineHeight, int width, int height, boolean offHandLeft) {
		int right = width / 2 - HOTBAR_HALF - (offHandLeft ? OFF_HAND_SLOT : 0) - GAP;
		int top = height - HOTBAR_HEIGHT + (HOTBAR_HEIGHT - lineHeight) / 2;
		return new Box(right - textWidth - 2 * ControlsRows.PADDING, top, right, top + lineHeight);
	}

	/**
	 * How far down the held item's name moves from {@code drawnAt}, the top the game gives its text {@code lineHeight}
	 * high, so that it ends 2 above {@code linesTop}, the top of the highest row of Building Planner's lines; negative,
	 * since the name moves up.
	 */
	public static int itemNameShift(int drawnAt, int lineHeight, int linesTop) {
		return linesTop - ABOVE_LINES - lineHeight - drawnAt;
	}
}
