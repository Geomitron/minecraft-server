package dev.buildingplanner.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.layer.LayerSection;
import dev.buildingplanner.sync.EditKind;
import dev.buildingplanner.sync.LayerReplica;
import dev.buildingplanner.sync.LayerChange;
import dev.buildingplanner.sync.Payloads;
import dev.buildingplanner.sync.PlayerMessage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;

/**
 * A server's {@link LayerSync} and simulated players' games joined by in-order queues, one each way per game, as one
 * connection keeps a player's packets in order. The test decides when the server reads each game's next message and
 * when each game reads its next one, so messages interleave as they would over real connections.
 *
 * <p>The worlds: the overworld's build height is -64 to 319, the Nether's 0 to 255, and each world border runs from
 * -{@value #BORDER} to {@value #BORDER} on x and z. The view distance is {@value #VIEW_DISTANCE}.
 */
final class LayerNetwork {
	static final Identifier OVERWORLD = Identifier.withDefaultNamespace("overworld");
	static final Identifier NETHER = Identifier.withDefaultNamespace("the_nether");
	static final int BORDER = 1000;
	static final int VIEW_DISTANCE = 4;
	static final int MAX_EDIT_BLOCKS = 100;

	final LayerSync server;
	private final Map<UUID, LayerSync.Place> places = new HashMap<>();
	private final Map<UUID, Game> games = new LinkedHashMap<>();

	LayerNetwork() {
		this(Map.of());
	}

	LayerNetwork(Map<Identifier, Layer> layers) {
		server = new LayerSync(MAX_EDIT_BLOCKS, layers, this::deliver, new LayerSync.World() {
			@Override
			public Optional<LayerSync.Place> place(UUID player) {
				return Optional.ofNullable(places.get(player));
			}

			@Override
			public int viewDistance() {
				return VIEW_DISTANCE;
			}

			@Override
			public boolean inBuildHeight(Identifier dimension, int y) {
				return dimension.equals(NETHER) ? y >= 0 && y < 256 : y >= -64 && y < 320;
			}

			@Override
			public boolean insideBorder(Identifier dimension, BlockPos pos) {
				return Math.abs(pos.getX()) <= BORDER && Math.abs(pos.getZ()) <= BORDER;
			}
		});
	}

	/** A player who joins in the overworld, standing in column 0, 0. */
	Game join(String name) {
		Game game = new Game(name);
		games.put(game.id, game);
		places.put(game.id, new LayerSync.Place(OVERWORLD, new ChunkPos(0, 0)));
		return game;
	}

	private void deliver(UUID player, CustomPacketPayload payload) {
		Game game = games.get(player);
		if (game.connected) {
			game.inbox.add(payload);
		}
	}

	/** Runs every message in every queue until all are empty. */
	void settle() {
		boolean moved = true;
		while (moved) {
			moved = false;
			for (Game game : games.values()) {
				while (game.serverReadsOne()) {
					moved = true;
				}
				while (game.readsOne()) {
					moved = true;
				}
			}
		}
	}

	/** Asserts the game's copy of each column it holds matches the server's layer, block for block. */
	void assertSameAsServer(Game game) {
		for (long key : game.replica.heldColumns()) {
			int chunkX = Layer.keyX(key);
			int chunkZ = Layer.keyZ(key);
			for (int y = -64; y < 320; y++) {
				for (int x = 0; x < LayerSection.SIDE; x++) {
					for (int z = 0; z < LayerSection.SIDE; z++) {
						BlockPos pos = new BlockPos(chunkX * LayerSection.SIDE + x, y, chunkZ * LayerSection.SIDE + z);
						assertEquals(server.get(game.dimension(), pos), game.copy().get(pos), game + " at " + pos);
					}
				}
			}
		}
		assertTrue(game.idle(), game + " still has messages");
	}

	/**
	 * One player's game with the add-on: its copy of the layer is the client's own {@link LayerReplica}, and the test
	 * says which chunks it loads and unloads.
	 */
	final class Game {
		final UUID id = UUID.randomUUID();
		final String name;
		final List<Payloads.Rejected> rejections = new ArrayList<>();
		final List<PlayerMessage> notices = new ArrayList<>();
		/** Every message the game read, in order. */
		final List<CustomPacketPayload> received = new ArrayList<>();
		final Deque<CustomPacketPayload> inbox = new ArrayDeque<>();
		private final Deque<CustomPacketPayload> outbox = new ArrayDeque<>();
		final LayerReplica replica = new LayerReplica(OVERWORLD, (chunkX, chunkZ, column, changed) -> {
		});
		boolean connected = true;
		private int clientSeq;

		private Game(String name) {
			this.name = name;
		}

		Identifier dimension() {
			return replica.dimension();
		}

		/** The game's copy of the layer. */
		Layer copy() {
			return replica.layer();
		}

		/** The game loads these chunks, and sends what its copy of the layer asks for. */
		void watch(ChunkPos... columns) {
			for (ChunkPos column : columns) {
				replica.chunkLoaded(column.x(), column.z());
			}
			flush();
		}

		/** Loads every chunk within {@code radius} of column 0, 0. */
		void watchAround(int radius) {
			List<ChunkPos> columns = new ArrayList<>();
			for (int x = -radius; x <= radius; x++) {
				for (int z = -radius; z <= radius; z++) {
					columns.add(new ChunkPos(x, z));
				}
			}
			watch(columns.toArray(ChunkPos[]::new));
		}

		/** The game unloads these chunks. */
		void unwatch(ChunkPos... columns) {
			for (ChunkPos column : columns) {
				replica.chunkUnloaded(column.x(), column.z());
			}
			flush();
		}

		/** Sends a message as it is, such as one a game with other rules would send. */
		void send(CustomPacketPayload payload) {
			outbox.add(payload);
		}

		/** The player goes to another dimension: the server notes it at once, and the game drops every column. */
		void moveTo(Identifier newDimension) {
			places.put(id, new LayerSync.Place(newDimension, new ChunkPos(0, 0)));
			server.changedDimension(id);
			replica.levelChanged(newDimension);
			flush();
		}

		/** The player walks to another column of their dimension. */
		void standIn(ChunkPos column) {
			places.put(id, new LayerSync.Place(dimension(), column));
		}

		/** Sends an edit in the game's dimension and returns its client sequence. */
		int edit(LayerChange... changes) {
			return edit(dimension(), changes);
		}

		int edit(Identifier editDimension, LayerChange... changes) {
			clientSeq++;
			outbox.add(new Payloads.Edit(editDimension, clientSeq, EditKind.SHAPE, List.of(changes)));
			return clientSeq;
		}

		void undo() {
			outbox.add(new Payloads.Undo());
		}

		void redo() {
			outbox.add(new Payloads.Redo());
		}

		void disconnect() {
			connected = false;
			inbox.clear();
			outbox.clear();
			places.remove(id);
			server.disconnect(id);
		}

		/** The player joins again, in the overworld at column 0, 0, with a game that holds no column yet. */
		void reconnect() {
			connected = true;
			moveTo(OVERWORLD);
		}

		private void flush() {
			outbox.addAll(replica.takeMessages());
		}

		/** The server reads this game's next message; false when there was none. */
		boolean serverReadsOne() {
			CustomPacketPayload payload = outbox.poll();
			if (payload == null) {
				return false;
			}
			switch (payload) {
				case Payloads.Watch p -> server.watch(id, p.dimension(), p.columns());
				case Payloads.Unwatch p -> server.unwatch(id, p.dimension(), p.columns());
				case Payloads.Edit p -> server.edit(id, p.dimension(), p.clientSeq(), p.changes());
				case Payloads.Undo p -> server.undo(id);
				case Payloads.Redo p -> server.redo(id);
				default -> throw new IllegalStateException("Not a game's payload: " + payload);
			}
			return true;
		}

		/** This game reads its next message from the server; false when there was none. */
		boolean readsOne() {
			CustomPacketPayload payload = inbox.poll();
			if (payload == null) {
				return false;
			}
			received.add(payload);
			switch (payload) {
				case Payloads.Columns p -> replica.receive(p);
				case Payloads.Applied p -> replica.receive(p);
				case Payloads.Rejected p -> rejections.add(p);
				case Payloads.Notice p -> notices.add(p.message());
				default -> {
				}
			}
			return true;
		}

		/** The {@code applied} messages this game read, in order. */
		List<Payloads.Applied> applied() {
			return received.stream().filter(Payloads.Applied.class::isInstance).map(Payloads.Applied.class::cast).toList();
		}

		boolean idle() {
			return inbox.isEmpty() && outbox.isEmpty();
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
