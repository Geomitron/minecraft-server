package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class ShapeLockTest {
	/** Aimed at the top of the block at 4 -61 7, a right click starts at 4 -60 7, the space above it. */
	private static final BlockHitResult ON_TOP = new BlockHitResult(new Vec3(4.25, -60, 7.75), Direction.UP,
		new BlockPos(4, -61, 7), false);
	private static final BlockPos ABOVE = new BlockPos(4, -60, 7);

	@Test
	void controlsActiveWithASessionAModeOtherThanDisableNoMouseModeAndABlockInHand() {
		assertTrue(ShapeLock.controlsActive(true, false, false, true));
	}

	@Test
	void controlsNotActiveWithoutASessionInDisableInAMouseModeOrWithoutABlockInHand() {
		assertFalse(ShapeLock.controlsActive(false, false, false, true));
		assertFalse(ShapeLock.controlsActive(true, true, false, true));
		assertFalse(ShapeLock.controlsActive(true, false, true, true));
		assertFalse(ShapeLock.controlsActive(true, false, false, false));
	}

	@Test
	void theFirstClicksBlockAloneIsDrawnWhileTheControlsAreActive() {
		assertTrue(ShapeLock.drawnBeforeFirstClick(true, 1));
		assertFalse(ShapeLock.drawnBeforeFirstClick(false, 1));
	}

	@Test
	void aSetOfCopiesIsLeftToBeDrawnAsItIs() {
		assertFalse(ShapeLock.drawnBeforeFirstClick(true, 3));
	}

	@Test
	void offUntilLocked() {
		ShapeLock lock = new ShapeLock();
		assertFalse(lock.on());
		assertFalse(lock.takesFirstClick(true, false));
	}

	@Test
	void lockedItTakesTheFirstClickWithTheHitItWasLockedWith() {
		ShapeLock lock = new ShapeLock();
		lock.lock(ABOVE, ON_TOP);
		assertTrue(lock.takesFirstClick(true, false));
		assertEquals(ABOVE, lock.pos());
		assertEquals(Direction.UP, lock.hit().getDirection());
		assertEquals(new Vec3(4.25, -60, 7.75), lock.hit().getLocation());
		assertEquals(ABOVE, lock.hit().getBlockPos());
	}

	@Test
	void itTakesNoClickDuringAShapeOrWhileItsControlsArentActive() {
		ShapeLock lock = new ShapeLock();
		lock.lock(ABOVE, ON_TOP);
		assertFalse(lock.takesFirstClick(true, true));
		assertFalse(lock.takesFirstClick(false, false));
		assertTrue(lock.on());
	}

	@Test
	void aNudgeMovesThePositionAndTheHitPointOneBlock() {
		ShapeLock lock = new ShapeLock();
		lock.lock(ABOVE, ON_TOP);
		lock.nudge(Direction.NORTH);
		lock.nudge(Direction.UP);
		assertEquals(new BlockPos(4, -59, 6), lock.pos());
		assertEquals(new Vec3(4.25, -59, 6.75), lock.hit().getLocation());
		assertEquals(Direction.UP, lock.hit().getDirection());
	}

	@Test
	void unlockingEndsIt() {
		ShapeLock lock = new ShapeLock();
		lock.lock(ABOVE, ON_TOP);
		lock.unlock();
		assertFalse(lock.on());
		assertFalse(lock.takesFirstClick(true, false));
	}

	@Test
	void aNudgeWhileOffDoesNothing() {
		ShapeLock lock = new ShapeLock();
		lock.nudge(Direction.EAST);
		assertFalse(lock.on());
	}

	@Test
	void aShapesFirstClickMovesWithANudge() {
		BlockHitResult moved = ShapeLock.moved(ON_TOP, Direction.EAST);
		assertEquals(new Vec3(5.25, -60, 7.75), moved.getLocation());
		assertEquals(new BlockPos(5, -61, 7), moved.getBlockPos());
		assertEquals(Direction.UP, moved.getDirection());
	}
}
