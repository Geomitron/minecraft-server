package dev.buildingplanner.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.layer.LayerColumn;
import java.util.ArrayList;
import java.util.List;
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

/** The player's copy of the layer on its own; {@code LayerSyncTest} runs it against the server. */
class LayerReplicaTest {
	private static final Identifier OVERWORLD = Identifier.withDefaultNamespace("overworld");
	private static final Identifier NETHER = Identifier.withDefaultNamespace("the_nether");
	private static final UUID AUTHOR = UUID.randomUUID();
	private static final BlockPos A = new BlockPos(1, 64, 2);
	private static final BlockPos B = new BlockPos(17, 64, 2);
	private static BlockState stone;

	/** What the listener heard: "x,z whole" for a replaced column, "x,z n" for n changed blocks, "+" when it holds blocks. */
	private final List<String> heard = new ArrayList<>();
	private LayerReplica replica;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		stone = Blocks.STONE.defaultBlockState();
	}

	@BeforeEach
	void start() {
		replica = new LayerReplica(OVERWORLD, (x, z, column, changed) ->
			heard.add(x + "," + z + " " + (changed == null ? "whole" : changed.size()) + (column == null ? "" : " +")));
	}

	private static Payloads.Columns snapshot(Identifier dimension, ChunkPos column, BlockPos... blocks) {
		Layer layer = new Layer();
		for (BlockPos pos : blocks) {
			layer.set(pos, stone);
		}
		var held = layer.column(column.x(), column.z());
		return new Payloads.Columns(dimension, List.of(new Payloads.Column(column, held == null ? List.of() : held.pack())));
	}

	private static Payloads.Applied applied(Identifier dimension, BlockPos... blocks) {
		List<LayerChange> changes = new ArrayList<>();
		for (BlockPos pos : blocks) {
			changes.add(new LayerChange(pos, stone));
		}
		return new Payloads.Applied(dimension, AUTHOR, Payloads.NO_CLIENT_SEQ, changes);
	}

	@Test
	void loadedChunksGoOutAsOneWatchAndUnloadedOnesAsOneUnwatch() {
		replica.chunkLoaded(0, 0);
		replica.chunkLoaded(1, 0);
		assertEquals(List.of(new Payloads.Watch(OVERWORLD, List.of(new ChunkPos(0, 0), new ChunkPos(1, 0)))),
			replica.takeMessages());
		assertEquals(List.of(), replica.takeMessages());

		replica.chunkUnloaded(1, 0);
		replica.chunkUnloaded(5, 5);
		assertEquals(List.of(new Payloads.Unwatch(OVERWORLD, List.of(new ChunkPos(1, 0)))), replica.takeMessages());
	}

	@Test
	void aChunkLoadedAndUnloadedInOneTickSendsNothing() {
		replica.chunkLoaded(0, 0);
		replica.chunkUnloaded(0, 0);
		assertEquals(List.<CustomPacketPayload>of(), replica.takeMessages());
	}

	@Test
	void editsChangeOnlyColumnsWhoseSnapshotArrived() {
		replica.chunkLoaded(0, 0);
		replica.chunkLoaded(1, 0);
		replica.takeMessages();
		replica.receive(snapshot(OVERWORLD, new ChunkPos(0, 0)));

		assertEquals(List.of(new LayerChange(A, stone)), replica.receive(applied(OVERWORLD, A, B)));
		assertEquals(stone, replica.layer().get(A));
		assertEquals(Blocks.AIR.defaultBlockState(), replica.layer().get(B));
		assertEquals(List.of("0,0 whole", "0,0 1 +"), heard);
	}

	@Test
	void theListenerHearsOfEachEditAndSnapshotBeforeItAppliesButNotOfADrop() {
		List<String> told = new ArrayList<>();
		replica = new LayerReplica(OVERWORLD, new LayerReplica.Listener() {
			@Override
			public void columnChanging(int x, int z, LayerColumn column, List<BlockPos> changing) {
				told.add("before " + x + "," + z + " " + (changing == null ? "whole" : changing.size()) + " "
					+ replica.layer().get(A).getBlock());
			}

			@Override
			public void columnChanged(int x, int z, LayerColumn column, List<BlockPos> changed) {
				told.add("after " + x + "," + z + " " + replica.layer().get(A).getBlock());
			}
		});
		replica.chunkLoaded(0, 0);
		replica.takeMessages();
		replica.receive(snapshot(OVERWORLD, new ChunkPos(0, 0)));
		replica.receive(applied(OVERWORLD, A, B));
		replica.chunkUnloaded(0, 0);
		assertEquals(List.of("before 0,0 whole Block{minecraft:air}", "after 0,0 Block{minecraft:air}",
			"before 0,0 1 Block{minecraft:air}", "after 0,0 Block{minecraft:stone}", "after 0,0 Block{minecraft:air}"),
			told);
	}

	@Test
	void aSnapshotReplacesTheColumnAndOneForAColumnNoLongerWatchedIsSkipped() {
		replica.chunkLoaded(0, 0);
		replica.takeMessages();
		replica.receive(snapshot(OVERWORLD, new ChunkPos(0, 0), A));
		assertEquals(stone, replica.layer().get(A));
		replica.receive(snapshot(OVERWORLD, new ChunkPos(0, 0)));
		assertEquals(Blocks.AIR.defaultBlockState(), replica.layer().get(A));

		replica.receive(snapshot(OVERWORLD, new ChunkPos(1, 0), B));
		replica.receive(snapshot(NETHER, new ChunkPos(0, 0), A));
		assertFalse(replica.holds(1, 0));
		assertEquals(Blocks.AIR.defaultBlockState(), replica.layer().get(A));
		assertEquals(List.of("0,0 whole +", "0,0 whole"), heard);
	}

	@Test
	void anUnloadedChunkDropsItsColumnAndReloadingAsksForItAgain() {
		replica.chunkLoaded(0, 0);
		replica.takeMessages();
		replica.receive(snapshot(OVERWORLD, new ChunkPos(0, 0), A));

		replica.chunkUnloaded(0, 0);
		assertFalse(replica.holds(0, 0));
		assertEquals(Blocks.AIR.defaultBlockState(), replica.layer().get(A));
		assertEquals(List.of(), replica.receive(applied(OVERWORLD, A)));

		// Loaded again before the unwatch went out: the server still watches it, but the game asks for a new snapshot.
		replica.chunkLoaded(0, 0);
		assertEquals(List.of(new Payloads.Watch(OVERWORLD, List.of(new ChunkPos(0, 0)))), replica.takeMessages());
		assertEquals(List.of("0,0 whole +", "0,0 whole"), heard);
	}

	@Test
	void aNewLevelDropsEveryColumnAndUnwatchesThemInTheOldDimension() {
		replica.chunkLoaded(0, 0);
		replica.takeMessages();
		replica.receive(snapshot(OVERWORLD, new ChunkPos(0, 0), A));
		replica.chunkLoaded(3, 3);

		replica.levelChanged(NETHER);
		replica.chunkLoaded(0, 0);
		assertEquals(NETHER, replica.dimension());
		assertFalse(replica.holds(0, 0));
		assertEquals(List.of(new Payloads.Unwatch(OVERWORLD, List.of(new ChunkPos(0, 0))),
			new Payloads.Watch(NETHER, List.of(new ChunkPos(0, 0)))), replica.takeMessages());
		assertEquals(List.of(), replica.receive(applied(OVERWORLD, A)));
		assertTrue(replica.layer().isEmpty());
		assertEquals(List.of("0,0 whole +", "0,0 whole"), heard);
	}
}
