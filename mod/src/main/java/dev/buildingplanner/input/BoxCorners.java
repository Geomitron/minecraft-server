package dev.buildingplanner.input;

import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * The corners of Building Planner's box for Copy, Delete and the material list, drawn with three left clicks as Effortless
 * Building's "Cube" draws ({@code Cube.findSecondPos}, which calls {@code Floor.findFloor}, and {@code
 * ThreeClicksBuildMode.findHeight}), with Building Planner's own reach and limits. Each corner comes from the player's
 * eyes and the way they look; none of them shrinks a box to fit a limit.
 */
public final class BoxCorners {
	/** How far from the player's eyes a corner may be aimed at. */
	public static final double REACH = 256;
	/** The most blocks a box may have along each side. */
	public static final int MAX_SIDE = 256;

	/** A limit a box breaks, so it draws red and its click sets no corner. */
	public enum Limit {
		/** A side longer than {@link #MAX_SIDE}. */
		SIDE,
		/** A chunk the box touches isn't loaded in the player's game, which then knows neither its blocks nor its ghost blocks. */
		NOT_LOADED,
		/** A Delete box holds more ghost blocks than the server's cap for one edit, so one erase can't take them. */
		TOO_MANY_BLOCKS
	}

	/** Whether the player's game has a chunk loaded. */
	@FunctionalInterface
	public interface LoadedChunks {
		boolean loaded(int chunkX, int chunkZ);
	}

	private BoxCorners() {
	}

	/**
	 * The first corner for the block the player aims at and the face they aim at: the space above the block for its top
	 * face, so a box started on the ground holds what stands on it, and otherwise the block itself.
	 */
	public static BlockPos first(BlockPos aimed, Direction face) {
		return face == Direction.UP ? aimed.above() : aimed;
	}

	/**
	 * The second corner: the block where the line of sight from {@code eye} along {@code look} crosses the first corner's
	 * level (the bottom of its block, as {@code BuildModes.findYBound} takes it). Null when the line runs level, crosses
	 * behind the player, or crosses further than {@link #REACH} from the eyes.
	 */
	public static BlockPos second(Vec3 eye, Vec3 look, BlockPos first) {
		if (Math.abs(look.y) < 1e-9) {
			return null;
		}
		double t = (first.getY() - eye.y) / look.y;
		if (t <= 0) {
			return null;
		}
		Vec3 crossing = eye.add(look.scale(t));
		if (crossing.distanceTo(eye) > REACH) {
			return null;
		}
		return new BlockPos((int) Math.floor(crossing.x), first.getY(), (int) Math.floor(crossing.z));
	}

	/**
	 * The height: the level of the block where the line of sight passes nearest the vertical line through the middle of
	 * the second corner. Empty when the player looks straight up or down, the nearest point is behind them, or that
	 * point on the vertical line is further than {@link #REACH} from the eyes.
	 */
	public static OptionalInt height(Vec3 eye, Vec3 look, BlockPos second) {
		double level = look.x * look.x + look.z * look.z;
		if (level < 1e-9) {
			return OptionalInt.empty();
		}
		double x = second.getX() + 0.5;
		double z = second.getZ() + 0.5;
		double t = ((x - eye.x) * look.x + (z - eye.z) * look.z) / level;
		if (t <= 0) {
			return OptionalInt.empty();
		}
		double y = eye.y + t * look.y;
		if (new Vec3(x, y, z).distanceTo(eye) > REACH) {
			return OptionalInt.empty();
		}
		return OptionalInt.of((int) Math.floor(y));
	}

	/** The corner moved up or down into the world's build limits, {@code minY} to {@code maxY} inclusive. */
	public static BlockPos withinHeight(BlockPos corner, int minY, int maxY) {
		return corner.atY(Math.clamp(corner.getY(), minY, maxY));
	}

	/** The box's size along x, y and z, in blocks, from two opposite corners. */
	public static int[] size(BlockPos a, BlockPos b) {
		return new int[] {Math.abs(a.getX() - b.getX()) + 1, Math.abs(a.getY() - b.getY()) + 1,
			Math.abs(a.getZ() - b.getZ()) + 1};
	}

	/** The limit the box between two opposite corners breaks, a side's length first, or null when it breaks none. */
	public static Limit limit(BlockPos a, BlockPos b, LoadedChunks loaded) {
		for (int side : size(a, b)) {
			if (side > MAX_SIDE) {
				return Limit.SIDE;
			}
		}
		for (int x = Math.min(a.getX(), b.getX()) >> 4; x <= Math.max(a.getX(), b.getX()) >> 4; x++) {
			for (int z = Math.min(a.getZ(), b.getZ()) >> 4; z <= Math.max(a.getZ(), b.getZ()) >> 4; z++) {
				if (!loaded.loaded(x, z)) {
					return Limit.NOT_LOADED;
				}
			}
		}
		return null;
	}
}
