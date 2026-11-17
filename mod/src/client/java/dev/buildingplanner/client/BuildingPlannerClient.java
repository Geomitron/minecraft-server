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
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The client side: a session for each connection to a server with Building Planner, started by the server's
 * {@code hello}, and the hooks' way into it. On any other server Building Planner stays off, silently.
 */
public final class BuildingPlannerClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY =
		KeyMapping.Category.register(Identifier.fromNamespaceAndPath(BuildingPlanner.MOD_ID, "main"));
	private static final KeyMapping START_PLAN =
		new KeyMapping("key.buildingplanner.start_plan", InputConstants.KEY_N, CATEGORY);
	private static final KeyMapping BLOCK_MIX =
		new KeyMapping("key.buildingplanner.block_mix", InputConstants.KEY_B, CATEGORY);
	// Copy and paste. None of these keys is a default key, or in a default key combination, of Minecraft, Litematica,
	// MaLiLib, Effortless Building or Freecam.
	private static final KeyMapping COPY = new KeyMapping("key.buildingplanner.copy", InputConstants.KEY_J, CATEGORY);
	private static final KeyMapping PASTE = new KeyMapping("key.buildingplanner.paste", InputConstants.KEY_K, CATEGORY);
	private static final KeyMapping TURN = new KeyMapping("key.buildingplanner.turn", InputConstants.KEY_U, CATEGORY);
	private static final KeyMapping MIRROR = new KeyMapping("key.buildingplanner.mirror", InputConstants.KEY_H, CATEGORY);
	private static final KeyMapping CLEAR =
		new KeyMapping("key.buildingplanner.clear", InputConstants.KEY_DELETE, CATEGORY);
	private static final EditGlow GLOW = new EditGlow();
	/** The player's block mix, kept while the game runs, across servers. */
	private static final BlockMix MIX = new BlockMix();

	/** The payloads a session sends; a server that can't receive them all doesn't run Building Planner. */
	private static final List<CustomPacketPayload.Type<?>> SERVERBOUND = List.of(Payloads.Subscribe.TYPE,
		Payloads.Unsubscribe.TYPE, Payloads.Edit.TYPE, Payloads.Undo.TYPE, Payloads.Redo.TYPE);

	/**
	 * Null while not connected to a server with Building Planner: on any other server, Building Planner is off, so
	 * Litematica's edits stay on the player's screen and Effortless Building's shapes and undo send nothing.
	 */
	private static ClientSession session;

	/** Hold to repeat in Effortless Building's Single mode; null without Effortless Building, whose classes it uses. */
	private static HeldClicks heldClicks;
	/** Whether Litematica's ghost block look has been set this run (see {@link LitematicaLook}). */
	private static boolean lookSet;

	@Override
	public void onInitializeClient() {
		if (FabricLoader.getInstance().isModLoaded("effortlessbuilding")) {
			heldClicks = new HeldClicks();
		}
		KeyMappingHelper.registerKeyMapping(START_PLAN);
		KeyMappingHelper.registerKeyMapping(BLOCK_MIX);
		List.of(COPY, PASTE, TURN, MIRROR, CLEAR).forEach(KeyMappingHelper::registerKeyMapping);
		RenderEventHandler.getInstance().registerWorldLastRenderer(GLOW);
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Hello.TYPE, (hello, context) -> hello(hello));
		// A singleplayer or LAN host's own world runs no server side.
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (!client.isLocalServer() && !SERVERBOUND.stream().allMatch(ClientPlayNetworking::canSend)) {
				BuildingPlanner.LOGGER.info("The server doesn't run Building Planner, so it's off");
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
			while (COPY.consumeClick()) {
				if (session != null) {
					session.copy(PASTE.getTranslatedKeyMessage().getString());
				}
			}
			while (PASTE.consumeClick()) {
				if (session != null) {
					session.paste(COPY.getTranslatedKeyMessage().getString());
				}
			}
			while (TURN.consumeClick()) {
				if (session != null) {
					session.turnCopy();
				}
			}
			while (MIRROR.consumeClick()) {
				if (session != null && client.player != null) {
					session.mirrorCopy(client.player.getDirection());
				}
			}
			while (CLEAR.consumeClick()) {
				if (session != null) {
					session.clearCopy();
				}
			}
			if (!lookSet) {
				// Litematica has loaded its config by the first tick.
				lookSet = true;
				LitematicaLook.setOnce();
			}
			if (session != null) {
				session.tick();
				if (heldClicks != null) {
					heldClicks.tick(client);
				}
			}
		});
		BuildingPlanner.LOGGER.info("Building Planner client loaded");
	}

	/**
	 * The server's {@code hello}: starts a session when the server speaks this client's protocol version, and otherwise
	 * leaves Building Planner off.
	 */
	static void hello(Payloads.Hello hello) {
		if (hello.protocolVersion() == Payloads.PROTOCOL_VERSION) {
			session = new ClientSession(hello.limits(), GLOW, MIX);
		} else {
			BuildingPlanner.LOGGER.warn("The server runs Building Planner protocol {}, this client {}",
				hello.protocolVersion(), Payloads.PROTOCOL_VERSION);
		}
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

	/**
	 * Whether Building Planner is off on a server (the player isn't in a singleplayer or LAN host's own world and has no
	 * session), so Effortless Building's packets that change real blocks must not be sent: its shapes edit only plans.
	 */
	public static boolean off() {
		return session == null &&!Minecraft.getInstance().isLocalServer();
	}

	/** Whether the player's mix is on, with a session. Hooks E5 and E6 also ask {@code EffortlessShapes.mixApplies}. */
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

	/**
	 * Hook E4: undoes the player's newest plan edit. Returns whether Effortless Building's own undo packet is held
	 * back: always while Building Planner is on or off on a server, never in a singleplayer world without it.
	 */
	public static boolean undo() {
		if (session == null) {
			return off();
		}
		session.undo();
		return true;
	}

	/** Hook E4: redoes the player's newest undone plan edit; returns whether the packet is held back, as for undo. */
	public static boolean redo() {
		if (session == null) {
			return off();
		}
		session.redo();
		return true;
	}
}
