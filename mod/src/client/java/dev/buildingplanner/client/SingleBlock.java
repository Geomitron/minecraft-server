package dev.buildingplanner.client;

import dev.buildingplanner.input.CopyMode;
import dev.buildingplanner.input.SingleRule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The one ghost block's state in Effortless Building's Single mode (see "One ghost block"), beside the copy's
 * {@link Clipboard}: where it's locked, with the face and hit point aimed at when it was locked, or nothing while it
 * follows the aim. It uses none of Effortless Building's classes.
 */
final class SingleBlock {
	/** The locked position, or null while the block follows the aim. */
	private BlockPos pos;
	/** The face aimed at when the block was locked. */
	private Direction face;
	/** The hit point aimed at when the block was locked, from the locked position, which nudges move it with. */
	private Vec3 hitFromPos;

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

	/** Moves the locked block one block that way; does nothing while it's unlocked. */
	void nudge(Direction direction) {
		if (pos != null) {
			pos = pos.relative(direction);
		}
	}

	/**
	 * The hit the locked block's state comes from, by its placement rules: the face aimed at when it was locked, and
	 * the hit point moved with each nudge, on the locked position, as hook E1 gives each block of a shape.
	 */
	BlockHitResult hit() {
		return new BlockHitResult(Vec3.atLowerCornerOf(pos).add(hitFromPos), face, pos, false);
	}

	/** The block as its clicks see it ({@link SingleRule}). */
	SingleRule.State state() {
		return new SingleRule.State(CopyMode.SINGLE, locked(), false);
	}
}
