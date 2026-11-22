package dev.buildingplanner.input;

/**
 * When the one ghost block's controls act (see "One ghost block"), in Effortless Building's Single mode, and which of
 * its clicks are Building Planner's rather than Effortless Building's (hooks E14, M1 and M2).
 */
public final class SingleRule {
	private SingleRule() {
	}

	/**
	 * The one ghost block as its clicks see it: its own mode, whether it's locked, and whether a Zoop or Vertical row of
	 * it is fixed (after its first click).
	 */
	public record State(CopyMode mode, boolean locked, boolean rowFixed) {
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
	 * Whether the block is drawn at the aim (hook E30): it follows the aim in Single, neither locked nor in a Zoop or
	 * Vertical row, so the preview draws it where a right click would place it, even as a set of one block.
	 */
	public static boolean drawnAtAim(State state) {
		return state.mode() == CopyMode.SINGLE && !state.locked() && !state.rowFixed();
	}

	/**
	 * Whether the place click is Building Planner's: while the block is locked, which places it where it's locked, or in
	 * Zoop or Vertical, whose clicks fix and place a row. Following the aim in Single, it's Effortless Building's.
	 */
	public static boolean takesPlace(State state) {
		return state.locked() || state.mode() != CopyMode.SINGLE;
	}

	/**
	 * Whether the break click is Building Planner's: while the block is locked, which erases every ghost block a right
	 * click would place, or a row is fixed, which the click drops. Otherwise it's Effortless Building's, erasing at the aim.
	 */
	public static boolean takesBreak(State state) {
		return state.locked() || state.rowFixed();
	}
}
