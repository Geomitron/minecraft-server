package dev.buildingplanner.client;

import dev.buildingplanner.input.CopyMode;
import dev.buildingplanner.input.SingleRule;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The one ghost block's state in Effortless Building's Single mode (see "One ghost block"), beside the copy's
 * {@link Clipboard}: its own mode, Single, Zoop or Vertical, and a fixed row's start, with the hit it was fixed with, and
 * the row's positions as they follow the aim. The lock is the build modes' own ({@code ShapeLock}). It uses none of
 * Effortless Building's classes.
 */
final class SingleBlock {
	/**
	 * The block's mode, apart from the copy's (980 Q3 A), which starts as Single each time the game starts and stays as
	 * the player last set it while the game runs, as the copy's does.
	 */
	private static CopyMode mode = CopyMode.SINGLE;
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

	/** Drops the block's row, as leaving, a mouse mode or another build mode does. */
	void clear() {
		dropRow();
	}

	/** Whether a Zoop or Vertical row is fixed, after its first click. */
	boolean rowFixed() {
		return rowHit != null;
	}

	/**
	 * Fixes a row's start at {@code start}, with {@code hit} the hit its blocks' states come from: the lock's or the
	 * aim's at the first click. The row is the start alone until it follows the aim.
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

	/** Drops the fixed row; the block follows the aim again. */
	void dropRow() {
		rowHit = null;
		row = List.of();
	}

	/**
	 * The hit the row's states come from, by its placement rules: the hit it was fixed with, as hook E1 gives each
	 * block of a shape. Only while {@link #rowFixed()}.
	 */
	BlockHitResult hit() {
		return rowHit;
	}

	/** The block as its clicks see it ({@link SingleRule}). */
	SingleRule.State state() {
		return new SingleRule.State(mode, rowFixed());
	}
}
