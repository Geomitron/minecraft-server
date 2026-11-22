package dev.buildingplanner.server;

import dev.buildingplanner.input.BlockTurn;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The turns hook S2 gives real blocks placed in Disable (see "Turning real blocks in Disable"): on the server, each
 * player's last {@code place_turn}, kept until the player leaves; on the player's computer, the turn hook M6 sent last.
 * Each side reads and writes its own on its own thread: the server's on the server thread, the local one on the client
 * thread.
 */
public final class PlaceTurns {
	/** The running server's turns, or null with no dedicated server running ({@link ServerSync}). */
	private static PlaceTurns server;
	/** The turn M6 sent last, which the player's computer turns its guess of the block with. */
	private static BlockTurn.Held local = BlockTurn.Held.NONE;

	private final Map<UUID, BlockTurn.Held> turns = new HashMap<>();

	PlaceTurns() {
	}

	/** The running server's turns, from its start to its stop; null clears them. */
	static void serve(PlaceTurns turns) {
		server = turns;
	}

	/** On the server, the player's turn, or none. */
	public static BlockTurn.Held of(UUID player) {
		PlaceTurns turns = server;
		return turns == null ? BlockTurn.Held.NONE : turns.turns.getOrDefault(player, BlockTurn.Held.NONE);
	}

	/** On the player's computer, the turn M6 sent last, or none. */
	public static BlockTurn.Held local() {
		return local;
	}

	/** M6 sent this turn; none at the start and end of each session. */
	public static void sentLocally(BlockTurn.Held turn) {
		local = turn;
	}

	/** The player's {@code place_turn}: their turn from now on, or none. */
	void set(UUID player, BlockTurn.Held turn) {
		if (turn.turn() == null) {
			turns.remove(player);
		} else {
			turns.put(player, turn);
		}
	}

	/** The player left: their turn goes. */
	void left(UUID player) {
		turns.remove(player);
	}
}
