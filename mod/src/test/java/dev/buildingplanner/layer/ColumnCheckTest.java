package dev.buildingplanner.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The self-check's comparisons of one column. */
class ColumnCheckTest {
	private static BlockState oak;
	private static BlockState stone;

	/** An oak floor of 10 by 10 at y 64 in column 0 0, and one oak block above it in the next section, at y 80. */
	private Layer layer;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		oak = Blocks.OAK_PLANKS.defaultBlockState();
		stone = Blocks.STONE.defaultBlockState();
	}

	@BeforeEach
	void floor() {
		layer = new Layer();
		for (int x = 0; x < 10; x++) {
			for (int z = 0; z < 10; z++) {
				layer.set(new BlockPos(x, 64, z), oak);
			}
		}
		layer.set(new BlockPos(0, 80, 0), oak);
	}

	/** A line of stone across the floor at z 3, from x 0 to 9, which replaces 10 of its blocks. */
	private static List<BlockPos> line() {
		List<BlockPos> line = new ArrayList<>();
		for (int x = 0; x < 10; x++) {
			line.add(new BlockPos(x, 64, 3));
		}
		return line;
	}

	private ColumnCheck.Outside edit(List<BlockPos> own, BlockState state, Runnable alsoAfter) {
		ColumnCheck.Before before = ColumnCheck.before(layer.column(0, 0), own);
		for (BlockPos pos : own) {
			layer.set(pos, state);
		}
		alsoAfter.run();
		return ColumnCheck.outside(before, layer.column(0, 0), own);
	}

	@Test
	void anEditsOwnPositionsAreIgnored() {
		assertEquals(new ColumnCheck.Outside(91, 91, 0), edit(line(), stone, () -> { }));
	}

	@Test
	void erasingAnEditsOwnPositionsIsIgnored() {
		assertEquals(new ColumnCheck.Outside(91, 91, 0), edit(line(), Blocks.AIR.defaultBlockState(), () -> { }));
	}

	@Test
	void aBlockLostOutsideTheEditIsReported() {
		assertEquals(new ColumnCheck.Outside(91, 90, 1),
			edit(line(), stone, () -> layer.set(new BlockPos(5, 64, 7), Blocks.AIR.defaultBlockState())));
	}

	@Test
	void aBlockChangedOutsideTheEditIsReported() {
		assertEquals(new ColumnCheck.Outside(91, 91, 1), edit(line(), stone, () -> layer.set(new BlockPos(5, 64, 7), stone)));
	}

	@Test
	void aSectionTheEditDoesntReachIsCompared() {
		assertEquals(new ColumnCheck.Outside(91, 90, 1),
			edit(line(), stone, () -> layer.set(new BlockPos(0, 80, 0), Blocks.AIR.defaultBlockState())));
	}

	@Test
	void anEditIntoAColumnWithNoGhostBlocks() {
		ColumnCheck.Before before = ColumnCheck.before(null, List.of(new BlockPos(20, 64, 0)));
		layer.set(new BlockPos(20, 64, 0), stone);
		assertEquals(new ColumnCheck.Outside(0, 0, 0),
			ColumnCheck.outside(before, layer.column(1, 0), List.of(new BlockPos(20, 64, 0))));
	}

	@Test
	void anEditThatEmptiesTheColumn() {
		List<BlockPos> all = new ArrayList<>(List.of(new BlockPos(0, 80, 0)));
		for (int x = 0; x < 10; x++) {
			for (int z = 0; z < 10; z++) {
				all.add(new BlockPos(x, 64, z));
			}
		}
		ColumnCheck.Before before = ColumnCheck.before(layer.column(0, 0), all);
		for (BlockPos pos : all) {
			layer.set(pos, Blocks.AIR.defaultBlockState());
		}
		assertEquals(new ColumnCheck.Outside(0, 0, 0), ColumnCheck.outside(before, layer.column(0, 0), all));
	}

	/** What Litematica holds: the layer's blocks in column 0 0, but for {@code lost}. */
	private BoxCount.Sections litematica(BlockPos... lost) {
		Map<BlockPos, BlockState> blocks = new HashMap<>();
		layer.forEachIn(new BlockPos(0, 64, 0), new BlockPos(15, 95, 15), (pos, state) -> blocks.put(pos.immutable(), state));
		for (BlockPos pos : lost) {
			blocks.remove(pos);
		}
		return TestSections.of(blocks);
	}

	@Test
	void litematicaHoldingTheLayerIsNoDifference() {
		assertEquals(new ColumnCheck.Held(101, 101, 0), ColumnCheck.held(layer.column(0, 0), 0, 0, litematica(), List.of()));
	}

	@Test
	void aBlockLitematicaLostIsReported() {
		assertEquals(new ColumnCheck.Held(101, 100, 1),
			ColumnCheck.held(layer.column(0, 0), 0, 0, litematica(new BlockPos(5, 64, 7)), List.of()));
	}

	@Test
	void litematicaHoldingNothingIsReported() {
		assertEquals(new ColumnCheck.Held(101, 0, 101),
			ColumnCheck.held(layer.column(0, 0), 0, 0, (x, y, z) -> null, List.of()));
	}

	@Test
	void otherPlacementsBoxesAreLeftOut() {
		List<BoundingBox> copy = List.of(new BoundingBox(4, 60, 6, 6, 70, 8));
		assertEquals(new ColumnCheck.Held(92, 92, 0),
			ColumnCheck.held(layer.column(0, 0), 0, 0, litematica(new BlockPos(5, 64, 7)), copy));
	}

	@Test
	void aColumnWarnsAtMostOnceAMinute() {
		ColumnCheck.Limit limit = new ColumnCheck.Limit();
		long column = Layer.key(0, 0);
		assertTrue(limit.allows(column, 1_000));
		assertFalse(limit.allows(column, 1_001));
		assertFalse(limit.allows(column, 60_999));
		assertTrue(limit.allows(Layer.key(1, 0), 2_000), "another column");
		assertTrue(limit.allows(column, 61_000));
		assertFalse(limit.allows(column, 61_000));
	}

	@Test
	void aColumnWaitsForARebuildThatBeganAtTheMark() {
		ColumnCheck.Rebuilds rebuilds = new ColumnCheck.Rebuilds();
		long column = Layer.key(0, 0);
		assertEquals(-1, rebuilds.settled(column, 10, false), "no rebuild yet");
		rebuilds.begin(column, 9);
		rebuilds.end(column, true);
		assertEquals(-1, rebuilds.settled(column, 10, false), "a rebuild that began before the mark");
		rebuilds.begin(column, 10);
		assertEquals(-1, rebuilds.settled(column, 10, false), "the rebuild is running");
		rebuilds.end(column, true);
		assertTrue(rebuilds.settled(column, 10, false) >= 0);
		assertEquals(-1, rebuilds.settled(Layer.key(1, 0), 10, false), "another column");
	}

	@Test
	void aColumnIsntJudgedWhileALaterRebuildRuns() {
		// A rebuild swaps an empty chunk in until it ends, so the rebuild that finished since the mark doesn't make
		// the column ready while the next one runs.
		ColumnCheck.Rebuilds rebuilds = new ColumnCheck.Rebuilds();
		long column = Layer.key(0, 0);
		rebuilds.begin(column, 10);
		rebuilds.end(column, true);
		rebuilds.begin(column, 12);
		assertEquals(-1, rebuilds.settled(column, 10, false));
		assertEquals(-1, rebuilds.settled(column, 10, true), "nor after the wait for a rebuild");
		rebuilds.end(column, true);
		assertTrue(rebuilds.settled(column, 10, false) >= 0);
	}

	@Test
	void aRebuildThatBeginsDuringTheReadVoidsIt() {
		ColumnCheck.Rebuilds rebuilds = new ColumnCheck.Rebuilds();
		long column = Layer.key(0, 0);
		rebuilds.begin(column, 10);
		rebuilds.end(column, true);
		long token = rebuilds.settled(column, 10, false);
		assertTrue(rebuilds.unchanged(column, token));
		rebuilds.begin(column, 11);
		assertFalse(rebuilds.unchanged(column, token), "running");
		rebuilds.end(column, true);
		assertFalse(rebuilds.unchanged(column, token), "began and ended during the read");
	}

	@Test
	void aFailedRebuildIsntWaitedFor() {
		ColumnCheck.Rebuilds rebuilds = new ColumnCheck.Rebuilds();
		long column = Layer.key(0, 0);
		rebuilds.begin(column, 10);
		rebuilds.end(column, false);
		assertEquals(-1, rebuilds.settled(column, 10, false));
		assertTrue(rebuilds.settled(column, 10, true) >= 0, "after the wait, the column is judged anyway");
	}

	@Test
	void afterTheWaitAColumnNeverRebuiltIsJudged() {
		ColumnCheck.Rebuilds rebuilds = new ColumnCheck.Rebuilds();
		long column = Layer.key(0, 0);
		long token = rebuilds.settled(column, 10, true);
		assertTrue(token >= 0);
		assertTrue(rebuilds.unchanged(column, token));
		rebuilds.begin(column, 11);
		assertFalse(rebuilds.unchanged(column, token));
	}

	@Test
	void manyColumnsEachKeepTheirMinute() {
		ColumnCheck.Limit limit = new ColumnCheck.Limit();
		for (int x = 0; x < 300; x++) {
			assertTrue(limit.allows(Layer.key(x, 0), 1_000));
		}
		assertFalse(limit.allows(Layer.key(0, 0), 2_000));
		assertFalse(limit.allows(Layer.key(299, 0), 2_000));
	}
}
