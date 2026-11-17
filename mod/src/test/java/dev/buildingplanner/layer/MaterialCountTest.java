package dev.buildingplanner.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
	private static BlockState air;
	private static BlockState bricks;
	private static BlockState stone;
	private static BlockState stairsNorth;
	private static BlockState stairsEast;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		air = Blocks.AIR.defaultBlockState();
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
			pos -> world.getOrDefault(pos, air), BlockState::equals);

		assertEquals(10, count.total().getInt(bricks));
		assertEquals(6, count.missing().getInt(bricks));
		assertEquals(1, count.wrong().getInt(bricks));
		assertEquals(1, count.total().size());
	}

	@Test
	void aRealBlockCountsAsBuiltWhenTheRuleSaysItIsTheSame() {
		Layer layer = new Layer();
		layer.set(new BlockPos(5, 70, 5), stairsNorth);
		BlockPos at = new BlockPos(5, 70, 5);

		MaterialCount strict = MaterialCount.of(layer, at, at, pos -> stairsEast, BlockState::equals);
		assertEquals(1, strict.missing().getInt(stairsNorth));
		assertEquals(1, strict.wrong().getInt(stairsNorth));

		MaterialCount sameBlock = MaterialCount.of(layer, at, at, pos -> stairsEast,
			(ghost, real) -> ghost.getBlock() == real.getBlock());
		assertEquals(1, sameBlock.total().getInt(stairsNorth));
		assertTrue(sameBlock.missing().isEmpty());
		assertTrue(sameBlock.wrong().isEmpty());
	}

	@Test
	void aBoxWithoutGhostBlocksIsEmpty() {
		Layer layer = new Layer();
		layer.set(new BlockPos(0, 64, 0), bricks);
		assertTrue(MaterialCount.of(layer, new BlockPos(1, 64, 0), new BlockPos(5, 64, 5), pos -> bricks,
			BlockState::equals).isEmpty());
	}
}
