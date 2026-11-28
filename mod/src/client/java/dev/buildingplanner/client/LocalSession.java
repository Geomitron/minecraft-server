package dev.buildingplanner.client;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.server.LayerSync;
import dev.buildingplanner.server.LocalFolders;
import dev.buildingplanner.mixin.minecraft.BlockStatePredictionHandlerAccessor;
import dev.buildingplanner.mixin.minecraft.ClientLevelInvoker;
import dev.buildingplanner.server.LocalServer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * A local session's server part, wired to the game: the {@link LocalServer} for the player's folder for the server
 * they joined ({@link LocalFolders}), answering from the game's own level and the dimensions the server told the game
 * of, and the link the session and its material lists send through.
 * The game hands the server's queued payloads to the session at the start of each client tick, and its real blocks'
 * changes (hook C1) and loaded chunks to the server, which removes the ghost blocks they fill at the end of the tick.
 */
final class LocalSession implements SessionLink {
	/** Each folder's undo steps, kept while the game runs, so they survive leaving and rejoining the server. */
	private static final Map<Path, LayerSync.History> HISTORIES = new HashMap<>();

	private final LocalServer server;

	private LocalSession(LocalServer server) {
		this.server = server;
	}

	/**
	 * Starts the server part for the player on the server the game just joined; null, with the reason logged, when
	 * the server has no folder name or its files can't be listed, so Building Planner stays off there.
	 */
	static LocalSession start(Minecraft mc, UUID player) {
		ServerData data = mc.getCurrentServer();
		Optional<String> name = data == null ? Optional.empty() : LocalFolders.server(data.ip, data.isRealm());
		if (name.isEmpty()) {
			BuildingPlanner.LOGGER.error("Building Planner has no folder for this server ({}), so it's off here",
				data == null ? "no server address" : data.ip);
			return null;
		}
		Path gameDir = mc.gameDirectory.toPath().toAbsolutePath().normalize();
		Path folder = LocalFolders.player(gameDir, name.get(), player);
		LocalServer server = LocalServer.start(folder, player, mc.getUser().getName(), world(mc, player),
			dimension -> mc.getConnection() != null
				&& mc.getConnection().levels().contains(ResourceKey.create(Registries.DIMENSION, dimension)),
			HISTORIES.computeIfAbsent(folder, key -> new LayerSync.History()), System.currentTimeMillis());
		if (server == null) {
			return null;
		}
		BuildingPlanner.LOGGER.info("The server doesn't run Building Planner, so your ghost blocks here are kept on "
			+ "this computer, in {}", gameDir.relativize(folder));
		return new LocalSession(server);
	}

	@Override
	public void send(CustomPacketPayload payload) {
		server.receive(payload, System.currentTimeMillis());
	}

	@Override
	public boolean local() {
		return true;
	}

	/** At the start of each client tick: the server part's own tick, then the payloads it queued, in order. */
	List<CustomPacketPayload> startTick() {
		server.tick(System.currentTimeMillis());
		return server.takeMessages();
	}

	/** The game's level changed: the player's game dropped every column it watched. */
	void levelChanged() {
		server.changedDimension();
	}

	/** The player left the server, or the game is closing: writes every change. */
	void stop() {
		server.stop();
	}

	/** Hook C1: a real block of the game's own level changed. */
	void realChanged(ClientLevel level, BlockPos pos, BlockState state) {
		server.realChanged(level.dimension().identifier(), pos, state);
	}

	/** The game loaded a chunk of its own level. */
	void chunkLoaded(ClientLevel level, LevelChunk chunk) {
		server.chunkLoaded(level.dimension().identifier(), chunk.getPos().x(), chunk.getPos().z(),
			LayerSync.RealChunk.of(chunk));
	}

	/** At the end of each client tick: the ghost blocks real blocks fill go. */
	void endTick() {
		server.removeFilled();
	}

	/**
	 * The game as the layer sees it: the player's dimension and column, the radius of the chunks the game keeps
	 * loaded, its level's build height and world border, and a real block only in a chunk the game has loaded.
	 */
	private static LayerSync.World world(Minecraft mc, UUID player) {
		return new LayerSync.World() {
			@Override
			public Optional<LayerSync.Place> place(UUID id) {
				if (!id.equals(player) || mc.level == null || mc.player == null) {
					return Optional.empty();
				}
				return Optional.of(new LayerSync.Place(mc.level.dimension().identifier(), mc.player.chunkPosition()));
			}

			@Override
			public int viewDistance() {
				// The game keeps chunks this far out (ClientChunkCache.calculateStorageRange).
				return Math.max(2, mc.options.renderDistance().get()) + 3;
			}

			@Override
			public boolean inBuildHeight(Identifier dimension, int y) {
				ClientLevel level = level(dimension);
				return level != null && !level.isOutsideBuildHeight(y);
			}

			@Override
			public boolean insideBorder(Identifier dimension, BlockPos pos) {
				ClientLevel level = level(dimension);
				return level != null && level.getWorldBorder().isWithinBounds(pos);
			}

			@Override
			public BlockState realBlock(Identifier dimension, BlockPos pos) {
				ClientLevel level = level(dimension);
				LevelChunk chunk = level == null ? null : (LevelChunk) level.getChunkSource().getChunk(pos.getX() >> 4,
					pos.getZ() >> 4, ChunkStatus.FULL, false);
				return chunk == null ? null : chunk.getBlockState(pos);
			}

			@Override
			public boolean awaitingServer(Identifier dimension, BlockPos pos) {
				// A block the game placed stays in the prediction handler's server states until the server answers.
				ClientLevel level = level(dimension);
				return level != null && ((BlockStatePredictionHandlerAccessor) ((ClientLevelInvoker) level)
					.buildingplanner$predictions()).buildingplanner$serverVerifiedStates().containsKey(pos.asLong());
			}

			private ClientLevel level(Identifier dimension) {
				ClientLevel level = mc.level;
				return level != null && level.dimension().identifier().equals(dimension) ? level : null;
			}
		};
	}
}
