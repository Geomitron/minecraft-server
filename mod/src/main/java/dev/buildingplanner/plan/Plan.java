package dev.buildingplanner.plan;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;

/**
 * A plan's blocks: its regions and Litematica's metadata. Placement (world position, rotation, mirror) and sharing
 * belong to Syncmatica, not to this model.
 */
public final class Plan {
	private final CompoundTag metadata;
	private final Map<String, PlanRegion> regions;

	Plan(CompoundTag metadata, Map<String, PlanRegion> regions) {
		if (regions.isEmpty()) {
			throw new IllegalArgumentException("A plan needs at least one region");
		}
		this.metadata = metadata;
		this.regions = new LinkedHashMap<>(regions);
	}

	/** A new plan with one empty region, with metadata as Litematica writes it for a new schematic. */
	public static Plan create(String name, String author, BlockPos min, Vec3i dimensions, long timeMillis) {
		CompoundTag metadata = new CompoundTag();
		metadata.putString("Name", name);
		metadata.putString("Author", author);
		metadata.putString("Description", "");
		metadata.putLong("TimeCreated", timeMillis);
		Plan plan = new Plan(metadata, Map.of(name, PlanRegion.empty(name, min, dimensions)));
		plan.refreshMetadata(timeMillis);
		return plan;
	}

	/** A plan holding a snapshot's regions, in order, with empty metadata. */
	public static Plan fromSnapshot(List<RegionSnapshot> snapshot) {
		Map<String, PlanRegion> regions = new LinkedHashMap<>();
		for (RegionSnapshot region : snapshot) {
			regions.put(region.name(), region.toRegion());
		}
		return new Plan(new CompoundTag(), regions);
	}

	/** Each region's snapshot, in order. */
	public List<RegionSnapshot> snapshot() {
		return regions.values().stream().map(RegionSnapshot::of).toList();
	}

	public String name() {
		return metadata.getStringOr("Name", "");
	}

	public String author() {
		return metadata.getStringOr("Author", "");
	}

	/** Litematica's {@code TimeCreated}, in milliseconds; 0 when the file has none. */
	public long timeCreated() {
		return metadata.getLongOr("TimeCreated", 0L);
	}

	/** Regions in the file's order; the first is where blocks outside every region go. */
	public Collection<PlanRegion> regions() {
		return Collections.unmodifiableCollection(regions.values());
	}

	public Optional<PlanRegion> region(String name) {
		return Optional.ofNullable(regions.get(name));
	}

	public PlanRegion firstRegion() {
		return regions.values().iterator().next();
	}

	public Optional<PlanRegion> regionContaining(BlockPos pos) {
		return regions.values().stream().filter(region -> region.contains(pos)).findFirst();
	}

	/**
	 * Brings Litematica's metadata up to date with the blocks: region count, total volume, total blocks, enclosing
	 * size and modified time. Call before saving a changed plan.
	 */
	public void refreshMetadata(long timeMillis) {
		long volume = 0;
		int blocks = 0;
		BlockPos min = null;
		BlockPos max = null;
		for (PlanRegion region : regions.values()) {
			volume += region.volume();
			blocks += region.nonAirBlocks();
			min = min == null ? region.minCorner() : BlockPos.min(min, region.minCorner());
			max = max == null ? region.maxCorner() : BlockPos.max(max, region.maxCorner());
		}
		metadata.putInt("RegionCount", regions.size());
		metadata.putInt("TotalVolume", (int) Math.min(volume, Integer.MAX_VALUE));
		metadata.putInt("TotalBlocks", blocks);
		CompoundTag enclosing = new CompoundTag();
		enclosing.putInt("x", max.getX() - min.getX() + 1);
		enclosing.putInt("y", max.getY() - min.getY() + 1);
		enclosing.putInt("z", max.getZ() - min.getZ() + 1);
		metadata.put("EnclosingSize", enclosing);
		metadata.putLong("TimeModified", timeMillis);
	}

	CompoundTag metadata() {
		return metadata;
	}
}
