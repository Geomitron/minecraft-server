package dev.buildingplanner.input;

/**
 * Repeats a click while its button stays down, at the pace the game itself repeats a held button. The tick that finds
 * the button newly down isn't a repeat: whoever handles the click handles it then.
 */
public final class HoldRepeat {
	/**
	 * Ticks between a held use click's repeats: vanilla 26.3 sets {@code Minecraft.rightClickDelay} to 4 in
	 * {@code startUseItem}, counts it down once a tick, and uses again when it reaches 0.
	 */
	public static final int USE_PERIOD = 4;

	/**
	 * Ticks between a held attack click's repeats: vanilla 26.3 sets {@code MultiPlayerGameMode.destroyDelay} to 5 after
	 * a block is destroyed, counts it down in {@code continueDestroyBlock} on 5 ticks, and destroys again on the next.
	 */
	public static final int ATTACK_PERIOD = 6;

	private final int period;
	private int ticksDown = -1;

	public HoldRepeat(int period) {
		if (period < 1) {
			throw new IllegalArgumentException("period " + period);
		}
		this.period = period;
	}

	/**
	 * Call once every client tick with whether the button is down. Returns whether the click repeats on this tick: every
	 * {@code period} ticks after the tick the button went down.
	 */
	public boolean tick(boolean down) {
		if (!down) {
			ticksDown = -1;
			return false;
		}
		ticksDown++;
		return ticksDown > 0 && ticksDown % period == 0;
	}
}
