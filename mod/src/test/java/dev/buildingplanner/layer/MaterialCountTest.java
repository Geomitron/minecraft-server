package dev.buildingplanner.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MaterialCountTest {
	private static BlockState planks;
	private static BlockState bricks;
	private static BlockState stone;
	private static BlockState stairsNorth;
	private static BlockState stairsEast;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		planks = Blocks.OAK_PLANKS.defaultBlockState();
		bricks = Blocks.STONE_BRICKS.defaultBlockState();
		stone = Blocks.STONE.defaultBlockState();
		stairsNorth = Blocks.OAK_STAIRS.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.NORTH);
		stairsEast = stairsNorth.setValue(BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.EAST);
	}

	/** Step 9's check in numbers: 10 ghost stone bricks in a row, 4 of them built, one more with stone in its place. */
	@Test
	void countsEachGhostBlockInTheBoxWithTheMissingAndTheWrongOnes() {
		Layer layer = new Layer();
		Map<BlockPos, BlockState> world = new HashMap<>();
		for (int x = 0; x < 10; x++) {
			layer.set(new BlockPos(x, 64, 0), bricks);
		}
		for (int x = 0; x < 4; x++) {
			world.put(new BlockPos(x, 64, 0), bricks);
		}
		world.put(new BlockPos(9, 64, 0), stone);
		// Outside the box: neither counts.
		layer.set(new BlockPos(10, 64, 0), bricks);
		layer.set(new BlockPos(0, 66, 0), bricks);

		MaterialCount count = MaterialCount.of(layer, new BlockPos(0, 63, -1), new BlockPos(9, 65, 1),
			TestSections.of(world), BlockState::equals, true);

		assertEquals(10, count.total().getInt(bricks));
		assertEquals(6, count.missing().getInt(bricks));
		assertEquals(1, count.wrong().getInt(bricks));
		assertEquals(0, count.total().getInt(stone), "the real stone under a ghost brick counts only through the brick");
		assertEquals(1, count.total().size());
	}

	/** The spec's worked example: 10 ghost oak planks, 6 of them built, and 20 real stone blocks with no ghost over them. */
	@Test
	void theWorkedExampleCountsTheRealBlocksInTotalAndNeverInMissing() {
		Layer layer = new Layer();
		Map<BlockPos, BlockState> world = new HashMap<>();
		for (int x = 0; x < 10; x++) {
			layer.set(new BlockPos(x, 64, 0), planks);
			if (x < 6) {
				world.put(new BlockPos(x, 64, 0), planks);
			}
		}
		for (int x = 0; x < 10; x++) {
			world.put(new BlockPos(x, 64, 1), stone);
			world.put(new BlockPos(x, 64, 2), stone);
		}
		BlockPos min = new BlockPos(0, 64, 0);
		BlockPos max = new BlockPos(9, 65, 2);

		MaterialCount count = MaterialCount.of(layer, min, max, TestSections.of(world), BlockState::equals, true);

		assertEquals(10, count.total().getInt(planks));
		assertEquals(4, count.missing().getInt(planks));
		assertEquals(0, count.wrong().getInt(planks));
		assertEquals(20, count.total().getInt(stone));
		assertEquals(0, count.missing().getInt(stone));
		assertEquals(0, count.wrong().getInt(stone));
		assertEquals(2, count.total().size());
		assertEquals(1, count.missing().size());
	}

	/** Hidden ghost blocks count as if there were none: the real blocks under them count, and nothing is missing. */
	@Test
	void whileGhostBlocksAreHiddenTheRealBlocksCountAndNothingIsMissing() {
		Layer layer = new Layer();
		Map<BlockPos, BlockState> world = new HashMap<>();
		for (int x = 0; x < 4; x++) {
			layer.set(new BlockPos(x, 64, 0), bricks);
			world.put(new BlockPos(x, 64, 0), x < 2 ? bricks : stone);
			world.put(new BlockPos(x, 64, 1), stone);
		}
		BlockPos min = new BlockPos(0, 64, 0);
		BlockPos max = new BlockPos(3, 64, 1);

		MaterialCount shown = MaterialCount.of(layer, min, max, TestSections.of(world), BlockState::equals, true);
		assertEquals(4, shown.total().getInt(bricks));
		assertEquals(2, shown.missing().getInt(bricks));
		assertEquals(2, shown.wrong().getInt(bricks));
		assertEquals(4, shown.total().getInt(stone), "the real stone beside the ghost bricks");
		assertEquals(2, shown.total().size());

		MaterialCount hidden = MaterialCount.of(layer, min, max, TestSections.of(world), BlockState::equals, false);
		assertEquals(2, hidden.total().getInt(bricks));
		assertEquals(6, hidden.total().getInt(stone));
		assertTrue(hidden.missing().isEmpty());
		assertTrue(hidden.wrong().isEmpty());
	}

	@Test
	void aRealBlockCountsAsBuiltWhenTheRuleSaysItIsTheSame() {
		Layer layer = new Layer();
		layer.set(new BlockPos(5, 70, 5), stairsNorth);
		BlockPos at = new BlockPos(5, 70, 5);
		BoxCount.Sections world = TestSections.of(Map.of(at, stairsEast));

		MaterialCount strict = MaterialCount.of(layer, at, at, world, BlockState::equals, true);
		assertEquals(1, strict.missing().getInt(stairsNorth));
		assertEquals(1, strict.wrong().getInt(stairsNorth));

		MaterialCount sameBlock = MaterialCount.of(layer, at, at, world,
			(ghost, real) -> ghost.getBlock() == real.getBlock(), true);
		assertEquals(1, sameBlock.total().getInt(stairsNorth));
		assertTrue(sameBlock.missing().isEmpty());
		assertTrue(sameBlock.wrong().isEmpty());
	}

	@Test
	void aBoxWithNoBlocksIsEmptyAndABoxWithOnlyRealBlocksIsNot() {
		Layer layer = new Layer();
		layer.set(new BlockPos(0, 64, 0), bricks);
		BlockPos min = new BlockPos(1, 64, 0);
		BlockPos max = new BlockPos(5, 64, 5);

		MaterialCount nothing = MaterialCount.of(layer, min, max, TestSections.of(Map.of()), BlockState::equals, true);
		assertTrue(nothing.isEmpty());
		assertTrue(nothing.missing().isEmpty());

		MaterialCount real = MaterialCount.of(layer, min, max, TestSections.of(Map.of(new BlockPos(2, 64, 2), bricks)),
			BlockState::equals, true);
		assertFalse(real.isEmpty());
		assertEquals(1, real.total().getInt(bricks));
		assertTrue(real.missing().isEmpty());
	}
}
