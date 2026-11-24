package dev.buildingplanner.client;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.phys.Vec3;

/**
 * Turns the player's view one step each frame, as a quick mouse sweep would, for Delete's marking, and times
 * frames, for what drawing the marks costs (step9.sh). Both act at the start of each pass of the game's loop, before
 * that pass draws its frame; the driver's own commands run in client ticks, which can come only every few frames.
 */
public final class FrameProbe {
	private static Vec3 turnFrom;
	private static Vec3 turnTo;
	private static int turnFrames;
	private static int turned;

	private static int timedFrames;
	private static int timed;
	private static long lastFrame;
	private static long totalNanos;
	private static long longestNanos;

	private FrameProbe() {
	}

	/**
	 * Turns the view to look at {@code from} in the next frame, then on in even steps to look at {@code to} in the
	 * {@code frames}th.
	 */
	static void turn(Vec3 from, Vec3 to, int frames) {
		turnFrom = from;
		turnTo = to;
		turnFrames = Math.max(1, frames);
		turned = 0;
	}

	/** Whether the last turn has reached its end. */
	static boolean turned() {
		return turnTo == null;
	}

	/** Times the next {@code frames} frames, each from the start of one pass of the loop to the start of the next. */
	static void time(int frames) {
		timedFrames = frames;
		timed = -1;
		totalNanos = 0;
		longestNanos = 0;
	}

	/** Whether the frames asked for have been timed. */
	static boolean timed() {
		return timed >= timedFrames;
	}

	/** The timed frames' mean and longest, in milliseconds, or how far the timing has got. */
	static String times() {
		if (!timed()) {
			return "timed " + Math.max(timed, 0) + " of " + timedFrames + " frames";
		}
		return String.format(Locale.ROOT, "%d frames, mean %.1f ms, longest %.1f ms", timed,
			totalNanos / 1e6 / timed, longestNanos / 1e6);
	}

	/** At the start of the game loop's pass, before it draws its frame. */
	public static void beforeFrame(Minecraft mc) {
		long now = System.nanoTime();
		if (timed < timedFrames) {
			if (timed >= 0) {
				long frame = now - lastFrame;
				totalNanos += frame;
				longestNanos = Math.max(longestNanos, frame);
			}
			timed++;
			lastFrame = now;
		}
		if (turnTo == null || mc.player == null) {
			return;
		}
		turned++;
		double t = turnFrames == 1 ? 1 : (turned - 1) / (double) (turnFrames - 1);
		mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, turnFrom.lerp(turnTo, t));
		if (turned >= turnFrames) {
			turnTo = null;
		}
	}
}
