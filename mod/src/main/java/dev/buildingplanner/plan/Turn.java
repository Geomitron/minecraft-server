package dev.buildingplanner.plan;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How a placement is mirrored and turned about its origin: the mirror first, then the rotation, as Litematica places
 * a schematic's blocks ({@code PositionUtils.getTransformedBlockPos}, {@code SchematicPlacingUtils}) and as vanilla
 * places a structure.
 */
public record Turn(Mirror mirror, Rotation rotation) {
	public static final Turn NONE = new Turn(Mirror.NONE, Rotation.NONE);

	/** A position relative to the origin, mirrored, then turned. */
	public BlockPos apply(BlockPos pos) {
		int x = pos.getX();
		int z = pos.getZ();
		switch (mirror) {
			case LEFT_RIGHT -> z = -z;
			case FRONT_BACK -> x = -x;
			default -> {
			}
		}
		return switch (rotation) {
			case CLOCKWISE_90 -> new BlockPos(-z, pos.getY(), x);
			case CLOCKWISE_180 -> new BlockPos(-x, pos.getY(), -z);
			case COUNTERCLOCKWISE_90 -> new BlockPos(z, pos.getY(), -x);
			default -> new BlockPos(x, pos.getY(), z);
		};
	}

	/** A block state mirrored, then turned. */
	public BlockState apply(BlockState state) {
		return state.mirror(mirror).rotate(rotation);
	}

	/** This turn followed by a quarter turn clockwise, seen from above. */
	public Turn turnedClockwise() {
		return new Turn(mirror, rotation.getRotated(Rotation.CLOCKWISE_90));
	}

	/**
	 * This turn followed by a flip left to right for a player facing {@code facing}: across the north-south line when they
	 * face north or south, across the east-west line when they face east or west.
	 */
	public Turn flippedLeftRight(Direction facing) {
		// A flip that reverses x is FRONT_BACK, one that reverses z is LEFT_RIGHT.
		Mirror flip = facing.getClockWise().getAxis() == Direction.Axis.X ? Mirror.FRONT_BACK : Mirror.LEFT_RIGHT;
		// flip after turn r after mirror m: flip then r equals r reversed then flip, and two flips are the flip they
		// share removed, or else a half turn.
		Rotation reversed = switch (rotation) {
			case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
			case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
			default -> rotation;
		};
		if (mirror == Mirror.NONE) {
			return new Turn(flip, reversed);
		}
		if (mirror == flip) {
			return new Turn(Mirror.NONE, reversed);
		}
		return new Turn(Mirror.NONE, reversed.getRotated(Rotation.CLOCKWISE_180));
	}
}
