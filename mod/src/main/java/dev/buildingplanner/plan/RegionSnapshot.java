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
			region.packedBlocks());
	}

	/** A region holding the snapshot's blocks, positioned at its minimum corner. */
	public PlanRegion toRegion() {
		if (dimensions.getX() <= 0 || dimensions.getY() <= 0 || dimensions.getZ() <= 0) {
			throw new IllegalArgumentException("Region " + name + " has an invalid snapshot: " + dimensions);
		}
		return PlanRegion.unpack(name, min, dimensions, palette, packed, new CompoundTag(), true);
	}
}
