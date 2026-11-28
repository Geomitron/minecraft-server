package dev.buildingplanner.plan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A saved copy's blocks in {@code <number>.litematic}, in Litematica's format: one region named as the copy's own,
 * {@value #REGION}, the box around the copy's blocks, with structure void in its empty space, as the copy has it.
 */
public final class CopyFile {
	public static final String EXTENSION = ".litematic";
	/** The region's name, the one the copy's own schematic gives its region. */
	public static final String REGION = "Copy";
	private static final BlockState EMPTY = Blocks.STRUCTURE_VOID.defaultBlockState();

	private CopyFile() {
	}

	/** The file of the copy numbered {@code number} in the player's copies folder. */
	public static Path path(Path folder, int number) {
		return folder.resolve(number + EXTENSION);
	}

	/**
	 * Writes the blocks (at any positions, at least one) through a temporary file and a rename, with Litematica's
	 * metadata: the name, the author and the time, the region count, the box's volume and size, and the block count.
	 */
	public static void write(Path file, Map<BlockPos, BlockState> blocks, String name, String author, long time)
		throws IOException {
		BlockPos min = null;
		BlockPos max = null;
		for (BlockPos pos : blocks.keySet()) {
			min = min == null ? pos : BlockPos.min(min, pos);
			max = max == null ? pos : BlockPos.max(max, pos);
		}
		Vec3i size = new Vec3i(max.getX() - min.getX() + 1, max.getY() - min.getY() + 1, max.getZ() - min.getZ() + 1);
		// Litematica reads a palette's first entry as air whatever it is, so the empty space is air turned to structure
		// void, position by position.
		PlanRegion region = PlanRegion.empty(REGION, BlockPos.ZERO, size);
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.ZERO,
			new BlockPos(size.getX() - 1, size.getY() - 1, size.getZ() - 1))) {
			region.set(pos, EMPTY);
		}
		BlockPos corner = min;
		blocks.forEach((pos, state) -> region.set(pos.subtract(corner), state));
		CompoundTag metadata = new CompoundTag();
		metadata.putString("Name", name);
		metadata.putString("Author", author);
		metadata.putString("Description", "");
		metadata.putLong("TimeCreated", time);
		metadata.putLong("TimeModified", time);
		metadata.putInt("RegionCount", 1);
		metadata.putInt("TotalVolume", (int) Math.min(region.volume(), Integer.MAX_VALUE));
		metadata.putInt("TotalBlocks", blocks.size());
		CompoundTag enclosing = new CompoundTag();
		enclosing.putInt("x", size.getX());
		enclosing.putInt("y", size.getY());
		enclosing.putInt("z", size.getZ());
		metadata.put("EnclosingSize", enclosing);
		Files.createDirectories(file.getParent());
		LitematicFormat.writeFile(new Plan(metadata, Map.of(REGION, region)), file);
	}

	/**
	 * The copy's blocks by their position from its region's lowest corner, leaving out air and structure void. A file
	 * that isn't a copy's (no region {@value #REGION}, a first palette entry other than air, or no block) is an error,
	 * as is one that can't be read.
	 */
	public static Map<BlockPos, BlockState> read(Path file) throws IOException {
		Plan plan = LitematicFormat.readFile(file);
		PlanRegion region = plan.region(REGION)
			.orElseThrow(() -> new IllegalArgumentException("No region " + REGION + " in " + file));
		if (!region.palette().getFirst().isAir()) {
			throw new IllegalArgumentException("A copy whose box is filled with " + region.palette().getFirst());
		}
		BlockPos corner = region.minCorner();
		Map<BlockPos, BlockState> blocks = new HashMap<>();
		region.forEachSetBlock((pos, state) -> {
			if (!empty(state)) {
				blocks.put(pos.subtract(corner), state);
			}
		});
		if (blocks.isEmpty()) {
			throw new IllegalArgumentException("A copy with no blocks in " + file);
		}
		return blocks;
	}

	private static boolean empty(BlockState state) {
		return state.isAir() || state.is(Blocks.STRUCTURE_VOID);
	}
}
