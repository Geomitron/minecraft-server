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
 * Connects the server's shared layer ({@link LayerSync}, protocol 3) to the game: one per running server, fed by the
 * payload handlers (which Fabric runs on the server thread) and sending only to players who can receive Building
 * Planner's payloads. The layer lives in {@code <world>/building-planner/layer/}; before that folder exists, the server
 * builds it from the plans Syncmatica shared ({@link PlanImport}). Runs only on a dedicated server.
 */
public final class ServerSync {
	private static LayerSync layer;
	private static LayerStore layerStore;

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
		});
		ServerTickEvents.START_SERVER_TICK.register(server -> {
			if (layer != null) {
				layerStore.tick(layer, System.currentTimeMillis());
			}
		});
		// Players are still connected, but the server reads no more edits.
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			if (layer != null) {
				layerStore.save(layer);
				layerStore.close();
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			layer = null;
			layerStore = null;
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			// Building Planner 0.2 takes a hello of another layout under the same name, so the layer's hello goes only
			// to a game that also takes the layer's columns.
			if (ServerPlayNetworking.canSend(handler, Payloads.Hello.TYPE)
				&& ServerPlayNetworking.canSend(handler, Payloads.Columns.TYPE)) {
				sender.sendPacket(new Payloads.Hello(Payloads.PROTOCOL_VERSION, limits.maxEditBlocks()));
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
		BuildingPlanner.LOGGER.info("Building Planner live sync: {} blocks an edit", limits.maxEditBlocks());
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
