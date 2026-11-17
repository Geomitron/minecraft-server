package dev.buildingplanner.server;

import static dev.buildingplanner.server.LayerNetwork.NETHER;
import static dev.buildingplanner.server.LayerNetwork.OVERWORLD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.server.LayerNetwork.Game;
import dev.buildingplanner.sync.LayerChange;
import dev.buildingplanner.sync.Payloads;
import dev.buildingplanner.sync.PlayerMessage;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The server's layer over the simulated network: snapshots, edits and their order, and the checks on edits. */
class LayerSyncTest {
	private static final BlockPos A = new BlockPos(1, 64, 1);
	private static final BlockPos B = new BlockPos(2, 64, 1);
	/** In column 2, 0, which the games here don't watch. */
	private static final BlockPos FAR = new BlockPos(40, 64, 1);
	private static BlockState stone;
	private static BlockState dirt;
	private static BlockState air;

	private LayerNetwork network;
	private Game alex;
	private Game blair;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		stone = Blocks.STONE.defaultBlockState();
		dirt = Blocks.DIRT.defaultBlockState();
		air = Blocks.AIR.defaultBlockState();
	}

	@BeforeEach
	void start() {
		network = new LayerNetwork();
		alex = network.join("Alex");
		blair = network.join("Blair");
		alex.watchAround(1);
		network.settle();
	}

	private static LayerChange set(BlockPos pos, BlockState state) {
		return new LayerChange(pos, state);
	}

	@Test
	void aWatchGetsEachColumnAskedForEvenAnEmptyOne() {
		alex.edit(set(A, stone), set(A.above(16), dirt));
		network.settle();

		blair.watch(new ChunkPos(0, 0), new ChunkPos(1, 0));
		network.settle();

		Payloads.Columns columns = assertInstanceOf(Payloads.Columns.class, blair.received.getFirst());
		assertEquals(OVERWORLD, columns.dimension());
		assertEquals(List.of(new ChunkPos(0, 0), new ChunkPos(1, 0)), columns.columns().stream().map(Payloads.Column::pos).toList());
		assertEquals(List.of(4, 5), columns.columns().get(0).sections().stream().map(section -> section.y()).toList());
		assertEquals(List.of(), columns.columns().get(1).sections());
		network.assertSameAsServer(blair);
	}

	@Test
	void aSnapshotThenLaterEditsKeepEveryGameTheSame() {
		alex.edit(set(A, stone));
		network.settle();

		// Blair's watch reaches the server before Alex's second edit: the snapshot has the first, an applied the second.
		blair.watchAround(1);
		alex.edit(set(B, dirt));
		blair.serverReadsOne();
		alex.serverReadsOne();
		network.settle();

		List<CustomPacketPayload> received = blair.received;
		assertInstanceOf(Payloads.Columns.class, received.get(0));
		Payloads.Applied applied = assertInstanceOf(Payloads.Applied.class, received.get(1));
		assertEquals(List.of(set(B, dirt)), applied.changes());
		assertEquals(alex.id, applied.author());
		network.assertSameAsServer(alex);
		network.assertSameAsServer(blair);
		assertEquals(dirt, blair.copy.get(B));
	}

	@Test
	void anEditTheServerAppliedBeforeAWatchArrivesInItsSnapshot() {
		alex.edit(set(A, stone));
		blair.watchAround(1);
		alex.serverReadsOne();
		blair.serverReadsOne();
		network.settle();

		assertEquals(List.of(), blair.applied());
		assertEquals(stone, blair.copy.get(A));
		network.assertSameAsServer(blair);
	}

	@Test
	void editsToOneBlockTakeTheLastInTheServersOrder() {
		blair.watchAround(1);
		network.settle();

		alex.edit(set(A, stone), set(B, stone));
		blair.edit(set(A, dirt));
		blair.serverReadsOne();
		alex.serverReadsOne();
		network.settle();

		assertEquals(stone, network.server.get(OVERWORLD, A));
		network.assertSameAsServer(alex);
		network.assertSameAsServer(blair);
		// Both games saw Blair's edit, then Alex's.
		for (Game game : List.of(alex, blair)) {
			assertEquals(List.of(blair.id, alex.id), game.applied().stream().map(Payloads.Applied::author).toList());
		}
	}

	@Test
	void theAuthorGetsEachEditBackWithItsSequenceAndOnlyWhatChanged() {
		alex.edit(set(A, stone));
		network.settle();

		int seq = alex.edit(set(A, stone), set(B, dirt), set(B, Blocks.CAVE_AIR.defaultBlockState()));
		network.settle();

		Payloads.Applied applied = alex.applied().getLast();
		assertEquals(seq, applied.clientSeq());
		// A is already stone; B goes to dirt then back to none, sent as air.
		assertEquals(List.of(set(B, dirt), set(B, air)), applied.changes());
		assertEquals(air, network.server.get(OVERWORLD, B));
	}

	@Test
	void anEditThatChangesNothingStillAnswersItsAuthorAlone() {
		blair.watchAround(1);
		network.settle();

		int seq = alex.edit(set(A, air));
		network.settle();

		assertEquals(seq, alex.applied().getLast().clientSeq());
		assertEquals(List.of(), alex.applied().getLast().changes());
		assertEquals(List.of(), blair.applied());
	}

	@Test
	void editsGoOnlyToPlayersWhoWatchAColumnTheyTouch() {
		blair.watch(new ChunkPos(1, 1));
		network.settle();

		alex.edit(set(A, stone));
		network.settle();
		assertEquals(List.of(), blair.applied());

		alex.edit(set(new BlockPos(17, 64, 17), stone), set(B, stone));
		network.settle();
		assertEquals(1, blair.applied().size());
		network.assertSameAsServer(blair);

		blair.unwatch(new ChunkPos(1, 1));
		network.settle();
		alex.edit(set(new BlockPos(18, 64, 17), stone));
		network.settle();
		assertEquals(1, blair.applied().size());
	}

	@Test
	void aWatchOfAnotherDimensionOrOutOfRangeIsIgnored() {
		blair.dimension = NETHER;
		blair.watch(new ChunkPos(0, 0));
		network.settle();
		assertEquals(List.of(), blair.received);

		blair.dimension = OVERWORLD;
		int range = LayerNetwork.VIEW_DISTANCE + LayerSync.WATCH_MARGIN;
		blair.watch(new ChunkPos(range, -range), new ChunkPos(range + 1, 0));
		network.settle();
		Payloads.Columns columns = assertInstanceOf(Payloads.Columns.class, blair.received.getFirst());
		assertEquals(List.of(new ChunkPos(range, -range)), columns.columns().stream().map(Payloads.Column::pos).toList());
	}

	@Test
	void everyFailedCheckRejectsTheWholeEditAndChangesNothing() {
		blair.watchAround(1);
		network.settle();

		List<Integer> seqs = List.of(
			alex.edit(NETHER, set(A, stone)),
			alex.edit(IntStream.rangeClosed(0, LayerNetwork.MAX_EDIT_BLOCKS)
				.mapToObj(i -> set(new BlockPos(i % 16, 64 + i / 16, 0), stone)).toArray(LayerChange[]::new)),
			alex.edit(set(A, stone), set(FAR, stone)),
			alex.edit(set(A, stone), set(new BlockPos(1, 320, 1), stone)),
			alex.edit(set(A, stone), set(new BlockPos(1, -65, 1), stone)));
		network.settle();

		assertEquals(seqs, alex.rejections.stream().map(Payloads.Rejected::clientSeq).toList());
		assertEquals(List.of(
				PlayerMessage.otherDimension(),
				PlayerMessage.tooManyBlocks(LayerNetwork.MAX_EDIT_BLOCKS + 1, LayerNetwork.MAX_EDIT_BLOCKS),
				PlayerMessage.notLoaded(),
				PlayerMessage.outsideBuildHeight(),
				PlayerMessage.outsideBuildHeight()),
			alex.rejections.stream().map(Payloads.Rejected::reason).toList());
		assertEquals(air, network.server.get(OVERWORLD, A));
		assertEquals(List.of(), alex.applied());
		assertEquals(1, blair.received.size(), "Blair got only the snapshot");
	}

	@Test
	void anEditPastTheWorldBorderIsRejected() {
		// Column 62, 0 runs from x 992 to 1007, across the border at 1000.
		alex.standIn(new ChunkPos(62, 0));
		alex.watch(new ChunkPos(62, 0));
		network.settle();

		int seq = alex.edit(set(new BlockPos(999, 64, 0), stone), set(new BlockPos(1001, 64, 0), stone));
		network.settle();

		assertEquals(List.of(new Payloads.Rejected(seq, PlayerMessage.outsideWorldBorder())), alex.rejections);
		assertEquals(air, network.server.get(OVERWORLD, new BlockPos(999, 64, 0)));
	}

	@Test
	void theHeightCheckFollowsTheEditsDimension() {
		alex.moveTo(NETHER);
		alex.watchAround(0);
		network.settle();

		int seq = alex.edit(set(new BlockPos(1, 256, 1), stone));
		alex.edit(set(new BlockPos(1, 255, 1), stone));
		network.settle();

		assertEquals(List.of(new Payloads.Rejected(seq, PlayerMessage.outsideBuildHeight())), alex.rejections);
		assertEquals(stone, network.server.get(NETHER, new BlockPos(1, 255, 1)));
	}

	@Test
	void aPlayerWhoChangesDimensionStopsGettingTheOldOnesEdits() {
		blair.watchAround(1);
		network.settle();

		blair.moveTo(NETHER);
		alex.edit(set(A, stone));
		network.settle();
		assertEquals(List.of(), blair.applied());

		// An edit for the dimension Blair left, sent before the move, is rejected.
		int seq = blair.edit(OVERWORLD, set(A, dirt));
		network.settle();
		assertEquals(List.of(new Payloads.Rejected(seq, PlayerMessage.otherDimension())), blair.rejections);

		blair.watchAround(1);
		blair.edit(set(A, dirt));
		network.settle();
		assertEquals(dirt, network.server.get(NETHER, A));
		assertEquals(stone, network.server.get(OVERWORLD, A));
		network.assertSameAsServer(blair);
	}

	@Test
	void aPlayerWhoLeftGetsNothingMore() {
		blair.watchAround(1);
		network.settle();
		blair.disconnect();

		alex.edit(set(A, stone));
		network.settle();

		assertEquals(List.of(), blair.applied());
		assertTrue(alex.idle());
	}
}
