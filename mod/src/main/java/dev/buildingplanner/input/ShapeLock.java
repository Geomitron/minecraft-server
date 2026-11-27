package dev.buildingplanner.input;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The build modes' lock (see "Locking and the first click"), a lock for each step of a shape: the step is the number of
 * positions the shape's clicks have set, 0 before the first click, 1 while a shape finds its second position and 2
 * while a mode of three clicks finds its third. Before the first click, the locked position keeps the face and hit
 * point aimed at when it was locked, moved by the nudges, and the first click and its preview take it (hooks E9 and
 * E32). During a shape, each step starts unlocked; locked, Effortless Building takes the step's locked position in
 * place of the one its aim finds (E37). A nudge along a direction the step sets moves the step's position, and along
 * any other the whole shape: the positions the clicks have set stay Effortless Building's own, which the caller moves
 * (E33), and the first position locked before the first click moves with them, so a cancelled shape leaves it locked
 * at the shape's first position. The click that finishes a shape ends the lock, placing or erasing (1239 Q1 A).
 */
public final class ShapeLock {
	/**
	 * How Effortless Building finds a shape's second or third position, which sets the directions a nudge changes it
	 * along ({@link #sets}): {@code Line.findLine}, {@code Wall.findWall}, {@code Floor.findFloor} and
	 * {@code ThreeClicksBuildMode.findHeight}.
	 */
	public enum Finder {
		LINE, WALL, FLOOR, HEIGHT
	}

	/** The first position locked before the first click, or null while that lock is off. */
	private BlockPos first;
	/** The face aimed at when the first position was locked. */
	private Direction face;
	/** The hit point aimed at when the first position was locked, from that position, which nudges move it with. */
	private Vec3 hitFromPos;
	/** The step a shape's step's lock is for: 1 or 2. */
	private int step;
	/** The shape's step's locked position, or null while that lock is off. */
	private BlockPos stepPos;

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

	/**
	 * Hook E38: the positions the red frame for the left click shows on before the first click (see "Locking and the
	 * first click": "The first click's blocks"). {@code leftClick} gives where a left click would start erasing, the
	 * aimed block with its copies by the mirror, radial mirror and array, or none while the player aims at nothing.
	 * Nothing shows while the lock's controls aren't active (no session, Disable, a mouse mode on, or no item that
	 * places a block in hand), during a shape (a step past 0), or while the first position is locked, where both
	 * clicks start, which shows the copy's box. In Single, a left click erases ghost blocks and nothing else, so only
	 * the ghost blocks among the positions ({@code ghost}) show it (1369); in the other modes, every position, where
	 * the erasing shape starts.
	 */
	public static List<BlockPos> framed(boolean controlsActive, boolean single, int step, boolean firstLocked,
		Supplier<List<BlockPos>> leftClick, Predicate<BlockPos> ghost) {
		if (!controlsActive || step != 0 || firstLocked) {
			return List.of();
		}
		List<BlockPos> positions = leftClick.get();
		return single ? positions.stream().filter(ghost).toList() : positions;
	}

	/**
	 * How the step finds its position: the third position by {@code findHeight}; the second by {@code findLine} in
	 * Line, {@code findWall} in Wall, and {@code findFloor} in every other mode of two or three clicks.
	 */
	public static Finder finder(boolean line, boolean wall, int step) {
		if (step >= 2) {
			return Finder.HEIGHT;
		}
		return line ? Finder.LINE : wall ? Finder.WALL : Finder.FLOOR;
	}

	/**
	 * The position {@code finder} finds the step's position from, among the positions the clicks have set (the first
	 * first): the second for {@code findHeight}, and otherwise the first.
	 */
	public static BlockPos from(Finder finder, List<BlockPos> set) {
		return finder == Finder.HEIGHT ? set.get(1) : set.getFirst();
	}

	/**
	 * Whether a nudge that way moves the step's position {@code pos}, found by {@code finder} from {@code from}, which
	 * changes the shape's size; a nudge along any other direction moves the whole shape. Line's second position: along
	 * the line, or every direction while the line is one block long. Wall's: up and down, and along the wall, or both
	 * level directions while it's one block wide. {@code findFloor}'s: both level directions. {@code findHeight}'s: up
	 * and down.
	 */
	public static boolean sets(Finder finder, BlockPos from, BlockPos pos, Direction direction) {
		Direction.Axis axis = direction.getAxis();
		return switch (finder) {
			case LINE -> from.equals(pos) || axis == lineAxis(from, pos);
			case WALL -> axis == Direction.Axis.Y || from.getX() == pos.getX() && from.getZ() == pos.getZ()
				|| axis == wallAxis(from, pos);
			case FLOOR -> axis != Direction.Axis.Y;
			case HEIGHT -> axis == Direction.Axis.Y;
		};
	}

	/**
	 * The axis a line between the two lies along, as {@code Line.getLineBlocks} takes it: X where they differ, then Y,
	 * then Z.
	 */
	private static Direction.Axis lineAxis(BlockPos from, BlockPos pos) {
		if (from.getX() != pos.getX()) {
			return Direction.Axis.X;
		}
		return from.getY() != pos.getY() ? Direction.Axis.Y : Direction.Axis.Z;
	}

	/**
	 * The level axis a wall between the two lies along, as {@code Wall.getWallBlocks} takes it: Z where they share
	 * their X, otherwise X.
	 */
	private static Direction.Axis wallAxis(BlockPos from, BlockPos pos) {
		return from.getX() == pos.getX() ? Direction.Axis.Z : Direction.Axis.X;
	}

	/**
	 * Whether the step under way is locked. Reading the lock for a step other than the one a shape's step was locked
	 * for ends that step's lock: a click has set its position, so the next step starts unlocked, or the shape has
	 * ended. The first position locked before the first click stays through a shape.
	 */
	public boolean on(int step) {
		follow(step);
		return step == 0 ? first != null : stepPos != null;
	}

	/** Ends a shape's step's lock once another step is under way. */
	private void follow(int step) {
		if (stepPos != null && this.step != step) {
			stepPos = null;
		}
	}

	/**
	 * Whether the next first click, and the first click's preview, take the locked first position (hooks E9 and E32):
	 * the lock's controls are active, no shape is under way (step 0), whose positions are already set, and the first
	 * position is locked.
	 */
	public boolean takesFirstClick(boolean controlsActive, int step) {
		return controlsActive && step == 0 && on(0);
	}

	/** The step's locked position; only while {@link #on} for that step. */
	public BlockPos pos(int step) {
		return step == 0 ? first : stepPos;
	}

	/**
	 * Hook E37: the position a shape's step takes, its locked position while it's locked, or otherwise the one
	 * Effortless Building's aim found, {@code found}.
	 */
	public BlockPos position(int step, BlockPos found) {
		return step > 0 && on(step) ? stepPos : found;
	}

	/**
	 * Locks the first position at {@code pos} before the first click, with {@code hit} the hit its blocks' states come
	 * from: the aim's at the block a right click would start from.
	 */
	public void lock(BlockPos pos, BlockHitResult hit) {
		this.first = pos.immutable();
		this.face = hit.getDirection();
		this.hitFromPos = hit.getLocation().subtract(Vec3.atLowerCornerOf(pos));
	}

	/** Locks a shape's step (1 or 2) at {@code pos}, the position the aim is setting for it at that moment. */
	public void lockStep(int step, BlockPos pos) {
		this.step = step;
		this.stepPos = pos.immutable();
	}

	/** H on a locked step: ends its lock; during a shape, the first position locked before the first click stays. */
	public void unlock(int step) {
		if (step == 0) {
			first = null;
		} else if (this.step == step) {
			stepPos = null;
		}
	}

	/** Ends the lock of every step: the click that finishes a shape, or anything else that ends the lock. */
	public void unlock() {
		first = null;
		stepPos = null;
	}

	/**
	 * A nudge that way while the step under way is locked, with {@code set} the positions the clicks have set (none
	 * before the first click) and {@code finder} how the step finds its position (unused before the first click).
	 * Before the first click, it moves the locked first position, and its hit point with it. During a shape, along a
	 * direction the step sets ({@link #sets}) it moves the step's position; along any other, the step's position and
	 * the first position locked before the first click, and it returns true: the caller moves the positions the clicks
	 * have set and the first click's hit (hook E33). Does nothing while the step isn't locked.
	 */
	public boolean nudge(List<BlockPos> set, Finder finder, Direction direction) {
		int step = set.size();
		if (!on(step)) {
			return false;
		}
		if (step == 0) {
			first = first.relative(direction);
			return false;
		}
		boolean whole = !sets(finder, from(finder, set), stepPos, direction);
		stepPos = stepPos.relative(direction);
		if (whole && first != null) {
			first = first.relative(direction);
		}
		return whole;
	}

	/**
	 * The hit the locked first position's blocks take their states from: the face aimed at when it was locked and the
	 * hit point moved with each nudge, on the locked position. Only while the first position is locked.
	 */
	public BlockHitResult hit() {
		return new BlockHitResult(Vec3.atLowerCornerOf(first).add(hitFromPos), face, first, false);
	}

	/** {@code hit} moved one block that way, as a nudge that moves the whole shape moves its first click (hook E33). */
	public static BlockHitResult moved(BlockHitResult hit, Direction direction) {
		return new BlockHitResult(hit.getLocation().add(direction.getStepX(), direction.getStepY(), direction.getStepZ()),
			hit.getDirection(), hit.getBlockPos().relative(direction), hit.isInside());
	}
}
