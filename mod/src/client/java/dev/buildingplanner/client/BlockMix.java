package dev.buildingplanner.client;

import it.unimi.dsi.fastutil.HashCommon;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The player's block mix: up to nine blocks, each with a weight, kept on this computer for as long as the game runs.
 * While it's on, shapes and Litematica's place, fill and replace use it instead of the held block. Each position's
 * block comes from a fixed hash of the position over the weights, so a shape's preview and the plan agree.
 */
final class BlockMix {
	static final int MAX_BLOCKS = 9;
	static final int MAX_WEIGHT = 999;

	/** One block of the mix and its weight, at least 1. */
	record Part(Block block, int weight) {
	}

	private List<Part> parts = List.of();
	private boolean on;

	List<Part> parts() {
		return parts;
	}

	boolean isOn() {
		return on;
	}

	/** Turns the mix on with these blocks; an empty list turns it off. */
	void use(List<Part> newParts) {
		parts = List.copyOf(newParts);
		on = !parts.isEmpty();
	}

	void turnOff() {
		on = false;
	}

	/** The mix's block at a position. Only call while the mix is on. */
	Block blockAt(BlockPos pos) {
		long total = 0;
		for (Part part : parts) {
			total += part.weight();
		}
		long pick = Math.floorMod(HashCommon.mix(pos.asLong()), total);
		for (Part part : parts) {
			pick -= part.weight();
			if (pick < 0) {
				return part.block();
			}
		}
		return parts.getLast().block();
	}

	/** {@code state} with its block swapped for the mix's block at a position, keeping the properties both share. */
	BlockState swap(BlockPos pos, BlockState state) {
		Block block = blockAt(pos);
		return block == state.getBlock() ? state : block.withPropertiesOf(state);
	}
}
