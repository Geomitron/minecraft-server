package dev.buildingplanner.layer;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * The material list's counts for the blocks in a box, by block state. {@code total} holds every block of the box by the
 * copy's rule ({@link BoxCount}): at each position the ghost block while ghost blocks show, and otherwise the real
 * block. {@code missing} and {@code wrong} count only ghost blocks, as Litematica counts a placement's blocks against
 * the world: one with air in the world is {@code missing}; one with another block there is both {@code missing} and
 * {@code wrong}. A real block with no ghost block over it is already built, so it counts only in {@code total}. While
 * ghost blocks are hidden, the box counts as if it held none.
 */
public record MaterialCount(Object2IntOpenHashMap<BlockState> total, Object2IntOpenHashMap<BlockState> missing,
	Object2IntOpenHashMap<BlockState> wrong) {
	private static final int MASK = LayerSection.SIDE - 1;

	/**
	 * Counts the blocks in the box from {@code min} to {@code max}, both corners inside it, with ghost blocks from
	 * {@code layer} and real blocks from {@code world}, where {@code same} says whether a real block (its second argument)
	 * is the ghost block (its first) built. {@code ghostsShown} is whether the player's ghost blocks show.
	 */
	public static MaterialCount of(Layer layer, BlockPos min, BlockPos max, BoxCount.Sections world,
		BiPredicate<BlockState, BlockState> same, boolean ghostsShown) {
		MaterialCount count = new MaterialCount(BoxCount.of(min, max, layer, world, ghostsShown),
			new Object2IntOpenHashMap<>(), new Object2IntOpenHashMap<>());
		if (!ghostsShown) {
			return count;
		}
		layer.forEachIn(min, max, (pos, ghost) -> {
			BlockState real = realAt(world, pos);
			if (real.isAir()) {
				count.missing.addTo(ghost, 1);
			} else if (!same.test(ghost, real)) {
				count.missing.addTo(ghost, 1);
				count.wrong.addTo(ghost, 1);
			}
		});
		return count;
	}

	private static BlockState realAt(BoxCount.Sections world, BlockPos pos) {
		LevelChunkSection section = world.at(pos.getX() >> LayerSection.SHIFT, pos.getY() >> LayerSection.SHIFT,
			pos.getZ() >> LayerSection.SHIFT);
		return section == null ? LayerSection.AIR
			: section.getBlockState(pos.getX() & MASK, pos.getY() & MASK, pos.getZ() & MASK);
	}

	/** Whether the box holds no block at all. */
	public boolean isEmpty() {
		return total.isEmpty();
	}
}
