package dev.buildingplanner.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.layer.TestSections;
import java.util.Map;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CopyRulesTest {
	private static BlockState stone;
	private static BlockState dirt;
	private static BlockState glass;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		stone = Blocks.STONE.defaultBlockState();
		dirt = Blocks.DIRT.defaultBlockState();
		glass = Blocks.GLASS.defaultBlockState();
	}

	@Test
	void takesTheGhostBlockOverTheRealOneAndTheRealOneWhereThereIsNoGhostBlock() {
		BlockPos ghostOnly = new BlockPos(0, 0, 0);
		BlockPos realOnly = new BlockPos(1, 0, 0);
		BlockPos both = new BlockPos(0, 1, 0);
		Layer layer = new Layer();
		layer.set(ghostOnly, stone);
		layer.set(both, glass);
		layer.set(new BlockPos(2, 0, 0), stone);
		Map<BlockPos, BlockState> world = Map.of(realOnly, dirt, both, dirt, new BlockPos(-1, 0, 0), dirt);

		Map<BlockPos, BlockState> copied = CopyRules.blocks(new BlockPos(0, 0, 0), new BlockPos(1, 1, 1), layer,
			TestSections.of(world), true);

		assertEquals(Map.of(ghostOnly, stone, realOnly, dirt, both, glass), copied);
	}

	@Test
	void whileGhostBlocksAreHiddenTakesOnlyTheRealBlocksLeavingOutTheGhostBlocksInTheBox() {
		BlockPos ghostOnly = new BlockPos(0, 0, 0);
		BlockPos realOnly = new BlockPos(1, 0, 0);
		BlockPos both = new BlockPos(0, 1, 0);
		Layer layer = new Layer();
		layer.set(ghostOnly, stone);
		layer.set(both, glass);
		Map<BlockPos, BlockState> world = Map.of(realOnly, dirt, both, dirt);

		Map<BlockPos, BlockState> copied = CopyRules.blocks(new BlockPos(0, 0, 0), new BlockPos(1, 1, 1), layer,
			TestSections.of(world), false);

		assertEquals(Map.of(realOnly, dirt, both, dirt), copied);
	}

	@Test
	void takesNothingFromABoxOfAirWithNoGhostBlocks() {
		assertEquals(Map.of(), CopyRules.blocks(new BlockPos(0, 0, 0), new BlockPos(3, 3, 3), new Layer(),
			TestSections.of(Map.of()), true));
	}

	@Test
	void takesNothingFromABoxOfOnlyGhostBlocksWhileTheyAreHidden() {
		Layer layer = new Layer();
		layer.set(new BlockPos(1, 1, 1), stone);
		assertEquals(Map.of(), CopyRules.blocks(new BlockPos(0, 0, 0), new BlockPos(3, 3, 3), layer,
			TestSections.of(Map.of()), false));
	}

	@Test
	void refusesAnEmptyCopyAndOneOverTheServersCap() {
		assertEquals(Optional.of(PlayerMessage.nothingToCopy()), CopyRules.refusal(0, 50_000));
		assertEquals(Optional.of(PlayerMessage.copyTooLarge(50_001, 50_000)), CopyRules.refusal(50_001, 50_000));
		assertEquals(Optional.empty(), CopyRules.refusal(50_000, 50_000));
		assertEquals(Optional.empty(), CopyRules.refusal(1, 50_000));
	}
}
