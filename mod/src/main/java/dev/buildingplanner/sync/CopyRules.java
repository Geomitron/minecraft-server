package dev.buildingplanner.sync;

import dev.buildingplanner.layer.Layer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** The rules for copying blocks. */
public final class CopyRules {
	private CopyRules() {
	}

	/**
	 * The blocks a copy of the box from {@code min} to {@code max} takes, by world position: while ghost blocks show
	 * ({@code ghostsShown}), at each position the ghost block from {@code layer}, and where there is none, the real
	 * block from {@code world} unless it's air; while they're hidden, only the real blocks, so the box's ghost blocks
	 * are left out.
	 */
	public static Map<BlockPos, BlockState> blocks(BlockPos min, BlockPos max, Layer layer,
		Function<BlockPos, BlockState> world, boolean ghostsShown) {
		Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
		for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
			BlockState state = ghostsShown ? layer.get(pos) : Blocks.AIR.defaultBlockState();
			if (state.isAir()) {
				state = world.apply(pos);
			}
			if (!state.isAir()) {
				blocks.put(pos.immutable(), state);
			}
		}
		return blocks;
	}

	/**
	 * Why a copy of {@code blocks} blocks is refused, or empty when it isn't: a box with none, or more than
	 * {@code maxEditBlocks}, the server's cap for an edit, which its paste would exceed.
	 */
	public static Optional<PlayerMessage> refusal(int blocks, int maxEditBlocks) {
		if (blocks == 0) {
			return Optional.of(PlayerMessage.nothingToCopy());
		}
		if (blocks > maxEditBlocks) {
			return Optional.of(PlayerMessage.copyTooLarge(blocks, maxEditBlocks));
		}
		return Optional.empty();
	}
}
