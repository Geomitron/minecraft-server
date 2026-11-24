package dev.buildingplanner.input;

/**
 * Delete's hold to erase (see "Delete"): holding the left button for one second ({@link #TICKS} ticks)
 * erases the marked ghost blocks, as holding it for a second dismantles in Satisfactory. A press starts the count, each
 * client tick the button stays down counts one, and letting go before the last erases nothing; the next press starts
 * again from nothing. Holding on past the erase erases nothing more until the button is let go and pressed again.
 */
public final class EraseHold {
	/** The ticks the left button is held to erase: one second. */
	public static final int TICKS = 20;

	/** The ticks the button has been held since the press, or -1 while no hold is counting. */
	private int held = -1;

	/** The left button went down (hook M1): the count starts. */
	public void press() {
		held = 0;
	}

	/** The hold stops counting, as when no mark is left to erase; the next press starts it again. */
	public void cancel() {
		held = -1;
	}

	/**
	 * Call once every client tick with whether the left button is down now. Returns whether the marks are erased on this
	 * tick: on the {@link #TICKS}th tick the button has stayed down since its press, and only then.
	 */
	public boolean tick(boolean down) {
		if (!down || held < 0) {
			held = -1;
			return false;
		}
		held++;
		if (held < TICKS) {
			return false;
		}
		held = -1;
		return true;
	}

	/** Whether the left button is held to erase, counting toward the erase. */
	public boolean holding() {
		return held >= 0;
	}

	/**
	 * The ticks the button has been held toward the erase, from 0 at the press to {@link #TICKS} - 1, or -1 while it
	 * isn't counting; the progress bar fills a {@link #TICKS}th for each.
	 */
	public int ticks() {
		return held;
	}
}
