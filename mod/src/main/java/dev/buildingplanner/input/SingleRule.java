package dev.buildingplanner.input;

/**
 * When the turning controls act (see "Locking and the first click": "Turning and flipping") in Effortless Building's
 * modes other than Disable.
 */
public final class SingleRule {
	private SingleRule() {
	}

	/**
	 * Whether the turning controls are active (Left Ctrl with the wheel and the middle click): Building Planner has a
	 * session, Effortless Building's mode isn't Disable, no mouse mode is on, the main hand holds an item that places a
	 * block, and no shape drawn with the break click is under way ({@code breakShape}), since that shape erases.
	 */
	public static boolean turning(boolean session, boolean disableMode, boolean mouseMode, boolean placeable,
		boolean breakShape) {
		return session && !disableMode && !mouseMode && placeable && !breakShape;
	}
}
