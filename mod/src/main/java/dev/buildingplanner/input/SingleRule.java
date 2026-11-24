package dev.buildingplanner.input;

/**
 * When the one ghost block's controls act (see "One ghost block"), in Effortless Building's Single mode, and which of
 * its clicks are Building Planner's rather than Effortless Building's (hooks E14, M1 and M2).
 */
public final class SingleRule {
	private SingleRule() {
	}

	/**
	 * The one ghost block as its clicks see it: its own mode, and whether a Zoop or Vertical row of it is fixed (after
	 * its first click).
	 */
	public record State(CopyMode mode, boolean rowFixed) {
	}

	/**
	 * Whether the block's controls are active: Building Planner has a session, Effortless Building's mode is Single, no
	 * mouse mode is on, and the main hand holds an item that places a block.
	 */
	public static boolean active(boolean session, boolean singleMode, boolean mouseMode, boolean placeable) {
		return session && singleMode && !mouseMode && placeable;
	}

	/**
	 * Whether the turning controls are active (Left Ctrl with the wheel and the middle click, see "One ghost block":
	 * "Turning and flipping"): under the block's conditions but in every one of Effortless Building's modes except
	 * Disable, and not while a shape drawn with the break click is under way ({@code breakShape}), since that shape
	 * erases.
	 */
	public static boolean turning(boolean session, boolean disableMode, boolean mouseMode, boolean placeable,
		boolean breakShape) {
		return session && !disableMode && !mouseMode && placeable && !breakShape;
	}

	/**
	 * Whether the place click is Building Planner's: in Zoop or Vertical, whose clicks fix and place a row. In Single it's
	 * Effortless Building's, locked or not (hook E32 hands it a locked position).
	 */
	public static boolean takesPlace(State state) {
		return state.mode() != CopyMode.SINGLE;
	}

	/**
	 * Whether the break click is Building Planner's: while a row is fixed, which the click drops. Otherwise it's
	 * Effortless Building's, erasing at the aim or at a locked position.
	 */
	public static boolean takesBreak(State state) {
		return state.rowFixed();
	}
}
