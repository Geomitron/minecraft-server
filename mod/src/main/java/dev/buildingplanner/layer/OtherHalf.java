package dev.buildingplanner.layer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * The other half of a two-block block: a door's or a tall plant's other half, or a bed's other part. The placing rules
 * give only the half placed first, which the game completes as it places it, and breaking either half of a real door
 * or bed breaks both; a ghost block is drawn and erased with both.
 */
public final class OtherHalf {
	private OtherHalf() {
	}

	/** Where the other half of a block in {@code state} at {@code pos} is, or null for a block of one. */
	public static BlockPos of(BlockPos pos, BlockState state) {
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
			return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
		}
		if (state.hasProperty(BlockStateProperties.BED_PART) && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
			return state.getValue(BlockStateProperties.BED_PART) == BedPart.FOOT ? pos.relative(facing)
				: pos.relative(facing.getOpposite());
		}
		return null;
	}

	/** The other half's state, for a state {@link #of} finds a position for. */
	public static BlockState state(BlockState state) {
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
			return state.cycle(BlockStateProperties.DOUBLE_BLOCK_HALF);
		}
		return state.cycle(BlockStateProperties.BED_PART);
	}

	/**
	 * Whether {@code other} is the other half of {@code state}, for a state {@link #of} finds a position for: the same
	 * block, as the other half.
	 */
	public static boolean matches(BlockState state, BlockState other) {
		if (other.getBlock() != state.getBlock()) {
			return false;
		}
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
			return other.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)
				!= state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
		}
		return other.getValue(BlockStateProperties.BED_PART) != state.getValue(BlockStateProperties.BED_PART);
	}
}
