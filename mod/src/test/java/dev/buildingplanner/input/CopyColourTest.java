package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.input.CopyColour.Refusal;
import dev.buildingplanner.input.CopyColour.Shown;
import dev.buildingplanner.input.CopyPlacement.Spot;
import dev.buildingplanner.layer.Layer;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CopyColourTest {
	/** A build height of -64 to 319 and a border from -100 to 99 on x and z, as blocks inside it. */
	private static final CopyColour.World WORLD = new CopyColour.World() {
		@Override
		public boolean inBuildHeight(int y) {
			return y >= -64 && y <= 319;
		}

		@Override
		public boolean insideBorder(BlockPos pos) {
			return pos.getX() >= -100 && pos.getX() <= 99 && pos.getZ() >= -100 && pos.getZ() <= 99;
		}
	};
	/** A copy 2 by 2 by 2 blocks: a stone at its lowest corner and a glass at its highest, so its box is the smallest. */
	private static final Vec3i SIZE = new Vec3i(2, 2, 2);
	private static BlockState stone;
	private static BlockState glass;
	private static BlockState dirt;
	private static Map<BlockPos, BlockState> blocks;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		stone = Blocks.STONE.defaultBlockState();
		glass = Blocks.GLASS.defaultBlockState();
		dirt = Blocks.DIRT.defaultBlockState();
		blocks = Map.of(BlockPos.ZERO, stone, new BlockPos(1, 1, 1), glass);
	}

	private static List<Spot> at(int x, int y, int z) {
		return List.of(new Spot(new BlockPos(x, y, z), SIZE));
	}

	private static Shown colour(List<Spot> row, Layer layer, int maxEditBlocks) {
		return CopyColour.of(row, blocks.size(), WORLD, maxEditBlocks, () -> CopyColour.overGhosts(row, blocks, layer));
	}

	@Test
	void aCopyOnClearGroundIsBlue() {
		assertEquals(Shown.BLUE, colour(at(0, 0, 0), new Layer(), 100));
	}

	@Test
	void aCopyBesideGhostBlocksIsStillBlue() {
		Layer layer = new Layer();
		// In the copy's box but in its empty space, and just outside its box.
		layer.set(new BlockPos(1, 0, 0), dirt);
		layer.set(new BlockPos(2, 0, 0), dirt);
		assertEquals(Shown.BLUE, colour(at(0, 0, 0), layer, 100));
	}

	@Test
	void aCopyWhoseBlockWouldReplaceAGhostBlockIsYellow() {
		Layer layer = new Layer();
		layer.set(new BlockPos(11, 1, 1), dirt);
		assertEquals(Shown.YELLOW, colour(at(10, 0, 0), layer, 100));
	}

	@Test
	void aGhostBlockTheSameAsTheCopysBlockThereIsntReplaced() {
		Layer layer = new Layer();
		layer.set(new BlockPos(10, 0, 0), stone);
		layer.set(new BlockPos(11, 1, 1), glass);
		assertEquals(Shown.BLUE, colour(at(10, 0, 0), layer, 100));
	}

	@Test
	void aRowIsYellowWhenAnyOfItsCopiesWouldReplaceAGhostBlock() {
		List<Spot> row = List.of(new Spot(new BlockPos(0, 0, 0), SIZE), new Spot(new BlockPos(2, 0, 0), SIZE),
			new Spot(new BlockPos(4, 0, 0), SIZE));
		Layer layer = new Layer();
		layer.set(new BlockPos(4, 0, 0), dirt);
		assertEquals(Shown.YELLOW, colour(row, layer, 100));
		assertEquals(Shown.BLUE, colour(row.subList(0, 2), layer, 100));
	}

	@Test
	void aCopyPartlyAboveOrBelowTheBuildHeightIsRed() {
		assertEquals(Shown.red(Refusal.BUILD_HEIGHT), colour(at(0, 319, 0), new Layer(), 100));
		assertEquals(Shown.red(Refusal.BUILD_HEIGHT), colour(at(0, -65, 0), new Layer(), 100));
		assertEquals(Shown.BLUE, colour(at(0, 318, 0), new Layer(), 100));
		assertEquals(Shown.BLUE, colour(at(0, -64, 0), new Layer(), 100));
	}

	@Test
	void aCopyPartlyOutsideTheWorldBorderIsRed() {
		assertEquals(Shown.red(Refusal.WORLD_BORDER), colour(at(99, 0, 0), new Layer(), 100));
		assertEquals(Shown.red(Refusal.WORLD_BORDER), colour(at(0, 0, -101), new Layer(), 100));
		assertEquals(Shown.BLUE, colour(at(98, 0, -100), new Layer(), 100));
	}

	@Test
	void redWinsOverYellow() {
		Layer layer = new Layer();
		layer.set(new BlockPos(99, 0, 0), dirt);
		assertEquals(Shown.red(Refusal.WORLD_BORDER), colour(at(99, 0, 0), layer, 100));
	}

	@Test
	void aRowOverTheBlockCapIsRed() {
		// 4 copies of 2 blocks: 8, against caps of 7 and 8.
		List<Spot> row = List.of(new Spot(new BlockPos(0, 0, 0), SIZE), new Spot(new BlockPos(2, 0, 0), SIZE),
			new Spot(new BlockPos(4, 0, 0), SIZE), new Spot(new BlockPos(6, 0, 0), SIZE));
		assertEquals(Shown.red(Refusal.TOO_MANY_BLOCKS), colour(row, new Layer(), 7));
		assertEquals(Shown.BLUE, colour(row, new Layer(), 8));
		assertEquals(Shown.BLUE, colour(row.subList(0, 3), new Layer(), 7));
	}

	@Test
	void aRowReachingPastTheBuildHeightIsRedThoughItsFirstCopyIsnt() {
		List<Spot> row = List.of(new Spot(new BlockPos(0, 317, 0), SIZE), new Spot(new BlockPos(0, 319, 0), SIZE));
		assertEquals(Shown.red(Refusal.BUILD_HEIGHT), colour(row, new Layer(), 100));
		assertEquals(Shown.BLUE, colour(row.subList(0, 1), new Layer(), 100));
	}

	@Test
	void theBlockCapComesFirstAsOnTheServer() {
		assertEquals(Shown.red(Refusal.TOO_MANY_BLOCKS), colour(at(99, 319, 0), new Layer(), 1));
	}

	@Test
	void theOneGhostBlockIsBlueYellowOrRedAsA1x1x1Spot() {
		BlockPos at = new BlockPos(5, 0, 5);
		Layer layer = new Layer();
		assertEquals(Shown.BLUE, CopyColour.ofBlocks(List.of(at), Map.of(at, stone), WORLD, 100, layer));
		layer.set(at, stone);
		assertEquals(Shown.BLUE, CopyColour.ofBlocks(List.of(at), Map.of(at, stone), WORLD, 100, layer));
		layer.set(at, dirt);
		assertEquals(Shown.YELLOW, CopyColour.ofBlocks(List.of(at), Map.of(at, stone), WORLD, 100, layer));
		BlockPos below = new BlockPos(5, -65, 5);
		assertEquals(Shown.red(Refusal.BUILD_HEIGHT),
			CopyColour.ofBlocks(List.of(below), Map.of(below, stone), WORLD, 100, new Layer()));
		BlockPos outside = new BlockPos(100, 0, 5);
		assertEquals(Shown.red(Refusal.WORLD_BORDER),
			CopyColour.ofBlocks(List.of(outside), Map.of(outside, stone), WORLD, 100, new Layer()));
	}

	@Test
	void aRowOfOneBlocksIsYellowWhereAnyOfItsBlocksWouldReplaceADifferentGhostBlockAndRedWherePartIsOutside() {
		List<BlockPos> row = List.of(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0), new BlockPos(2, 0, 0));
		// A mix's blocks along the row: the ghost glass at the middle is the glass placed there, the dirt at the end isn't.
		Map<BlockPos, BlockState> placed = Map.of(row.get(0), stone, row.get(1), glass, row.get(2), stone);
		Layer layer = new Layer();
		layer.set(row.get(1), glass);
		assertEquals(Shown.BLUE, CopyColour.ofBlocks(row, placed, WORLD, 100, layer));
		layer.set(row.get(2), dirt);
		assertEquals(Shown.YELLOW, CopyColour.ofBlocks(row, placed, WORLD, 100, layer));
		List<BlockPos> crossing = List.of(new BlockPos(98, 0, 0), new BlockPos(99, 0, 0), new BlockPos(100, 0, 0));
		assertEquals(Shown.red(Refusal.WORLD_BORDER), CopyColour.ofBlocks(crossing,
			Map.of(crossing.get(0), stone, crossing.get(1), stone), WORLD, 100, new Layer()));
	}

	@Test
	void ghostBlocksAreReadOnlyWhileARedCheckPasses() {
		boolean[] asked = new boolean[1];
		CopyColour.of(at(0, 319, 0), 2, WORLD, 100, () -> asked[0] = true);
		assertFalse(asked[0]);
		CopyColour.of(at(0, 0, 0), 2, WORLD, 100, () -> asked[0] = true);
		assertTrue(asked[0]);
	}
}
