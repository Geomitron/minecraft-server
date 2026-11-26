package dev.buildingplanner.input;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Turning and flipping the block in hand (see "Locking and the first click": "Turning and flipping"), as pure functions of block
 * states. A turn is the values of the block's orientation properties, which {@link #apply} sets on every block placed
 * that has the property, after its placement rules, which still set the rest.
 */
public final class BlockTurn {
	/**
	 * The orientation properties a turn keeps: facing (with the hopper's and the crafter's orientation), axis, rotation,
	 * top or bottom half, slab type, attached face and hanging. A door's upper or lower half, a chest's type and the
	 * other properties of the same names aren't among them, so a turn never sets them.
	 */
	static final List<Property<?>> ORIENTATION = List.of(BlockStateProperties.FACING,
		BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.FACING_HOPPER, BlockStateProperties.ORIENTATION,
		BlockStateProperties.AXIS, BlockStateProperties.HORIZONTAL_AXIS, BlockStateProperties.ROTATION_16,
		BlockStateProperties.HALF, BlockStateProperties.SLAB_TYPE, BlockStateProperties.ATTACH_FACE,
		BlockStateProperties.HANGING);

	/**
	 * A turn: the orientation properties' values by name, and the horizontal facing the block goes back to after a flip
	 * up and down ({@link #flipped}), which the wheel turns too.
	 */
	public record Turn(Map<String, String> values, Direction horizontal) {
		public Turn {
			values = Map.copyOf(values);
		}
	}

	/** A turn and the item it was set for, or none ({@link #NONE}). */
	public record Held(Item item, Turn turn) {
		public static final Held NONE = new Held(null, null);

		/**
		 * The turn as it stands with {@code inHand} in the main hand: kept while the item is the one it was set for, in
		 * any hotbar slot, and dropped once it's another, so switching to another block and back drops it too.
		 */
		public Held follow(Item inHand) {
			return inHand == item ? this : NONE;
		}
	}

	private BlockTurn() {
	}

	/** Whether a quarter turn changes any of the block's states: a stair, a chest, a log or a sign, but not stone. */
	public static boolean turns(Block block) {
		return block.getStateDefinition().getPossibleStates().stream()
			.anyMatch(state -> state.rotate(Rotation.CLOCKWISE_90) != state);
	}

	/** Whether the block has another way to sit that the middle click flips it to ({@link #flipped}). */
	public static boolean flips(Block block) {
		return flipProperty(block.defaultBlockState()) != null;
	}

	/**
	 * The turn of a block as Minecraft would place it now, before the first turn or flip. Its horizontal facing is the
	 * block's own when it faces or lies sideways, and otherwise {@code toward}, the way the block would face set on its
	 * side (toward the player, as a piston placed sideways faces).
	 */
	public static Turn start(BlockState placed, Direction toward) {
		Direction horizontal = toward;
		if (placed.hasProperty(BlockStateProperties.FACING)
			&& placed.getValue(BlockStateProperties.FACING).getAxis().isHorizontal()) {
			horizontal = placed.getValue(BlockStateProperties.FACING);
		} else if (placed.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			horizontal = placed.getValue(BlockStateProperties.HORIZONTAL_FACING);
		} else if (placed.hasProperty(BlockStateProperties.AXIS)
			&& placed.getValue(BlockStateProperties.AXIS).isHorizontal()) {
			horizontal = Direction.fromAxisAndDirection(placed.getValue(BlockStateProperties.AXIS),
				Direction.AxisDirection.POSITIVE);
		}
		return new Turn(values(placed), horizontal);
	}

	/**
	 * The turn after a quarter turn, clockwise seen from above or counterclockwise, as Minecraft turns a block in a
	 * turned structure ({@code BlockState.rotate}); {@code block} is any state of the block the turn is for.
	 */
	public static Turn turned(Turn turn, BlockState block, boolean clockwise) {
		Rotation rotation = clockwise ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90;
		return new Turn(values(apply(block, turn).rotate(rotation)), rotation.rotate(turn.horizontal()));
	}

	/**
	 * The turn after a flip to the next of the other ways the block can sit, in a fixed order, back to the first after
	 * the last: a piston's, an observer's or a dispenser's facing from its horizontal facing to up, down and back; a log's
	 * or a chain's axis from its horizontal axis up and back; a button's or a lever's face from the floor to a wall, the
	 * ceiling and back; a stair's or a trapdoor's half and a slab's type between bottom and top (never double, which
	 * flips to bottom); a lantern hanging or standing. A block with none of these keeps its turn.
	 */
	public static Turn flipped(Turn turn, BlockState block) {
		BlockState state = apply(block, turn);
		Property<?> property = flipProperty(state);
		if (property == null) {
			return turn;
		}
		Direction horizontal = turn.horizontal();
		BlockState next;
		if (property == BlockStateProperties.FACING) {
			next = state.setValue(BlockStateProperties.FACING,
				following(List.of(horizontal, Direction.UP, Direction.DOWN), state.getValue(BlockStateProperties.FACING)));
		} else if (property == BlockStateProperties.AXIS) {
			next = state.setValue(BlockStateProperties.AXIS,
				following(List.of(horizontal.getAxis(), Direction.Axis.Y), state.getValue(BlockStateProperties.AXIS)));
		} else if (property == BlockStateProperties.ATTACH_FACE) {
			next = state.setValue(BlockStateProperties.ATTACH_FACE,
				following(List.of(AttachFace.FLOOR, AttachFace.WALL, AttachFace.CEILING),
					state.getValue(BlockStateProperties.ATTACH_FACE)));
		} else if (property == BlockStateProperties.HALF) {
			next = state.cycle(BlockStateProperties.HALF);
		} else if (property == BlockStateProperties.SLAB_TYPE) {
			next = state.setValue(BlockStateProperties.SLAB_TYPE, following(List.of(SlabType.BOTTOM, SlabType.TOP),
				state.getValue(BlockStateProperties.SLAB_TYPE)));
		} else {
			next = state.cycle(BlockStateProperties.HANGING);
		}
		return new Turn(values(next), horizontal);
	}

	/**
	 * The state with the turn: each orientation property it has takes the turn's value for that name, where the value
	 * is one the property allows (a stair takes a piston's facing east, not up). Every other property stays as it is.
	 */
	public static BlockState apply(BlockState state, Turn turn) {
		for (Property<?> property : state.getProperties()) {
			if (ORIENTATION.contains(property)) {
				String value = turn.values().get(property.getName());
				if (value != null) {
					state = withValue(state, property, value);
				}
			}
		}
		return state;
	}

	/** The property a flip changes, the first the block has of the order in {@link #flipped}, or null. */
	private static Property<?> flipProperty(BlockState state) {
		for (Property<?> property : List.of(BlockStateProperties.FACING, BlockStateProperties.AXIS,
			BlockStateProperties.ATTACH_FACE, BlockStateProperties.HALF, BlockStateProperties.SLAB_TYPE,
			BlockStateProperties.HANGING)) {
			if (state.hasProperty(property)) {
				return property;
			}
		}
		return null;
	}

	/** The value after {@code value} in {@code order}, back to the first after the last, or the first if it's not there. */
	private static <T> T following(List<T> order, T value) {
		return order.get((order.indexOf(value) + 1) % order.size());
	}

	/** The state's orientation properties' values by name. */
	static Map<String, String> values(BlockState state) {
		Map<String, String> values = new TreeMap<>();
		for (Property<?> property : state.getProperties()) {
			if (ORIENTATION.contains(property)) {
				values.put(property.getName(), name(state, property));
			}
		}
		return values;
	}

	private static <T extends Comparable<T>> String name(BlockState state, Property<T> property) {
		return property.getName(state.getValue(property));
	}

	private static <T extends Comparable<T>> BlockState withValue(BlockState state, Property<T> property, String value) {
		return property.getValue(value).map(v -> state.setValue(property, v)).orElse(state);
	}
}
