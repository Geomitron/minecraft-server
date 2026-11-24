package dev.buildingplanner.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.ClientConfig;
import dev.buildingplanner.input.BlockTurn;
import dev.buildingplanner.input.BoxPurpose;
import dev.buildingplanner.input.CopyColour;
import dev.buildingplanner.input.CopyPlacement;
import dev.buildingplanner.input.DeleteMode;
import dev.buildingplanner.input.DisablePreview;
import dev.buildingplanner.input.DisableTurn;
import dev.buildingplanner.input.MouseMode;
import dev.buildingplanner.input.PlaceTurn;
import dev.buildingplanner.input.SingleRule;
import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.look.ControlsLine;
import dev.buildingplanner.look.GhostsShown;
import dev.buildingplanner.server.PlaceTurns;
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
import java.util.Optional;
import java.util.function.Predicate;
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
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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
		Payloads.DeleteList.TYPE, Payloads.OpenList.TYPE, Payloads.CurrentList.TYPE, Payloads.PlaceTurn.TYPE);

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
	/** The copy's own keys; null while Building Planner is off for want of Litematica or MaLiLib. */
	private static CopyKeys copyKeys;
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
		CopyKeys.register();
		copyKeys = new CopyKeys();
		glow = EditGlow.registered();
		BoxOutline.registered(BuildingPlannerClient::boxShown);
		CopyOutline.registered(BuildingPlannerClient::outlineShown);
		// The sweep runs before the marks' outline, which then draws the marks as they stand.
		DeleteAim.registered(() -> session);
		MarksOutline.registered(() -> session != null && session.aimOn() ? session.marks() : null);
		modeHud = new ModeHud(BuildingPlannerClient::mouseMode, BuildingPlannerClient::boxShown,
			BuildingPlannerClient::copyLine, BuildingPlannerClient::aimLines, BuildingPlannerClient::modelessLine,
			() -> effortless && EffortlessShapes.shapeUnderWay(),
			() -> CopyKeys.names(Minecraft.getInstance()).mode(),
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
			if (client.player != null) {
				HeldTurn.follow(client.player.getMainHandItem().getItem());
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
				session.tick(client.options.keyAttack.isDown());
				if (heldClicks != null) {
					// A shape started while ghost blocks are hidden shows them, so the player sees what it joins; a box
					// for Copy or the material list, which isn't a shape, leaves them hidden.
					if (EffortlessShapes.shapeUnderWay()) {
						session.showForDrawing();
					}
					heldClicks.tick(client);
				}
				if (singleActive() && session.single().rowFixed()) {
					EffortlessShapes.followRow(session.single());
				}
			}
			copyKeys.tick(client, session);
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
		// A new connection's server has no turn for this player, and without a session M6 sends none.
		PlaceTurns.sentLocally(BlockTurn.Held.NONE);
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
	 * or ends a box for Copy, or switches a box for Delete or the material list to Copy. Does nothing while Building
	 * Planner is off.
	 */
	static void copy() {
		boxButton(BoxPurpose.COPY, MouseMode.Action.COPY_BUTTON);
	}

	/**
	 * The radial menu's Delete button: in Delete by aim, ends it; otherwise as {@link #copy()}, for a box whose ghost
	 * blocks are erased, except that with no box drawn and Aim the last Delete mode the player used, it starts Delete by
	 * aim instead (see "Delete by aim").
	 */
	static void delete() {
		if (session == null) {
			return;
		}
		if (session.aimOn()) {
			session.modeAction(MouseMode.Action.DELETE_BUTTON);
			return;
		}
		if (!session.box().on() && ClientSession.deleteMode() == DeleteMode.AIM) {
			session.modeAction(MouseMode.Action.DELETE_BUTTON);
			if (effortless) {
				ClickRule.cancelShape();
			}
			session.startAim();
			modeStarted();
			return;
		}
		boxButton(BoxPurpose.DELETE, MouseMode.Action.DELETE_BUTTON);
	}

	/** The radial menu's "Material list" button: as {@link #copy()}, for a box whose blocks the material list counts. */
	static void materialList() {
		boxButton(BoxPurpose.MATERIAL_LIST, MouseMode.Action.MATERIAL_LIST_BUTTON);
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
	private static void boxButton(BoxPurpose purpose, MouseMode.Action button) {
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

	/** The copy at the aim's controls line, with its keys as the player has them bound, or null. */
	private static ControlsLine.Line copyLine() {
		ControlsLine.CopyState state = session == null ? null : session.copyState();
		return state == null ? null : ControlsLine.copy(state, CopyKeys.names(Minecraft.getInstance()));
	}

	/** Delete by aim's controls lines, with its keys as the player has them bound, or none while it's off. */
	private static List<ControlsLine.Line> aimLines() {
		ControlsLine.AimState state = session == null ? null : session.aimState();
		return state == null ? List.of() : ControlsLine.deleteAim(state, CopyKeys.names(Minecraft.getInstance()));
	}

	/**
	 * The box {@link CopyOutline} draws, in its colour: the copy at the aim's, with its row's boxes; or the one ghost
	 * block's while it's locked or its row is fixed and its controls are active, around the locked block or each block
	 * of the row; or null.
	 */
	static CopyOutline.Shown outlineShown() {
		if (session == null) {
			return null;
		}
		CopyColour.Shown colour = session.copyShown();
		if (colour != null) {
			return new CopyOutline.Shown(colour, session.clipboard().rowSpots());
		}
		SingleBlock block = shownSingle();
		if (block == null) {
			return null;
		}
		List<BlockPos> own = block.rowFixed() ? block.row() : List.of(block.pos());
		return new CopyOutline.Shown(EffortlessShapes.singleColour(block),
			own.stream().map(pos -> new CopyPlacement.Spot(pos, new Vec3i(1, 1, 1))).toList());
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
	static boolean drawingBoxFor(BoxPurpose purpose) {
		return session != null && session.mouseMode() == MouseMode.BOX && session.box().purpose() == purpose;
	}

	/** Whether Delete is on, a Delete box or Delete by aim, so the radial menu's Delete button shows selected. */
	static boolean deleteOn() {
		return session != null && session.deleteOn();
	}

	/** Building Planner's mode that takes over the mouse now, or null. */
	static MouseMode mouseMode() {
		return session == null ? null : session.mouseMode();
	}

	/** Whether a mouse mode is on (hooks E15, E22 and L11). */
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
	 * Hook M3: whether holding the attack button breaks nothing: while a mouse mode is on, while the one ghost block is
	 * locked or its row is fixed, and while the button stays down after the click that finished a box, erased at the
	 * locked block or dropped the block's row.
	 */
	public static boolean holdingAttackTaken() {
		return session != null && (attackHeld || session.mouseMode() != null || singleTakesBreak());
	}

	/**
	 * Whether the one ghost block's controls are active ({@link SingleRule#active}): a session, Effortless Building's
	 * Single mode, no mouse mode, and an item that places a block in the main hand.
	 */
	static boolean singleActive() {
		return effortless && SingleRule.active(session != null, EffortlessShapes.singleMode(), mouseModeOn(),
			ClickRule.holdsPlaceable());
	}

	/**
	 * The one ghost block while it's locked or its row is fixed and its controls are active (hooks E27, E28 and E30), or
	 * null.
	 */
	static SingleBlock shownSingle() {
		return singleActive() && (session.single().locked() || session.single().rowFixed()) ? session.single() : null;
	}

	/**
	 * Whether the one ghost block follows the aim with its controls active, in Single or in Zoop or Vertical before the
	 * first click ({@link SingleRule#drawnAtAim}), so hook E30 draws it there.
	 */
	static boolean singleAtAim() {
		return singleActive() && SingleRule.drawnAtAim(session.single().state());
	}

	/**
	 * Whether the turning controls are active (Left Ctrl with the wheel and the middle click, see "One ghost block":
	 * "Turning and flipping"): in the modes whose shapes take the turn ({@link #shapesTakeTurn}), or in Disable
	 * ({@link #disableTurning}).
	 */
	static boolean turningActive() {
		return shapesTakeTurn() || disableTurning();
	}

	/**
	 * Whether hooks E1 and E28 give the blocks placed the turn ({@link SingleRule#turning}): a session, any of Effortless
	 * Building's modes but Disable, no mouse mode, an item that places a block in the main hand, and no shape drawn with
	 * the break click under way.
	 */
	static boolean shapesTakeTurn() {
		return effortless && SingleRule.turning(session != null, EffortlessShapes.disableMode(), mouseModeOn(),
			ClickRule.holdsPlaceable(), EffortlessShapes.breakShapeUnderWay());
	}

	/**
	 * Whether Disable's turning controls are active ({@link DisableTurn#active}): a session, Disable, no mouse mode, and
	 * a block in hand that turns or flips; the turn then goes to the real block a right click places (hooks M6 and S2).
	 */
	private static boolean disableTurning() {
		return effortless && Minecraft.getInstance().player != null && DisableTurn.active(session != null,
			EffortlessShapes.disableMode(), mouseModeOn(), heldCan(BlockTurn::turns) || heldCan(BlockTurn::flips));
	}

	/**
	 * Hooks E28 to E30: whether Disable's preview shows ({@link DisablePreview#shows}): Disable's turning controls are
	 * active, the sprint key is held, and Freecam doesn't keep the right click from the world.
	 */
	static boolean disablePreviewOn() {
		return DisablePreview.shows(disableTurning(), Minecraft.getInstance().options.keySprint.isDown(),
			Aim.interactionBlocked());
	}

	/**
	 * Hook M6, before a right click on a block: sends {@code place_turn} when the turn the click takes
	 * ({@link DisableTurn#clickTakes}: the turn of the block in hand while the sprint key is held in Disable, and
	 * otherwise none) differs from the one sent last, and keeps it as the one this computer's guess of the block takes.
	 */
	public static void beforeUseItemOn() {
		Minecraft mc = Minecraft.getInstance();
		if (session == null || mc.player == null) {
			return;
		}
		Item inHand = mc.player.getMainHandItem().getItem();
		BlockTurn.Turn turn = HeldTurn.of(inHand);
		BlockTurn.Held now = DisableTurn.clickTakes(disableTurning(), mc.options.keySprint.isDown(),
			turn == null ? BlockTurn.Held.NONE : new BlockTurn.Held(inHand, turn));
		if (DisableTurn.sends(PlaceTurns.local(), now)) {
			ClientPlayNetworking.send(new Payloads.PlaceTurn(Optional.ofNullable(now.item()), PlaceTurn.properties(now)));
			PlaceTurns.sentLocally(now);
		}
	}

	/**
	 * Hooks M4, M5 and L11: whether the turning controls take the wheel or the middle click now: they're active, the
	 * sprint key is held, and the block in hand, or a block of the mix while it applies, turns or flips as asked.
	 */
	private static boolean turnTakes(Predicate<Block> can) {
		return turningActive() && Minecraft.getInstance().options.keySprint.isDown() && heldCan(can);
	}

	/**
	 * Whether the block in hand, or a block of the mix while it applies, turns or flips as asked; in Disable, the block
	 * Minecraft's right click would place counts too, so a torch aimed at a wall turns as the wall torch it places.
	 */
	private static boolean heldCan(Predicate<Block> can) {
		if (!(Minecraft.getInstance().player.getMainHandItem().getItem() instanceof BlockItem item)) {
			return false;
		}
		if (can.test(item.getBlock())
			|| EffortlessShapes.mixApplies() && MIX.parts().stream().anyMatch(part -> can.test(part.block()))) {
			return true;
		}
		Block placed = EffortlessShapes.disableMode() ? EffortlessShapes.placedInDisable() : null;
		return placed != null && placed != item.getBlock() && can.test(placed);
	}

	/**
	 * The line that shows with no mouse mode on, with the keys as the player has them bound: while the one ghost block's
	 * controls are active, Single's line ({@link ControlsLine#single}), red while the locked block or its row is; in
	 * Effortless Building's modes other than Single and Disable, while the turning controls are active and the block in
	 * hand turns or flips, the turning line ({@link ControlsLine#turning}); in Disable, while its turning controls are
	 * active, Disable's line ({@link ControlsLine#disable}), with whether the block a right click would place hangs on
	 * the block behind it and whether the turned block can stay; or null.
	 */
	private static ControlsLine.Line modelessLine() {
		boolean single = singleActive();
		boolean disable = disableTurning();
		if (!single && !disable && (!shapesTakeTurn() || EffortlessShapes.singleMode())) {
			return null;
		}
		Minecraft mc = Minecraft.getInstance();
		boolean sprint = mc.options.keySprint.isDown();
		ControlsLine.TurnState turn = new ControlsLine.TurnState(heldCan(BlockTurn::turns), heldCan(BlockTurn::flips),
			sprint, HeldTurn.of(mc.player.getMainHandItem().getItem()) != null);
		ControlsLine.Keys keys = CopyKeys.names(mc);
		if (single) {
			SingleBlock block = session.single();
			boolean boxed = block.locked() || block.rowFixed();
			return ControlsLine.single(new ControlsLine.SingleState(SingleBlock.mode(), block.locked(),
				block.rowFixed() ? block.row().size() : 0, boxed && EffortlessShapes.singleColour(block).red(), turn), keys);
		}
		String sprintKey = keys.sprint();
		if (!disable) {
			return ControlsLine.turning(turn, sprintKey);
		}
		EffortlessShapes.DisableShown shown = sprint ? EffortlessShapes.disableShown() : null;
		return ControlsLine.disable(new ControlsLine.DisableState(turn, EffortlessShapes.placedHangs(),
			shown == null || shown.goes()), sprintKey);
	}

	/** Hook L11: whether the middle click flips the block in hand, so Litematica picks no block with it. */
	public static boolean flipTakesMiddle() {
		return effortless && turnTakes(BlockTurn::flips);
	}

	/** Hooks E14 and M2: whether the place click is the one ghost block's ({@link SingleRule#takesPlace}). */
	public static boolean singleTakesPlace() {
		return singleActive() && SingleRule.takesPlace(session.single().state());
	}

	/** Hooks E14, M1 and M3: whether the break click is the one ghost block's ({@link SingleRule#takesBreak}). */
	public static boolean singleTakesBreak() {
		return singleActive() && SingleRule.takesBreak(session.single().state());
	}

	/**
	 * "Lock or unlock" while the one ghost block's controls are active: locks it where a right click would place it now,
	 * or with nothing aimed at does nothing; locked, unlocks it, and it follows the aim again.
	 */
	static void toggleSingleLock() {
		SingleBlock block = session.single();
		if (block.locked()) {
			block.unlock();
		} else {
			EffortlessShapes.lock(block);
		}
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
	 * Hook M1: the left click sets the box's next corner, pastes the copy at the aim, or in Delete by aim starts the hold
	 * that erases the marks; with the one ghost block's row fixed, it drops the row, and with the block locked, it erases
	 * every ghost block a right click would place; holding the button then does nothing more. Returns whether the click
	 * is taken from the game.
	 */
	public static boolean leftClick() {
		MouseMode mode = mouseMode();
		if (mode == null) {
			if (!singleTakesBreak()) {
				return false;
			}
			EffortlessShapes.leftClick(session.single());
			attackHeld = Minecraft.getInstance().options.keyAttack.isDown();
			return true;
		}
		switch (mode) {
			case BOX -> {
				session.boxClick();
				if (session.mouseMode() != MouseMode.BOX) {
					attackHeld = Minecraft.getInstance().options.keyAttack.isDown();
				}
			}
			case COPY_AT_AIM -> session.paste();
			// Delete by aim's left click attacks and breaks nothing; held a second, it erases the marks.
			case DELETE_AIM -> session.pressErase();
		}
		return true;
	}

	/**
	 * Hook M2: the right click puts the copy away, or during a zoop, after its first click, drops the row and leaves the
	 * copy at the aim; in Delete by aim it clears the marks, or with none ends the mode. With the one ghost block locked
	 * or in Zoop or Vertical, it places the block where it's locked, fixes the row's start, or places the row
	 * ({@link EffortlessShapes#rightClick}). Returns whether the game's use is skipped, which it is too while the button
	 * stays down after a mode started or ended or the block's click acted, so holding it does no more.
	 */
	public static boolean rightClick() {
		if (session == null) {
			return false;
		}
		if (session.mouseMode() != null && !useHeld) {
			if (!session.dropRow() && !session.clearMarks()) {
				session.modeAction(MouseMode.Action.RIGHT_CLICK);
			}
			useHeld = true;
			return true;
		}
		if (!useHeld && singleTakesPlace()) {
			EffortlessShapes.rightClick(session.single());
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
		if (mode == null && turnTakes(BlockTurn::flips)) {
			EffortlessShapes.flipHeld();
			return true;
		}
		if (mode == null || aimer == null) {
			return false;
		}
		if (mode == MouseMode.COPY_AT_AIM) {
			session.mirrorCopy(aimer.getDirection());
		}
		return true;
	}

	/**
	 * Hook M5: each notch of the scroll wheel gives the copy at the aim a quarter turn around its middle, clockwise for a
	 * notch up ({@code wheel} above 0) and counterclockwise for a notch down, as Satisfactory's wheel turns a hologram;
	 * returns whether the hotbar slot changes as without the add-on.
	 */
	public static boolean scroll(int wheel) {
		if (mouseMode() == null && turnTakes(BlockTurn::turns)) {
			for (int notch = 0; notch < Math.abs(wheel); notch++) {
				EffortlessShapes.turnHeld(wheel > 0);
			}
			return false;
		}
		if (mouseMode() != MouseMode.COPY_AT_AIM) {
			return true;
		}
		for (int notch = 0; notch < Math.abs(wheel); notch++) {
			session.turnCopy(wheel > 0);
		}
		return false;
	}

	/** Whether Building Planner is on for this server, so Effortless Building's hooks draw ghost blocks. */
	public static boolean active() {
		return session != null;
	}

	/** Whether the placement is one of the tiles that hand the layer to Litematica. */
	public static boolean isTile(SchematicPlacement placement) {
		return session != null && session.tiles().holds(placement);
	}

	/**
	 * Hooks L4 and L5: whether the placement is a tile or the copy or one of its row's, which Litematica never selects
	 * or lists.
	 */
	public static boolean isOurs(SchematicPlacement placement) {
		return session != null && (session.tiles().holds(placement) || session.clipboard().holds(placement));
	}

	/** Hook L1: whether Litematica's block container is a tile's. */
	public static boolean isTileContainer(LitematicaBlockStateContainer container) {
		return session != null && session.tiles().holds(container);
	}

	/** Hooks L5 and L6: the placements without the tiles, the copy and its row. */
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
