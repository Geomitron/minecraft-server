package dev.buildingplanner.input;

/**
 * Repeats a click while its button stays down, every so many ticks (the player's {@link RepeatSpeed}). The tick that
 * finds the button newly down isn't a repeat: whoever handles the click handles it then. The first repeat waits at
 * least {@link #FIRST_WAIT} ticks, the game's own wait before a held click repeats, so a quick click acts once at
 * every speed.
 */
public final class HoldRepeat {
	/**
	 * The least wait before the first repeat, in ticks: 0.2 seconds. Vanilla 26.3's {@code Minecraft.startUseItem} sets
	 * {@code rightClickDelay} to 4, so a held use button first acts again 4 ticks after it went down.
	 */
	public static final int FIRST_WAIT = 4;

	private int ticksDown = -1;

	/**
	 * Call once every client tick with whether the button is down and the ticks between repeats. Returns whether the
	 * click repeats on this tick: first {@code max(FIRST_WAIT, period)} ticks after the tick the button went down, then
	 * every {@code period} ticks.
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
		int first = Math.max(FIRST_WAIT, period);
		return ticksDown >= first && (ticksDown - first) % period == 0;
	}
}
