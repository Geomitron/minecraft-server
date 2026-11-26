package dev.buildingplanner.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Delete's marks: the sweep between two aims, the box, unmarking, the cap and marks dropping with their ghost blocks.
 */
class DeleteMarksTest {
	private static BlockState bricks;
	private static BlockState air;
	private static BlockState lowerDoor;
	private static BlockState upperDoor;
	/** The eyes, 8 blocks from a row of ghost blocks at x 0 to 9, y 64, z 8, in front of its fourth. */
	private static final Vec3 EYE = new Vec3(3.5, 64.5, 0.5);

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		bricks = Blocks.STONE_BRICKS.defaultBlockState();
		air = Blocks.AIR.defaultBlockState();
		lowerDoor = Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
			DoubleBlockHalf.LOWER);
		upperDoor = lowerDoor.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
	}

	/** A row of 10 ghost stone bricks at x 0 to 9, y 64, z 8. */
	private static Layer row() {
		Layer layer = new Layer();
		for (int x = 0; x < 10; x++) {
			layer.set(new BlockPos(x, 64, 8), bricks);
		}
		return layer;
	}

	/**
	 * Aims through {@code layer} and the real blocks {@code real}, in small steps along the ray, up to 256 blocks: the
	 * first block met, a ghost block or a real one.
	 */
	private static DeleteMarks.Tracer tracer(Layer layer, Set<BlockPos> real) {
		return (eye, look) -> {
			for (double d = 0; d <= 256; d += 0.01) {
				BlockPos at = BlockPos.containing(eye.add(look.scale(d)));
				if (real.contains(at)) {
					return new DeleteMarks.Hit(null, d);
				}
				if (!layer.get(at).isAir()) {
					return new DeleteMarks.Hit(at, d);
				}
			}
			return new DeleteMarks.Hit(null, 256);
		};
	}

	/** The view from the eyes at the middle of the block's face toward them. */
	private static DeleteMarks.View at(int x) {
		return new DeleteMarks.View(EYE, new Vec3(x + 0.5, 64.5, 8).subtract(EYE).normalize());
	}

	private static Set<BlockPos> rowFrom(int... xs) {
		return IntStream.of(xs).mapToObj(x -> new BlockPos(x, 64, 8)).collect(Collectors.toSet());
	}

	private static Set<BlockPos> marked(DeleteMarks marks) {
		Set<BlockPos> marked = new HashSet<>();
		marks.positions().forEach(key -> marked.add(BlockPos.of(key)));
		return marked;
	}

	@Test
	void aSweepBetweenTwoAimsTenBlocksApartMarksAllTenWithNoGap() {
		Layer layer = row();
		DeleteMarks marks = new DeleteMarks(200_000);
		DeleteMarks.Tracer tracer = tracer(layer, Set.of());
		marks.startSweep();
		marks.sweep(null, at(0), tracer, layer);
		marks.sweep(at(0), at(9), tracer, layer);

		assertEquals(rowFrom(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), marked(marks));
		assertEquals(10, marks.eraseSize());
	}

	@Test
	void aSweepAimsAtMostSixtyFourTimesAFrame() {
		Layer layer = row();
		int[] aims = {0};
		DeleteMarks.Tracer counting = (eye, look) -> {
			aims[0]++;
			return tracer(layer, Set.of()).trace(eye, look);
		};
		DeleteMarks.swept(new DeleteMarks.View(EYE, new Vec3(0, 0, 1)), new DeleteMarks.View(EYE, new Vec3(0, 0, -1)),
			counting);

		assertEquals(DeleteMarks.MAX_STEPS + 1, aims[0], "both ends' aims, and the steps between them");
	}

	@Test
	void aRealBlockInFrontOfAGhostBlockStopsTheAim() {
		Layer layer = row();
		// A real block right in front of the row's fourth block, and one beyond the row's eighth.
		Set<BlockPos> real = Set.of(new BlockPos(3, 64, 7), new BlockPos(7, 64, 9));
		DeleteMarks marks = new DeleteMarks(200_000);
		DeleteMarks.Tracer tracer = tracer(layer, real);
		marks.startSweep();
		marks.sweep(null, at(0), tracer, layer);
		marks.sweep(at(0), at(9), tracer, layer);

		assertEquals(rowFrom(0, 1, 2, 4, 5, 6, 7, 8, 9), marked(marks), "no mark on the block behind it, nor a real one");
	}

	@Test
	void aSweepThatStartsOnAMarkUnmarksUntilItEnds() {
		Layer layer = row();
		DeleteMarks marks = new DeleteMarks(200_000);
		marks.startSweep();
		for (int x = 0; x < 6; x++) {
			marks.pass(new BlockPos(x, 64, 8), layer);
		}
		marks.endSweep();
		assertFalse(marks.unmarking());

		marks.startSweep();
		marks.pass(new BlockPos(2, 64, 8), layer);
		assertTrue(marks.unmarking());
		marks.pass(new BlockPos(1, 64, 8), layer);
		marks.pass(new BlockPos(0, 64, 8), layer);
		marks.pass(new BlockPos(8, 64, 8), layer);
		assertEquals(rowFrom(3, 4, 5), marked(marks), "unmarked the first three, and marked nothing unmarked");
		marks.endSweep();

		marks.startSweep();
		marks.pass(new BlockPos(8, 64, 8), layer);
		marks.pass(new BlockPos(4, 64, 8), layer);
		assertFalse(marks.unmarking());
		assertEquals(rowFrom(3, 4, 5, 8), marked(marks), "a sweep that starts on no mark marks again");
	}

	@Test
	void aBoxMarksEveryGhostBlockBetweenItsCornersAtEveryHeight() {
		Layer layer = row();
		layer.set(new BlockPos(2, 66, 8), bricks);
		layer.set(new BlockPos(2, 67, 8), bricks);
		DeleteMarks marks = new DeleteMarks(200_000);
		marks.startSweep();
		assertFalse(marks.startBox(new BlockPos(2, 64, 8)), "a box started on no mark marks");

		assertTrue(marks.box(new BlockPos(2, 64, 8), new BlockPos(4, 66, 8), layer, false));
		assertEquals(Set.of(new BlockPos(2, 64, 8), new BlockPos(3, 64, 8), new BlockPos(4, 64, 8),
			new BlockPos(2, 66, 8)), marked(marks));
	}

	@Test
	void aBoxStartedOnAMarkUnmarksTheMarksInItAndLeavesTheRest() {
		Layer layer = row();
		DeleteMarks marks = new DeleteMarks(200_000);
		marks.startSweep();
		marks.box(new BlockPos(0, 64, 8), new BlockPos(9, 64, 8), layer, false);
		marks.endSweep();

		marks.startSweep();
		assertTrue(marks.startBox(new BlockPos(3, 64, 8)));
		assertTrue(marks.unmarking(), "the hold unmarks what its sweep passes over too");
		assertTrue(marks.box(new BlockPos(3, 64, 8), new BlockPos(5, 64, 8), layer, true));
		assertEquals(rowFrom(0, 1, 2, 6, 7, 8, 9), marked(marks));
	}

	@Test
	void aHoldsSweepDecidesBeforeItsBoxDoes() {
		Layer layer = row();
		DeleteMarks marks = new DeleteMarks(200_000);
		marks.startSweep();
		marks.pass(new BlockPos(5, 64, 8), layer);
		marks.endSweep();

		marks.startSweep();
		marks.pass(new BlockPos(0, 64, 8), layer);
		assertFalse(marks.startBox(new BlockPos(5, 64, 8)), "the sweep met an unmarked ghost block first, so it marks");
	}

	@Test
	void aBoxThatWouldPassTheCapMarksNothing() {
		Layer layer = row();
		DeleteMarks marks = new DeleteMarks(3);
		marks.startSweep();
		marks.pass(new BlockPos(0, 64, 8), layer);

		assertFalse(marks.fits(layer, new BlockPos(1, 64, 8), new BlockPos(3, 64, 8)));
		assertFalse(marks.box(new BlockPos(1, 64, 8), new BlockPos(3, 64, 8), layer, false));
		assertEquals(rowFrom(0), marked(marks));

		assertTrue(marks.fits(layer, new BlockPos(0, 64, 8), new BlockPos(2, 64, 8)), "a mark already in it adds none");
		assertTrue(marks.box(new BlockPos(0, 64, 8), new BlockPos(2, 64, 8), layer, false));
		assertEquals(rowFrom(0, 1, 2), marked(marks));
		assertTrue(marks.full());
	}

	@Test
	void aBoxCountsTheOtherHalfOfADoorItCuts() {
		Layer layer = new Layer();
		layer.set(new BlockPos(0, 64, 0), lowerDoor);
		layer.set(new BlockPos(0, 65, 0), upperDoor);
		layer.set(new BlockPos(1, 64, 0), bricks);

		DeleteMarks capped = new DeleteMarks(2);
		capped.startSweep();
		assertFalse(capped.box(new BlockPos(0, 64, 0), new BlockPos(1, 64, 0), layer, false),
			"the door's upper half makes 3 positions to erase");

		DeleteMarks marks = new DeleteMarks(200_000);
		marks.startSweep();
		marks.box(new BlockPos(0, 64, 0), new BlockPos(1, 64, 0), layer, false);
		assertEquals(2, marks.size());
		assertEquals(3, marks.eraseSize());
	}

	@Test
	void aDoorMarkedByItsLowerHalfIsOneMarkAndTwoPositionsToErase() {
		Layer layer = new Layer();
		layer.set(new BlockPos(0, 64, 0), lowerDoor);
		layer.set(new BlockPos(0, 65, 0), upperDoor);
		DeleteMarks marks = new DeleteMarks(200_000);
		marks.startSweep();
		marks.pass(new BlockPos(0, 64, 0), layer);

		assertEquals(1, marks.size());
		assertEquals(2, marks.eraseSize());

		marks.pass(new BlockPos(0, 65, 0), layer);
		assertEquals(2, marks.size());
		assertEquals(2, marks.eraseSize(), "both halves marked still erase two positions");
	}

	@Test
	void theMarksStopAtTheCapCountingTheOtherHalves() {
		Layer layer = row();
		layer.set(new BlockPos(0, 70, 0), lowerDoor);
		layer.set(new BlockPos(0, 71, 0), upperDoor);
		DeleteMarks marks = new DeleteMarks(3);
		marks.startSweep();
		marks.pass(new BlockPos(0, 64, 8), layer);
		marks.pass(new BlockPos(1, 64, 8), layer);
		marks.pass(new BlockPos(0, 70, 0), layer);
		assertEquals(rowFrom(0, 1), marked(marks), "a door, with its upper half, would make 4");
		assertFalse(marks.full());

		marks.pass(new BlockPos(2, 64, 8), layer);
		marks.pass(new BlockPos(3, 64, 8), layer);
		assertEquals(rowFrom(0, 1, 2), marked(marks));
		assertTrue(marks.full());
	}

	@Test
	void marksDropWhenTheirGhostBlocksLeaveTheLayer() {
		Layer layer = row();
		layer.set(new BlockPos(0, 70, 0), lowerDoor);
		layer.set(new BlockPos(0, 71, 0), upperDoor);
		DeleteMarks marks = new DeleteMarks(200_000);
		marks.startSweep();
		for (int x = 0; x < 4; x++) {
			marks.pass(new BlockPos(x, 64, 8), layer);
		}
		marks.pass(new BlockPos(0, 70, 0), layer);
		long before = marks.version();

		layer.set(new BlockPos(1, 64, 8), air);
		layer.set(new BlockPos(0, 71, 0), air);
		marks.retain(layer);

		Set<BlockPos> expected = new HashSet<>(rowFrom(0, 2, 3));
		expected.add(new BlockPos(0, 70, 0));
		assertEquals(expected, marked(marks));
		assertEquals(4, marks.eraseSize(), "the door's lower half no longer takes an upper one");
		assertTrue(marks.version() != before);

		long kept = marks.version();
		marks.retain(layer);
		assertEquals(kept, marks.version(), "nothing left the layer, so the marks didn't change");
	}

	@Test
	void theChangesListEachMarkAndUnmarkUntilAllChange() {
		Layer layer = row();
		DeleteMarks marks = new DeleteMarks(200_000);
		assertEquals(null, marks.takeChanged(), "at first every mark counts as changed");
		marks.startSweep();
		marks.pass(new BlockPos(0, 64, 8), layer);
		marks.pass(new BlockPos(1, 64, 8), layer);
		marks.endSweep();
		assertEquals(Set.of(new BlockPos(0, 64, 8).asLong(), new BlockPos(1, 64, 8).asLong()), marks.takeChanged());
		assertEquals(Set.of(), marks.takeChanged(), "taking them empties them");

		marks.startSweep();
		marks.pass(new BlockPos(1, 64, 8), layer);
		marks.endSweep();
		layer.set(new BlockPos(0, 64, 8), air);
		marks.retain(layer);
		assertEquals(Set.of(new BlockPos(0, 64, 8).asLong(), new BlockPos(1, 64, 8).asLong()), marks.takeChanged(),
			"an unmark and a mark dropped with its ghost block");

		marks.startSweep();
		marks.pass(new BlockPos(2, 64, 8), layer);
		marks.endSweep();
		marks.clear();
		assertEquals(null, marks.takeChanged(), "after a clear every mark counts as changed");
	}

	@Test
	void anAimAtNothingOrARealBlockMarksNothing() {
		Layer layer = row();
		DeleteMarks.Tracer tracer = tracer(layer, Set.of(new BlockPos(4, 64, 7)));
		assertEquals(List.of(), DeleteMarks.swept(null, new DeleteMarks.View(EYE, new Vec3(0, 1, 0)), tracer));
		assertEquals(List.of(), DeleteMarks.swept(null, at(4), tracer));
	}
}
