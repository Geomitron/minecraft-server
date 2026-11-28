package dev.buildingplanner.input;

import dev.buildingplanner.plan.Turn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

/**
 * Where the copy at the aim sits, as boxes of world positions: against the face the player aims at, with the middle of
 * the copy's side that touches the face on the block beside it, as a Satisfactory hologram sits on the surface under
 * the crosshair. A side an even number of blocks across has its middle on the block before the halfway point, in the
 * direction of lower coordinates. Turning and mirroring keep the copy's middle where it is.
 */
public final class CopyPlacement {
	private CopyPlacement() {
	}

	/** A box of world positions from {@code min}, {@code size} blocks along each axis. */
	public record Spot(BlockPos min, Vec3i size) {
		public BlockPos max() {
			return min.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
		}

		/** The block in the middle of the box, the lower one along a side an even number of blocks across. */
		public BlockPos middle() {
			return min.offset(CopyPlacement.middle(size.getX()), CopyPlacement.middle(size.getY()),
				CopyPlacement.middle(size.getZ()));
		}

		public boolean contains(BlockPos pos) {
			BlockPos max = max();
			return pos.getX() >= min.getX() && pos.getX() <= max.getX() && pos.getY() >= min.getY()
				&& pos.getY() <= max.getY() && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
		}

		/** The box from {@code min} to {@code max}, both included. */
		public static Spot between(BlockPos min, BlockPos max) {
			return new Spot(min, new Vec3i(max.getX() - min.getX() + 1, max.getY() - min.getY() + 1,
				max.getZ() - min.getZ() + 1));
		}
	}

	/** How far into a side of {@code length} blocks its middle block is. */
	public static int middle(int length) {
		return (length - 1) / 2;
	}

	/** The size of a copy of {@code size} once turned: its sides along x and z swap for a quarter turn. */
	public static Vec3i turnedSize(Vec3i size, Turn turn) {
		return switch (turn.rotation()) {
			case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> new Vec3i(size.getZ(), size.getY(), size.getX());
			default -> size;
		};
	}

	/**
	 * Where the turned copy's lowest corner is from the placement's origin: the schematic's positions 0 to size - 1 on
	 * each axis, mirrored and turned about the origin as Litematica places them, reach no lower than this.
	 */
	public static BlockPos cornerFromOrigin(Vec3i size, Turn turn) {
		BlockPos a = turn.apply(BlockPos.ZERO);
		BlockPos b = turn.apply(new BlockPos(size.getX() - 1, size.getY() - 1, size.getZ() - 1));
		return new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
	}

	/** The placement's origin that puts a copy of {@code size}, turned by {@code turn}, with its lowest corner at {@code min}. */
	public static BlockPos origin(BlockPos min, Vec3i size, Turn turn) {
		return min.subtract(cornerFromOrigin(size, turn));
	}

	/**
	 * The box of a copy, {@code size} blocks once turned, resting against the face {@code face} of the block {@code
	 * aimed}: on top of a top face, under a bottom face, beside a side face, with the middle of its side that touches the
	 * face on the block beside the face.
	 */
	public static Spot againstFace(BlockPos aimed, Direction face, Vec3i size) {
		BlockPos beside = aimed.relative(face);
		int[] min = new int[3];
		for (Direction.Axis axis : Direction.Axis.values()) {
			int at = axis.choose(beside.getX(), beside.getY(), beside.getZ());
			int length = axis.choose(size.getX(), size.getY(), size.getZ());
			int i = axis.ordinal();
			if (axis != face.getAxis()) {
				min[i] = at - middle(length);
			} else if (face.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
				min[i] = at;
			} else {
				min[i] = at - length + 1;
			}
		}
		return new Spot(new BlockPos(min[0], min[1], min[2]), size);
	}

	/**
	 * The box of a copy, {@code size} blocks once turned, where it shows while the player aims at no block (1464 Q1 A):
	 * on the level of the player's feet {@code feet}, its near side two blocks in front of them in the horizontal
	 * direction {@code facing} the player faces, and the middle of its width across that direction in line with the
	 * feet, as a copy against a face has its middle on the block beside the face. It never covers the player's feet or
	 * the block in front of them, whichever way the player faces.
	 */
	public static Spot inFront(BlockPos feet, Direction facing, Vec3i size) {
		BlockPos min = againstFace(feet.relative(facing), facing, size).min();
		return new Spot(min.atY(feet.getY()), size);
	}

	/**
	 * The box of a copy turned or mirrored from {@code before} to {@code size} blocks, keeping the middle block where it
	 * was, so it spins in place.
	 */
	public static Spot aroundMiddle(Spot before, Vec3i size) {
		BlockPos middle = before.middle();
		return new Spot(middle.offset(-middle(size.getX()), -middle(size.getY()), -middle(size.getZ())), size);
	}
}
