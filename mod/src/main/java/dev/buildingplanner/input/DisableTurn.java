package dev.buildingplanner.input;

import java.util.Objects;

/**
 * Disable's turn (see "Turning real blocks in Disable"), as pure functions: when its turning controls are active, the
 * turn hook M6 sends before a right click on a block, and when it sends one.
 */
public final class DisableTurn {
	private DisableTurn() {
	}

	/**
	 * Whether Disable's turning controls are active (Left Ctrl with the wheel and the middle click): Building Planner has
	 * a session, Effortless Building's mode is Disable, no mouse mode is on, and the block in hand turns or flips.
	 */
	public static boolean active(boolean session, boolean disableMode, boolean mouseMode, boolean turnsOrFlips) {
		return session && disableMode && !mouseMode && turnsOrFlips;
	}

	/**
	 * The turn a right click takes: the turn of the block in hand ({@code held}, for the item in hand) while Disable's
	 * turning controls are active and the sprint key is held; otherwise none, so the click places as Minecraft does.
	 */
	public static BlockTurn.Held clickTakes(boolean active, boolean sprint, BlockTurn.Held held) {
		return active && sprint && held.turn() != null ? held : BlockTurn.Held.NONE;
	}

	/**
	 * Whether M6 sends {@code now}: when it differs from the turn sent last, in its item or its properties' values, so
	 * a held right click, repeating every 4 ticks with the same turn, sends it once.
	 */
	public static boolean sends(BlockTurn.Held last, BlockTurn.Held now) {
		return last.item() != now.item()
			|| !Objects.equals(PlaceTurn.properties(last), PlaceTurn.properties(now));
	}
}
