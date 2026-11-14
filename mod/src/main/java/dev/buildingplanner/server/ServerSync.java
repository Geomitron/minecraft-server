package dev.buildingplanner.server;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.sync.Payloads;
import dev.buildingplanner.sync.SyncLimits;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Connects {@link PlanSync} to the game: one per running server, fed by the payload handlers (which Fabric runs on the
 * server thread) and sending only to players who can receive Building Planner's payloads.
 */
public final class ServerSync {
	private static PlanSync current;

	private ServerSync() {
	}

	/** The running server's sync, for the Syncmatica listener to add and remove plans. */
	public static Optional<PlanSync> current() {
		return Optional.ofNullable(current);
	}

	public static void register() {
		SyncLimits limits;
		try {
			limits = ServerConfig.load(FabricLoader.getInstance().getConfigDir().resolve("buildingplanner.properties"));
		} catch (IOException e) {
			throw new UncheckedIOException("Couldn't read Building Planner's config file", e);
		}
		ServerLifecycleEvents.SERVER_STARTING.register(server -> current = new PlanSync(limits, outbox(server)));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> current = null);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (ServerPlayNetworking.canSend(handler, Payloads.Hello.TYPE)) {
				sender.sendPacket(new Payloads.Hello(Payloads.PROTOCOL_VERSION, limits));
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
			current().ifPresent(sync -> sync.disconnect(handler.getPlayer().getUUID())));

		ServerPlayNetworking.registerGlobalReceiver(Payloads.Subscribe.TYPE, (payload, context) ->
			current().ifPresent(sync -> sync.subscribe(context.player().getUUID(), payload.plan())));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.Unsubscribe.TYPE, (payload, context) ->
			current().ifPresent(sync -> sync.unsubscribe(context.player().getUUID(), payload.plan())));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.Edit.TYPE, (payload, context) ->
			current().ifPresent(sync -> sync.edit(context.player().getUUID(), payload.plan(), payload.clientSeq(),
				payload.kind(), payload.changes())));
		BuildingPlanner.LOGGER.info("Building Planner live sync: {} blocks an edit, {} blocks a plan side",
			limits.maxEditBlocks(), limits.maxPlanSide());
	}

	private static PlanSync.Outbox outbox(MinecraftServer server) {
		return (playerId, payload) -> {
			ServerPlayer player = server.getPlayerList().getPlayer(playerId);
			if (player != null && ServerPlayNetworking.canSend(player, payload.type())) {
				ServerPlayNetworking.send(player, payload);
			}
		};
	}
}
