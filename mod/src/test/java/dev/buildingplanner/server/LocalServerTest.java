package dev.buildingplanner.server;

import static dev.buildingplanner.server.LayerNetwork.NETHER;
import static dev.buildingplanner.server.LayerNetwork.OVERWORLD;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.layer.LayerFiles;
import dev.buildingplanner.sync.EditKind;
import dev.buildingplanner.sync.LayerChange;
import dev.buildingplanner.sync.LayerReplica;
import dev.buildingplanner.sync.Payloads;
import dev.buildingplanner.sync.PlayerMessage;
import dev.buildingplanner.sync.SyncLimits;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The local server a session runs on the player's computer, on a server that doesn't run Building Planner: the game's
 * own {@link LayerReplica} sends it each payload at once, as the client does, and reads what it queued at the start of
 * the next tick ({@link #tick}). The world is LayerNetwork's: the overworld and the Nether, a view distance of 4, and
 * every chunk loaded with only air unless the test says otherwise; the game's real blocks reach the local server as
 * hook C1 and the check at chunk load hand them over, and {@code awaiting} holds the blocks the game placed that the
 * server hasn't answered for yet.
 */
class LocalServerTest {
	private static final UUID PLAYER = UUID.fromString("527e7f9d-ae90-3a44-aa88-30a47a9cdf34");
	private static final BlockPos A = new BlockPos(1, 64, 1);
	private static final BlockPos B = new BlockPos(2, 64, 1);
	private static BlockState stone;
	private static BlockState glass;
	private static BlockState air;

	@TempDir
	Path game;

	private final Map<BlockPos, BlockState> real = new HashMap<>();
	private final Set<ChunkPos> unloaded = new HashSet<>();
	/** Positions where the game placed a block the server hasn't answered for yet. */
	private final Set<BlockPos> awaiting = new HashSet<>();
	private Identifier dimension = OVERWORLD;
	private LayerSync.History history;
	private LocalServer server;
	private LayerReplica replica;
	private final List<CustomPacketPayload> received = new ArrayList<>();
	private int clientSeq;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		stone = Blocks.STONE.defaultBlockState();
		glass = Blocks.GLASS.defaultBlockState();
		air = Blocks.AIR.defaultBlockState();
	}

	@BeforeEach
	void start() {
		history = new LayerSync.History();
		join();
	}

	private Path folder() {
		return LocalFolders.player(game, "localhost_25565", PLAYER);
	}

	private Path areaFile(BlockPos pos) {
		return new LayerFiles(LocalFolders.layer(folder())).areaFile(dimension, pos.getX() >> 9, pos.getZ() >> 9);
	}

	/** The player joins the server: the local server starts, its hello arrives, and the game watches columns -1 to 1. */
	private void join() {
		server = LocalServer.start(folder(), PLAYER, world(), history, 0);
		assertTrue(server != null);
		replica = new LayerReplica(dimension, (chunkX, chunkZ, column, changed) -> {
		});
		received.clear();
		tick();
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				replica.chunkLoaded(x, z);
			}
		}
		send(replica.takeMessages());
		tick();
	}

	private void leave() {
		server.stop();
	}

	private void send(List<CustomPacketPayload> payloads) {
		payloads.forEach(server::receive);
	}

	/** The start of the game's next tick: it reads every payload the local server queued, in order. */
	private void tick() {
		for (CustomPacketPayload payload : server.takeMessages()) {
			received.add(payload);
			switch (payload) {
				case Payloads.Columns p -> replica.receive(p);
				case Payloads.Applied p -> replica.receive(p);
				default -> {
				}
			}
		}
	}

	private void edit(BlockState state, BlockPos... positions) {
		server.receive(new Payloads.Edit(dimension, ++clientSeq, state.isAir() ? EditKind.ERASE : EditKind.SHAPE,
			List.of(positions).stream().map(pos -> new LayerChange(pos, state)).toList()));
		tick();
	}

	private void undo() {
		server.receive(new Payloads.Undo());
		tick();
	}

	private void redo() {
		server.receive(new Payloads.Redo());
		tick();
	}

	private BlockState at(BlockPos pos) {
		return replica.layer().get(pos);
	}

	private List<PlayerMessage> notices() {
		return received.stream().filter(Payloads.Notice.class::isInstance).map(p -> ((Payloads.Notice) p).message())
			.toList();
	}

	@Test
	void theFirstPayloadIsAHelloOfThisVersionWithTheDefaultCap() {
		LocalServer fresh = LocalServer.start(game.resolve("other"), PLAYER, world(), new LayerSync.History(), 0);

		assertEquals(new Payloads.Hello(Payloads.PROTOCOL_VERSION, SyncLimits.DEFAULT.maxEditBlocks()),
			fresh.takeMessages().getFirst());
	}

	@Test
	void anEditReachesTheGamesCopyAtItsNextTick() {
		server.receive(new Payloads.Edit(dimension, ++clientSeq, EditKind.SHAPE, List.of(new LayerChange(A, stone))));

		assertEquals(air, at(A));
		tick();
		assertEquals(stone, at(A));
		assertEquals(stone, server.layer().get(OVERWORLD, A));
	}

	@Test
	void anEraseRemovesGhostBlocks() {
		edit(stone, A, B);

		edit(air, A);

		assertEquals(air, at(A));
		assertEquals(stone, at(B));
	}

	@Test
	void undoAndRedoTakeAnEditBackAndForward() {
		edit(stone, A, B);
		edit(glass, A);

		undo();
		assertEquals(stone, at(A));
		undo();
		assertEquals(air, at(A));
		assertEquals(air, at(B));
		undo();
		assertEquals(List.of(PlayerMessage.nothingToUndo()), notices());

		redo();
		redo();
		assertEquals(glass, at(A));
		assertEquals(stone, at(B));
	}

	@Test
	void anEditOutsideTheWatchedColumnsIsRejected() {
		edit(stone, new BlockPos(100, 64, 100));

		assertEquals(Payloads.Rejected.class, received.getLast().getClass());
	}

	@Test
	void theStepsSurviveLeavingAndRejoiningAndChangingDimension() {
		edit(stone, A);
		leave();
		join();
		edit(glass, B);

		dimension = NETHER;
		server.changedDimension();
		replica.levelChanged(NETHER);
		undo();
		dimension = OVERWORLD;
		server.changedDimension();
		replica.levelChanged(OVERWORLD);
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				replica.chunkLoaded(x, z);
			}
		}
		send(replica.takeMessages());
		tick();
		assertEquals(air, at(B));
		undo();

		assertEquals(air, at(A));
		redo();
		assertEquals(stone, at(A));
	}

	@Test
	void leavingWritesTheFilesAndTheNextSessionReadsThem() {
		edit(stone, A);
		Path file = areaFile(A);
		assertFalse(Files.exists(file));

		leave();
		assertTrue(Files.exists(file));
		history = new LayerSync.History();
		join();

		assertEquals(stone, at(A));
		assertEquals(stone, server.layer().get(OVERWORLD, A));
	}

	@Test
	void changesAreWrittenEveryThirtySeconds() {
		edit(stone, A);

		server.tick(LayerStore.SAVE_INTERVAL_MILLIS - 1);
		assertFalse(Files.exists(areaFile(A)));
		server.tick(LayerStore.SAVE_INTERVAL_MILLIS);
		// The background thread writes it; stopping waits for it.
		leave();
		assertTrue(Files.exists(areaFile(A)));
	}

	@Test
	void anUnreadableFileIsLeftOutAndNeverWrittenOver() throws IOException {
		edit(stone, A);
		leave();
		Path file = areaFile(A);
		byte[] unreadable = "not a layer file".getBytes();
		Files.write(file, unreadable);
		history = new LayerSync.History();

		join();
		assertEquals(air, at(A));
		edit(glass, B);
		leave();

		assertArrayEquals(unreadable, Files.readAllBytes(file));
	}

	@Test
	void aRealBlockThatFillsAGhostBlockRemovesItAtTheEndOfTheTickWithNoUndoStep() {
		edit(stone, A, B);
		real.put(A, stone);
		real.put(B, glass);

		server.realChanged(OVERWORLD, A, stone);
		server.realChanged(OVERWORLD, B, glass);
		assertEquals(stone, at(A));
		server.removeFilled();
		tick();

		assertEquals(air, at(A));
		assertEquals(stone, at(B));
		Payloads.Applied filled = (Payloads.Applied) received.getLast();
		assertEquals(Payloads.FILLED_AUTHOR, filled.author());
		assertEquals(List.of(new LayerChange(A, air)), filled.changes());
		undo();
		undo();
		assertEquals(List.of(PlayerMessage.nothingToUndo()), notices());
	}

	@Test
	void theCheckAtChunkLoadFindsABlockPlacedWhileTheChunkWasntLoaded() {
		edit(stone, A);
		unloaded.add(new ChunkPos(0, 0));
		real.put(A, stone);
		server.removeFilled();
		tick();
		assertEquals(stone, at(A));

		unloaded.clear();
		server.chunkLoaded(OVERWORLD, 0, 0, sectionY -> sectionY == A.getY() >> 4
			? (x, y, z) -> real.getOrDefault(new BlockPos(x, (sectionY << 4) + y, z), air) : null);
		server.removeFilled();
		tick();

		assertEquals(air, at(A));
	}

	@Test
	void aBlockAwaitingTheServersAnswerWaitsForIt() {
		edit(stone, A);
		real.put(A, stone);
		awaiting.add(A);

		server.realChanged(OVERWORLD, A, stone);
		server.removeFilled();
		tick();
		server.removeFilled();
		tick();
		assertEquals(stone, at(A));

		awaiting.remove(A);
		server.removeFilled();
		tick();
		assertEquals(air, at(A));
	}

	@Test
	void aBlockTheServerRefusesLeavesItsGhostBlock() {
		edit(stone, A);
		real.put(A, stone);
		awaiting.add(A);
		server.realChanged(OVERWORLD, A, stone);
		server.removeFilled();
		tick();

		// The server's answer puts back the block it had there.
		real.remove(A);
		awaiting.remove(A);
		server.realChanged(OVERWORLD, A, air);
		server.removeFilled();
		tick();

		assertEquals(stone, at(A));
	}

	@Test
	void afterStoppingItTakesNothingMore() {
		leave();

		server.receive(new Payloads.Edit(dimension, ++clientSeq, EditKind.SHAPE, List.of(new LayerChange(A, stone))));

		assertEquals(List.of(), server.takeMessages());
		assertEquals(air, server.layer().get(OVERWORLD, A));
	}

	private LayerSync.World world() {
		return new LayerSync.World() {
			@Override
			public Optional<LayerSync.Place> place(UUID player) {
				return player.equals(PLAYER) ? Optional.of(new LayerSync.Place(dimension, new ChunkPos(0, 0)))
					: Optional.empty();
			}

			@Override
			public int viewDistance() {
				return LayerNetwork.VIEW_DISTANCE;
			}

			@Override
			public boolean inBuildHeight(Identifier in, int y) {
				return y >= -64 && y < 320;
			}

			@Override
			public boolean insideBorder(Identifier in, BlockPos pos) {
				return Math.abs(pos.getX()) <= LayerNetwork.BORDER && Math.abs(pos.getZ()) <= LayerNetwork.BORDER;
			}

			@Override
			public BlockState realBlock(Identifier in, BlockPos pos) {
				if (!in.equals(dimension) || unloaded.contains(new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4))) {
					return null;
				}
				return in.equals(OVERWORLD) ? real.getOrDefault(pos, air) : air;
			}

			@Override
			public boolean awaitingServer(Identifier in, BlockPos pos) {
				return in.equals(dimension) && awaiting.contains(pos);
			}
		};
	}
}
