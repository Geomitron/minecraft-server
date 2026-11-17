package dev.buildingplanner.look;

/**
 * The size of Building Planner's small text (the mix panel, and the radial menu's speed slider labels) beside the
 * game's own: the smallest at or above 70% at which each of the font's pixels covers a whole number of screen pixels,
 * since at 70% they come out uneven and hard to read. That is full size at GUI scales 1 to 3, 75% at 4, 80% at 5 and
 * 5/6 at 6.
 */
public final class SmallText {
	private static final double LEAST = 0.7;

	private SmallText() {
	}

	/** The size at that GUI scale (screen pixels to a GUI pixel). */
	public static float scale(int guiScale) {
		return (float) Math.ceil(guiScale * LEAST - 1e-9) / guiScale;
	}
}
