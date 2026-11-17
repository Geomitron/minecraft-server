package dev.buildingplanner.client;

import it.unimi.dsi.fastutil.HashCommon;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

/**
 * The player's block mix: up to nine blocks, each with a weight, kept on this computer for as long as the game runs.
 * While it applies, shapes use it instead of the held block. Each position's block comes from a fixed hash of the
 * position over the weights, so a shape's preview and the ghost blocks agree.
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

	/** The sum of the blocks' weights. */
	int total() {
		return parts.stream().mapToInt(Part::weight).sum();
	}

	/** A weight's share of {@code total} as a whole percentage, such as "80%"; "0%" when the total is 0. */
	static String percent(int weight, int total) {
		return total == 0 ? "0%" : Math.round(100.0 * weight / total) + "%";
	}

	/** The mix's block at a position. Only call while the mix is on. */
	Block blockAt(BlockPos pos) {
		long total = total();
		long pick = Math.floorMod(HashCommon.mix(pos.asLong()), total);
		for (Part part : parts) {
			pick -= part.weight();
			if (pick < 0) {
				return part.block();
			}
		}
		return parts.getLast().block();
	}
}
