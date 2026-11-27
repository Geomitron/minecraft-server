package dev.buildingplanner.client;

import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Hook M9 (see "Place real blocks": "The preview"): while the preview works a shape out in the server's order
 * ({@link RealShape}), the blocks it has placed so far stand in the player's level for Minecraft's own checks of the
 * blocks after them, as they would stand on the server. Only the game's own thread sees them, and only during that
 * work; at any other time the level reads as it is.
 */
public final class ShapeOverlay {
	private static Level level;
	private static Thread thread;
	private static Map<BlockPos, BlockState> standing;

	private ShapeOverlay() {
	}

	/** Runs {@code work} with {@code blocks} standing in {@code on}, which may add to them as it goes. */
	public static <T> T with(Level on, Map<BlockPos, BlockState> blocks, Supplier<T> work) {
		level = on;
		thread = Thread.currentThread();
		standing = blocks;
		try {
			return work.get();
		} finally {
			standing = null;
			level = null;
			thread = null;
		}
	}

	/** Hook M9: the block at {@code pos} in {@code in}, {@code real} unless a block of the shape stands there now. */
	public static BlockState state(Level in, BlockPos pos, BlockState real) {
		BlockState shape = standing(in, pos);
		return shape != null ? shape : real;
	}

	/** Hook M9: the fluid at {@code pos} in {@code in}, {@code real} unless a block of the shape stands there now. */
	public static FluidState fluid(Level in, BlockPos pos, FluidState real) {
		BlockState shape = standing(in, pos);
		return shape != null ? shape.getFluidState() : real;
	}

	private static BlockState standing(Level in, BlockPos pos) {
		Map<BlockPos, BlockState> blocks = standing;
		return blocks == null || in != level || Thread.currentThread() != thread ? null : blocks.get(pos);
	}
}
