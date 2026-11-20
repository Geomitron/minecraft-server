package dev.buildingplanner.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.ClientConfig;
import dev.buildingplanner.input.MouseMode;
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
import net.minecraft.world.entity.Entity;
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
	static final KeyMapping BLOCK_MIX =
		new KeyMapping("key.buildingplanner.block_mix", InputConstants.KEY_B, CATEGORY);
	static final KeyMapping SHOW_HIDE =
		new KeyMapping("key.buildingplanner.show_hide", InputConstants.KEY_N, CATEGORY);
	/**
	 * The edit glow, drawn through MaLiLib; null while Building Planner is off for want of Litematica or MaLiLib, so this
	 * class never loads their classes then.
	 */
	private static EditGlow glow;
	/** The player's block mix, kept while the game runs, across servers. */
	private static final BlockMix MIX = new BlockMix();

	/** The payloads a session sends; a server that can't receive them all doesn't run Building Planner. */
	private static final List<CustomPacketPayload.Type<?>> SERVERBOUND = List.of(Payloads.Watch.TYPE,
		Payloads.Unwatch.TYPE, Payloads.Edit.TYPE, Payloads.Undo.TYPE, Payloads.Redo.TYPE, Payloads.SaveList.TYPE,
		Payloads.DeleteList.TYPE, Payloads.OpenList.TYPE, Payloads.CurrentList.TYPE);

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
	/** The line of a mouse mode's controls; null while Building Planner is off for want of Litematica or MaLiLib. */
	private static ModeHud modeHud;
	/** Whether Effortless Building is loaded, whose shape under way a box drops. */
	private static boolean effortless;
	/**
	 * Whether the use button has stayed down since a mouse mode started or the right click ended one, so that holding
	 * it, which the game repeats every 4 ticks, neither ends the mode it started in nor acts once the mode has ended
	 * (hooks M2 and E14). It clears at the first tick the button is up.
	 */
	private static boolean useHeld;
	/**
	 * Whether the attack button has stayed down since the left click that finished a box, so that holding it breaks
	 * nothing once the mode has ended (hooks M3 and E14). It clears at the first tick the button is up.
	 */
	private static boolean attackHeld;

	/** The heading of Building Planner's keys in Controls, which hook E23 gives Effortless Building's keys too. */
	public static KeyMapping.Category category() {
		return CATEGORY;
	}

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
			effortless = true;
			heldClicks = new HeldClicks(config);
			mixPanel = new MixPanel(MIX, EffortlessShapes::mixApplies, BLOCK_MIX::getTranslatedKeyMessage);
			HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(BuildingPlanner.MOD_ID, "mix_panel"), mixPanel);
		}
		KeyMappingHelper.registerKeyMapping(BLOCK_MIX);
		KeyMappingHelper.registerKeyMapping(SHOW_HIDE);
		glow = EditGlow.registered();
		BoxOutline.registered(BuildingPlannerClient::boxShown);
		modeHud = new ModeHud(BuildingPlannerClient::mouseMode, BuildingPlannerClient::boxShown,
			() -> session == null ? List.of() : session.boxList());
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(BuildingPlanner.MOD_ID, "mode_controls"),
			modeHud);
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(BuildingPlanner.MOD_ID, "material_panel"),
			new MaterialPanel());
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
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Lists.TYPE, (payload, context) -> {
			if (session != null) {
				session.lists().receive(payload);
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(Payloads.OpenedList.TYPE, (payload, context) -> {
			if (session != null) {
				session.lists().receive(payload);
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Current.TYPE, (payload, context) -> {
			if (session != null) {
				session.lists().receive(payload);
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
				blockMix();
			}
			while (SHOW_HIDE.consumeClick()) {
				toggleGhosts();
			}
			if (!client.options.keyUse.isDown()) {
				useHeld = false;
			}
			if (!client.options.keyAttack.isDown()) {
				attackHeld = false;
			}
			if (!lookSet) {
				// Litematica has loaded its config by the first tick.
				lookSet = true;
				LitematicaLook.setOnce(config);
			}
			if (session != null) {
				session.tick();
				if (heldClicks != null) {
					// A shape started while ghost blocks are hidden shows them, so the player sees what it joins; a box
					// for Copy or the material list, which isn't a shape, leaves them hidden.
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

	/** The session, or null while Building Planner is off. */
	static ClientSession session() {
		return session;
	}

	private static Identifier dimension(ClientLevel level) {
		return level == null ? null : level.dimension().identifier();
	}

	/**
	 * The mix key and the radial menu's "Block mix" button: put the copy away and open the picker on the player's mix, to
	 * choose it, change it or turn it off. Do nothing while Building Planner is off.
	 */
	static void blockMix() {
		if (session == null) {
			return;
		}
		session.modeAction(MouseMode.Action.BLOCK_MIX);
		BlockMixScreen.open(MIX,
			() -> ClientSession.show(PlayerMessage.mixOn(BLOCK_MIX.getTranslatedKeyMessage().getString())),
			() -> ClientSession.show(PlayerMessage.mixOff()));
	}

	/** Whether ghost blocks show for this player, as the switch is set, with or without a session. */
	public static boolean ghostsShown() {
		return session == null ? GhostsShown.get(config) : session.ghostsShown();
	}

	/** The show or hide key and the radial menu's button: do nothing while Building Planner is off. */
	public static void toggleGhosts() {
		if (session != null) {
			session.toggleGhosts();
		}
	}

	/**
	 * The radial menu's Copy button: puts the copy away and starts a box around the ghost blocks and real blocks to copy,
	 * or ends a box for Copy, or switches a box for the material list to Copy. Does nothing while Building Planner is off.
	 */
	static void copy() {
		boxButton(BoxDraw.Purpose.COPY, MouseMode.Action.COPY_BUTTON);
	}

	/** The radial menu's "Material list" button: as {@link #copy()}, for a box whose blocks the material list counts. */
	static void materialList() {
		boxButton(BoxDraw.Purpose.MATERIAL_LIST, MouseMode.Action.MATERIAL_LIST_BUTTON);
	}

	/**
	 * The radial menu's "Material lists" button: the current material list's screen, or "Saved lists" when there's no
	 * current list. Opening a screen keeps the mouse mode.
	 */
	static void materialLists() {
		if (session != null) {
			session.lists().openCurrent();
		}
	}

	/** After the button's action on the mouse mode, starts, ends or switches a box, dropping a shape under way. */
	private static void boxButton(BoxDraw.Purpose purpose, MouseMode.Action button) {
		if (session == null) {
			return;
		}
		session.modeAction(button);
		if (session.boxButton(purpose)) {
			if (effortless) {
				ClickRule.cancelShape();
			}
			modeStarted();
		}
	}

	/** The box being drawn now, as it shows, or null. */
	private static BoxDraw.Shown boxShown() {
		return session == null ? null : session.boxShown();
	}

	/**
	 * The radial menu's Paste button: brings the most recent copy back to the aim, ending a box and dropping a shape
	 * under way, whose clicks the copy now takes; with no copy, says so. Only the radial menu, which is Effortless
	 * Building's, calls it.
	 */
	static void paste() {
		if (session == null) {
			return;
		}
		session.modeAction(MouseMode.Action.PASTE_BUTTON);
		if (session.clipboard().exists()) {
			ClickRule.cancelShape();
		}
		if (session.bringBackCopy()) {
			modeStarted();
		}
	}

	/** Whether a box is drawn for that purpose, whose radial menu button then shows selected. */
	static boolean drawingBoxFor(BoxDraw.Purpose purpose) {
		return session != null && session.mouseMode() == MouseMode.BOX && session.box().purpose() == purpose;
	}

	/** Building Planner's mode that takes over the mouse now, or null. */
	static MouseMode mouseMode() {
		return session == null ? null : session.mouseMode();
	}

	/** Whether a mouse mode is on (hooks E15 and E22). */
	public static boolean mouseModeOn() {
		return mouseMode() != null;
	}

	/**
	 * Hook E14: whether Effortless Building's clicks stay out: while a mouse mode is on, while the use button stays down
	 * after one started or ended, and while the attack button stays down after the click that finished a box.
	 */
	public static boolean mouseTaken() {
		return session != null && (useHeld || attackHeld || session.mouseMode() != null);
	}

	/**
	 * Hook M3: whether holding the attack button breaks nothing: while a mouse mode is on, and while the button stays
	 * down after the click that finished a box.
	 */
	public static boolean holdingAttackTaken() {
		return session != null && (attackHeld || session.mouseMode() != null);
	}

	/** A mouse mode started, perhaps within a click of the use button, whose hold mustn't end it. */
	private static void modeStarted() {
		useHeld = Minecraft.getInstance().options.keyUse.isDown();
	}

	/** Hook E21: the player chose a mode in Effortless Building's radial menu. */
	public static void buildModeChosen() {
		if (session != null) {
			session.modeAction(MouseMode.Action.CHOOSE_BUILD_MODE);
		}
	}

	/**
	 * Hook M1: the left click sets the box's next corner, or pastes the copy at the aim; returns whether the click is
	 * taken from the game.
	 */
	public static boolean leftClick() {
		MouseMode mode = mouseMode();
		if (mode == null) {
			return false;
		}
		switch (mode) {
			case BOX -> {
				session.boxClick();
				if (session.mouseMode() != MouseMode.BOX) {
					attackHeld = Minecraft.getInstance().options.keyAttack.isDown();
				}
			}
			case COPY_AT_AIM -> session.paste();
		}
		return true;
	}

	/**
	 * Hook M2: the right click puts the copy away; returns whether the game's use is skipped, which it is too while the
	 * button stays down after a mode started or ended.
	 */
	public static boolean rightClick() {
		if (session == null) {
			return false;
		}
		if (session.mouseMode() != null && !useHeld) {
			session.modeAction(MouseMode.Action.RIGHT_CLICK);
			useHeld = true;
			return true;
		}
		return useHeld;
	}

	/**
	 * Hook M4: the middle click mirrors the copy at the aim left to right for the way the player faces, or the camera
	 * while Freecam's is on ({@link Aim}), and does nothing while a box is drawn; returns whether the game's pick block
	 * is skipped.
	 */
	public static boolean middleClick() {
		MouseMode mode = mouseMode();
		Entity aimer = Aim.entity();
		if (mode == null || aimer == null) {
			return false;
		}
		if (mode == MouseMode.COPY_AT_AIM) {
			session.mirrorCopy(aimer.getDirection());
		}
		return true;
	}

	/**
	 * Hook M5: each notch of the scroll wheel gives the copy at the aim a quarter turn, clockwise for a notch down
	 * ({@code wheel} below 0) and counterclockwise for a notch up; returns whether the hotbar slot changes as without
	 * the add-on.
	 */
	public static boolean scroll(int wheel) {
		if (mouseMode() != MouseMode.COPY_AT_AIM) {
			return true;
		}
		for (int notch = 0; notch < Math.abs(wheel); notch++) {
			session.turnCopy(wheel < 0);
		}
		return false;
	}

	/** Whether Building Planner is on for this server, so Effortless Building's hooks draw ghost blocks. */
	public static boolean active() {
		return session != null;
	}

	/** Hook L6: whether the placement is one of the tiles that hand the layer to Litematica. */
	public static boolean isTile(SchematicPlacement placement) {
		return session != null && session.tiles().holds(placement);
	}

	/** Hooks L4 and L5: whether the placement is a tile or the copy, which Litematica never selects or lists. */
	public static boolean isOurs(SchematicPlacement placement) {
		return session != null && (session.tiles().holds(placement) || session.clipboard().holds(placement));
	}

	/** Hook L1: whether Litematica's block container is a tile's. */
	public static boolean isTileContainer(LitematicaBlockStateContainer container) {
		return session != null && session.tiles().holds(container);
	}

	/** Hook L6: the placements without the tiles. */
	public static List<SchematicPlacement> withoutTiles(Collection<SchematicPlacement> placements) {
		if (session == null) {
			return placements instanceof List<SchematicPlacement> list ? list : List.copyOf(placements);
		}
		Tiles tiles = session.tiles();
		return placements.stream().filter(placement -> !tiles.holds(placement)).toList();
	}

	/** Hook L5: the placements without the tiles and the copy. */
	public static List<SchematicPlacement> withoutOurs(Collection<SchematicPlacement> placements) {
		if (session == null) {
			return placements instanceof List<SchematicPlacement> list ? list : List.copyOf(placements);
		}
		return placements.stream().filter(placement -> !isOurs(placement)).toList();
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
