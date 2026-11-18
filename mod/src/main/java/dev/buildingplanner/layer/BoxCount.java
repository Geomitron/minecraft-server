package dev.buildingplanner.layer;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * The blocks in a box by the copy's rule: at each position the ghost block from the layer while ghost blocks show, and
 * otherwise the real block, leaving out air. It reads whole 16-block sections: a section the box takes in whole, with no
 * ghost blocks, is counted from its palette; a section of only air with no ghost blocks is skipped; only the sections
 * the box cuts or that hold ghost blocks are read position by position.
 */
public final class BoxCount {
	private static final int MASK = LayerSection.SIDE - 1;

	/** The player's real blocks, by section. */
	@FunctionalInterface
	public interface Sections {
		/** The real section at those section coordinates, or null where the game holds none. */
		LevelChunkSection at(int sectionX, int sectionY, int sectionZ);
	}

	private BoxCount() {
	}

	/**
	 * How many of each block the box from {@code min} to {@code max} (both corners inside it) holds by the copy's rule,
	 * with ghost blocks from {@code layer} while they're {@code ghostsShown}, and real blocks from {@code world}.
	 */
	public static Object2IntOpenHashMap<BlockState> of(BlockPos min, BlockPos max, Layer layer, Sections world,
		boolean ghostsShown) {
		Object2IntOpenHashMap<BlockState> counts = new Object2IntOpenHashMap<>();
		walk(min, max, layer, world, ghostsShown, (pos, state) -> counts.addTo(state, 1), counts);
		return counts;
	}

	/**
	 * Calls {@code action} for each position of the box from {@code min} to {@code max} that holds a block by the copy's
	 * rule, with that block, skipping sections with no block.
	 */
	public static void forEach(BlockPos min, BlockPos max, Layer layer, Sections world, boolean ghostsShown,
		BiConsumer<BlockPos, BlockState> action) {
		walk(min, max, layer, world, ghostsShown, action, null);
	}

	/** Walks the box's sections; with {@code counts}, a whole section with no ghost blocks goes there from its palette. */
	private static void walk(BlockPos min, BlockPos max, Layer layer, Sections world, boolean ghostsShown,
		BiConsumer<BlockPos, BlockState> action, Object2IntOpenHashMap<BlockState> counts) {
		for (int sx = min.getX() >> LayerSection.SHIFT; sx <= max.getX() >> LayerSection.SHIFT; sx++) {
			int x0 = Math.max(min.getX(), sx << LayerSection.SHIFT);
			int x1 = Math.min(max.getX(), (sx << LayerSection.SHIFT) + MASK);
			for (int sz = min.getZ() >> LayerSection.SHIFT; sz <= max.getZ() >> LayerSection.SHIFT; sz++) {
				int z0 = Math.max(min.getZ(), sz << LayerSection.SHIFT);
				int z1 = Math.min(max.getZ(), (sz << LayerSection.SHIFT) + MASK);
				LayerColumn column = ghostsShown ? layer.column(sx, sz) : null;
				for (int sy = min.getY() >> LayerSection.SHIFT; sy <= max.getY() >> LayerSection.SHIFT; sy++) {
					int y0 = Math.max(min.getY(), sy << LayerSection.SHIFT);
					int y1 = Math.min(max.getY(), (sy << LayerSection.SHIFT) + MASK);
					LayerSection ghosts = column == null ? null : column.sections().get(sy);
					LevelChunkSection real = world.at(sx, sy, sz);
					boolean noReal = real == null || real.hasOnlyAir();
					if (ghosts == null && noReal) {
						continue;
					}
					boolean whole = x1 - x0 == MASK && y1 - y0 == MASK && z1 - z0 == MASK;
					if (ghosts == null && whole && counts != null) {
						real.getStates().count((state, n) -> {
							if (!state.isAir()) {
								counts.addTo(state, n);
							}
						});
						continue;
					}
					BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
					for (int y = y0; y <= y1; y++) {
						for (int z = z0; z <= z1; z++) {
							for (int x = x0; x <= x1; x++) {
								BlockState state = ghosts == null ? LayerSection.AIR
									: ghosts.get(LayerSection.index(x, y, z));
								if (state.isAir() && !noReal) {
									state = real.getBlockState(x & MASK, y & MASK, z & MASK);
								}
								if (!state.isAir()) {
									action.accept(counts == null ? pos.set(x, y, z).immutable() : pos.set(x, y, z),
										state);
								}
							}
						}
					}
				}
			}
		}
	}
}
