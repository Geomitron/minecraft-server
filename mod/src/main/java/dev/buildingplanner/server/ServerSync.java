package dev.buildingplanner.server;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.sync.Payloads;
import dev.buildingplanner.sync.SyncLimits;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Connects the server's shared layer ({@link LayerSync}, protocol 4) and each player's material lists
 * ({@link MaterialLists}) to the game: one of each per running server, fed by the payload handlers (which Fabric runs on
 * the server thread) and sending only to players who can receive Building Planner's payloads. The layer lives in
 * {@code <world>/building-planner/layer/}; before that folder exists, the server builds it from the plans Syncmatica
 * shared ({@link PlanImport}). The lists live in {@code <world>/building-planner/material-lists/}. Runs only on a
 * dedicated server.
 */
public final class ServerSync {
	private static LayerSync layer;
	private static LayerStore layerStore;
	private static MaterialLists lists;
	private static ListFiles listFiles;

	private ServerSync() {
	}

	public static void register() {
		SyncLimits limits;
		try {
			limits = ServerConfig.load(FabricLoader.getInstance().getConfigDir().resolve("buildingplanner.properties"));
		} catch (IOException e) {
			throw new UncheckedIOException("Couldn't read Building Planner's config file", e);
		}
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			Path worldDir = server.getWorldPath(LevelResource.ROOT).normalize();
			Path layerRoot = worldDir.resolve("building-planner").resolve("layer");
			if (!Files.exists(layerRoot)) {
				try {
					PlanImport.run(worldDir, FabricLoader.getInstance().getGameDir(), layerRoot);
				} catch (IOException e) {
					// Starting without the import would leave an empty layer, and the plans would never move into it.
					throw new UncheckedIOException("Couldn't move the shared plans into Building Planner's layer", e);
				}
			}
			layerStore = new LayerStore(layerRoot, System.currentTimeMillis());
			Map<Identifier, Layer> layers;
			try {
				layers = layerStore.load();
			} catch (IOException e) {
				// Starting without the layer would let edits overwrite its files.
				throw new UncheckedIOException("Couldn't list Building Planner's layer files", e);
			}
			layer = new LayerSync(limits.maxEditBlocks(), layers, (player, payload) -> send(server, player, payload),
				world(server));
			listFiles = new ListFiles(worldDir.resolve("building-planner").resolve("material-lists"));
			lists = new MaterialLists(listFiles, (player, payload) -> send(server, player, payload),
				dimension -> server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension)) != null,
				System.currentTimeMillis());
		});
		ServerTickEvents.START_SERVER_TICK.register(server -> {
			if (layer != null) {
				layerStore.tick(layer, System.currentTimeMillis());
				lists.tick(System.currentTimeMillis());
			}
		});
		// Players are still connected, but the server reads no more edits or list messages.
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			if (layer != null) {
				layerStore.save(layer);
				layerStore.close();
				lists.save();
				listFiles.close();
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			layer = null;
			layerStore = null;
			lists = null;
			listFiles = null;
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			// Building Planner 0.2 takes a hello of another layout under the same name, so the layer's hello goes only
			// to a game that also takes the layer's columns.
			if (ServerPlayNetworking.canSend(handler, Payloads.Hello.TYPE)
				&& ServerPlayNetworking.canSend(handler, Payloads.Columns.TYPE)) {
				sender.sendPacket(new Payloads.Hello(Payloads.PROTOCOL_VERSION, limits.maxEditBlocks()));
				if (lists != null && ServerPlayNetworking.canSend(handler, Payloads.Lists.TYPE)
					&& ServerPlayNetworking.canSend(handler, Payloads.Current.TYPE)) {
					lists.join(handler.getPlayer().getUUID()).forEach(sender::sendPacket);
				}
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if (layer != null) {
				layer.disconnect(handler.getPlayer().getUUID());
			}
		});
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, from, to) -> {
			if (layer != null) {
				layer.changedDimension(player.getUUID());
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(Payloads.Watch.TYPE, (payload, context) -> {
			if (layer != null) {
				layer.watch(context.player().getUUID(), payload.dimension(), payload.columns());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(Payloads.Unwatch.TYPE, (payload, context) -> {
			if (layer != null) {
				layer.unwatch(context.player().getUUID(), payload.dimension(), payload.columns());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(Payloads.Edit.TYPE, (payload, context) -> {
			if (layer != null) {
				layer.edit(context.player().getUUID(), payload.dimension(), payload.clientSeq(), payload.changes());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(Payloads.Undo.TYPE, (payload, context) -> {
			if (layer != null) {
				layer.undo(context.player().getUUID());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(Payloads.Redo.TYPE, (payload, context) -> {
			if (layer != null) {
				layer.redo(context.player().getUUID());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(Payloads.SaveList.TYPE, (payload, context) -> {
			if (lists != null) {
				lists.save(context.player().getUUID(), playerName(context.player()), payload, System.currentTimeMillis());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(Payloads.DeleteList.TYPE, (payload, context) -> {
			if (lists != null) {
				lists.delete(context.player().getUUID(), playerName(context.player()), payload, System.currentTimeMillis());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(Payloads.OpenList.TYPE, (payload, context) -> {
			if (lists != null) {
				lists.open(context.player().getUUID(), playerName(context.player()), payload, System.currentTimeMillis());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(Payloads.CurrentList.TYPE, (payload, context) -> {
			if (lists != null) {
				lists.current(context.player().getUUID(), playerName(context.player()), payload, System.currentTimeMillis());
			}
		});
		BuildingPlanner.LOGGER.info("Building Planner live sync: {} blocks an edit", limits.maxEditBlocks());
	}

	private static String playerName(ServerPlayer player) {
		return player.getGameProfile().name();
	}

	private static void send(MinecraftServer server, UUID playerId, CustomPacketPayload payload) {
		ServerPlayer player = server.getPlayerList().getPlayer(playerId);
		if (player != null && ServerPlayNetworking.canSend(player, payload.type())) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	private static LayerSync.World world(MinecraftServer server) {
		return new LayerSync.World() {
			@Override
			public Optional<LayerSync.Place> place(UUID playerId) {
				ServerPlayer player = server.getPlayerList().getPlayer(playerId);
				return player == null ? Optional.empty()
					: Optional.of(new LayerSync.Place(player.level().dimension().identifier(), player.chunkPosition()));
			}

			@Override
			public int viewDistance() {
				return server.getPlayerList().getViewDistance();
			}

			@Override
			public boolean inBuildHeight(Identifier dimension, int y) {
				ServerLevel level = level(dimension);
				return level != null && !level.isOutsideBuildHeight(y);
			}

			@Override
			public boolean insideBorder(Identifier dimension, BlockPos pos) {
				ServerLevel level = level(dimension);
				return level != null && level.getWorldBorder().isWithinBounds(pos);
			}

			private ServerLevel level(Identifier dimension) {
				return server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
			}
		};
	}
}
