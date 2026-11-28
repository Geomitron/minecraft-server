package dev.buildingplanner.server;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.sync.Payloads;
import dev.buildingplanner.sync.SyncLimits;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Building Planner's server part for one player, on their own computer, during a session with a server that doesn't
 * run Building Planner (see "Servers without Building Planner": "The local server"). It runs the dedicated server's
 * own {@link LayerSync} and {@link LayerStore}, and {@link MaterialLists} with {@link ListFiles}, on the player's
 * folder for that server ({@link LocalFolders}), with the block cap of {@link SyncLimits#DEFAULT}, and queues each
 * payload for the player, as a connection would, until the game takes them ({@link #takeMessages}) at the start of
 * its next tick. The game's side wires it to the game:
 * the {@link LayerSync.World} it answers from, and when it ticks and stops. Every method runs on the game's main
 * thread.
 */
public final class LocalServer {
	private final UUID player;
	private final String playerName;
	private final LayerStore layerStore;
	private final LayerSync layer;
	private final ListFiles listFiles;
	private final MaterialLists lists;
	private final List<CustomPacketPayload> outbox = new ArrayList<>();
	private boolean stopped;

	private LocalServer(UUID player, String playerName, LayerStore layerStore, Map<Identifier, Layer> layers,
		LayerSync.World world, LayerSync.History history, Path playerDir, Predicate<Identifier> hasDimension,
		long nowMillis) {
		this.player = player;
		this.playerName = playerName;
		this.layerStore = layerStore;
		LayerSync.Outbox toPlayer = (to, payload) -> {
			if (to.equals(player)) {
				outbox.add(payload);
			}
		};
		this.layer = new LayerSync(SyncLimits.DEFAULT.maxEditBlocks(), layers, toPlayer, world, history);
		this.listFiles = new ListFiles(LocalFolders.materialLists(playerDir));
		this.lists = new MaterialLists(listFiles, toPlayer, hasDimension, nowMillis);
		outbox.add(new Payloads.Hello(Payloads.PROTOCOL_VERSION, SyncLimits.DEFAULT.maxEditBlocks()));
		outbox.addAll(lists.join(player));
	}

	/**
	 * Reads every layer file in {@code playerDir} and starts, with the {@code hello} first in the queue and then the
	 * player's material lists ({@code lists} and {@code current}), read from their file; {@code playerName} names the
	 * player in the lists' warnings, {@code hasDimension} says whether the server has a dimension, and
	 * {@code history} is the player's undo steps from this folder's earlier sessions while the game runs. Null, with
	 * an error logged, when the layer's files can't be listed, so Building Planner stays off rather than run without
	 * them.
	 */
	public static LocalServer start(Path playerDir, UUID player, String playerName, LayerSync.World world,
		Predicate<Identifier> hasDimension, LayerSync.History history, long nowMillis) {
		LayerStore store = new LayerStore(LocalFolders.layer(playerDir), nowMillis);
		Map<Identifier, Layer> layers;
		try {
			layers = store.load();
		} catch (IOException e) {
			store.close();
			BuildingPlanner.LOGGER.error("Building Planner couldn't list its layer files in {}, so it's off on this server",
				playerDir, e);
			return null;
		}
		return new LocalServer(player, playerName, store, layers, world, history, playerDir, hasDimension, nowMillis);
	}

	/**
	 * A payload from the player's game at {@code nowMillis}, handled at once as the server would handle it; the place
	 * and the turn, which act on real blocks, are dropped.
	 */
	public void receive(CustomPacketPayload payload, long nowMillis) {
		if (stopped) {
			return;
		}
		switch (payload) {
			case Payloads.Watch p -> layer.watch(player, p.dimension(), p.columns());
			case Payloads.Unwatch p -> layer.unwatch(player, p.dimension(), p.columns());
			case Payloads.Edit p -> layer.edit(player, p.dimension(), p.clientSeq(), p.changes());
			case Payloads.Undo p -> layer.undo(player);
			case Payloads.Redo p -> layer.redo(player);
			case Payloads.SaveList p -> lists.save(player, playerName, p, nowMillis);
			case Payloads.DeleteList p -> lists.delete(player, playerName, p, nowMillis);
			case Payloads.OpenList p -> lists.open(player, playerName, p, nowMillis);
			case Payloads.CurrentList p -> lists.current(player, playerName, p, nowMillis);
			default -> {
			}
		}
	}

	/** The payloads queued for the player since the last call, in order. */
	public List<CustomPacketPayload> takeMessages() {
		List<CustomPacketPayload> messages = List.copyOf(outbox);
		outbox.clear();
		return messages;
	}

	/**
	 * At the start of each client tick: writes the changed areas every {@link LayerStore#SAVE_INTERVAL_MILLIS}, and
	 * the changed lists every {@link MaterialLists#SAVE_INTERVAL_MILLIS}.
	 */
	public void tick(long nowMillis) {
		if (!stopped) {
			layerStore.tick(layer, nowMillis);
			lists.tick(nowMillis);
		}
	}

	/** The player changed dimension: their game dropped every column it watched. */
	public void changedDimension() {
		layer.changedDimension(player);
	}

	/**
	 * Hook C1: a real block changed in a chunk the game has loaded. When it fills the ghost block there, the position
	 * waits for {@link #removeFilled}.
	 */
	public void realChanged(Identifier dimension, BlockPos pos, BlockState real) {
		if (!stopped) {
			layer.realChanged(dimension, pos, real);
		}
	}

	/** The game loaded a chunk: the ghost blocks its real blocks fill wait for {@link #removeFilled}. */
	public void chunkLoaded(Identifier dimension, int chunkX, int chunkZ, LayerSync.RealChunk real) {
		if (!stopped) {
			layer.chunkLoaded(dimension, chunkX, chunkZ, real);
		}
	}

	/**
	 * At the end of each client tick: removes the ghost blocks real blocks still fill, as one edit by no player,
	 * which reaches the game at its next tick; a position whose real block the server hasn't answered for yet waits.
	 */
	public void removeFilled() {
		if (!stopped) {
			layer.removeFilled();
		}
	}

	/** The player left the server, or the game is closing: writes every change and waits for the writes. */
	public void stop() {
		if (stopped) {
			return;
		}
		stopped = true;
		layer.disconnect(player);
		layerStore.save(layer);
		layerStore.close();
		lists.save();
		listFiles.close();
		outbox.clear();
	}

	/** The local layer, for the unit tests. */
	LayerSync layer() {
		return layer;
	}
}
