package dev.buildingplanner.client;

import dev.buildingplanner.input.BlockTurn;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The turn of the block in hand and the item it was set for (see "Locking and the first click": "Turning and
 * flipping"), kept apart from the lock's state since the turn reaches every mode. It uses none of Effortless Building's
 * classes. Only the client thread uses it.
 */
final class HeldTurn {
	private static BlockTurn.Held held = BlockTurn.Held.NONE;

	private HeldTurn() {
	}

	/** Once a client tick: drops the turn once the main hand holds another item ({@link BlockTurn.Held#follow}). */
	static void follow(Item inHand) {
		held = held.follow(inHand);
	}

	/** The turn of {@code inHand}, or null while it has none. */
	static BlockTurn.Turn of(Item inHand) {
		return held.follow(inHand).turn();
	}

	/**
	 * A notch of the wheel: a quarter turn of {@code item}, clockwise seen from above or counterclockwise. The first turn
	 * or flip starts from {@code now}, the block as Minecraft would place it at that moment, whose horizontal facing set on
	 * its side is {@code toward}.
	 */
	static void turn(Item item, BlockState now, Direction toward, boolean clockwise) {
		held = new BlockTurn.Held(item, BlockTurn.turned(started(item, now, toward), now, clockwise));
	}

	/** The middle click: flips {@code item} to the next way it can sit, starting as {@link #turn} does. */
	static void flip(Item item, BlockState now, Direction toward) {
		held = new BlockTurn.Held(item, BlockTurn.flipped(started(item, now, toward), now));
	}

	/** The state with the turn of {@code inHand}, or as it is while that item has none. */
	static BlockState apply(BlockState state, Item inHand) {
		BlockTurn.Turn turn = of(inHand);
		return turn == null ? state : BlockTurn.apply(state, turn);
	}

	private static BlockTurn.Turn started(Item item, BlockState now, Direction toward) {
		BlockTurn.Turn turn = of(item);
		return turn != null ? turn : BlockTurn.start(now, toward);
	}
}
