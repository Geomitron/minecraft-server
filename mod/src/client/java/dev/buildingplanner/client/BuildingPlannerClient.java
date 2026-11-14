package dev.buildingplanner.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.sync.EditKind;
import dev.buildingplanner.sync.Payloads;
import dev.buildingplanner.sync.PlayerMessage;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.malilib.event.RenderEventHandler;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
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
	private static final KeyMapping BLOCK_MIX =
		new KeyMapping("key.buildingplanner.block_mix", InputConstants.KEY_B, CATEGORY);
	private static final EditGlow GLOW = new EditGlow();
	/** The player's block mix, kept while the game runs, across servers. */
	private static final BlockMix MIX = new BlockMix();

	/** Null while not connected to a server with Building Planner. */
	private static ClientSession session;

	@Override
	public void onInitializeClient() {
		KeyMappingHelper.registerKeyMapping(START_PLAN);
		KeyMappingHelper.registerKeyMapping(BLOCK_MIX);
		RenderEventHandler.getInstance().registerWorldLastRenderer(GLOW);
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Hello.TYPE, (hello, context) -> {
			if (hello.protocolVersion() == Payloads.PROTOCOL_VERSION) {
				session = new ClientSession(hello.limits(), GLOW, MIX);
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
			while (BLOCK_MIX.consumeClick()) {
				if (session != null) {
					toggleMix();
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

	/** The mix key: turns the mix off while it's on, or else opens the picker. */
	private static void toggleMix() {
		if (MIX.isOn()) {
			MIX.turnOff();
			LitematicaPlans.showMessage(PlayerMessage.mixOff());
		} else {
			BlockMixScreen.open(MIX, () -> LitematicaPlans.showMessage(
				PlayerMessage.mixOn(BLOCK_MIX.getTranslatedKeyMessage().getString())));
		}
	}

	/**
	 * Hook L1: Litematica is about to write {@code state} into {@code container}. Returns the state to write, which the
	 * mix may have changed.
	 */
	public static BlockState captureWrite(LitematicaBlockStateContainer container, int x, int y, int z, BlockState state) {
		return session == null ? state : session.captureWrite(container, x, y, z, state);
	}

	/** Whether Building Planner is on for this server, so Effortless Building's hooks draw into plans. */
	public static boolean active() {
		return session != null;
	}

	/** Hooks E5 and E6: whether shapes take their blocks from the mix. */
	public static boolean mixOn() {
		return session != null && MIX.isOn();
	}

	/** Hook E5: the mix's block at a world position. Only call while {@link #mixOn()}. */
	public static Block mixBlockAt(BlockPos pos) {
		return MIX.blockAt(pos);
	}

	/**
	 * Hooks E1 and E2: a shape the player drew, as world positions and states in the order drawn, and the positions it
	 * started at, which pick its plan.
	 */
	public static void captureShape(EditKind kind, Map<BlockPos, BlockState> blocks, List<BlockPos> starts) {
		if (session != null) {
			session.captureShape(kind, blocks, starts);
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
