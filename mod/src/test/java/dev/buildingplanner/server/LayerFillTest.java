package dev.buildingplanner.server;

import static dev.buildingplanner.server.LayerNetwork.MAX_EDIT_BLOCKS;
import static dev.buildingplanner.server.LayerNetwork.OVERWORLD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.server.LayerNetwork.Game;
import dev.buildingplanner.sync.LayerChange;
import dev.buildingplanner.sync.Payloads;
import dev.buildingplanner.sync.PlayerMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Ghost blocks the real world fills: the end-of-tick edit, the check at chunk load, and edits, undo and redo. */
class LayerFillTest {
	private static final BlockPos A = new BlockPos(1, 64, 1);
	private static final BlockPos B = new BlockPos(2, 64, 1);
	private static final BlockPos C = new BlockPos(3, 64, 1);
	private static BlockState stone;
	private static BlockState fence;
	private static BlockState joinedFence;
	private static BlockState air;

	private LayerNetwork network;
	private Game alex;
	private Game blair;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		stone = Blocks.STONE.defaultBlockState();
		fence = Blocks.OAK_FENCE.defaultBlockState();
		joinedFence = fence.setValue(BlockStateProperties.EAST, true);
		air = Blocks.AIR.defaultBlockState();
	}

	@BeforeEach
	void start() {
		network = new LayerNetwork();
		alex = network.join("Alex");
		blair = network.join("Blair");
		alex.watchAround(1);
		blair.watchAround(1);
		network.settle();
	}

	private void edit(Game game, BlockState state, BlockPos... positions) {
		game.edit(List.of(positions).stream().map(pos -> new LayerChange(pos, state)).toArray(LayerChange[]::new));
		network.settle();
	}

	private BlockState at(BlockPos pos) {
		return network.server.get(OVERWORLD, pos);
	}

	@Test
	void aFilledGhostBlockLeavesAtTheEndOfTheTickWithNoAuthorAndNoGlow() {
		edit(alex, fence, A, B);
		int before = blair.applied().size();

		network.build(A, joinedFence);
		assertEquals(fence, at(A), "the ghost block stays until the tick ends");
		network.endTick();

		assertEquals(air, at(A));
		assertEquals(fence, at(B));
		Payloads.Applied removed = blair.applied().getLast();
		assertEquals(before + 1, blair.applied().size());
		assertEquals(Payloads.FILLED_AUTHOR, removed.author());
		assertEquals(Payloads.NO_CLIENT_SEQ, removed.clientSeq());
		assertEquals(List.of(new LayerChange(A, air)), removed.changes());
		assertEquals(removed, alex.applied().getLast());
		network.assertSameAsServer(alex);
		network.assertSameAsServer(blair);
	}

	@Test
	void aBlockPlacedAnotherWayLeavesTheGhostBlock() {
		BlockState north = Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
			Direction.NORTH);
		edit(alex, north, A);
		int before = blair.applied().size();

		network.build(A, north.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
		network.endTick();

		assertEquals(north, at(A));
		assertEquals(before, blair.applied().size());
	}

	@Test
	void breakingAFilledBlockBringsNoGhostBlockBack() {
		edit(alex, stone, A);
		network.build(A, stone);
		network.endTick();

		network.build(A, air);
		network.endTick();

		assertEquals(air, at(A));
	}

	@Test
	void aBlockChangedAgainBeforeTheTickEndsLeavesTheGhostBlock() {
		edit(alex, stone, A);
		network.build(A, stone);
		network.build(A, Blocks.DIRT.defaultBlockState());
		network.endTick();

		assertEquals(stone, at(A));
	}

	@Test
	void theRemovalIsSplitAtTheBlockCap() {
		List<BlockPos> wall = IntStream.range(0, 2 * MAX_EDIT_BLOCKS + 50)
			.mapToObj(i -> new BlockPos(i % 16, 64 + i / 16, 2)).toList();
		for (int from = 0; from < wall.size(); from += MAX_EDIT_BLOCKS) {
			edit(alex, stone, wall.subList(from, Math.min(wall.size(), from + MAX_EDIT_BLOCKS)).toArray(BlockPos[]::new));
		}
		int before = blair.applied().size();

		wall.forEach(pos -> network.build(pos, stone));
		network.endTick();

		List<Payloads.Applied> removals = blair.applied().subList(before, blair.applied().size());
		assertEquals(List.of(MAX_EDIT_BLOCKS, MAX_EDIT_BLOCKS, 50), removals.stream().map(a -> a.changes().size()).toList());
		assertTrue(wall.stream().allMatch(pos -> at(pos) == air));
		network.assertSameAsServer(blair);
	}

	@Test
	void anEditSkipsPositionsARealBlockFills() {
		network.build(A, joinedFence);
		network.build(B, Blocks.OAK_PLANKS.defaultBlockState());
		network.endTick();

		int seq = alex.edit(new LayerChange(A, fence), new LayerChange(B, fence), new LayerChange(C, fence));
		network.settle();

		assertEquals(air, at(A));
		assertEquals(fence, at(B));
		assertEquals(fence, at(C));
		Payloads.Applied answer = alex.applied().getLast();
		assertEquals(seq, answer.clientSeq());
		assertEquals(List.of(new LayerChange(B, fence), new LayerChange(C, fence)), answer.changes());
	}

	@Test
	void aPasteOntoBuiltBlocksChangesNothing() {
		network.build(A, stone);
		network.build(B, joinedFence);
		network.endTick();
		int before = blair.applied().size();

		int seq = alex.edit(new LayerChange(A, stone), new LayerChange(B, fence));
		network.settle();

		assertEquals(air, at(A));
		assertEquals(air, at(B));
		assertEquals(seq, alex.applied().getLast().clientSeq());
		assertEquals(List.of(), alex.applied().getLast().changes());
		assertEquals(before, blair.applied().size());
		alex.undo();
		network.settle();
		assertEquals(PlayerMessage.nothingToUndo(), alex.notices.getLast());
	}

	@Test
	void whereTheServerHasNotLoadedTheChunkTheEditWritesTheGhostBlock() {
		network.build(A, stone);
		network.unloaded.add(new ChunkPos(0, 0));

		edit(alex, stone, A);

		assertEquals(stone, at(A));
	}

	@Test
	void theUndoNoticeCountsOnlyBlocksAnotherPlayerChanged() {
		edit(alex, stone, A, B, C);
		network.build(A, stone);
		network.endTick();
		edit(blair, fence, B);

		alex.undo();
		network.settle();

		assertEquals(air, at(A));
		assertEquals(fence, at(B));
		assertEquals(air, at(C));
		assertEquals(PlayerMessage.blocksLeftAsTheyAre(1), alex.notices.getLast());
	}

	@Test
	void anUndoOfBuiltBlocksAloneSaysNothing() {
		edit(alex, stone, A, B);
		network.build(A, stone);
		network.build(B, stone);
		network.endTick();
		int notices = alex.notices.size();

		alex.undo();
		network.settle();

		assertEquals(notices, alex.notices.size());
	}

	@Test
	void anUndoAndARedoSkipPositionsBuiltSince() {
		edit(alex, stone, A, B);
		edit(alex, air, A, B);
		network.build(A, stone);

		// The undo of the erase would write stone over the real stone at A.
		alex.undo();
		network.settle();
		assertEquals(air, at(A));
		assertEquals(stone, at(B));

		alex.redo();
		network.settle();
		assertEquals(air, at(B));
		network.build(B, stone);
		network.endTick();
		alex.undo();
		network.settle();
		assertEquals(air, at(B));
		alex.redo();
		network.settle();
		assertEquals(air, at(A));
		assertEquals(air, at(B));
		network.assertSameAsServer(alex);
	}

	@Test
	void aChunkLoadRemovesGhostBlocksBuiltWhileTheHookWasMissing() {
		edit(alex, fence, A, B);
		edit(alex, stone, new BlockPos(5, 100, 5));
		network.real.put(A, joinedFence);
		network.real.put(new BlockPos(5, 100, 5), stone);
		List<Integer> asked = new ArrayList<>();

		network.server.chunkLoaded(OVERWORLD, 0, 0, sectionY -> {
			asked.add(sectionY);
			return (x, y, z) -> network.real.getOrDefault(new BlockPos(x, (sectionY << 4) + y, z), air);
		});
		network.endTick();

		assertEquals(List.of(4, 6), asked, "only the sections that hold ghost blocks are read");
		assertEquals(air, at(A));
		assertEquals(fence, at(B));
		assertEquals(air, at(new BlockPos(5, 100, 5)));
		assertEquals(Payloads.FILLED_AUTHOR, blair.applied().getLast().author());
		network.assertSameAsServer(blair);
	}

	@Test
	void aChunkLoadSkipsSectionsOfOnlyAir() {
		edit(alex, stone, A);
		network.real.put(A, stone);

		network.server.chunkLoaded(OVERWORLD, 0, 0, sectionY -> null);
		network.server.chunkLoaded(OVERWORLD, 5, 5, sectionY -> {
			throw new AssertionError("a column without ghost blocks reads no section");
		});
		network.endTick();

		assertEquals(stone, at(A));
	}
}
