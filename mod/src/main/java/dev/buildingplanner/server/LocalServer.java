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
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Building Planner's server part for one player, on their own computer, during a session with a server that doesn't
 * run Building Planner (see "Servers without Building Planner": "The local server"). It runs the dedicated server's
 * own {@link LayerSync} and {@link LayerStore} on the player's folder for that server ({@link LocalFolders}), with
 * the block cap of {@link SyncLimits#DEFAULT}, and queues each payload for the player, as a connection would, until
 * the game takes them ({@link #takeMessages}) at the start of its next tick. The game's side wires it to the game:
 * the {@link LayerSync.World} it answers from, and when it ticks and stops. Every method runs on the game's main
 * thread.
 */
public final class LocalServer {
	private final UUID player;
	private final LayerStore layerStore;
	private final LayerSync layer;
	private final List<CustomPacketPayload> outbox = new ArrayList<>();
	private boolean stopped;

	private LocalServer(UUID player, LayerStore layerStore, Map<Identifier, Layer> layers, LayerSync.World world,
		LayerSync.History history) {
		this.player = player;
		this.layerStore = layerStore;
		this.layer = new LayerSync(SyncLimits.DEFAULT.maxEditBlocks(), layers, (to, payload) -> {
			if (to.equals(player)) {
				outbox.add(payload);
			}
		}, world, history);
		outbox.add(new Payloads.Hello(Payloads.PROTOCOL_VERSION, SyncLimits.DEFAULT.maxEditBlocks()));
	}

	/**
	 * Reads every layer file in {@code playerDir} and starts, with the {@code hello} first in the queue; {@code history}
	 * is the player's undo steps from this folder's earlier sessions while the game runs. Null, with an error logged,
	 * when the layer's files can't be listed, so Building Planner stays off rather than run without them.
	 */
	public static LocalServer start(Path playerDir, UUID player, LayerSync.World world, LayerSync.History history,
		long nowMillis) {
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
		return new LocalServer(player, store, layers, world, history);
	}

	/** A payload from the player's game, handled at once as the server would handle it; others are dropped. */
	public void receive(CustomPacketPayload payload) {
		if (stopped) {
			return;
		}
		switch (payload) {
			case Payloads.Watch p -> layer.watch(player, p.dimension(), p.columns());
			case Payloads.Unwatch p -> layer.unwatch(player, p.dimension(), p.columns());
			case Payloads.Edit p -> layer.edit(player, p.dimension(), p.clientSeq(), p.changes());
			case Payloads.Undo p -> layer.undo(player);
			case Payloads.Redo p -> layer.redo(player);
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

	/** At the start of each client tick: writes the changed areas every {@link LayerStore#SAVE_INTERVAL_MILLIS}. */
	public void tick(long nowMillis) {
		if (!stopped) {
			layerStore.tick(layer, nowMillis);
		}
	}

	/** The player changed dimension: their game dropped every column it watched. */
	public void changedDimension() {
		layer.changedDimension(player);
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
		outbox.clear();
	}

	/** The local layer, for the unit tests. */
	LayerSync layer() {
		return layer;
	}
}
