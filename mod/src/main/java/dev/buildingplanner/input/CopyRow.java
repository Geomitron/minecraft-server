package dev.buildingplanner.input;

import dev.buildingplanner.input.CopyPlacement.Spot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * A zoop's row, as Satisfactory zoops foundations: copies end to end from the first, each the copy's own length (as
 * it's turned) from the one before, so they touch without overlapping, up to 10 copies, or 32 for the one ghost block
 * (a 1×1×1 copy). In Zoop the row goes along whichever of the four compass directions from the first copy the aim is
 * farthest along; in Vertical, up or down.
 */
public final class CopyRow {
	/** The most copies in a row of the copy, as Satisfactory zoops up to 10. */
	public static final int MAX_COPIES = 10;
	/** The most blocks in a row of the one ghost block (Geo's answer to inbox request 980, Q2). */
	public static final int MAX_BLOCKS = 32;

	private CopyRow() {
	}

	/**
	 * The boxes of the row's copies, the first copy's box {@code first} first, reaching toward the block {@code toward}:
	 * the row ends with the copy that holds it along the row's direction, or with copy number {@code longest}. In Zoop
	 * the direction is the compass direction in which {@code toward} lies farthest from the first copy's middle, east or
	 * west on a tie; in Vertical it's up or down. In Single, and with {@code toward} within the first copy along the
	 * direction, the row is the first copy alone.
	 */
	public static List<Spot> row(CopyMode mode, Spot first, BlockPos toward, int longest) {
		BlockPos middle = first.middle();
		Direction direction = switch (mode) {
			case SINGLE -> null;
			case ZOOP -> {
				int dx = toward.getX() - middle.getX();
				int dz = toward.getZ() - middle.getZ();
				yield Math.abs(dx) >= Math.abs(dz) ? (dx >= 0 ? Direction.EAST : Direction.WEST)
					: (dz >= 0 ? Direction.SOUTH : Direction.NORTH);
			}
			case VERTICAL -> toward.getY() >= middle.getY() ? Direction.UP : Direction.DOWN;
		};
		if (direction == null) {
			return List.of(first);
		}
		Direction.Axis axis = direction.getAxis();
		int length = axis.choose(first.size().getX(), first.size().getY(), first.size().getZ());
		int at = axis.choose(toward.getX(), toward.getY(), toward.getZ());
		int low = axis.choose(first.min().getX(), first.min().getY(), first.min().getZ());
		int high = low + length - 1;
		int beyond = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? at - low : high - at;
		int count = Math.clamp(Math.floorDiv(beyond, length) + 1, 1, longest);
		List<Spot> row = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			row.add(new Spot(first.min().relative(direction, i * length), first.size()));
		}
		return row;
	}

	/**
	 * Where a Vertical row reaches for a view from {@code eye} looking along {@code look} (a unit vector), up to
	 * {@code reach} blocks: the block, above or below the first copy's middle, of the point on the view's line nearest
	 * the vertical line through the middle of that block, so the row follows the view up and down without a block to
	 * aim at. A view straight up or down reaches as far as {@code reach}.
	 */
	public static BlockPos verticalAim(Spot first, Vec3 eye, Vec3 look, double reach) {
		BlockPos middle = first.middle();
		double lookAcross = look.x * look.x + look.z * look.z;
		double along = lookAcross < 1e-9 ? reach
			: ((middle.getX() + 0.5 - eye.x) * look.x + (middle.getZ() + 0.5 - eye.z) * look.z) / lookAcross;
		double y = eye.y + Math.clamp(along, 0, reach) * look.y;
		return new BlockPos(middle.getX(), (int) Math.floor(y), middle.getZ());
	}

	/** The box around the whole row. */
	public static Spot box(List<Spot> row) {
		BlockPos min = row.getFirst().min();
		BlockPos max = row.getFirst().max();
		for (Spot spot : row) {
			min = BlockPos.min(min, spot.min());
			max = BlockPos.max(max, spot.max());
		}
		return Spot.between(min, max);
	}
}
