package dev.buildingplanner.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.sync.Payloads;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.malilib.event.RenderEventHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The client side: a session for each connection to a server with Building Planner, started by the server's
 * {@code hello}, and the hooks' way into it.
 */
public final class BuildingPlannerClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY =
		KeyMapping.Category.register(Identifier.fromNamespaceAndPath(BuildingPlanner.MOD_ID, "main"));
	private static final KeyMapping START_PLAN =
		new KeyMapping("key.buildingplanner.start_plan", InputConstants.KEY_N, CATEGORY);
	private static final EditGlow GLOW = new EditGlow();

	/** Null while not connected to a server with Building Planner. */
	private static ClientSession session;

	@Override
	public void onInitializeClient() {
		KeyMappingHelper.registerKeyMapping(START_PLAN);
		RenderEventHandler.getInstance().registerWorldLastRenderer(GLOW);
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Hello.TYPE, (hello, context) -> {
			if (hello.protocolVersion() == Payloads.PROTOCOL_VERSION) {
				session = new ClientSession(hello.limits(), GLOW);
			} else {
				BuildingPlanner.LOGGER.warn("The server runs Building Planner protocol {}, this client {}",
					hello.protocolVersion(), Payloads.PROTOCOL_VERSION);
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Snapshot.TYPE, (payload, context) -> receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Applied.TYPE, (payload, context) -> receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Rejected.TYPE, (payload, context) -> receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Notice.TYPE, (payload, context) -> receive(payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			if (session != null) {
				session.close();
				session = null;
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (START_PLAN.consumeClick()) {
				if (session != null) {
					StartPlanScreen.open();
				}
			}
			if (session != null) {
				session.tick();
			}
		});
		BuildingPlanner.LOGGER.info("Building Planner client loaded");
	}

	private static void receive(CustomPacketPayload payload) {
		if (session != null) {
			session.receive(payload);
		}
	}

	/** Hook L1: Litematica is about to write {@code state} into {@code container}. */
	public static void captureWrite(LitematicaBlockStateContainer container, int x, int y, int z, BlockState state) {
		if (session != null) {
			session.captureWrite(container, x, y, z, state);
		}
	}

	/** Hook E4: undoes the player's newest plan edit; returns false when Building Planner isn't on. */
	public static boolean undo() {
		if (session == null) {
			return false;
		}
		session.undo();
		return true;
	}

	/** Hook E4: redoes the player's newest undone plan edit; returns false when Building Planner isn't on. */
	public static boolean redo() {
		if (session == null) {
			return false;
		}
		session.redo();
		return true;
	}
}
