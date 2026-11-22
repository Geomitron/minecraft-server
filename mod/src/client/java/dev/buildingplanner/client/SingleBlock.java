package dev.buildingplanner.client;

import dev.buildingplanner.input.CopyMode;
import dev.buildingplanner.input.SingleRule;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The one ghost block's state in Effortless Building's Single mode (see "One ghost block"), beside the copy's
 * {@link Clipboard}: its own mode, Single, Zoop or Vertical; where it's locked, with the face and hit point aimed at
 * when it was locked, or nothing while it follows the aim; and a fixed row's start, with the hit it was fixed with, and
 * the row's positions as they follow the aim. It uses none of Effortless Building's classes.
 */
final class SingleBlock {
	/**
	 * The block's mode, apart from the copy's (980 Q3 A), which starts as Single each time the game starts and stays as
	 * the player last set it while the game runs, as the copy's does.
	 */
	private static CopyMode mode = CopyMode.SINGLE;
	/** The locked position, or null while the block follows the aim. */
	private BlockPos pos;
	/** The face aimed at when the block was locked. */
	private Direction face;
	/** The hit point aimed at when the block was locked, from the locked position, which nudges move it with. */
	private Vec3 hitFromPos;
	/** The hit a fixed row was fixed with, on its start, or null while no row is fixed. */
	private BlockHitResult rowHit;
	/** The fixed row's positions, its start first. */
	private List<BlockPos> row = List.of();

	/** The block's mode. */
	static CopyMode mode() {
		return mode;
	}

	/** "Change Zoop or Delete mode" for the block: Single, Zoop and Vertical, in that order. */
	static void cycleMode() {
		mode = mode.next();
	}

	/** Whether the block is locked. */
	boolean locked() {
		return pos != null;
	}

	/** The locked position; only while {@link #locked()}. */
	BlockPos pos() {
		return pos;
	}

	/** Locks the block at {@code pos}, the block a right click on {@code hit} would place. */
	void lock(BlockPos pos, BlockHitResult hit) {
		this.pos = pos.immutable();
		this.face = hit.getDirection();
		this.hitFromPos = hit.getLocation().subtract(Vec3.atLowerCornerOf(pos));
	}

	/** Unlocks the block, which follows the aim again. */
	void unlock() {
		pos = null;
	}

	/** Unlocks the block and drops its row, as leaving, a mouse mode or another build mode does. */
	void clear() {
		unlock();
		dropRow();
	}

	/** Moves the locked block one block that way; does nothing while it's unlocked. */
	void nudge(Direction direction) {
		if (pos != null) {
			pos = pos.relative(direction);
		}
	}

	/** Whether a Zoop or Vertical row is fixed, after its first click. */
	boolean rowFixed() {
		return rowHit != null;
	}

	/**
	 * Fixes a row's start at {@code start}, with {@code hit} the hit its blocks' states come from: the locked block's
	 * ({@link #hit}) or the aim's at the first click. The row is the start alone until it follows the aim.
	 */
	void fixRow(BlockPos start, BlockHitResult hit) {
		rowHit = new BlockHitResult(hit.getLocation(), hit.getDirection(), start.immutable(), false);
		row = List.of(start.immutable());
	}

	/** The fixed row's start; only while {@link #rowFixed()}. */
	BlockPos rowStart() {
		return rowHit.getBlockPos();
	}

	/** The fixed row's positions, its start first; only while {@link #rowFixed()}. */
	List<BlockPos> row() {
		return row;
	}

	/** Lays the fixed row along these positions, its start first. */
	void showRow(List<BlockPos> positions) {
		row = List.copyOf(positions);
	}

	/** Drops the fixed row; the block follows the aim again, or stays locked where it was. */
	void dropRow() {
		rowHit = null;
		row = List.of();
	}

	/**
	 * The hit the block's states come from, by its placement rules: while a row is fixed, the hit it was fixed with;
	 * otherwise the locked block's, the face aimed at when it was locked and the hit point moved with each nudge, on the
	 * locked position, as hook E1 gives each block of a shape.
	 */
	BlockHitResult hit() {
		if (rowHit != null) {
			return rowHit;
		}
		return new BlockHitResult(Vec3.atLowerCornerOf(pos).add(hitFromPos), face, pos, false);
	}

	/** The block as its clicks see it ({@link SingleRule}). */
	SingleRule.State state() {
		return new SingleRule.State(mode, locked(), rowFixed());
	}
}
