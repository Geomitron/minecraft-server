package dev.buildingplanner.layer;

import java.util.Set;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Whether a real block fills a ghost block ("Ghost blocks the real world fills"): the same block placed the same way,
 * whatever it connects to. Hook S1, the edits and the check at chunk load all ask this one rule.
 */
public final class FillRule {
	/**
	 * The properties a player sets by placing a block: facing (with the hopper's, a dripstone's tip and the crafter's
	 * orientation), axis, top or bottom half, slab type, door hinge, bed part, attach face and rotation, and the count of
	 * a block that goes several to a spot (candles, sea pickles, snow layers, turtle eggs, pink petals and leaf litter).
	 * Every other property (connections, stair shape, waterlogged, powered, open, lit, age, distance and the like) is
	 * the world's, and takes any value.
	 */
	static final Set<Property<?>> PLACED = Set.of(BlockStateProperties.FACING, BlockStateProperties.HORIZONTAL_FACING,
		BlockStateProperties.FACING_HOPPER, BlockStateProperties.VERTICAL_DIRECTION, BlockStateProperties.ORIENTATION,
		BlockStateProperties.AXIS, BlockStateProperties.HORIZONTAL_AXIS, BlockStateProperties.HALF,
		BlockStateProperties.DOUBLE_BLOCK_HALF, BlockStateProperties.SLAB_TYPE, BlockStateProperties.DOOR_HINGE,
		BlockStateProperties.BED_PART, BlockStateProperties.ATTACH_FACE, BlockStateProperties.ROTATION_16,
		BlockStateProperties.CANDLES, BlockStateProperties.PICKLES, BlockStateProperties.LAYERS,
		BlockStateProperties.EGGS, BlockStateProperties.FLOWER_AMOUNT, BlockStateProperties.SEGMENT_AMOUNT);

	private FillRule() {
	}

	/** True when {@code real} fills {@code ghost}; air fills nothing, and nothing fills air. */
	public static boolean fills(BlockState real, BlockState ghost) {
		if (ghost.isAir() || real.getBlock() != ghost.getBlock()) {
			return false;
		}
		if (real == ghost) {
			return true;
		}
		for (Property<?> property : ghost.getProperties()) {
			if (PLACED.contains(property) && !real.getValue(property).equals(ghost.getValue(property))) {
				return false;
			}
		}
		return true;
	}
}
