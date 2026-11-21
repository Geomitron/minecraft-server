package dev.buildingplanner.input;

/**
 * How often a key of the copy's acts: once for each press, and while it's held, again after 0.2 seconds, then every
 * {@link #TICKS} ticks (5 times a second), the game's own pace for a held use button ({@link HoldRepeat}). The
 * computer's own key repeat, which the game counts as more presses, doesn't count while the key stays down.
 */
public final class KeyRepeat {
	/** The ticks between repeats, and the wait before the first: 0.2 seconds. */
	public static final int TICKS = HoldRepeat.FIRST_WAIT;

	private final HoldRepeat hold = new HoldRepeat();
	private boolean wasDown;

	/**
	 * Call once every client tick with the key's presses since the last tick and whether it's down now. Returns how many
	 * times the key acts on this tick.
	 */
	public int tick(int presses, boolean down) {
		boolean held = wasDown;
		wasDown = down;
		int repeat = hold.tick(down, TICKS) ? 1 : 0;
		return held ? repeat : presses + repeat;
	}

	/** Forgets the key's state, as when the key doesn't act; its next press acts at once. */
	public void reset() {
		hold.tick(false, TICKS);
		wasDown = false;
	}
}
