package dev.buildingplanner.sync;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

/** A region's box after it grew: its new minimum corner (also its new position) and its size, positive on every axis. */
public record RegionBox(String region, BlockPos min, Vec3i dimensions) {
	public BlockPos max() {
		return min.offset(dimensions.getX() - 1, dimensions.getY() - 1, dimensions.getZ() - 1);
	}
}
