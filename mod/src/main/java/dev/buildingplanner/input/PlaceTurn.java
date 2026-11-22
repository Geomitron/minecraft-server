package dev.buildingplanner.input;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiPredicate;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The turn of a real block a right click places in Disable (see "Turning real blocks in Disable"), as pure functions:
 * the turn the server takes from {@code place_turn}, and the state hook S2 makes of the one Minecraft would place.
 */
public final class PlaceTurn {
	/** The most characters a property's name or value may have in {@code place_turn}, past the longest Minecraft's. */
	public static final int MAX_TEXT = 32;

	private PlaceTurn() {
	}

	/**
	 * The turn a player's {@code place_turn} sets: for {@code item}, the properties among {@link BlockTurn}'s
	 * orientation properties, by name; none for no item, or with none of those properties. Its horizontal facing, which
	 * only the wheel and the middle click use, is left out.
	 */
	public static BlockTurn.Held accepted(Item item, Map<String, String> properties) {
		if (item == null) {
			return BlockTurn.Held.NONE;
		}
		Map<String, String> kept = new TreeMap<>();
		properties.forEach((name, value) -> {
			if (BlockTurn.ORIENTATION.stream().anyMatch(property -> property.getName().equals(name))) {
				kept.put(name, value);
			}
		});
		return kept.isEmpty() ? BlockTurn.Held.NONE : new BlockTurn.Held(item, new BlockTurn.Turn(kept, null));
	}

	/**
	 * The state S2 gives a right click's block: {@code placed}, as Minecraft would place it, with the player's turn when
	 * the turn is for the item in hand and that hand is the main hand. Each orientation property the state has takes the
	 * turn's value where the property allows it ({@link BlockTurn#apply}); any other value is refused, so the state stays
	 * as Minecraft would place it there.
	 */
	public static BlockState turned(BlockState placed, BlockTurn.Held turn, Item inHand, boolean mainHand) {
		if (placed == null || turn.turn() == null || !mainHand || inHand != turn.item()) {
			return placed;
		}
		return BlockTurn.apply(placed, turn.turn());
	}

	/**
	 * Whether the neighbouring blocks set the turned state's shape again ({@code Block.updateFromNeighbourShapes}), as
	 * a turned stair joins the stairs beside it: not for a block of two parts (a door, a tall plant, a bed), whose other
	 * part isn't placed yet, so its shape update would remove it.
	 */
	public static boolean reshapes(BlockState state) {
		return !state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && !state.hasProperty(BlockStateProperties.BED_PART);
	}

	/**
	 * The way from a bed's foot to the space its head goes in, toward the bed's facing, which Minecraft checks only for
	 * the player's facing; null for any other block.
	 */
	public static Direction otherPart(BlockState state) {
		return state.getBlock() instanceof AbstractBedBlock && state.getValue(AbstractBedBlock.PART) == BedPart.FOOT
			? state.getValue(AbstractBedBlock.FACING) : null;
	}

	/**
	 * Whether the turned state can stay: Minecraft's own check of the state ({@code placeable}, as
	 * {@code BlockItem.canPlace} gives it), and for a bed, that the space toward its turned facing takes a block
	 * ({@code spaceTakes}, given the direction from the foot).
	 */
	public static boolean canStay(BlockState turned, boolean placeable, BiPredicate<BlockState, Direction> spaceTakes) {
		Direction other = otherPart(turned);
		return placeable && (other == null || spaceTakes.test(turned, other));
	}

	/** The turn's properties as {@code place_turn} carries them, by name, or none without a turn. */
	public static Map<String, String> properties(BlockTurn.Held turn) {
		return turn.turn() == null ? Map.of() : turn.turn().values();
	}
}
