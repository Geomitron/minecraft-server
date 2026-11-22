package dev.buildingplanner.server;

import dev.buildingplanner.input.BlockTurn;
import dev.buildingplanner.input.PlaceTurn;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

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

	/** The state a right click places with a turn, and whether it can stay there. */
	public record Placed(BlockState state, boolean stays) {
	}

	/**
	 * Hook S2's step, which Disable's preview takes too: {@code placed}, the state Minecraft would place for the context,
	 * with {@code turn} when it's for the item in the main hand ({@link PlaceTurn#turned}), its shape set again from its
	 * neighbours ({@code Block.updateFromNeighbourShapes}) unless it's a block of two parts, and whether the turned
	 * block can stay: Minecraft's own check ({@code placeable}, as {@code BlockItem.canPlace} gives it), a block the
	 * shape update changes into another (a torch turned away from its wall turns to air) never, and a bed only where the
	 * space toward its turned facing takes a block inside the world border. A state the turn leaves as it was stays, as
	 * Minecraft would place it.
	 */
	public static Placed place(BlockPlaceContext context, BlockState placed, BlockTurn.Held turn,
		Predicate<BlockState> placeable) {
		BlockState turned = PlaceTurn.turned(placed, turn, context.getItemInHand().getItem(),
			context.getHand() == InteractionHand.MAIN_HAND);
		if (turned == placed) {
			return new Placed(placed, true);
		}
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (PlaceTurn.reshapes(turned)) {
			BlockState shaped = Block.updateFromNeighbourShapes(turned, level, pos);
			if (shaped.getBlock() != turned.getBlock()) {
				return new Placed(turned, false);
			}
			turned = shaped;
		}
		return new Placed(turned, PlaceTurn.canStay(turned, placeable.test(turned), (state, toward) -> {
			BlockPos other = pos.relative(toward);
			return level.getBlockState(other).canBeReplaced(context) && level.getWorldBorder().isWithinBounds(other);
		}));
	}
}
