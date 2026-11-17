package dev.buildingplanner.look;

import dev.buildingplanner.ClientConfig;
import java.io.IOException;

/**
 * How Building Planner sets Litematica up on a player's computer, once: ghost blocks translucent, at 0.8 opacity, and
 * Litematica's tool item off, since the stick and Litematica's tool modes play no part in planning. An option the
 * player has changed from Litematica's default keeps the player's value, and once the defaults are set, the player's
 * {@link ClientConfig} records it, so a later change of the player's own is never overwritten.
 */
public final class LookDefaults {
	/** What Building Planner sets {@code renderBlocksAsTranslucent} to. */
	public static final boolean TRANSLUCENT = true;
	/** What Building Planner sets {@code ghostBlockAlpha} to. */
	public static final double GHOST_ALPHA = 0.8;
	/** What Building Planner sets {@code toolItemEnabled} to. */
	public static final boolean TOOL_ITEM = false;

	static final String APPLIED = "look-defaults-applied";

	private LookDefaults() {
	}

	/** The value an option takes: Building Planner's when the player left it at Litematica's default, else the player's. */
	public static boolean choose(boolean current, boolean litematicaDefault, boolean ours) {
		return current == litematicaDefault ? ours : current;
	}

	/** The value an option takes: Building Planner's when the player left it at Litematica's default, else the player's. */
	public static double choose(double current, double litematicaDefault, double ours) {
		return Double.compare(current, litematicaDefault) == 0 ? ours : current;
	}

	/** Whether the player's config records that the defaults were set. */
	public static boolean applied(ClientConfig config) {
		return Boolean.parseBoolean(config.get(APPLIED));
	}

	/** Records in the player's config that the defaults were set. */
	public static void markApplied(ClientConfig config) throws IOException {
		config.set(APPLIED, "true");
	}
}
