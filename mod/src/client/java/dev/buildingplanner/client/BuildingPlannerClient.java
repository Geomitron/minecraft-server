package dev.buildingplanner.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.ClientConfig;
import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.look.GhostsShown;
import dev.buildingplanner.sync.EditKind;
import dev.buildingplanner.sync.Payloads;
import dev.buildingplanner.sync.PlayerMessage;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The client side: a session for each connection to a server with Building Planner, started by the server's
 * {@code hello}, and the hooks' way into it. On any other server Building Planner stays off, silently.
 */
public final class BuildingPlannerClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY =
		KeyMapping.Category.register(Identifier.fromNamespaceAndPath(BuildingPlanner.MOD_ID, "main"));
	private static final KeyMapping BLOCK_MIX =
		new KeyMapping("key.buildingplanner.block_mix", InputConstants.KEY_B, CATEGORY);
	static final KeyMapping SHOW_HIDE =
		new KeyMapping("key.buildingplanner.show_hide", InputConstants.KEY_N, CATEGORY);
	// Copy and paste. None of these keys is a default key, or in a default key combination, of Minecraft, Litematica,
	// MaLiLib, Effortless Building or Freecam.
	static final KeyMapping COPY = new KeyMapping("key.buildingplanner.copy", InputConstants.KEY_J, CATEGORY);
	static final KeyMapping PASTE = new KeyMapping("key.buildingplanner.paste", InputConstants.KEY_K, CATEGORY);
	private static final KeyMapping TURN = new KeyMapping("key.buildingplanner.turn", InputConstants.KEY_U, CATEGORY);
	private static final KeyMapping MIRROR = new KeyMapping("key.buildingplanner.mirror", InputConstants.KEY_H, CATEGORY);
	private static final KeyMapping CLEAR =
		new KeyMapping("key.buildingplanner.clear", InputConstants.KEY_DELETE, CATEGORY);
	/**
	 * The edit glow, drawn through MaLiLib; null while Building Planner is off for want of Litematica or MaLiLib, so this
	 * class never loads their classes then.
	 */
	private static EditGlow glow;
	/** The player's block mix, kept while the game runs, across servers. */
	private static final BlockMix MIX = new BlockMix();

	/** The payloads a session sends; a server that can't receive them all doesn't run Building Planner. */
	private static final List<CustomPacketPayload.Type<?>> SERVERBOUND = List.of(Payloads.Watch.TYPE,
		Payloads.Unwatch.TYPE, Payloads.Edit.TYPE, Payloads.Undo.TYPE, Payloads.Redo.TYPE);

	/**
	 * Null while not connected to a server with Building Planner: on any other server, Building Planner is off, so
	 * Effortless Building's shapes and undo send nothing.
	 */
	private static ClientSession session;
	/** The chunks of the game's level loaded now, by {@link Layer#key}, which a session watches from its start. */
	private static final LongSet LOADED = new LongOpenHashSet();

	/** Building Planner's settings on this computer, read when the game starts. */
	private static ClientConfig config;
	/** Hold to repeat in Effortless Building's Single mode; null without Effortless Building, whose classes it uses. */
	private static HeldClicks heldClicks;
	/** Whether Litematica's ghost block look has been set this run (see {@link LitematicaLook}). */
	private static boolean lookSet;
	/** The mix panel; null without Effortless Building, whose mode the mix rule reads. */
	private static MixPanel mixPanel;

	@Override
	public void onInitializeClient() {
		FabricLoader loader = FabricLoader.getInstance();
		config = ClientConfig.load(loader.getConfigDir().resolve("buildingplanner-client.properties"));
		// The mixin plugin skips the Litematica hooks without Litematica or MaLiLib; with no session, Effortless
		// Building's shapes send nothing on a server, as on a server without the add-on.
		if (!loader.isModLoaded("litematica") || !loader.isModLoaded("malilib")) {
			BuildingPlanner.LOGGER.info("Litematica or MaLiLib is missing, so Building Planner is off");
			return;
		}
		if (loader.isModLoaded("effortlessbuilding")) {
			heldClicks = new HeldClicks(config);
			mixPanel = new MixPanel(MIX, EffortlessShapes::mixApplies);
			HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(BuildingPlanner.MOD_ID, "mix_panel"), mixPanel);
		}
		KeyMappingHelper.registerKeyMapping(BLOCK_MIX);
		KeyMappingHelper.registerKeyMapping(SHOW_HIDE);
		List.of(COPY, PASTE, TURN, MIRROR, CLEAR).forEach(KeyMappingHelper::registerKeyMapping);
		glow = EditGlow.registered();
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Hello.TYPE, (hello, context) -> hello(hello));
		// A singleplayer or LAN host's own world runs no server side.
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (!client.isLocalServer() && !SERVERBOUND.stream().allMatch(ClientPlayNetworking::canSend)) {
				BuildingPlanner.LOGGER.info("The server doesn't run Building Planner, so it's off");
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Columns.TYPE, (payload, context) -> {
			if (session != null) {
				session.receive(payload);
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Applied.TYPE, (payload, context) -> {
			if (session != null) {
				session.receive(payload);
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Rejected.TYPE, (payload, context) -> {
			if (session != null) {
				ClientSession.show(payload.reason());
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Notice.TYPE, (payload, context) -> {
			if (session != null) {
				ClientSession.show(payload.message());
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> endSession());
		// Litematica draws only in loaded chunks, so the game watches the columns of the chunks it has loaded. Events
		// from a level the game has left are ignored.
		ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
			ChunkPos pos = chunk.getPos();
			if (level == Minecraft.getInstance().level && LOADED.add(Layer.key(pos.x(), pos.z())) && session != null) {
				session.chunkLoaded(pos.x(), pos.z());
			}
		});
		ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
			ChunkPos pos = chunk.getPos();
			if (level == Minecraft.getInstance().level && LOADED.remove(Layer.key(pos.x(), pos.z())) && session != null) {
				session.chunkUnloaded(pos.x(), pos.z());
			}
		});
		ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> {
			LOADED.clear();
			if (session != null) {
				session.levelChanged(dimension(level));
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (BLOCK_MIX.consumeClick()) {
				if (session != null) {
					openMix();
				}
			}
			while (SHOW_HIDE.consumeClick()) {
				if (session != null) {
					session.toggleGhosts();
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
				LitematicaLook.setOnce(config);
			}
			if (session != null) {
				session.tick();
				if (heldClicks != null) {
					// A shape started while ghost blocks are hidden shows them, so the player sees what it joins.
					if (EffortlessShapes.shapeUnderWay()) {
						session.showForDrawing();
					}
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
		endSession();
		if (hello.protocolVersion() == Payloads.PROTOCOL_VERSION) {
			session = new ClientSession(hello.maxEditBlocks(), glow, dimension(Minecraft.getInstance().level), LOADED,
				config, () -> SHOW_HIDE.getTranslatedKeyMessage().getString());
		} else {
			BuildingPlanner.LOGGER.warn("The server runs Building Planner protocol {}, this client {}",
				hello.protocolVersion(), Payloads.PROTOCOL_VERSION);
		}
	}

	/** Building Planner's settings on this computer, such as the speed sliders' in the radial menu (hook E11). */
	public static ClientConfig config() {
		return config;
	}

	private static void endSession() {
		if (session != null) {
			session.close();
			session = null;
		}
	}

	private static Identifier dimension(ClientLevel level) {
		return level == null ? null : level.dimension().identifier();
	}

	/** The mix key: opens the picker on the player's mix, to choose it, change it or turn it off. */
	private static void openMix() {
		BlockMixScreen.open(MIX,
			() -> ClientSession.show(PlayerMessage.mixOn(BLOCK_MIX.getTranslatedKeyMessage().getString())),
			() -> ClientSession.show(PlayerMessage.mixOff()));
	}

	/** Whether ghost blocks show for this player, as the switch is set, with or without a session. */
	public static boolean ghostsShown() {
		return session == null ? GhostsShown.get(config) : session.ghostsShown();
	}

	/** The radial menu's show or hide button, as the key: does nothing while Building Planner is off. */
	public static void toggleGhosts() {
		if (session != null) {
			session.toggleGhosts();
		}
	}

	/** Whether Building Planner is on for this server, so Effortless Building's hooks draw ghost blocks. */
	public static boolean active() {
		return session != null;
	}

	/** Hooks L4 and L6: whether the placement is one of the tiles that hand the layer to Litematica. */
	public static boolean isTile(SchematicPlacement placement) {
		return session != null && session.tiles().holds(placement);
	}

	/** Hook L1: whether Litematica's block container is a tile's. */
	public static boolean isTileContainer(LitematicaBlockStateContainer container) {
		return session != null && session.tiles().holds(container);
	}

	/** Hooks L5 and L6: the placements without the tiles. */
	public static List<SchematicPlacement> withoutTiles(Collection<SchematicPlacement> placements) {
		if (session == null) {
			return placements instanceof List<SchematicPlacement> list ? list : List.copyOf(placements);
		}
		Tiles tiles = session.tiles();
		return placements.stream().filter(placement -> !tiles.holds(placement)).toList();
	}

	/**
	 * Whether Building Planner is off on a server (the player isn't in a singleplayer or LAN host's own world and has no
	 * session), so Effortless Building's packets that change real blocks must not be sent: its shapes edit only ghost
	 * blocks.
	 */
	public static boolean off() {
		return session == null && !Minecraft.getInstance().isLocalServer();
	}

	/**
	 * Whether Building Planner decides what Effortless Building's shapes do: on a server, with a session (ghost blocks)
	 * or without one (nothing). A singleplayer world or a LAN host's own world without a session leaves Effortless
	 * Building as it is.
	 */
	public static boolean ownsShapes() {
		return session != null || !Minecraft.getInstance().isLocalServer();
	}

	/** Whether the player's mix is on, with a session. Hooks E5 and E6 also ask {@code EffortlessShapes.mixApplies}. */
	public static boolean mixOn() {
		return session != null && MIX.isOn();
	}

	/** Hook E5: the mix's block at a world position. Only call while {@link #mixOn()}. */
	public static Block mixBlockAt(BlockPos pos) {
		return MIX.blockAt(pos);
	}

	/** Hooks E1 and E2: a shape the player drew, as world positions and states in the order drawn (air to erase). */
	public static void captureShape(EditKind kind, Map<BlockPos, BlockState> blocks) {
		if (session != null) {
			session.sendShape(kind, blocks);
		}
	}

	/**
	 * Hook E4: undoes the player's newest edit to the layer. Returns whether Effortless Building's own undo packet is
	 * held back: always while Building Planner is on or off on a server, never in a singleplayer world without it.
	 */
	public static boolean undo() {
		if (session == null) {
			return off();
		}
		session.undo();
		return true;
	}

	/** Hook E4: redoes the player's newest undone edit; returns whether the packet is held back, as for undo. */
	public static boolean redo() {
		if (session == null) {
			return off();
		}
		session.redo();
		return true;
	}
}
