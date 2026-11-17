package dev.buildingplanner.layer;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.function.BiPredicate;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The material list's counts for the ghost blocks in a box, by block state, as Litematica counts a placement's blocks
 * against the world: every ghost block counts in {@code total}; one with air in the world is {@code missing}; one with
 * another block there is both {@code missing} and {@code wrong}.
 */
public record MaterialCount(Object2IntOpenHashMap<BlockState> total, Object2IntOpenHashMap<BlockState> missing,
	Object2IntOpenHashMap<BlockState> wrong) {

	/**
	 * Counts the ghost blocks of {@code layer} in the box from {@code min} to {@code max}, both corners inside it, where
	 * {@code world} gives the real block at a position and {@code same} whether a real block (its second argument) is
	 * the ghost block (its first) built.
	 */
	public static MaterialCount of(Layer layer, BlockPos min, BlockPos max, Function<BlockPos, BlockState> world,
		BiPredicate<BlockState, BlockState> same) {
		MaterialCount count = new MaterialCount(new Object2IntOpenHashMap<>(), new Object2IntOpenHashMap<>(),
			new Object2IntOpenHashMap<>());
		layer.forEachIn(min, max, (pos, ghost) -> {
			count.total.addTo(ghost, 1);
			BlockState real = world.apply(pos);
			if (real.isAir()) {
				count.missing.addTo(ghost, 1);
			} else if (!same.test(ghost, real)) {
				count.missing.addTo(ghost, 1);
				count.wrong.addTo(ghost, 1);
			}
		});
		return count;
	}

	/** Whether the box holds no ghost block. */
	public boolean isEmpty() {
		return total.isEmpty();
	}
}
