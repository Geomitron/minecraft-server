package dev.buildingplanner.input;

import dev.buildingplanner.ClientConfig;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * How fast a held button keeps placing or erasing: the ticks between {@link HoldRepeat}'s repeats, from 1 (20 blocks a
 * second) to 20 (1 a second), set by a slider in Effortless Building's radial menu and kept in the player's
 * {@link ClientConfig}. Each starts at the pace the game itself repeats a held button.
 */
public enum RepeatSpeed {
	/**
	 * The use button. Vanilla 26.3 sets {@code Minecraft.rightClickDelay} to 4 in {@code startUseItem}, counts it down
	 * once a tick, and uses again when it reaches 0.
	 */
	PLACING("place-repeat-ticks", 4, "buildingplanner.ui.radial.build_speed"),
	/**
	 * The attack button. Vanilla 26.3 sets {@code MultiPlayerGameMode.destroyDelay} to 5 after a block is destroyed,
	 * counts it down in {@code continueDestroyBlock} on 5 ticks, and destroys again on the next.
	 */
	ERASING("erase-repeat-ticks", 6, "buildingplanner.ui.radial.break_speed");

	public static final int FASTEST = 1;
	public static final int SLOWEST = 20;
	/** The text a slider shows, its speed in blocks a second ({@link #blocksPerSecond}): "5 blocks/s". */
	public static final String SPEED_TEXT = "buildingplanner.ui.radial.speed";
	private static final int TICKS_A_SECOND = 20;

	private final String key;
	private final int firstTicks;
	private final String heading;

	RepeatSpeed(String key, int firstTicks, String heading) {
		this.key = key;
		this.firstTicks = firstTicks;
		this.heading = heading;
	}

	/** The key of the heading over the speed's slider: "Build speed" or "Break speed". */
	public String heading() {
		return heading;
	}

	/** The ticks between repeats a player has before they move the slider. */
	public int firstTicks() {
		return firstTicks;
	}

	/** The player's ticks between repeats: the config's, when it holds a whole number from 1 to 20, else the first. */
	public int ticks(ClientConfig config) {
		String value = config.get(key);
		if (value != null) {
			try {
				int ticks = Integer.parseInt(value);
				if (ticks >= FASTEST && ticks <= SLOWEST) {
					return ticks;
				}
			} catch (NumberFormatException e) {
				// Falls back to the first setting.
			}
		}
		return firstTicks;
	}

	/** Keeps the player's ticks between repeats in the config. */
	public void set(ClientConfig config, int ticks) throws IOException {
		config.set(key, Integer.toString(ticks));
	}

	/** The speed of a gap of {@code ticks} ticks in blocks a second, to two decimals at most: 5, 3.33, 6.67. */
	public static String blocksPerSecond(int ticks) {
		return BigDecimal.valueOf(TICKS_A_SECOND).divide(BigDecimal.valueOf(ticks), 2, RoundingMode.HALF_UP)
			.stripTrailingZeros().toPlainString();
	}

	/** Where a gap of {@code ticks} ticks sits on a slider, from 0 (the slowest, at the left) to 1 (the fastest). */
	public static double position(int ticks) {
		return (double) (SLOWEST - ticks) / (SLOWEST - FASTEST);
	}

	/** The gap in ticks nearest a slider position from 0 to 1. */
	public static int ticksAt(double position) {
		double clamped = Math.clamp(position, 0, 1);
		return SLOWEST - (int) Math.round(clamped * (SLOWEST - FASTEST));
	}
}
