package dev.buildingplanner.plan;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A region's blocks as a snapshot carries them: its name, minimum corner, size (positive on every axis), palette and
 * palette indices packed as Litematica packs them.
 */
public record RegionSnapshot(String name, BlockPos min, Vec3i dimensions, List<BlockState> palette, long[] packed) {
	public RegionSnapshot {
		palette = List.copyOf(palette);
	}

	public static RegionSnapshot of(PlanRegion region) {
		return new RegionSnapshot(region.name(), region.minCorner(), region.dimensions(), region.palette(),
			PackedBlocks.pack(region.paletteIndices(), PackedBlocks.bitsFor(region.palette().size())));
	}

	/** A region holding the snapshot's blocks, positioned at its minimum corner. */
	public PlanRegion toRegion() {
		long volume = (long) dimensions.getX() * dimensions.getY() * dimensions.getZ();
		if (dimensions.getX() <= 0 || dimensions.getY() <= 0 || dimensions.getZ() <= 0 || volume > Integer.MAX_VALUE
			|| palette.isEmpty()) {
			throw new IllegalArgumentException("Region " + name + " has an invalid snapshot: " + dimensions);
		}
		int[] blocks = PackedBlocks.unpack(packed, PackedBlocks.bitsFor(palette.size()), (int) volume);
		for (int i = 0; i < blocks.length; i++) {
			if (blocks[i] >= palette.size()) {
				throw new IllegalArgumentException("Region " + name + " has a block past its palette");
			}
		}
		return new PlanRegion(name, min, dimensions, palette, blocks, new CompoundTag());
	}
}
