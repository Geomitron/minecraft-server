package dev.buildingplanner.plan;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A region's blocks as a snapshot carries them: its name, minimum corner, size (positive on every axis), palette, and
 * only the 16-block cubes (sections) that hold something other than palette index 0, which fills the rest of the box.
 * So a snapshot's size follows the blocks a plan holds, not its box.
 */
public record RegionSnapshot(String name, BlockPos min, Vec3i dimensions, List<BlockState> palette,
	List<Section> sections) {
	/**
	 * One section: its cube in section coordinates (plan coordinates divided by 16, rounded down) and its 4,096 palette
	 * indices, y slowest, then z, then x, packed as Litematica packs a region's blocks. Positions outside the region's
	 * box hold 0.
	 */
	public record Section(int x, int y, int z, long[] packed) {
	}

	public RegionSnapshot {
		palette = List.copyOf(palette);
		sections = List.copyOf(sections);
	}

	public static RegionSnapshot of(PlanRegion region) {
		return new RegionSnapshot(region.name(), region.minCorner(), region.dimensions(), region.palette(),
			region.snapshotSections());
	}

	/** A region holding the snapshot's blocks, positioned at its minimum corner. */
	public PlanRegion toRegion() {
		if (dimensions.getX() <= 0 || dimensions.getY() <= 0 || dimensions.getZ() <= 0) {
			throw new IllegalArgumentException("Region " + name + " has an invalid snapshot: " + dimensions);
		}
		return PlanRegion.fromSections(name, min, dimensions, palette, sections);
	}
}
