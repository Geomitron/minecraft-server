package dev.buildingplanner.server;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.OpenChoices;
import dev.buildingplanner.sync.Payloads;
import dev.buildingplanner.sync.SyncLimits;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Connects {@link PlanSync} to the game: one per running server, fed by the payload handlers (which Fabric runs on the
 * server thread) and sending only to players who can receive Building Planner's payloads, with its plans kept in step
 * with Syncmatica's placements and saved under {@code <world>/building-planner/}. Runs only on a dedicated server.
 */
public final class ServerSync {
	private static PlanSync current;
	private static SyncmaticaPlans plans;

	private ServerSync() {
	}

	/** The running server's sync. */
	public static Optional<PlanSync> current() {
		return Optional.ofNullable(current);
	}

	/** The delete check (hook S1); true where Building Planner's server side isn't running. */
	public static boolean mayDelete(GameProfile sender, UUID placementId) {
		return plans == null || plans.mayDelete(sender, placementId);
	}

	public static void register() {
		SyncLimits limits;
		try {
			limits = ServerConfig.load(FabricLoader.getInstance().getConfigDir().resolve("buildingplanner.properties"));
		} catch (IOException e) {
			throw new UncheckedIOException("Couldn't read Building Planner's config file", e);
		}
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			PlanSync.Outbox outbox = outbox(server);
			current = new PlanSync(limits, outbox);
			PlanStore store = new PlanStore(server.getWorldPath(LevelResource.ROOT).resolve("building-planner").normalize(),
				System.currentTimeMillis());
			plans = new SyncmaticaPlans(server, current, store, outbox, System::currentTimeMillis);
		});
		ServerTickEvents.START_SERVER_TICK.register(server -> {
			if (plans != null) {
				plans.tick();
			}
		});
		// Players are still connected, but the server reads no more edits.
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			if (plans != null) {
				plans.stop();
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			current = null;
			plans = null;
		});

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
		ServerPlayNetworking.registerGlobalReceiver(Payloads.Undo.TYPE, (payload, context) ->
			current().ifPresent(sync -> sync.undo(context.player().getUUID())));
		if (OpenChoices.REDO) {
			ServerPlayNetworking.registerGlobalReceiver(Payloads.Redo.TYPE, (payload, context) ->
				current().ifPresent(sync -> sync.redo(context.player().getUUID())));
		}
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
