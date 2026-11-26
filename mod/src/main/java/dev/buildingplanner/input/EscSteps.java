package dev.buildingplanner.input;

/**
 * Esc's steps out of the modes (see "Esc"; Geo's comments c10 and c17): with no screen open, each press of Esc takes
 * the first step that applies, and pauses the game only when none is left (hook M7).
 */
public final class EscSteps {
	private EscSteps() {
	}

	/** A step Esc takes, in the order they apply. */
	public enum Step {
		/** A box for Copy or the material list ends, as its right click ends it. */
		END_BOX,
		/** Delete's box, being drawn with the sprint key, ends and marks nothing. */
		END_DELETE_BOX,
		/** Delete's marks clear, as its right click clears them. */
		CLEAR_MARKS,
		/** Delete, with no marks, ends. */
		END_DELETE,
		/** The copy's set, after its first click, drops, and the copy follows the aim in the same mode (1238 Q3 A). */
		DROP_SET,
		/** The copy at the aim, with no set fixed, is put away. */
		PUT_COPY_AWAY,
		/** A shape under way in a build mode is cancelled, as Effortless Building's left click cancels it. */
		CANCEL_SHAPE,
		/** A build mode other than Disable becomes Disable, which also ends its lock. */
		DISABLE_BUILD_MODE,
		/** No step is left: the game pauses. */
		PAUSE
	}

	/**
	 * The modes' state Esc steps out of: Building Planner's mouse mode, or null; in Delete, whether its box is drawn and
	 * whether any ghost block is marked; with the copy at the aim, whether a set is fixed; whether a shape is under way;
	 * and whether Effortless Building's mode is one other than Disable.
	 */
	public record State(MouseMode mouseMode, boolean deleteBox, boolean marked, boolean setFixed,
		boolean shapeUnderWay, boolean buildMode) {
	}

	/** The step the next press of Esc takes. */
	public static Step next(State state) {
		if (state.mouseMode() != null) {
			return switch (state.mouseMode()) {
				case BOX -> Step.END_BOX;
				case DELETE -> state.deleteBox() ? Step.END_DELETE_BOX
					: state.marked() ? Step.CLEAR_MARKS : Step.END_DELETE;
				case COPY_AT_AIM -> state.setFixed() ? Step.DROP_SET : Step.PUT_COPY_AWAY;
			};
		}
		if (state.shapeUnderWay()) {
			return Step.CANCEL_SHAPE;
		}
		return state.buildMode() ? Step.DISABLE_BUILD_MODE : Step.PAUSE;
	}
}
