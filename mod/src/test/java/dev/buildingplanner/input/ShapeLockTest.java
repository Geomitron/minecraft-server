package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.input.ShapeLock.Finder;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
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
	private static final BlockPos FIRST = new BlockPos(0, -60, 0);
	private static final Set<Direction> LEVEL = EnumSet.of(Direction.NORTH, Direction.SOUTH, Direction.EAST,
		Direction.WEST);
	private static final Set<Direction> UP_DOWN = EnumSet.of(Direction.UP, Direction.DOWN);

	/** The directions a nudge sets the step's position {@code pos} along, found by {@code finder} from {@code from}. */
	private static Set<Direction> setAlong(Finder finder, BlockPos from, BlockPos pos) {
		Set<Direction> set = EnumSet.noneOf(Direction.class);
		for (Direction direction : Direction.values()) {
			if (ShapeLock.sets(finder, from, pos, direction)) {
				set.add(direction);
			}
		}
		return set;
	}

	private static Set<Direction> along(Direction.Axis... axes) {
		Set<Direction> set = EnumSet.noneOf(Direction.class);
		for (Direction direction : Direction.values()) {
			if (List.of(axes).contains(direction.getAxis())) {
				set.add(direction);
			}
		}
		return set;
	}

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
		assertFalse(lock.on(0));
		assertFalse(lock.on(1));
		assertFalse(lock.takesFirstClick(true, 0));
	}

	@Test
	void lockedItTakesTheFirstClickWithTheHitItWasLockedWith() {
		ShapeLock lock = new ShapeLock();
		lock.lock(ABOVE, ON_TOP);
		assertTrue(lock.takesFirstClick(true, 0));
		assertEquals(ABOVE, lock.pos(0));
		assertEquals(Direction.UP, lock.hit().getDirection());
		assertEquals(new Vec3(4.25, -60, 7.75), lock.hit().getLocation());
		assertEquals(ABOVE, lock.hit().getBlockPos());
	}

	@Test
	void itTakesNoFirstClickDuringAShapeOrWhileItsControlsArentActive() {
		ShapeLock lock = new ShapeLock();
		lock.lock(ABOVE, ON_TOP);
		assertFalse(lock.takesFirstClick(true, 1));
		assertFalse(lock.takesFirstClick(false, 0));
		assertTrue(lock.on(0));
	}

	@Test
	void hLocksEachStepOfAShape() {
		ShapeLock lock = new ShapeLock();
		lock.lock(ABOVE, ON_TOP);
		assertTrue(lock.on(0));
		lock.lockStep(1, new BlockPos(4, -60, 3));
		assertTrue(lock.on(1));
		assertEquals(new BlockPos(4, -60, 3), lock.pos(1));
		lock.lockStep(2, new BlockPos(4, -57, 3));
		assertTrue(lock.on(2));
		assertEquals(new BlockPos(4, -57, 3), lock.pos(2));
	}

	@Test
	void aLockedStepTakesItsClickInPlaceOfTheAim() {
		ShapeLock lock = new ShapeLock();
		BlockPos aimed = new BlockPos(9, -60, 0);
		assertEquals(aimed, lock.position(1, aimed));
		lock.lockStep(1, new BlockPos(4, -60, 0));
		assertEquals(new BlockPos(4, -60, 0), lock.position(1, aimed));
		assertEquals(new BlockPos(4, -60, 0), lock.position(1, null));
	}

	@Test
	void afterEachClickTheNextStepStartsUnlocked() {
		ShapeLock lock = new ShapeLock();
		lock.lock(ABOVE, ON_TOP);
		assertFalse(lock.on(1));
		lock.lockStep(1, new BlockPos(4, -60, 3));
		BlockPos aimed = new BlockPos(4, -55, 3);
		assertFalse(lock.on(2));
		assertEquals(aimed, lock.position(2, aimed));
		assertFalse(lock.on(1));
	}

	@Test
	void hOnALockedStepUnlocksItAlone() {
		ShapeLock lock = new ShapeLock();
		lock.lock(ABOVE, ON_TOP);
		lock.lockStep(1, new BlockPos(4, -60, 3));
		lock.unlock(1);
		assertFalse(lock.on(1));
		assertTrue(lock.on(0));
	}

	@Test
	void aCancelKeepsAFirstPositionLockedBeforeTheFirstClick() {
		ShapeLock lock = new ShapeLock();
		lock.lock(ABOVE, ON_TOP);
		lock.lockStep(1, new BlockPos(4, -60, 3));
		assertTrue(lock.on(0));
		assertTrue(lock.takesFirstClick(true, 0));
		assertEquals(ABOVE, lock.pos(0));
		assertFalse(lock.on(1));
	}

	@Test
	void unlockingEndsEveryStepsLock() {
		ShapeLock lock = new ShapeLock();
		lock.lock(ABOVE, ON_TOP);
		lock.lockStep(1, new BlockPos(4, -60, 3));
		lock.unlock();
		assertFalse(lock.on(1));
		assertFalse(lock.on(0));
		assertFalse(lock.takesFirstClick(true, 0));
	}

	@Test
	void eachModesStepsFindTheirPositionsByTheirFinder() {
		assertEquals(Finder.LINE, ShapeLock.finder(true, false, 1));
		assertEquals(Finder.WALL, ShapeLock.finder(false, true, 1));
		assertEquals(Finder.FLOOR, ShapeLock.finder(false, false, 1));
		assertEquals(Finder.HEIGHT, ShapeLock.finder(false, false, 2));
		BlockPos second = new BlockPos(3, -60, 2);
		assertEquals(FIRST, ShapeLock.from(Finder.FLOOR, List.of(FIRST)));
		assertEquals(second, ShapeLock.from(Finder.HEIGHT, List.of(FIRST, second)));
	}

	@Test
	void aLineOneBlockLongIsSetAlongEveryDirection() {
		assertEquals(EnumSet.allOf(Direction.class), setAlong(Finder.LINE, FIRST, FIRST));
	}

	@Test
	void aLongerLineIsSetAlongItself() {
		assertEquals(along(Direction.Axis.X), setAlong(Finder.LINE, FIRST, FIRST.east(4)));
		assertEquals(along(Direction.Axis.Y), setAlong(Finder.LINE, FIRST, FIRST.above(4)));
		assertEquals(along(Direction.Axis.Z), setAlong(Finder.LINE, FIRST, FIRST.north(4)));
	}

	@Test
	void aWallOneBlockWideIsSetAlongEveryDirection() {
		assertEquals(EnumSet.allOf(Direction.class), setAlong(Finder.WALL, FIRST, FIRST.above(3)));
	}

	@Test
	void aWiderWallIsSetUpAndDownAndAlongItself() {
		assertEquals(along(Direction.Axis.X, Direction.Axis.Y), setAlong(Finder.WALL, FIRST, FIRST.east(4).above(3)));
		assertEquals(along(Direction.Axis.Z, Direction.Axis.Y), setAlong(Finder.WALL, FIRST, FIRST.south(4).above(3)));
	}

	@Test
	void findFloorsStepIsSetAlongBothLevelDirections() {
		assertEquals(LEVEL, setAlong(Finder.FLOOR, FIRST, FIRST));
		assertEquals(LEVEL, setAlong(Finder.FLOOR, FIRST, FIRST.east(3).south(2)));
	}

	@Test
	void findHeightsStepIsSetUpAndDown() {
		BlockPos second = FIRST.east(3).south(2);
		assertEquals(UP_DOWN, setAlong(Finder.HEIGHT, second, second));
		assertEquals(UP_DOWN, setAlong(Finder.HEIGHT, second, second.above(2)));
	}

	@Test
	void beforeTheFirstClickANudgeMovesThePositionAndTheHitPointOneBlock() {
		ShapeLock lock = new ShapeLock();
		lock.lock(ABOVE, ON_TOP);
		assertFalse(lock.nudge(List.of(), null, Direction.NORTH));
		assertFalse(lock.nudge(List.of(), null, Direction.UP));
		assertEquals(new BlockPos(4, -59, 6), lock.pos(0));
		assertEquals(new Vec3(4.25, -59, 6.75), lock.hit().getLocation());
		assertEquals(Direction.UP, lock.hit().getDirection());
	}

	@Test
	void aNudgeAcrossALineOf5MovesTheWholeLineAndAlongItItsEnd() {
		ShapeLock lock = new ShapeLock();
		lock.lockStep(1, FIRST.east(4));
		assertTrue(lock.nudge(List.of(FIRST), Finder.LINE, Direction.NORTH));
		assertEquals(FIRST.east(4).north(), lock.pos(1));
		assertFalse(lock.nudge(List.of(FIRST.north()), Finder.LINE, Direction.EAST));
		assertEquals(FIRST.east(5).north(), lock.pos(1));
	}

	@Test
	void aNudgeUpMakesAWallTallerAndAcrossItMovesTheWholeWall() {
		ShapeLock lock = new ShapeLock();
		lock.lockStep(1, FIRST.east(4).above(2));
		assertFalse(lock.nudge(List.of(FIRST), Finder.WALL, Direction.UP));
		assertEquals(FIRST.east(4).above(3), lock.pos(1));
		assertTrue(lock.nudge(List.of(FIRST), Finder.WALL, Direction.NORTH));
		assertEquals(FIRST.east(4).above(3).north(), lock.pos(1));
	}

	@Test
	void aNudgeUpMovesTheWholeFloorAndALevelOneSizesIt() {
		ShapeLock lock = new ShapeLock();
		lock.lockStep(1, FIRST.east(3).south(2));
		assertTrue(lock.nudge(List.of(FIRST), Finder.FLOOR, Direction.UP));
		assertEquals(FIRST.east(3).south(2).above(), lock.pos(1));
		assertFalse(lock.nudge(List.of(FIRST.above()), Finder.FLOOR, Direction.EAST));
		assertEquals(FIRST.east(4).south(2).above(), lock.pos(1));
	}

	@Test
	void aNudgeUpRaisesACubesHeightAndALevelOneMovesTheWholeCube() {
		ShapeLock lock = new ShapeLock();
		BlockPos second = FIRST.east(3).south(2);
		lock.lockStep(2, second.above(2));
		assertFalse(lock.nudge(List.of(FIRST, second), Finder.HEIGHT, Direction.UP));
		assertEquals(second.above(3), lock.pos(2));
		assertTrue(lock.nudge(List.of(FIRST, second), Finder.HEIGHT, Direction.WEST));
		assertEquals(second.above(3).west(), lock.pos(2));
	}

	@Test
	void aNudgeThatMovesTheWholeShapeMovesAFirstPositionLockedBeforeTheFirstClick() {
		ShapeLock lock = new ShapeLock();
		lock.lock(ABOVE, ON_TOP);
		lock.lockStep(1, ABOVE.east(4));
		assertTrue(lock.nudge(List.of(ABOVE), Finder.LINE, Direction.SOUTH));
		assertFalse(lock.nudge(List.of(ABOVE.south()), Finder.LINE, Direction.EAST));
		assertEquals(ABOVE.south(), lock.pos(0));
		assertEquals(new Vec3(4.25, -60, 8.75), lock.hit().getLocation());
	}

	@Test
	void aNudgeWhileTheStepIsntLockedDoesNothing() {
		ShapeLock lock = new ShapeLock();
		lock.lock(ABOVE, ON_TOP);
		assertFalse(lock.nudge(List.of(ABOVE), Finder.LINE, Direction.EAST));
		assertFalse(lock.on(1));
		assertEquals(ABOVE, lock.pos(0));
	}

	@Test
	void aShapesFirstClickMovesWithANudge() {
		BlockHitResult moved = ShapeLock.moved(ON_TOP, Direction.EAST);
		assertEquals(new Vec3(5.25, -60, 7.75), moved.getLocation());
		assertEquals(new BlockPos(5, -61, 7), moved.getBlockPos());
		assertEquals(Direction.UP, moved.getDirection());
	}
}
