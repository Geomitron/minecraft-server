package dev.buildingplanner.input;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The build modes' lock (see "Locking and the first click"): whether it's on, and the locked first position, with the
 * face and hit point aimed at when it was locked, moved by the nudges. Before a shape's first click, the first click
 * and its preview take that position (hooks E9 and E32). During a shape, the positions its clicks have set stay
 * Effortless Building's own, and the nudges move them together with this one (E33), so a cancelled shape leaves the
 * lock at the shape's first position. The click that finishes a shape ends the lock, placing or erasing (1239 Q1 A).
 */
public final class ShapeLock {
	/** The locked first position, or null while the lock is off. */
	private BlockPos pos;
	/** The face aimed at when the position was locked. */
	private Direction face;
	/** The hit point aimed at when the position was locked, from the locked position, which nudges move it with. */
	private Vec3 hitFromPos;

	/**
	 * Whether the lock's controls are active: Building Planner has a session, Effortless Building's mode isn't Disable,
	 * no mouse mode is on, and the main hand holds an item that places a block.
	 */
	public static boolean controlsActive(boolean session, boolean disableMode, boolean mouseMode, boolean placeable) {
		return session && !disableMode && !mouseMode && placeable;
	}

	/**
	 * Hook E30: whether Effortless Building's preview draws a set of {@code size} blocks it would skip before a shape's
	 * first click: the first click's block alone, at the aim or at the locked position, while the lock's controls are
	 * active. A set of more than one block, the mirror's, radial mirror's or array's copies with it, is drawn as it is.
	 */
	public static boolean drawnBeforeFirstClick(boolean controlsActive, int size) {
		return controlsActive && size == 1;
	}

	/** Whether the lock is on. */
	public boolean on() {
		return pos != null;
	}

	/**
	 * Whether the next first click, and the first click's preview, take the locked position (hooks E9 and E32): the
	 * lock is on, its controls are active, and no shape is under way, whose positions are already set.
	 */
	public boolean takesFirstClick(boolean controlsActive, boolean shapeUnderWay) {
		return on() && controlsActive && !shapeUnderWay;
	}

	/** The locked first position; only while {@link #on()}. */
	public BlockPos pos() {
		return pos;
	}

	/**
	 * Locks the first position at {@code pos}, with {@code hit} the hit its blocks' states come from: before the first
	 * click, the aim's at the block a right click would start from; during a shape, its first click's.
	 */
	public void lock(BlockPos pos, BlockHitResult hit) {
		this.pos = pos.immutable();
		this.face = hit.getDirection();
		this.hitFromPos = hit.getLocation().subtract(Vec3.atLowerCornerOf(pos));
	}

	/** Ends the lock: H again, the click that finishes a shape, or anything else that ends it. */
	public void unlock() {
		pos = null;
	}

	/** Moves the locked position, and its hit point with it, one block that way; does nothing while the lock is off. */
	public void nudge(Direction direction) {
		if (pos != null) {
			pos = pos.relative(direction);
		}
	}

	/**
	 * The hit the locked position's blocks take their states from: the face aimed at when it was locked and the hit
	 * point moved with each nudge, on the locked position. Only while {@link #on()}.
	 */
	public BlockHitResult hit() {
		return new BlockHitResult(Vec3.atLowerCornerOf(pos).add(hitFromPos), face, pos, false);
	}

	/** {@code hit} moved one block that way, as a nudge moves a shape's first click (hook E33). */
	public static BlockHitResult moved(BlockHitResult hit, Direction direction) {
		return new BlockHitResult(hit.getLocation().add(direction.getStepX(), direction.getStepY(), direction.getStepZ()),
			hit.getDirection(), hit.getBlockPos().relative(direction), hit.isInside());
	}
}
