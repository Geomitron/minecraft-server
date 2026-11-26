package dev.buildingplanner.input;

import dev.buildingplanner.input.CopyPlacement.Spot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * A set of copies in Line, Wall or Floor (see "Copy and paste": "The copy's mode"): copies side by side from the first,
 * each the copy's own size along each direction (as it's turned) from the one before, so they touch without
 * overlapping, up to the copy that holds the block Effortless Building's {@code Line.findLine}, {@code Wall.findWall}
 * or {@code Floor.findFloor} gives from the first copy's middle. Those give a block on one line through the middle
 * (Line), in one upright plane (Wall) or on the middle's level (Floor), so the set is a row, or a grid in that line's
 * or plane's directions.
 */
public final class CopyGrid {
	/** The most copies along each direction, as Satisfactory zoops up to 10 (1238 Q4 A). */
	public static final int MAX_COPIES = 10;

	private CopyGrid() {
	}

	/**
	 * The boxes of the set's copies, the first copy's box {@code first} first, reaching to the block {@code reached}:
	 * along each of east-west, up-down and north-south, from the first copy toward {@code reached}, as many copies as
	 * reach the copy that holds it, at most {@code most}; along a direction where {@code reached} lies within the first
	 * copy, one. The copies come in the order they lie from the first, row by row: along east-west first, then
	 * north-south, then up-down. Off is the first copy alone.
	 */
	public static List<Spot> of(CopyMode mode, Spot first, BlockPos reached, int most) {
		if (mode == CopyMode.OFF) {
			return List.of(first);
		}
		int[] counts = new int[3];
		Direction[] directions = new Direction[3];
		Direction.Axis[] axes = {Direction.Axis.X, Direction.Axis.Z, Direction.Axis.Y};
		for (int i = 0; i < axes.length; i++) {
			Direction.Axis axis = axes[i];
			int length = axis.choose(first.size().getX(), first.size().getY(), first.size().getZ());
			int low = axis.choose(first.min().getX(), first.min().getY(), first.min().getZ());
			int high = low + length - 1;
			int at = axis.choose(reached.getX(), reached.getY(), reached.getZ());
			int middle = axis.choose(first.middle().getX(), first.middle().getY(), first.middle().getZ());
			boolean positive = at >= middle;
			directions[i] = Direction.fromAxisAndDirection(axis,
				positive ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
			int beyond = positive ? at - low : high - at;
			counts[i] = Math.clamp(Math.floorDiv(beyond, length) + 1, 1, most);
		}
		List<Spot> set = new ArrayList<>(counts[0] * counts[1] * counts[2]);
		for (int up = 0; up < counts[2]; up++) {
			for (int across = 0; across < counts[1]; across++) {
				for (int along = 0; along < counts[0]; along++) {
					BlockPos min = first.min()
						.relative(directions[0], along * first.size().getX())
						.relative(directions[1], across * first.size().getZ())
						.relative(directions[2], up * first.size().getY());
					set.add(new Spot(min, first.size()));
				}
			}
		}
		return set;
	}

	/** The box around the whole set. */
	public static Spot box(List<Spot> set) {
		BlockPos min = set.getFirst().min();
		BlockPos max = set.getFirst().max();
		for (Spot spot : set) {
			min = BlockPos.min(min, spot.min());
			max = BlockPos.max(max, spot.max());
		}
		return Spot.between(min, max);
	}
}
