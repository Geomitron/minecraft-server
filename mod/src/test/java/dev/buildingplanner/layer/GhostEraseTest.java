package dev.buildingplanner.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Erasing ghost blocks: the Delete box's count and erase edit, and hook E2's filter. */
class GhostEraseTest {
	private static BlockState stone;
	private static BlockState air;
	private static BlockState lowerDoor;
	private static BlockState upperDoor;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		stone = Blocks.STONE.defaultBlockState();
		air = Blocks.AIR.defaultBlockState();
		lowerDoor = Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
			DoubleBlockHalf.LOWER);
		upperDoor = lowerDoor.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
	}

	@Test
	void theBoxErasesOnlyThePositionsWithGhostBlocks() {
		Layer layer = new Layer();
		layer.set(new BlockPos(0, 64, 0), stone);
		layer.set(new BlockPos(3, 66, 2), Blocks.GLASS.defaultBlockState());
		layer.set(new BlockPos(5, 64, 0), stone);

		Map<BlockPos, BlockState> erase = GhostErase.inBox(layer, new BlockPos(0, 64, 0), new BlockPos(4, 70, 4));

		assertEquals(Map.of(new BlockPos(0, 64, 0), air, new BlockPos(3, 66, 2), air), erase);
	}

	@Test
	void theBoxErasesBothHalvesOfADoorItCuts() {
		Layer layer = new Layer();
		layer.set(new BlockPos(1, 64, 1), lowerDoor);
		layer.set(new BlockPos(1, 65, 1), upperDoor);

		assertEquals(Set.of(new BlockPos(1, 64, 1), new BlockPos(1, 65, 1)),
			GhostErase.inBox(layer, new BlockPos(0, 60, 0), new BlockPos(2, 64, 2)).keySet(), "the lower half in the box");
		assertEquals(Set.of(new BlockPos(1, 64, 1), new BlockPos(1, 65, 1)),
			GhostErase.inBox(layer, new BlockPos(0, 65, 0), new BlockPos(2, 70, 2)).keySet(), "the upper half in the box");
	}

	@Test
	void theBoxErasesBothPartsOfABed() {
		Layer layer = new Layer();
		BlockState foot = Blocks.STRAW_BED.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
			Direction.EAST).setValue(BlockStateProperties.BED_PART, BedPart.FOOT);
		layer.set(new BlockPos(0, 64, 0), foot);
		layer.set(new BlockPos(1, 64, 0), foot.setValue(BlockStateProperties.BED_PART, BedPart.HEAD));

		assertEquals(Set.of(new BlockPos(0, 64, 0), new BlockPos(1, 64, 0)),
			GhostErase.inBox(layer, new BlockPos(0, 64, 0), new BlockPos(0, 64, 0)).keySet());
	}

	@Test
	void anotherBlockWhereTheOtherHalfWouldBeStays() {
		Layer layer = new Layer();
		layer.set(new BlockPos(1, 64, 1), lowerDoor);
		layer.set(new BlockPos(1, 65, 1), stone);
		layer.set(new BlockPos(2, 64, 1), lowerDoor);
		layer.set(new BlockPos(2, 65, 1), lowerDoor);

		assertEquals(Set.of(new BlockPos(1, 64, 1), new BlockPos(2, 64, 1)),
			GhostErase.inBox(layer, new BlockPos(1, 64, 1), new BlockPos(2, 64, 1)).keySet());
	}

	@Test
	void aBoxWithoutGhostBlocksErasesNothing() {
		Layer layer = new Layer();
		layer.set(new BlockPos(20, 64, 20), stone);

		assertTrue(GhostErase.inBox(layer, new BlockPos(0, 60, 0), new BlockPos(10, 70, 10)).isEmpty());
	}

	@Test
	void hookE2SendsAirOnlyWhereTheShapeMeetsGhostBlocks() {
		Layer layer = new Layer();
		layer.set(new BlockPos(1, 64, 0), stone);
		layer.set(new BlockPos(3, 64, 0), lowerDoor);
		layer.set(new BlockPos(3, 65, 0), upperDoor);
		List<BlockPos> shape = List.of(new BlockPos(0, 64, 0), new BlockPos(1, 64, 0), new BlockPos(2, 64, 0),
			new BlockPos(3, 64, 0), new BlockPos(4, 64, 0));

		Map<BlockPos, BlockState> erase = GhostErase.of(layer, shape);

		assertEquals(List.of(new BlockPos(1, 64, 0), new BlockPos(3, 64, 0), new BlockPos(3, 65, 0)),
			List.copyOf(erase.keySet()), "the ghost blocks among the shape's positions, in order, and the door's top");
		assertTrue(erase.values().stream().allMatch(BlockState::isAir));
		assertTrue(GhostErase.of(layer, List.of(new BlockPos(0, 64, 0), new BlockPos(2, 64, 0))).isEmpty(),
			"a shape over no ghost blocks erases nothing");
	}

	@Test
	void theCountTakesOnlyGhostBlocks() {
		Layer layer = new Layer();
		for (BlockPos pos : BlockPos.betweenClosed(0, 64, 0, 3, 64, 3)) {
			layer.set(pos.immutable(), stone);
		}
		layer.set(new BlockPos(0, 65, 0), Blocks.GLASS.defaultBlockState());
		layer.set(new BlockPos(9, 64, 9), stone);

		var counts = GhostErase.counts(layer, new BlockPos(0, 60, 0), new BlockPos(5, 70, 5));

		assertEquals(2, counts.size());
		assertEquals(16, counts.getInt(stone));
		assertEquals(1, counts.getInt(Blocks.GLASS.defaultBlockState()));
	}

	@Test
	void aBoxPassesTheCapOnlyWithMoreGhostBlocksThanIt() {
		Layer layer = new Layer();
		// A whole section of 4,096 ghost blocks, counted from its total, and 10 more in a section the box cuts.
		for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 15, 15, 15)) {
			layer.set(pos.immutable(), stone);
		}
		for (int x = 0; x < 10; x++) {
			layer.set(new BlockPos(x, 16, 0), stone);
		}
		BlockPos min = new BlockPos(0, 0, 0);
		BlockPos max = new BlockPos(15, 20, 15);

		assertFalse(GhostErase.overCap(layer, min, max, 4106));
		assertTrue(GhostErase.overCap(layer, min, max, 4105));
		assertFalse(GhostErase.overCap(layer, min, new BlockPos(15, 15, 15), 4096), "the whole section alone");
		assertTrue(GhostErase.overCap(layer, min, new BlockPos(15, 15, 15), 4095));
		assertFalse(GhostErase.overCap(layer, new BlockPos(0, 16, 1), new BlockPos(15, 20, 15), 0),
			"a box beside the ghost blocks holds none");
	}
}
