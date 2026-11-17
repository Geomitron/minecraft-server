package dev.buildingplanner.input;

/**
 * Repeats a click while its button stays down, every so many ticks (the player's {@link RepeatSpeed}). The tick that
 * finds the button newly down isn't a repeat: whoever handles the click handles it then.
 */
public final class HoldRepeat {
	private int ticksDown = -1;

	/**
	 * Call once every client tick with whether the button is down and the ticks between repeats. Returns whether the
	 * click repeats on this tick: every {@code period} ticks after the tick the button went down.
	 */
	public boolean tick(boolean down, int period) {
		if (period < 1) {
			throw new IllegalArgumentException("period " + period);
		}
		if (!down) {
			ticksDown = -1;
			return false;
		}
		ticksDown++;
		return ticksDown > 0 && ticksDown % period == 0;
	}
}
