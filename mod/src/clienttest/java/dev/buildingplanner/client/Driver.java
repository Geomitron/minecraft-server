package dev.buildingplanner.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.buildingplanner.input.BlockTurn;
import dev.buildingplanner.input.CopyColour;
import dev.buildingplanner.input.CopyNudge;
import dev.buildingplanner.input.CopyPlacement;
import dev.buildingplanner.input.RepeatSpeed;
import dev.buildingplanner.look.ControlsLine;
import dev.buildingplanner.look.PanelRoom;
import dev.buildingplanner.sync.EditKind;
import dev.buildingplanner.sync.LayerChange;
import dev.buildingplanner.sync.Payloads;
import dev.buildingplanner.sync.SyncLimits;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.gui.GuiSchematicPlacementsList;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.render.infohud.StatusInfoRenderer;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.SchematicUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Drives a dev client for the in-game checks in {@code clienttest/}. It runs the commands in {@code driver-in.txt} in
 * the game folder, one at a time as lines are added, and appends each with its result to {@code driver-out.txt}.
 * Without that file it does nothing. It's a mod of its own, on the dev clients' classpath only.
 */
public final class Driver implements ClientModInitializer {
	private static final int AWAIT_TICKS = 1200;
	/**
	 * The most client ticks a {@code quickclick} lasts: the most a click can last and still act once, since a held
	 * button first repeats on its fifth tick down ({@code HoldRepeat.FIRST_WAIT}).
	 */
	private static final int QUICK_CLICK_TICKS = 3;

	private int done;
	private int wait;
	private String awaiting;
	private Supplier<String> condition;
	private int awaitedTicks;
	/** The button {@code quickclick} armed, and the client ticks it has been down since. */
	private KeyMapping quickClick;
	private int quickClickTicks;
	/** The button to let go at the start of the next client tick. */
	private KeyMapping release;
	/** The key {@code keyticks} counts, and the client ticks it has been down, with no screen open, since. */
	private KeyMapping countedKey;
	private int countedKeyTicks;
	/** The real block {@code watchblock} reads at the end of every client tick, and the states it read, in order. */
	private BlockPos watchedPos;
	private final List<String> watchedStates = new ArrayList<>();
	/**
	 * The client ticks of this game run with the player in a world, and those of them at whose end Litematica's status
	 * panel was to draw ({@code StatusInfoRenderer.shouldRenderStatusInfoHud()}).
	 */
	private static int inGameTicks;
	private static int statusPanelTicks;

	@Override
	public void onInitializeClient() {
		PageProbe.register();
		ClientTickEvents.START_CLIENT_TICK.register(mc -> {
			SentPackets.startTick();
			if (release != null) {
				release.setDown(false);
				release = null;
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
	}

	private void tick(Minecraft mc) {
		SentPackets.buttons(mc.options.keyUse.isDown() && mc.gui.screen() == null,
			mc.options.keyAttack.isDown() && mc.gui.screen() == null);
		if (countedKey != null && countedKey.isDown() && mc.gui.screen() == null) {
			countedKeyTicks++;
		}
		if (watchedPos != null && mc.level != null) {
			watchedStates.add(mc.level.getBlockState(watchedPos).toString());
		}
		if (mc.level != null && mc.player != null) {
			inGameTicks++;
			if (StatusInfoRenderer.getInstance().shouldRenderStatusInfoHud()) {
				statusPanelTicks++;
			}
		}
		if (quickClick != null) {
			if (quickClick.isDown() && mc.gui.screen() == null) {
				if (++quickClickTicks == QUICK_CLICK_TICKS) {
					release = quickClick;
					quickClick = null;
				}
			} else if (quickClickTicks > 0) {
				// The real button came up first.
				quickClick = null;
			}
		}
		if (wait > 0) {
			wait--;
			return;
		}
		if (condition != null) {
			String result = evaluate(condition);
			if (result == null && ++awaitedTicks < AWAIT_TICKS) {
				return;
			}
			write(mc, awaiting, result == null ? "TIMEOUT " + evaluate(() -> describe(awaiting)) : result);
			condition = null;
		}
		Path in = mc.gameDirectory.toPath().resolve("driver-in.txt");
		if (!Files.exists(in)) {
			return;
		}
		List<String> lines;
		try {
			lines = Files.readAllLines(in, StandardCharsets.UTF_8);
		} catch (Exception e) {
			return;
		}
		while (done < lines.size() && wait == 0 && condition == null) {
			String line = lines.get(done++).strip();
			if (line.isEmpty()) {
				continue;
			}
			try {
				String result = run(mc, line);
				if (condition == null) {
					write(mc, line, result);
				}
			} catch (Throwable t) {
				write(mc, line, "ERROR " + t);
			}
		}
	}

	private static String evaluate(Supplier<String> condition) {
		try {
			return condition.get();
		} catch (Throwable t) {
			return null;
		}
	}

	private static void write(Minecraft mc, String line, String result) {
		try {
			Files.writeString(mc.gameDirectory.toPath().resolve("driver-out.txt"), line + " => " + result + "\n",
				StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private String run(Minecraft mc, String line) throws Exception {
		String[] a = line.split(" ");
		switch (a[0]) {
			case "wait" -> {
				wait = Integer.parseInt(a[1]);
				return "ok";
			}
			case "await" -> {
				// Waits, up to a minute, until the state the rest of the line names holds.
				awaiting = line;
				awaitedTicks = 0;
				condition = () -> {
					try {
						return holds(line.substring(6)) ? "ok" : null;
					} catch (Exception e) {
						return null;
					}
				};
				return null;
			}
			case "mark" -> {
				return "ok";
			}
			case "cmd" -> {
				mc.player.connection.sendCommand(line.substring(4));
				return "ok";
			}
			case "selmode" -> {
				return DataManager.getSelectionManager().getSelectionMode().name();
			}
			case "fill" -> {
				Method m = SchematicUtils.class.getDeclaredMethod("setSchematicBlockStates", BlockPos.class,
					BlockPos.class, BlockState.class);
				m.setAccessible(true);
				return String.valueOf(m.invoke(null, pos(a, 1), pos(a, 4), state(a[7])));
			}
			case "place" -> {
				Method m = SchematicUtils.class.getDeclaredMethod("setTargetedSchematicBlockState", BlockPos.class,
					BlockState.class);
				m.setAccessible(true);
				return String.valueOf(m.invoke(null, pos(a, 1), state(a[4])));
			}
			case "get" -> {
				return blockId(pos(a, 1));
			}
			case "state" -> {
				// state x y z: the full block state Litematica's schematic world shows there.
				return SchematicWorldHandler.getSchematicWorld().getBlockState(pos(a, 1)).toString();
			}
			case "copy" -> {
				return copy();
			}
			case "copymode" -> {
				// copymode: the copy's mode, OFF, LINE, WALL or FLOOR.
				return ClientSession.copyMode().name();
			}
			case "copycolour" -> {
				// copycolour: the colour Building Planner draws the box of the copy at the aim, or around the build modes'
				// locked positions, in: BLUE, YELLOW or RED with the server's reason (RED BUILD_HEIGHT, say), or
				// none.
				return copyColour();
			}
			case "copyset" -> {
				// copyset: the copy's set (see set()).
				return set();
			}
			case "pastehere" -> {
				// pastehere x1 y1 z1 x2 y2 z2: copies the box, as Copy does, and pastes the copy at once where it was
				// copied from, as a right click with the copy there would; the box's lowest corner must hold a block.
				// Gives the copy as it was pasted, since after the paste it follows the aim again.
				session().copy(pos(a, 1), pos(a, 4));
				clipboard().moveTo(pos(a, 1));
				String pasted = copy();
				session().paste();
				return pasted;
			}
			case "lookat" -> {
				// lookat x y z: turns the player to look at that point, which may be fractional, as a real mouse move
				// would; the copy then follows the aim.
				mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(Double.parseDouble(a[1]),
					Double.parseDouble(a[2]), Double.parseDouble(a[3])));
				return "ok";
			}
			case "fly" -> {
				// fly <true|false>: the player flies, as a double jump in creative starts, so a teleport leaves them there.
				mc.player.getAbilities().flying = Boolean.parseBoolean(a[1]);
				mc.player.onUpdateAbilities();
				return "ok";
			}
			case "flying" -> {
				// flying: whether the player flies.
				return String.valueOf(mc.player.getAbilities().flying);
			}
			case "drawbox" -> {
				// drawbox: the box for Copy or the material list as it shows: what it's for, how many corners are set,
				// its lowest and highest corners for the aim now and its colour, with "unaimed" when the aim gives no
				// next corner, or none; then Effortless Building's mode.
				return session().box().describe() + " " + EffortlessDriver.mode();
			}
			case "presses" -> {
				// presses: how many key and mouse button presses the game has handled, for "await pressedafter".
				return String.valueOf(SentPackets.presses());
			}
			case "matlist" -> {
				// matlist: Litematica's material list as the title of the open screen when it's the list, or "closed",
				// then its counts ("total 10 missing 6 wrong 0") and each entry's item, total and missing.
				Screen screen = mc.gui.screen();
				MaterialListBase list = DataManager.getMaterialList();
				StringBuilder out = new StringBuilder(screen instanceof GuiMaterialList open
					? "open " + open.getMaterialList().getTitle() : "closed");
				if (list != null) {
					out.append(": total ").append(list.getCountTotal()).append(" missing ")
						.append(list.getCountMissing()).append(" wrong ").append(list.getCountMismatched());
					for (MaterialListEntry entry : list.getMaterialsAll()) {
						out.append(" | ").append(BuiltInRegistries.ITEM.getKey(entry.getStack().getItem())).append(' ')
							.append(entry.getCountTotal()).append('/').append(entry.getCountMissing());
					}
				}
				return out.toString();
			}
			case "currentlist" -> {
				// currentlist: the current material list when it's Building Planner's, as its box, the screen's
				// settings and its rows (item total/missing/wrong), or none.
				return ListProbe.current();
			}
			case "listfile" -> {
				// listfile <path>: "Last counted" in the lists file at that path from the game folder, as currentlist
				// reads a list.
				return ListProbe.lastCounted(mc.gameDirectory.toPath().resolve(a[1]));
			}
			case "malilibbuttons" -> {
				// malilibbuttons: each MaLiLib button on the open screen (Litematica's material list's, say) as its label and
				// window box (x y width height), separated by " | ".
				return ListScreenProbe.malilibButtons(mc);
			}
			case "malilibbutton" -> {
				// malilibbutton <text>: the window position of the middle of the MaLiLib button whose label starts with
				// <text>, or none.
				return ListScreenProbe.malilibButton(mc, line.substring(a[0].length() + 1));
			}
			case "infoicon" -> {
				// infoicon: the window position of the middle of the (i) icon on the open MaLiLib screen, or none.
				return ListScreenProbe.infoIcon(mc);
			}
			case "widgets" -> {
				// widgets: each shown widget on the open screen as its text (a text field's value as field=<value>),
				// "(greyed out)" when it isn't active, and its window box, separated by " | ".
				return ListScreenProbe.widgets(mc);
			}
			case "savedlists" -> {
				// savedlists: the rows the open "Saved lists" screen shows, each as its name / its second line.
				return ListScreenProbe.savedLists(mc);
			}
			case "listindex" -> {
				// listindex: the index of the player's lists the game keeps, with each list's size.
				return ListScreenProbe.index();
			}
			case "openlist" -> {
				// openlist: opens the current material list's screen.
				GuiBase.openGui(new GuiMaterialList(DataManager.getMaterialList()));
				return "ok";
			}
			case "savedlistscreen" -> {
				// savedlistscreen: opens the "Saved lists" screen with no screen under it.
				SavedListsScreen.open(null);
				return "ok";
			}
			case "available" -> {
				// available: each entry of the current material list as its item and its "Available" count.
				return DataManager.getMaterialList().getMaterialsAll().stream()
					.map(entry -> BuiltInRegistries.ITEM.getKey(entry.getStack().getItem()) + " "
						+ entry.getCountAvailable())
					.collect(Collectors.joining(" | "));
			}
			case "matpanel" -> {
				// matpanel: the material list panel's lines, separated by " | ", each row as its item, its total, available
				// and missing, and missing's colour; or hidden.
				return MaterialPanel.text();
			}
			case "matpanelbox" -> {
				// matpanelbox: the material list panel's box in the window (x y width height), or hidden.
				return MaterialPanel.box();
			}
			case "clickpanel" -> {
				// clickpanel: the click's panel's lines, separated by " | ", each row as its item, how many the click
				// uses, holds and is missing, and missing's colour; or hidden.
				return BuildingPlannerClient.clickPanel().text();
			}
			case "clickpanelbox" -> {
				// clickpanelbox: the click's panel's box in the window (x y width height), or hidden.
				return BuildingPlannerClient.clickPanel().box();
			}
			case "clickpanelkeptout" -> {
				// clickpanelkeptout: everything the click's panel keeps out of while it shows (the other panels, the
				// effect icons, Litematica's lines, the crosshair, chat, the controls lines and the hotbar), each as
				// its box in the window (x y width height), separated by " | "; or hidden.
				return BuildingPlannerClient.clickPanel().keptOut();
			}
			case "clickpanelcost" -> {
				// clickpanelcost <repeats>: the mean time, in microseconds, of the shape preview's count of the items
				// its blocks use (once a tick) and of the panel's work for a frame, each run that many times, with how
				// many blocks go in.
				int repeats = Integer.parseInt(a[1]);
				List<dev.buildingplanner.input.RealPlace.Entry> going = RealShape.shownGoingIn();
				long start = System.nanoTime();
				for (int i = 0; i < repeats; i++) {
					dev.buildingplanner.input.RealPlace.needs(going);
				}
				long counted = System.nanoTime() - start;
				ClickPanel panel = BuildingPlannerClient.clickPanel();
				start = System.nanoTime();
				for (int i = 0; i < repeats; i++) {
					panel.text();
				}
				long drawn = System.nanoTime() - start;
				return "blocks " + going.size() + " count " + counted / 1000 / repeats + " panel " + drawn / 1000 / repeats;
			}
			case "linesbox" -> {
				// linesbox: Litematica's lines about the ghost block at the crosshair, as the click's panel keeps clear
				// of them, in the window (x y width height), or none.
				return ClickPanel.linesBox();
			}
			case "stacksbox" -> {
				// stacksbox: the box Effortless Building's icons beside the crosshair covered in the last whole frame,
				// in the window (x y width height), or none.
				return StackProbe.box();
			}
			case "mixpanelbox" -> {
				// mixpanelbox: the mix panel's box in the window (x y width height), or hidden.
				PanelRoom.Box box = BuildingPlannerClient.mixPanelBox();
				int scale = mc.getWindow().getGuiScale();
				return box == null ? "hidden" : box.left() * scale + " " + box.top() * scale + " "
					+ (box.right() - box.left()) * scale + " " + (box.bottom() - box.top()) * scale;
			}
			case "infohud" -> {
				// infohud: whether the current material list's Info HUD, the panel's switch, is on.
				return String.valueOf(DataManager.getMaterialList().getHudRenderer().getShouldRenderCustom());
			}
			case "matheader" -> {
				// matheader <column>: the window position in the open material list screen's heading of that column (1
				// "Total", 2 "Missing", 3 "Available"), where a click sorts by it, or none.
				return ListScreenProbe.header(mc, Integer.parseInt(a[1]));
			}
			case "matmultiplier" -> {
				// matmultiplier: the window position of the open material list screen's "Multiplier" field.
				return ListScreenProbe.multiplierField(mc);
			}
			case "listfilesaved" -> {
				// listfilesaved <path>: the names of the saved lists in the lists file at that path from the game folder,
				// newest first, or "no file".
				return ListProbe.saved(mc.gameDirectory.toPath().resolve(a[1]));
			}
			case "pastlimits" -> {
				// pastlimits <longname | overtotal <name> | burst <name>>: list messages past the server's limits.
				return ListScreenProbe.sendPastLimits(a);
			}
			case "placements" -> {
				List<String> names = new ArrayList<>();
				for (SchematicPlacement p : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements()) {
					names.add(p.getName());
				}
				return String.join(",", names);
			}
			case "edit" -> {
				// edit x1 y1 z1 x2 y2 z2 block: every position of the box as one edit to the layer, as a shape sends it;
				// air erases.
				BlockState state = state(a[7]);
				Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
				for (BlockPos pos : BlockPos.betweenClosed(pos(a, 1), pos(a, 4))) {
					blocks.put(pos.immutable(), state);
				}
				BuildingPlannerClient.captureShape(state.isAir() ? EditKind.ERASE : EditKind.SHAPE, blocks);
				return "ok";
			}
			case "layer" -> {
				// layer x y z: the ghost block in this game's copy of the layer there.
				return BuiltInRegistries.BLOCK.getKey(session().replica().layer().get(pos(a, 1)).getBlock()).toString();
			}
			case "layerstate" -> {
				// layerstate x y z: the ghost block there with its properties, such as minecraft:candle[candles=4].
				BlockState state = session().replica().layer().get(pos(a, 1));
				String properties = state.getProperties().stream()
					.map(property -> property.getName() + "=" + state.getValue(property)).sorted()
					.collect(java.util.stream.Collectors.joining(","));
				return BuiltInRegistries.BLOCK.getKey(state.getBlock()) + "[" + properties + "]";
			}
			case "boxstates" -> {
				// boxstates x1 y1 z1 x2 y2 z2 <text>: how many ghost blocks the box holds in this game's copy of the layer,
				// and how many of them have the text in their state as layerstate gives it, as "N M".
				int all = 0;
				int with = 0;
				for (BlockPos pos : BlockPos.betweenClosed(pos(a, 1), pos(a, 4))) {
					BlockState state = session().replica().layer().get(pos);
					if (state.isAir()) {
						continue;
					}
					all++;
					String properties = state.getProperties().stream()
						.map(property -> property.getName() + "=" + state.getValue(property)).sorted()
						.collect(java.util.stream.Collectors.joining(","));
					if ((BuiltInRegistries.BLOCK.getKey(state.getBlock()) + "[" + properties + "]").contains(a[7])) {
						with++;
					}
				}
				return all + " " + with;
			}
			case "tile" -> {
				// tile <chunk x> <chunk z>: that column's tile as its lowest..highest block y, or none.
				return tile(Integer.parseInt(a[1]), Integer.parseInt(a[2]));
			}
			case "tiles" -> {
				// tiles: how many tiles there are, and how many placements Litematica has.
				return session().tiles().count() + " tiles, "
					+ DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().size() + " placements";
			}
			case "uuid" -> {
				// uuid <player>: that player's id, from the player list.
				for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
					if (info.getProfile().name().equals(a[1])) {
						return info.getProfile().id().toString();
					}
				}
				return "none";
			}
			case "glows" -> {
				return glows();
			}
			case "selected" -> {
				// selected: Litematica's selected placement: none, a tile, the copy, or another placement's name.
				SchematicPlacement p = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
				return p == null ? "none" : BuildingPlannerClient.isTile(p) ? "a tile"
					: clipboard().holds(p) ? "the copy" : p.getName();
			}
			case "selecttile" -> {
				// selecttile: asks Litematica to select a tile, as its placement list or aim would.
				for (SchematicPlacement p : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements()) {
					if (BuildingPlannerClient.isTile(p)) {
						DataManager.getSchematicPlacementManager().setSelectedSchematicPlacement(p);
						return "ok";
					}
				}
				return "no tile";
			}
			case "placementlist" -> {
				// placementlist: opens Litematica's placement list and says how many of its entries are tiles, and the
				// other entries' names.
				GuiBase.openGui(new GuiSchematicPlacementsList());
				Method getList = GuiListBase.class.getDeclaredMethod("getListWidget");
				getList.setAccessible(true);
				return describe(((WidgetListBase<?, ?>) getList.invoke(mc.gui.screen())).getCurrentEntries());
			}
			case "boxes" -> {
				// boxes: the placements whose boxes Litematica draws, as placementlist describes them.
				OverlayRenderer.getInstance().updatePlacementCache();
				Field placements = OverlayRenderer.class.getDeclaredField("placements");
				placements.setAccessible(true);
				return describe(((Map<?, ?>) placements.get(OverlayRenderer.getInstance())).keySet());
			}
			case "overlay" -> {
				// overlay x y z: the mark Litematica's chunk renderer gives the real block there against its ghost block.
				Object renderer = LitematicaRenderer.getInstance().getWorldRenderer();
				Field infos = renderer.getClass().getDeclaredField("renderInfos");
				infos.setAccessible(true);
				List<?> chunks = (List<?>) infos.get(renderer);
				if (chunks.isEmpty()) {
					return "no chunk renderer";
				}
				Method overlay = chunks.getFirst().getClass().getDeclaredMethod("getOverlayType", BlockState.class,
					BlockState.class);
				overlay.setAccessible(true);
				return String.valueOf(overlay.invoke(chunks.getFirst(),
					SchematicWorldHandler.getSchematicWorld().getBlockState(pos(a, 1)), mc.level.getBlockState(pos(a, 1))));
			}
			case "oldplans" -> {
				// oldplans x y z: an empty plan's file each in Syncmatica's client folder and in "Start a plan"'s, with a
				// placement of each that Litematica saves, as for plans players shared before the layer.
				oldPlan(mc.gameDirectory.toPath().resolve("schematics").resolve("sync"), "Old shared plan", pos(a, 1));
				oldPlan(DataManager.getSchematicsBaseDirectory().resolve("Building Planner"), "Old started plan",
					pos(a, 1).east(4));
				DataManager.save();
				return "ok";
			}
			case "ebmode" -> {
				// ebmode [<mode>]: sets Effortless Building's build mode, as its radial menu does, or without one, gives it.
				if (a.length == 1) {
					return EffortlessDriver.mode();
				}
				EffortlessDriver.mode(a[1]);
				return "ok";
			}
			case "ebarray" -> {
				// ebarray <count> [<dx> <dy> <dz>]: Effortless Building's array modifier alone; a count of 0 clears it.
				int count = Integer.parseInt(a[1]);
				EffortlessDriver.array(count, count > 0 ? Integer.parseInt(a[2]) : 0, count > 0 ? Integer.parseInt(a[3]) : 0,
					count > 0 ? Integer.parseInt(a[4]) : 0);
				return "ok";
			}
			case "ebmirror" -> {
				// ebmirror <x>: Effortless Building's mirror modifier alone, east to west across the plane at x, around
				// the player's feet; ebarray 0 clears it.
				EffortlessDriver.mirror(Double.parseDouble(a[1]), mc.player.getY(), mc.player.getZ());
				return "ok";
			}
			case "ebsent" -> {
				// ebsent: the kinds of Effortless Building packet this client has sent, comma-separated, or "none".
				return SentPackets.effortless();
			}
			case "holdreset" -> {
				// holdreset: starts the hold log again, and disarms quickclick.
				SentPackets.resetHoldLog();
				quickClick = null;
				return "ok";
			}
			case "quickclick" -> {
				// quickclick <use|attack>: the next press of that button lasts at most 3 client ticks: the driver lets
				// it go in the game then, however late the real button comes up, so a slow frame the game catches up
				// on can't stretch the click to the ticks after which a held button repeats.
				quickClick = a[1].equals("use") ? mc.options.keyUse : mc.options.keyAttack;
				quickClickTicks = 0;
				return "ok";
			}
			case "release" -> {
				// release <use|attack>: lets go of that button in the game at the start of the next client tick, however
				// late the real button comes up.
				release = a[1].equals("use") ? mc.options.keyUse : mc.options.keyAttack;
				return "ok";
			}
			case "holdlog" -> {
				// holdlog: ticks each mouse button was down, and the edits Building Planner sent since holdreset.
				return SentPackets.holdLog();
			}
			case "placesent" -> {
				// placesent: the place payloads Building Planner sent since holdreset, each by its number of blocks,
				// comma-separated, or none.
				return SentPackets.places();
			}
			case "countblocks" -> {
				// countblocks <x1> <x2> <y> <z> <block>: how many blocks of that id the plans show along that row.
				int count = 0;
				for (int x = Integer.parseInt(a[1]); x <= Integer.parseInt(a[2]); x++) {
					if (blockId(new BlockPos(x, Integer.parseInt(a[3]), Integer.parseInt(a[4]))).equals(a[5])) {
						count++;
					}
				}
				return String.valueOf(count);
			}
			case "countbox" -> {
				// countbox x1 y1 z1 x2 y2 z2 <block>: how many positions of the box hold that block in this game's copy
				// of the layer, and how many Litematica's schematic world shows it at, as "layer N drawn N".
				int layer = 0;
				int drawn = 0;
				for (BlockPos pos : BlockPos.betweenClosed(pos(a, 1), pos(a, 4))) {
					if (BuiltInRegistries.BLOCK.getKey(session().replica().layer().get(pos).getBlock()).toString()
						.equals(a[7])) {
						layer++;
					}
					if (blockId(pos).equals(a[7])) {
						drawn++;
					}
				}
				return "layer " + layer + " drawn " + drawn;
			}
			case "rendered" -> {
				// rendered <chunk x1> <chunk z1> <chunk x2> <chunk z2>: what Litematica's renderer last built for each
				// of those columns (see RenderProbe).
				return RenderProbe.columns(Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]),
					Integer.parseInt(a[4]));
			}
			case "rerender" -> {
				// rerender: Litematica throws away what it built and builds every column again.
				RenderProbe.rerender();
				return "ok";
			}
			case "unhold" -> {
				// unhold x y z: takes the block there out of the tile Litematica holds, leaving the game's copy of the
				// layer, and has Litematica rebuild the column, so Litematica's copy alone loses it.
				return RenderProbe.unhold(pos(a, 1));
			}
			case "look" -> {
				// look: Litematica's renderBlocksAsTranslucent and ghostBlockAlpha now.
				return "translucent=" + Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue()
					+ " alpha=" + Configs.Visuals.GHOST_BLOCK_ALPHA.getDoubleValue();
			}
			case "setlook" -> {
				// setlook <translucent> <alpha>: the player's own change in Litematica's settings, saved as its screen does.
				Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.setBooleanValue(Boolean.parseBoolean(a[1]));
				Configs.Visuals.GHOST_BLOCK_ALPHA.setDoubleValue(Double.parseDouble(a[2]));
				Configs.saveToFile();
				return "ok";
			}
			case "statusoption" -> {
				// statusoption: Litematica's statusInfoHudAuto now, which shows its status panel by itself.
				return String.valueOf(Configs.InfoOverlays.STATUS_INFO_HUD_AUTO.getBooleanValue());
			}
			case "setstatusoption" -> {
				// setstatusoption <on>: the player's own change in Litematica's settings, saved as its screen does.
				Configs.InfoOverlays.STATUS_INFO_HUD_AUTO.setBooleanValue(Boolean.parseBoolean(a[1]));
				Configs.saveToFile();
				return "ok";
			}
			case "statuspanel" -> {
				// statuspanel: of the client ticks since the player joined in this game run, how many ended with
				// Litematica's status panel to draw.
				return "drawn " + statusPanelTicks + " of " + inGameTicks + " ticks";
			}
			case "areaoption" -> {
				// areaoption: Litematica's enableAreaSelectionBoxesRendering now.
				return String.valueOf(Configs.Visuals.ENABLE_AREA_SELECTION_RENDERING.getBooleanValue());
			}
			case "setareaoption" -> {
				// setareaoption <on>: the player's own change in Litematica's settings, saved as its screen does.
				Configs.Visuals.ENABLE_AREA_SELECTION_RENDERING.setBooleanValue(Boolean.parseBoolean(a[1]));
				Configs.saveToFile();
				return "ok";
			}
			case "selbox" -> {
				// selbox <x1> <y1> <z1> <x2> <y2> <z2> [<ox> <oy> <oz>]: Litematica's simple area selection becomes that
				// one box, with that origin of its own when one is given, saved as Litematica saves it for the server
				// and dimension.
				return AreaProbe.selectBox(pos(a, 1), pos(a, 4), a.length > 7 ? pos(a, 7) : null);
			}
			case "selaway" -> {
				// selaway: Litematica's simple area selection moves to a box far from everything, so no view shows it.
				return AreaProbe.moveAway();
			}
			case "selinfo" -> {
				// selinfo: Litematica's selection mode and the boxes of its simple area selection, or none.
				return AreaProbe.selection();
			}
			case "menukeys" -> {
				// menukeys: Freecam's "Config GUI" key and Litematica's menu hotkey now.
				return KeyProbe.menuKeys();
			}
			case "setmenukeys" -> {
				// setmenukeys <K or M>: the player's own change: Freecam's key to that letter and Litematica's menu
				// hotkey back to M, saved as their screens save them.
				return KeyProbe.setMenuKeys(a[1]);
			}
			case "layerkeys" -> {
				// layerkeys: Litematica's layer hotkeys now, as "next=PAGE_UP previous=PAGE_DOWN" or none for each.
				return KeyProbe.layerKeys();
			}
			case "setlayerkeys" -> {
				// setlayerkeys <next> <previous>: the player's own change: each layer hotkey to PAGE_UP, PAGE_DOWN, K or
				// none, saved as Litematica's screen saves it.
				return KeyProbe.setLayerKeys(a[1], a[2]);
			}
			case "pagepresses" -> {
				// pagepresses: how many Page Up presses reached the driver's key mapping bound to it since the last call.
				return String.valueOf(PageProbe.take());
			}
			case "unbindprobe" -> {
				// unbindprobe: the driver's Page Up key mapping gets no key, so Controls shows no clash with "Nudge the
				// copy up".
				PageProbe.unbind();
				return "ok";
			}
			case "copylock" -> {
				// copylock: whether the copy at the aim is locked: locked, unlocked, away or none.
				Clipboard clipboard = clipboard();
				return !clipboard.exists() ? "none" : !clipboard.atAim() ? "away"
					: clipboard.locked() ? "locked" : "unlocked";
			}
			case "lock" -> {
				// lock: the build modes' lock: "locked" and each locked position as x,y,z (during a shape, the positions
				// its clicks have set, its first first), or "unlocked", then whether its controls are active now; await
				// lock <text> waits for it.
				return lock();
			}
			case "preview" -> {
				// preview: the positions Effortless Building's preview drew in the last whole frame, as x,y,z, or none;
				// await preview <positions> waits for them.
				return PreviewProbe.positions();
			}
			case "previewred" -> {
				// previewred: the positions Effortless Building's preview drew red in the last whole frame, as blocks
				// it can't place, as x,y,z, or none.
				return PreviewProbe.red();
			}
			case "stacks" -> {
				// stacks: the item icons Effortless Building drew beside the crosshair in the last whole frame, each as
				// its item, count and "red" where drawn as missing, such as "minecraft:oak_planks 19,
				// minecraft:oak_planks 1 red", or none.
				return StackProbe.drawn();
			}
			case "redframe" -> {
				// redframe: the positions the red frame for the left click covered in the last whole frame (hook
				// E38), as x,y,z, or none.
				return PreviewProbe.frame();
			}
			case "ghostframe" -> {
				// ghostframe: the ghost blocks the last frame drew the red frame on more strongly, after the world, as
				// x,y,z in order, or none.
				List<BlockPos> drawn = BuildingPlannerClient.ghostFrameDrawn();
				return drawn.isEmpty() ? "none" : drawn.stream()
					.map(at -> at.getX() + "," + at.getY() + "," + at.getZ()).collect(Collectors.joining(" "));
			}
			case "previewstate" -> {
				// previewstate x y z: the state Effortless Building's preview drew there in the last whole frame, with its
				// properties, such as minecraft:oak_stairs[facing=east,...], or none.
				return PreviewProbe.state(pos(a, 1));
			}
			case "sendplace" -> {
				// sendplace <face> <hx> <hy> <hz> shape|paste {<x1> <y1> <z1> <x2> <y2> <z2> <state>}...: sends the
				// server one place of every block of the boxes, each box's blocks in its state, with that face and hit.
				// Gives "N blocks" sent.
				List<LayerChange> blocks = new ArrayList<>();
				for (int i = 6; i + 6 < a.length; i += 7) {
					BlockState state = state(a[i + 6]);
					for (BlockPos pos : BlockPos.betweenClosed(pos(a, i), pos(a, i + 3))) {
						blocks.add(new LayerChange(pos.immutable(), state));
					}
				}
				ClientPlayNetworking.send(new Payloads.Place(mc.level.dimension().identifier(), Direction.byName(a[1]),
					vec(a, 2), a[5].equals("paste"), blocks));
				return blocks.size() + " blocks";
			}
			case "itemcount" -> {
				// itemcount <item>: how many of the item, such as minecraft:oak_planks, the player's inventory holds in
				// all its slots.
				int count = 0;
				for (int slot = 0; slot < mc.player.getInventory().getContainerSize(); slot++) {
					ItemStack stack = mc.player.getInventory().getItem(slot);
					if (BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(a[1])) {
						count += stack.getCount();
					}
				}
				return String.valueOf(count);
			}
			case "soundreset" -> {
				// soundreset: forgets the sounds played so far, for sounds and serversounds.
				SoundProbe.reset();
				return "ok";
			}
			case "sounds", "serversounds" -> {
				// sounds <text>: how many sounds this game played in its world since soundreset whose id has the text,
				// such as block.wood.place, its own included (Effortless Building's as its preview changes);
				// serversounds <text>: how many of them the server sent.
				return String.valueOf(SoundProbe.count(a[1], a[0].equals("serversounds")));
			}
			case "heldturn" -> {
				// heldturn: the turn of the block in hand, its properties' values as name=value, or none.
				BlockTurn.Turn turn = HeldTurn.of(mc.player.getMainHandItem().getItem());
				return turn == null ? "none" : String.join(",", turn.values().entrySet().stream()
					.map(e -> e.getKey() + "=" + e.getValue()).sorted().toList());
			}
			case "keyticks" -> {
				// keyticks <nudge|lock> [reset]: the client ticks that key of the copy's (a CopyNudge name, or lock) has
				// been down, with no screen open, since reset, which starts counting it.
				KeyMapping key = a[1].equals("lock") ? CopyKeys.LOCK : CopyKeys.NUDGES.get(CopyNudge.valueOf(a[1]));
				if (a.length > 2) {
					countedKey = key;
					countedKeyTicks = 0;
				}
				return String.valueOf(countedKey == key ? countedKeyTicks : 0);
			}
			case "screenclass" -> {
				// screenclass: the class of the screen open now, or none.
				return screenClass();
			}
			case "freecam" -> {
				// freecam <true|false>: Freecam's camera on or off, as its key switches it; its state after.
				return FreecamProbe.set(Boolean.parseBoolean(a[1]));
			}
			case "playercontrol" -> {
				// playercontrol <true|false>: Freecam's "Player control" on or off; its state after.
				return FreecamProbe.setPlayerControl(Boolean.parseBoolean(a[1]));
			}
			case "allowinteraction" -> {
				// allowinteraction [true|false]: whether Freecam's "Allow interaction" option is on, after setting it, as
				// its config screen does, when given.
				if (a.length > 1) {
					FreecamProbe.setAllowInteraction(Boolean.parseBoolean(a[1]));
				}
				return FreecamProbe.allowInteraction();
			}
			case "camera" -> {
				// camera: the game's camera entity's class, eye position and facing.
				return FreecamProbe.camera();
			}
			case "camtp" -> {
				// camtp x y z yaw pitch: the camera entity's eyes to that point, turned that way.
				return FreecamProbe.moveCamera(Double.parseDouble(a[1]), Double.parseDouble(a[2]),
					Double.parseDouble(a[3]), Float.parseFloat(a[4]), Float.parseFloat(a[5]));
			}
			case "camlookat" -> {
				// camlookat x y z: turns the camera entity to look at that point, which may be fractional.
				return FreecamProbe.lookAt(Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3]));
			}
			case "shotdiff" -> {
				// shotdiff <shot> <shot>: how many pixels of the two screenshots' upper 6 tenths (the view, not the chat
				// and the lines of text below it) differ by more than a little.
				return AreaProbe.shotDifference(mc.gameDirectory.toPath().resolve("screenshots"), a[1], a[2]);
			}
			case "real" -> {
				return realBlockId(pos(a, 1));
			}
			case "realstate" -> {
				// realstate x y z: the full state of the real block there, such as a door's open property.
				return mc.level.getBlockState(pos(a, 1)).toString();
			}
			case "watchblock" -> {
				// watchblock x y z: from now on, reads this game's real block there at the end of every client tick, for
				// watched.
				watchedPos = pos(a, 1);
				watchedStates.clear();
				return "ok";
			}
			case "watched" -> {
				// watched: the states watchblock read, each run of ticks with one state as "<ticks>x <state>", separated by
				// " | ", and stops watching.
				List<String> runs = new ArrayList<>();
				for (int i = 0; i < watchedStates.size();) {
					int end = i;
					while (end < watchedStates.size() && watchedStates.get(end).equals(watchedStates.get(i))) {
						end++;
					}
					runs.add((end - i) + "x " + watchedStates.get(i));
					i = end;
				}
				watchedPos = null;
				watchedStates.clear();
				return runs.isEmpty() ? "none" : String.join(" | ", runs);
			}
			case "screenbox" -> {
				// screenbox x1 y1 z1 x2 y2 z2: the window's pixels the blocks of that box cover, as "left top right
				// bottom", from the camera's position and field of view and the player's facing, for outlining them in
				// a screenshot taken while the player stands still.
				return screenBox(mc, pos(a, 1), pos(a, 4));
			}
			case "realstates" -> {
				// realstates x1 y1 z1 x2 y2 z2 <text>: how many real blocks other than air the box holds in this game's
				// world, and how many of them have the text in their state as realstate gives it, as "N M".
				int all = 0;
				int with = 0;
				for (BlockPos pos : BlockPos.betweenClosed(pos(a, 1), pos(a, 4))) {
					BlockState state = mc.level.getBlockState(pos);
					if (!state.isAir()) {
						all++;
						with += state.toString().contains(a[7]) ? 1 : 0;
					}
				}
				return all + " " + with;
			}
			case "mix" -> {
				// The player's mix: on or off, then each block and its weight.
				BlockMix mix = mix();
				List<String> parts = new ArrayList<>();
				for (BlockMix.Part part : mix.parts()) {
					parts.add(BuiltInRegistries.BLOCK.getKey(part.block()) + ":" + part.weight());
				}
				return (mix.isOn() ? "on " : "off ") + String.join(",", parts);
			}
			case "setmix" -> {
				// setmix [block:weight,...]: turns the player's mix on with those blocks, or off with none, as "Use mix" and
				// "Clear Mix" do, for shots of the mix panel beside other things.
				List<BlockMix.Part> parts = new ArrayList<>();
				if (a.length > 1) {
					for (String part : a[1].split(",")) {
						int colon = part.lastIndexOf(':');
						parts.add(new BlockMix.Part(state(part.substring(0, colon)).getBlock(),
							Integer.parseInt(part.substring(colon + 1))));
					}
				}
				mix().use(parts);
				return "ok";
			}
			case "mixat" -> {
				return BuiltInRegistries.BLOCK.getKey(mix().blockAt(pos(a, 1))).toString();
			}
			case "panel" -> {
				// panel: the mix panel's lines, separated by " | ", or hidden.
				Field f = BuildingPlannerClient.class.getDeclaredField("mixPanel");
				f.setAccessible(true);
				return ((MixPanel) f.get(null)).text();
			}
			case "pickermix" -> {
				// pickermix: the mix the open picker shows, each block and its weight.
				Field f = BlockMixScreen.class.getDeclaredField("parts");
				f.setAccessible(true);
				List<String> parts = new ArrayList<>();
				for (Object part : (List<?>) f.get(mc.gui.screen())) {
					BlockMix.Part p = (BlockMix.Part) part;
					parts.add(BuiltInRegistries.BLOCK.getKey(p.block()) + ":" + p.weight());
				}
				return String.join(",", parts);
			}
			case "pickershown" -> {
				Field shown = BlockMixScreen.class.getDeclaredField("shown");
				shown.setAccessible(true);
				return String.valueOf(((List<?>) shown.get(mc.gui.screen())).size());
			}
			case "gridcell" -> {
				// gridcell <index>: the window position of the centre of that cell of the picker's block grid.
				for (GuiEventListener child : mc.gui.screen().children()) {
					if (child.getClass().getSimpleName().equals("BlockGrid")) {
						Field columns = child.getClass().getDeclaredField("columns");
						columns.setAccessible(true);
						int n = Integer.parseInt(a[1]);
						int perRow = (int) columns.get(child);
						AbstractWidget grid = (AbstractWidget) child;
						return scaled(mc, grid.getX() + 9 + 18 * (n % perRow), grid.getY() + 9 + 18 * (n / perRow));
					}
				}
				return "none";
			}
			case "widget" -> {
				// widget <n> <text>: the window position of the centre of the nth shown widget (from 0) on the open
				// screen whose text (a speed slider's heading) starts with <text>, or none.
				int n = Integer.parseInt(a[1]);
				String prefix = line.substring(a[0].length() + a[1].length() + 2);
				for (GuiEventListener child : mc.gui.screen().children()) {
					if (child instanceof AbstractWidget w && w.visible && label(w).startsWith(prefix) && n-- == 0) {
						return scaled(mc, w.getX() + w.getWidth() / 2.0, w.getY() + w.getHeight() / 2.0);
					}
				}
				return "none";
			}
			case "sliderat" -> {
				// sliderat <ticks> <text>: the window position on the slider whose heading starts with <text> that sets
				// a gap of <ticks> ticks, where a vanilla slider takes a click at x as (x - (left + 4)) / (width - 8).
				int ticks = Integer.parseInt(a[1]);
				String prefix = line.substring(a[0].length() + a[1].length() + 2);
				for (GuiEventListener child : mc.gui.screen().children()) {
					if (child instanceof RepeatSlider w && label(w).startsWith(prefix)) {
						return scaled(mc, w.getX() + 4 + RepeatSpeed.position(ticks) * (w.getWidth() - 8),
							w.getY() + w.getHeight() / 2.0);
					}
				}
				return "none";
			}
			case "box" -> {
				// box <text>: the window position, width and height of the first widget on the open screen whose text
				// (a speed slider's heading) starts with <text>, for outlining it in a screenshot; a speed slider's as
				// it's drawn, which can fall between GUI pixels.
				String prefix = line.substring(a[0].length() + 1);
				double scale = mc.getWindow().getGuiScale();
				for (GuiEventListener child : mc.gui.screen().children()) {
					if (child instanceof AbstractWidget w && label(w).startsWith(prefix)) {
						double[] at = w instanceof RepeatSlider slider ? slider.drawnAt()
							: new double[] {w.getX(), w.getY()};
						return scaled(mc, at[0], at[1]) + " " + (int) (w.getWidth() * scale) + " "
							+ (int) (w.getHeight() * scale);
					}
				}
				return "none";
			}
			case "texts" -> {
				// texts: the text of each shown widget on the open screen, separated by " | ".
				List<String> texts = new ArrayList<>();
				for (GuiEventListener child : mc.gui.screen().children()) {
					if (child instanceof AbstractWidget w && w.visible) {
						texts.add(w.getMessage().getString());
					}
				}
				return String.join(" | ", texts);
			}
			case "drawntexts" -> {
				// drawntexts: each string the last frame drew as plain text, separated by " | ".
				return ScreenProbe.drawnTexts();
			}
			case "operator" -> {
				// operator: whether the game counts the player an operator, as the radial menu asks before it adds
				// "Functional Settings": "operator" or "not an operator".
				return mc.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) ? "operator"
					: "not an operator";
			}
			case "tooltips" -> {
				// tooltips: each tooltip the last frame set (a radial menu button's hover text, say), its lines separated
				// by spaces, the tooltips by " | ", or none.
				return ScreenProbe.tooltips();
			}
			case "cornertext" -> {
				// cornertext: the string the last frame drew at the screen's lower right, as Effortless Building's
				// radial menu draws its name, then " | " and its window box (left top right bottom), or none.
				return ScreenProbe.cornerText(mc);
			}
			case "keybinds" -> {
				// keybinds: opens the key binds screen (Options, Controls, Key Binds).
				ScreenProbe.openKeyBinds(mc);
				return "ok";
			}
			case "settings" -> {
				// settings: opens Effortless Building's "Settings" screen, as its radial menu's button does.
				EffortlessDriver.settings(mc);
				return "ok";
			}
			case "settingrow" -> {
				// settingrow <label>: the text of the widget on the open screen's row whose label the last frame drew as
				// <label> (a switch's "ON" or "OFF"), then the widget's centre in window pixels; or none.
				return ScreenProbe.settingRow(mc, line.substring("settingrow ".length()));
			}
			case "keybindlist" -> {
				// keybindlist: the key binds screen's headings, each with its keys in order: "Heading: key, key ; ...".
				return ScreenProbe.keyBinds(mc);
			}
			case "keybindscroll" -> {
				// keybindscroll <heading>: scrolls the key binds screen to that heading, then gives the window boxes
				// (left top right bottom) of its row and of the heading with its keys.
				return ScreenProbe.scrollKeyBinds(mc, line.substring("keybindscroll ".length()));
			}
			case "keybindrows" -> {
				// keybindrows <heading>: the window boxes of the key binds list's visible area and of each key's row
				// under that heading, as scrolled now.
				return ScreenProbe.keyRows(mc, line.substring("keybindrows ".length()));
			}
			case "ebline" -> {
				// ebline: the components the last frame drew 58 above the screen's bottom, where Effortless Building draws
				// its own line during a shape, left first, each in [ ], or none.
				return ScreenProbe.shapeLine(mc);
			}
			case "eblinecolours" -> {
				// eblinecolours: the colours of each of those components, its runs of one colour joined by "+"
				// ("white+dark_aqua"), the components separated by spaces, or none.
				return ScreenProbe.shapeLineColours(mc);
			}
			case "eblinerects" -> {
				// eblinerects: each of those components as a rectangle, as moderects gives the controls': its window box
				// (left top right bottom, in the window's pixels) and its text in [ ], separated by " | ", or none.
				return ScreenProbe.shapeLineRects(mc);
			}
			case "modehud" -> {
				// modehud: the line of the mouse mode's controls as the player reads it, or hidden.
				return modeHud().text();
			}
			case "actionbar" -> {
				// actionbar: the action bar's text while it shows, as Effortless Building's block count during a shape,
				// or none.
				Field text = mc.gui.hud.getClass().getDeclaredField("overlayMessageString");
				Field time = mc.gui.hud.getClass().getDeclaredField("overlayMessageTime");
				text.setAccessible(true);
				time.setAccessible(true);
				Object message = text.get(mc.gui.hud);
				return message == null || time.getInt(mc.gui.hud) <= 0 ? "none"
					: ((Component) message).getString();
			}
			case "drawnbox" -> {
				// drawnbox <text>: the window box (left top right bottom) of each text the last frame drew as a component
				// whose text is that text, separated by " | ", or none; as the held item's name and the mode's name.
				return ScreenProbe.drawnBoxes(mc, line.substring("drawnbox ".length()));
			}
			case "drawntextbox" -> {
				// drawntextbox <text>: the window box (left top right bottom) of each string the last frame drew as
				// plain text that is that text, separated by " | ", or none; as the radial menu draws its options'
				// headings.
				return ScreenProbe.drawnTextBoxes(mc, line.substring("drawntextbox ".length()));
			}
			case "optionrow" -> {
				// optionrow <section>: the window box (left top right bottom) of the open radial menu's row of three
				// option buttons in that section (0 for the first), as the menu places them at its full size.
				return EffortlessDriver.optionRow(mc, Integer.parseInt(a[1]));
			}
			case "ringat" -> {
				// ringat <mode>: the window position on the open radial menu's ring segment of that build mode, 45 GUI
				// pixels from the menu's middle along the segment's middle line, for pointing at it.
				return EffortlessDriver.segmentAt(mc, a[1]);
			}
			case "modebox" -> {
				// modebox: the window box (left top right bottom, in the window's pixels) of the controls as drawn now, or
				// hidden; for the shots' outlines.
				return modeHud().windowBox();
			}
			case "modepart" -> {
				// modepart <text>: the window box (left top right bottom, in the window's pixels) of the first rectangle
				// of the controls drawn now whose text holds that text, or none; for the shots' outlines.
				return modeHud().partBox(line.substring("modepart ".length()));
			}
			case "modecolours" -> {
				// modecolours: the colour of each rectangle's text as drawn now, row after row, separated by spaces.
				return modeHud().rectColours();
			}
			case "moderects" -> {
				// moderects: each rectangle of the controls as drawn now, its window box (left top right bottom, in the
				// window's pixels) and its text in [ ], separated by " | ", the rows by " / ", or hidden.
				return modeHud().rectBoxes();
			}
			case "erasebar" -> {
				// erasebar: the erase's progress bar as drawn now, its window box (left top right bottom, in the window's
				// pixels) and "fill" with the right of its fill, or hidden.
				return modeHud().eraseBar();
			}
			case "markbox" -> {
				// markbox: Delete's box as it shows: "mark" or "unmark", its lowest and highest corners and its colour,
				// with "unaimed" while the aim meets nothing, or none.
				return session().markBox().describe();
			}
			case "erasing" -> {
				// erasing: the ticks Delete's left button has been held toward the erase, or -1 while it isn't held to
				// erase or Delete is off; await erasing <ticks> waits for it.
				return String.valueOf(erasing());
			}
			case "bindkey" -> {
				// bindkey <key mapping> <key>: binds the key mapping (key.buildingplanner.lock_copy, say) to the key
				// (key.keyboard.k, say), as Controls does, and gives the key as Minecraft saves it.
				KeyMapping mapping = KeyMapping.get(a[1]);
				mapping.setKey(InputConstants.getKey(a[2]));
				KeyMapping.resetMapping();
				return mapping.saveString();
			}
			case "modewrap" -> {
				// modewrap: the controls as they wrap on the screen now, lines separated by " / ", or hidden.
				return modeHud().wrappedText();
			}
			case "boxlistbackgrounds" -> {
				// boxlistbackgrounds: the window box (left top right bottom) of the dark background under each row of
				// the list under the crosshair, top row first, separated by " | ", or hidden.
				return modeHud().listBackgrounds();
			}
			case "boxshade" -> {
				// boxshade: the block whose top face shows shaded under the box's first corner, as x,y,z, or none.
				return session().box().describeShade();
			}
			case "shotdarker" -> {
				// shotdarker <shot> <shot>: how many pixels of the first screenshot's upper 6 tenths are darker than the
				// second's by more than a little, as something dark drawn over the view makes them.
				return AreaProbe.shotDarker(mc.gameDirectory.toPath().resolve("screenshots"), a[1], a[2]);
			}
			case "boxlist" -> {
				// boxlist: the list under the crosshair: the right edge of its cells, the screen's width, the bottom of
				// its rows and the top of the controls, in GUI pixels, then its rows; or hidden.
				return modeHud().listText();
			}
			case "breaklist" -> {
				// breaklist: Effortless Building's shape under way and how many kinds of real block its break list
				// holds.
				return EffortlessDriver.breakList();
			}
			case "slot" -> {
				// slot: the hotbar slot selected, from 0, and the item in the main hand, e.g. "0 minecraft:stone_bricks".
				return mc.player.getInventory().getSelectedSlot() + " "
					+ BuiltInRegistries.ITEM.getKey(mc.player.getMainHandItem().getItem());
			}
			case "handcount" -> {
				// handcount: how many items the main hand's stack holds.
				return String.valueOf(mc.player.getMainHandItem().getCount());
			}
			case "radialclear" -> {
				// radialclear: "clear" when Building Planner's buttons, and its speed sliders and their headings while
				// they show, in the open radial menu meet nothing of the menu's, each other or the screen's edges, else
				// each meeting.
				return EffortlessDriver.radialConflicts(mc.gui.screen());
			}
			case "buttons" -> {
				// buttons: each of Building Planner's buttons on the open screen as its name and its window box (x y width
				// height), separated by " | ", for outlining them in a screenshot.
				return buttonBoxes(mc);
			}
			case "selectedbuttons" -> {
				// selectedbuttons: the names of Building Planner's buttons on the open screen that show selected,
				// separated by " | ", or none.
				List<String> names = new ArrayList<>();
				for (GuiEventListener child : mc.gui.screen().children()) {
					if (child instanceof RadialButton w && w.visible && w.selected()) {
						names.add(w.getMessage().getString());
					}
				}
				return names.isEmpty() ? "none" : String.join(" | ", names);
			}
			case "menuframes" -> {
				// menuframes arm <name>: shoots the radial menu's next opening 1 and 2 frames after it, as
				// <name>-frame1 and <name>-frame2. menuframes: those frames, each with the menu's size then and its
				// buttons' window boxes, ending "nearer and smaller" when each button was nearer the middle and smaller
				// than in the menu open now.
				if (a.length > 2 && a[1].equals("arm")) {
					MenuFrames.arm(a[2]);
					return "ok";
				}
				return MenuFrames.report(mc);
			}
			case "ringselected" -> {
				// ringselected <shot>: the build modes whose segment of the radial menu's ring the shot shows selected,
				// in the menu's blue, or none; the menu is open at full size, with the pointer off the ring.
				return EffortlessDriver.selectedSegments(
					mc.gameDirectory.toPath().resolve("screenshots").resolve(a[1] + ".png"), mc.getWindow().getGuiScale());
			}
			case "slots" -> {
				// slots: each of Building Planner's buttons on the open screen as its hover text, its lines separated by a
				// space, then @ and its middle from the screen's middle in GUI pixels (x,y), separated by " | ".
				Screen screen = mc.gui.screen();
				List<String> slots = new ArrayList<>();
				for (GuiEventListener child : screen.children()) {
					if (child instanceof RadialButton w && w.visible) {
						slots.add(w.hoverText().stream().map(Component::getString).collect(Collectors.joining(" "))
							+ " @ " + (w.getX() + w.getWidth() / 2 - screen.width / 2) + ","
							+ (w.getY() + w.getHeight() / 2 - screen.height / 2));
					}
				}
				return String.join(" | ", slots);
			}
			case "ghosts" -> {
				// ghosts: whether ghost blocks show, with how many tiles there are and how many placements Litematica has.
				return (BuildingPlannerClient.ghostsShown() ? "shown, " : "hidden, ") + session().tiles().count()
					+ " tiles, " + DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().size()
					+ " placements";
			}
			case "throughwalls" -> {
				// throughwalls: the ghost blocks the last frame drew through walls, as "lightblue x,y,z ... red x,y,z ...
				// orange x,y,z ...", or none.
				return ThroughWallsProbe.drawn();
			}
			case "throughwallschanged" -> {
				// throughwallschanged: the game time of the scan that last changed the ghost blocks drawn through walls.
				return String.valueOf(BuildingPlannerClient.throughWallsChangedAt());
			}
			case "gametime" -> {
				// gametime: the level's game time now, in ticks.
				return String.valueOf(mc.level.getGameTime());
			}
			case "wrongscan" -> {
				// wrongscan <times>: how long finding the ghost blocks drawn through walls takes with ghost blocks filling
				// its range, and building their shapes whole and again after a 1-block edit.
				return ThroughWallsProbe.scanCost(Integer.parseInt(a[1]));
			}
			case "toolitem" -> {
				// toolitem: whether Litematica's tool item is on.
				return String.valueOf(Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue());
			}
			case "repeat" -> {
				// repeat: the ticks between repeats that hold to repeat uses now, for placing and for erasing.
				return "place=" + RepeatSpeed.PLACING.ticks(BuildingPlannerClient.config())
					+ " erase=" + RepeatSpeed.ERASING.ticks(BuildingPlannerClient.config());
			}
			case "mousemode" -> {
				// mousemode: Building Planner's mouse mode now (BOX, COPY_AT_AIM, DELETE), or none.
				return String.valueOf(session().mouseMode()).replace("null", "none");
			}
			case "marks" -> {
				// marks: Delete's marks: how many, how many positions erasing them takes, then each as x,y,z
				// when there are no more than 40.
				return MarksProbe.marks(session());
			}
			case "markall" -> {
				// markall x1 y1 z1 x2 y2 z2: marks every ghost block in the box, as one sweep over each would.
				return MarksProbe.markAll(session(), pos(a, 1), pos(a, 4));
			}
			case "turn" -> {
				// turn x1 y1 z1 x2 y2 z2 <frames>: turns the view to look at the first point in the next frame, then on in
				// even steps to the second in the <frames>th, as a quick mouse sweep would; await turned waits for it.
				FrameProbe.turn(vec(a, 1), vec(a, 4), Integer.parseInt(a[7]));
				return "ok";
			}
			case "frametimes" -> {
				// frametimes <frames>: starts timing that many frames; await framestimed waits for it. frametimes: the
				// timed frames' mean and longest.
				if (a.length > 1) {
					FrameProbe.time(Integer.parseInt(a[1]));
					return "ok";
				}
				return FrameProbe.times();
			}
			case "timeaims" -> {
				// timeaims x1 y1 z1 x2 y2 z2 <repeats>: times a frame's sweep from looking at the first point to looking
				// at the second, aiming as Delete's sweep does, that many times.
				return MarksProbe.timeAims(session(), vec(a, 1), vec(a, 4), Integer.parseInt(a[7]));
			}
			case "timeoutline" -> {
				// timeoutline: times building the shape of the marks now. timeoutline scattered <count>: of that many
				// marks that touch no other.
				return a.length > 2 ? MarksProbe.timeScatteredOutline(Integer.parseInt(a[2]))
					: MarksProbe.timeOutline(session());
			}
			case "close" -> {
				mc.setScreenAndShow(null);
				return "ok";
			}
			case "notoasts" -> {
				// notoasts: clears the game's pop-ups (recipes, advancements, tips) and ends its tutorial, for screenshots.
				mc.gui.toastManager().clear();
				mc.getTutorial().setStep(TutorialSteps.NONE);
				return "ok";
			}
			case "window" -> {
				// window <width> <height> <gui scale>: resizes the window and sets the GUI scale, for screenshots at other
				// sizes.
				mc.getWindow().setWindowed(Integer.parseInt(a[1]), Integer.parseInt(a[2]));
				mc.options.guiScale().set(Integer.parseInt(a[3]));
				mc.resizeGui();
				return mc.getWindow().getWidth() + "x" + mc.getWindow().getHeight() + " at "
					+ mc.getWindow().getGuiScale();
			}
			case "hello" -> {
				// hello <version>: handles a server hello with that protocol version, as one from another release would be.
				BuildingPlannerClient.hello(new Payloads.Hello(Integer.parseInt(a[1]), SyncLimits.DEFAULT.maxEditBlocks()));
				return "ok";
			}
			case "shot" -> {
				Screenshot.grab(mc.gameDirectory, a[1] + ".png", mc.gameRenderer.mainRenderTarget(), 1, c -> {
				});
				writeShotBoxes(mc, a[1]);
				return "ok";
			}
			default -> {
				return "unknown command";
			}
		}
	}

	/** The driver's reads written beside each shot, for outlining what the shot shows. */
	private static final List<String> SHOT_READS = List.of("moderects", "eblinerects", "erasebar", "modebox", "buttons",
		"widgets", "malilibbuttons", "infoicon", "drawbox", "markbox", "marks", "preview", "previewred", "copy",
		"copyset", "lock", "throughwalls", "matpanelbox", "clickpanelbox", "mixpanelbox", "stacksbox", "linesbox");
	private static final java.util.regex.Pattern POSITION = java.util.regex.Pattern.compile("(-?\\d+),(-?\\d+),(-?\\d+)");

	/**
	 * Writes {@code screenshots/<name>.boxes} beside the shot: the window's size and GUI scale; each of
	 * {@link #SHOT_READS} as the driver gives it; for each read that names block positions, the window box the blocks
	 * between its lowest and highest positions cover ("world <read> left top right bottom"), and for the ghost blocks
	 * drawn through walls each block's own, with its colour; then every text and tooltip background the last frame
	 * drew ({@link ScreenProbe#drawnAll}).
	 */
	private void writeShotBoxes(Minecraft mc, String name) {
		List<String> out = new ArrayList<>();
		out.add("window " + mc.getWindow().getWidth() + " " + mc.getWindow().getHeight() + " scale "
			+ mc.getWindow().getGuiScale());
		for (String read : SHOT_READS) {
			String result;
			try {
				result = run(mc, read);
			} catch (Throwable t) {
				result = "ERROR " + t;
			}
			out.add(read + ": " + result);
			if (mc.player == null || result.startsWith("ERROR")) {
				continue;
			}
			java.util.regex.Matcher m = POSITION.matcher(result);
			List<BlockPos> positions = new ArrayList<>();
			while (m.find()) {
				positions.add(new BlockPos(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
					Integer.parseInt(m.group(3))));
			}
			if (positions.isEmpty()) {
				continue;
			}
			BlockPos min = positions.stream().reduce((p, q) -> new BlockPos(Math.min(p.getX(), q.getX()),
				Math.min(p.getY(), q.getY()), Math.min(p.getZ(), q.getZ()))).orElseThrow();
			BlockPos max = positions.stream().reduce((p, q) -> new BlockPos(Math.max(p.getX(), q.getX()),
				Math.max(p.getY(), q.getY()), Math.max(p.getZ(), q.getZ()))).orElseThrow();
			out.add("world " + read + " " + screenBox(mc, min, max));
			if (read.equals("throughwalls")) {
				// The colour names come before their positions: "lightblue x,y,z ... red ... orange ...".
				String colour = "";
				for (String word : result.split(" ")) {
					m = POSITION.matcher(word);
					if (!m.matches()) {
						colour = word;
						continue;
					}
					BlockPos pos = new BlockPos(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
						Integer.parseInt(m.group(3)));
					out.add("world throughwall " + colour + " " + word + " " + screenBox(mc, pos, pos));
				}
			}
		}
		out.addAll(ScreenProbe.drawnAll(mc));
		try {
			Path dir = mc.gameDirectory.toPath().resolve("screenshots");
			Files.createDirectories(dir);
			Files.write(dir.resolve(name + ".boxes"), out, StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/** The class of the screen open now, or {@code none}. */
	private static String screenClass() {
		Screen screen = Minecraft.getInstance().gui.screen();
		return screen == null ? "none" : screen.getClass().getName();
	}

	/** The ticks Delete's left button has been held toward the erase, or -1 while it isn't counting or Delete is off. */
	private static int erasing() throws Exception {
		return session() == null ? -1 : session().eraseTicks();
	}

	/** The build modes' lock, for the driver's {@code lock}. */
	private static String lock() throws Exception {
		List<BlockPos> locked = EffortlessShapes.lockedPositions(session().lock());
		StringBuilder text = new StringBuilder(locked.isEmpty() ? "unlocked" : "locked");
		for (BlockPos pos : locked) {
			text.append(' ').append(pos.getX()).append(',').append(pos.getY()).append(',').append(pos.getZ());
		}
		return text.append(BuildingPlannerClient.lockActive() ? " active" : " inactive").toString();
	}

	/** The conditions {@code await} takes. */
	private static boolean holds(String what) throws Exception {
		String[] a = what.split(" ");
		Minecraft mc = Minecraft.getInstance();
		return switch (a[0]) {
			case "session" -> session() != null;
			case "nosession" -> session() == null;
			case "screen" -> mc.gui.screen() != null
				&& mc.gui.screen().getTitle().getString().equals(what.substring(7));
			case "noscreen" -> mc.gui.screen() == null;
			case "screenclass" -> screenClass().equals(what.substring(12));
			case "placement" -> placement(a[1]) != null;
			case "noplacement" -> placement(a[1]) == null;
			case "block" -> List.of(a[4].split("\\|")).contains(blockId(pos(a, 1)));
			case "real" -> realBlockId(pos(a, 1)).equals(a[4]);
			case "tile" -> String.valueOf(tile(Integer.parseInt(a[1]), Integer.parseInt(a[2]))).equals(a[3]);
			case "glow" -> glows().contains(what.substring(5));
			case "copy" -> copy().startsWith(what.substring(5));
			case "set" -> set().startsWith(what.substring(4));
			case "copycolour" -> copyColour().equals(what.substring(11));
			case "sprint" -> mc.options.keySprint.isDown() == Boolean.parseBoolean(a[1]);
			case "preview" -> PreviewProbe.positions().equals(what.substring(8));
			case "lock" -> lock().equals(what.substring(5));
			case "turned" -> FrameProbe.turned();
			case "framestimed" -> FrameProbe.timed();
			case "erasing" -> String.valueOf(erasing()).equals(a[1]);
			case "noglow" -> glows().isEmpty();
			case "held" -> SentPackets.held(a[1].equals("use"));
			case "pressedafter" -> SentPackets.presses() > Integer.parseInt(a[1]);
			case "throughwalls" -> ThroughWallsProbe.drawn().equals(what.substring(13));
			case "inlayer" -> BuiltInRegistries.BLOCK.getKey(session().replica().layer().get(pos(a, 1)).getBlock())
				.toString().equals(a[4]);
			case "ingame" -> inGameTicks >= Integer.parseInt(a[1]);
			case "layer" -> LayerProbe.matches(mc.gameDirectory.toPath().resolve(a[1]));
			case "listfile" -> ListProbe.fileHasCurrent(mc.gameDirectory.toPath().resolve(a[1]));
			default -> throw new IllegalArgumentException(what);
		};
	}

	private static String describe(String awaitLine) {
		String[] a = awaitLine.split(" ");
		try {
			return switch (a[1]) {
				case "block" -> blockId(pos(a, 2)) + " (layer " + BuiltInRegistries.BLOCK.getKey(
					session().replica().layer().get(pos(a, 2)).getBlock()) + ", tile "
					+ tile(pos(a, 2).getX() >> 4, pos(a, 2).getZ() >> 4) + ", tile block " + tileBlock(pos(a, 2))
					+ ")";
				case "real" -> realBlockId(pos(a, 2));
				case "tile" -> tile(Integer.parseInt(a[2]), Integer.parseInt(a[3]));
				case "glow" -> glows();
				case "erasing" -> String.valueOf(erasing());
				case "copy" -> copy();
				case "set" -> set();
				case "copycolour" -> copyColour();
				case "layer" -> LayerProbe.compare(Minecraft.getInstance().gameDirectory.toPath().resolve(a[2]));
				case "listfile" -> "file " + ListProbe.lastCounted(Minecraft.getInstance().gameDirectory.toPath()
					.resolve(a[2])) + ", game " + ListProbe.current();
				case "screen", "noscreen" -> String.valueOf(Minecraft.getInstance().gui.screen());
				case "screenclass" -> screenClass();
				case "held" -> "screen " + Minecraft.getInstance().gui.screen() + ", use down "
					+ Minecraft.getInstance().options.keyUse.isDown() + ", attack down "
					+ Minecraft.getInstance().options.keyAttack.isDown() + ", mouse grabbed "
					+ Minecraft.getInstance().mouseHandler.isMouseGrabbed() + ", window active "
					+ Minecraft.getInstance().isWindowActive();
				default -> "";
			};
		} catch (Throwable t) {
			return t.toString();
		}
	}

	/**
	 * The block in the container of the tile whose box holds the position, as Litematica would draw it there, or none:
	 * against blockId, it tells a tile that's out of date from a schematic world that hasn't drawn it yet.
	 */
	private static String tileBlock(BlockPos pos) {
		for (SchematicPlacement p : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements()) {
			if (!BuildingPlannerClient.isTile(p)) {
				continue;
			}
			BlockPos origin = p.getOrigin();
			var container = p.getSchematic().getSubRegionContainer(Tiles.NAME);
			var size = container.getSize();
			int x = pos.getX() - origin.getX();
			int y = pos.getY() - origin.getY();
			int z = pos.getZ() - origin.getZ();
			if (x >= 0 && y >= 0 && z >= 0 && x < size.getX() && y < size.getY() && z < size.getZ()) {
				return BuiltInRegistries.BLOCK.getKey(container.get(x, y, z).getBlock()).toString();
			}
		}
		return "none";
	}

	private static String blockId(BlockPos pos) {
		BlockState state = SchematicWorldHandler.getSchematicWorld().getBlockState(pos);
		return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
	}

	/** The window's pixels the box from {@code a} to {@code b} covers, its 8 corners seen through a perspective view. */
	private static String screenBox(Minecraft mc, BlockPos a, BlockPos b) {
		// The camera's own field of view, which the sprint and other effects widen beyond the option's.
		Vec3 eye = mc.gameRenderer.mainCamera().position();
		Vec3 forward = mc.player.getLookAngle();
		Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
		Vec3 up = right.cross(forward);
		double tan = Math.tan(Math.toRadians(mc.gameRenderer.mainCamera().getFov()) / 2);
		int width = mc.getWindow().getWidth();
		int height = mc.getWindow().getHeight();
		double aspect = (double) width / height;
		double left = Double.MAX_VALUE;
		double top = Double.MAX_VALUE;
		double rightEdge = -Double.MAX_VALUE;
		double bottom = -Double.MAX_VALUE;
		for (int i = 0; i < 8; i++) {
			Vec3 corner = new Vec3((i & 1) == 0 ? Math.min(a.getX(), b.getX()) : Math.max(a.getX(), b.getX()) + 1,
				(i & 2) == 0 ? Math.min(a.getY(), b.getY()) : Math.max(a.getY(), b.getY()) + 1,
				(i & 4) == 0 ? Math.min(a.getZ(), b.getZ()) : Math.max(a.getZ(), b.getZ()) + 1).subtract(eye);
			double depth = corner.dot(forward);
			double x = (corner.dot(right) / (depth * tan * aspect) + 1) / 2 * width;
			double y = (1 - corner.dot(up) / (depth * tan)) / 2 * height;
			left = Math.min(left, x);
			top = Math.min(top, y);
			rightEdge = Math.max(rightEdge, x);
			bottom = Math.max(bottom, y);
		}
		return Math.round(left) + " " + Math.round(top) + " " + Math.round(rightEdge) + " " + Math.round(bottom);
	}

	private static String realBlockId(BlockPos pos) {
		BlockState state = Minecraft.getInstance().level.getBlockState(pos);
		return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
	}

	private static SchematicPlacement placement(String name) {
		for (SchematicPlacement p : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements()) {
			if (p.getName().equals(name)) {
				return p;
			}
		}
		return null;
	}

	/** A column's tile as {@code lowest..highest} block y, or {@code none}. */
	private static String tile(int chunkX, int chunkZ) throws Exception {
		String tile = session().tiles().describe(chunkX, chunkZ);
		return tile == null ? "none" : tile;
	}

	/** Placements as how many are tiles, then the others' names. */
	private static String describe(Collection<?> placements) {
		int tiles = 0;
		List<String> others = new ArrayList<>();
		for (Object p : placements) {
			if (BuildingPlannerClient.isTile((SchematicPlacement) p)) {
				tiles++;
			} else {
				others.add(((SchematicPlacement) p).getName());
			}
		}
		return tiles + " tiles; others: " + (others.isEmpty() ? "none" : String.join(",", others));
	}

	/** Saves an empty 3 by 1 by 3 plan named {@code name} in {@code folder} and adds a placement of it at {@code corner}. */
	private static void oldPlan(Path folder, String name, BlockPos corner) {
		AreaSelection area = new AreaSelection();
		area.addSubRegionBox(new Box(corner, corner.offset(2, 0, 2), name), false);
		LitematicaSchematic schematic = LitematicaSchematic.createEmptySchematic(area, "");
		schematic.getMetadata().setName(name);
		if (!schematic.writeToFile(folder, name, true)) {
			throw new IllegalStateException("couldn't write " + name);
		}
		LitematicaSchematic loaded = SchematicHolder.getInstance().getOrLoad(folder.resolve(name + ".litematic"));
		DataManager.getSchematicPlacementManager().addSchematicPlacement(
			SchematicPlacement.createFor(loaded, corner, name, true, true), false);
	}

	/** The glows showing now, as {@code editor:blocks} pairs. */
	private static String glows() throws Exception {
		Field glowField = BuildingPlannerClient.class.getDeclaredField("glow");
		glowField.setAccessible(true);
		Object glow = glowField.get(null);
		Field listField = glow.getClass().getDeclaredField("glows");
		listField.setAccessible(true);
		List<String> out = new ArrayList<>();
		for (Object g : new ArrayList<>((List<?>) listField.get(glow))) {
			Method editor = g.getClass().getDeclaredMethod("editor");
			Method blocks = g.getClass().getDeclaredMethod("blocks");
			editor.setAccessible(true);
			blocks.setAccessible(true);
			out.add(editor.invoke(g) + ":" + ((List<?>) blocks.invoke(g)).size());
		}
		return String.join(",", out);
	}

	/**
	 * Each of Building Planner's buttons on the open screen as its name and its window box (x y width height), separated
	 * by " | ".
	 */
	static String buttonBoxes(Minecraft mc) {
		double scale = mc.getWindow().getGuiScale();
		List<String> boxes = new ArrayList<>();
		for (GuiEventListener child : mc.gui.screen().children()) {
			if (child instanceof RadialButton w && w.visible) {
				boxes.add(w.getMessage().getString() + ": " + scaled(mc, w.getX(), w.getY()) + " "
					+ (int) (w.getWidth() * scale) + " " + (int) (w.getHeight() * scale));
			}
		}
		return String.join(" | ", boxes);
	}

	/** A widget's text, as the commands that find widgets by it read it: a speed slider's heading, which names it. */
	private static String label(AbstractWidget w) {
		return w instanceof RepeatSlider slider ? slider.heading().getString() : w.getMessage().getString();
	}

	private static String scaled(Minecraft mc, double x, double y) {
		double scale = mc.getWindow().getGuiScale();
		return (int) (x * scale) + " " + (int) (y * scale);
	}

	private static BlockMix mix() throws Exception {
		Field f = BuildingPlannerClient.class.getDeclaredField("MIX");
		f.setAccessible(true);
		return (BlockMix) f.get(null);
	}

	private static ModeHud modeHud() throws Exception {
		Field f = BuildingPlannerClient.class.getDeclaredField("modeHud");
		f.setAccessible(true);
		return (ModeHud) f.get(null);
	}

	static ClientSession session() throws Exception {
		Field f = BuildingPlannerClient.class.getDeclaredField("session");
		f.setAccessible(true);
		return (ClientSession) f.get(null);
	}

	private static Clipboard clipboard() throws Exception {
		return session().clipboard();
	}

	/**
	 * The player's copy: {@code none}, or {@code at} and the box it fills, as it's turned, from its lowest corner to its
	 * highest ({@code at 4,-60,20..6,-59,21}), while it's at the aim and {@code away} while it's put away, then its
	 * mirror, rotation, whether it's the selected placement, and how many blocks it has; {@code nosession} while
	 * Building Planner is off.
	 */
	private static String copy() throws Exception {
		if (session() == null) {
			return "nosession";
		}
		Clipboard clipboard = clipboard();
		if (!clipboard.exists()) {
			return "none";
		}
		SchematicPlacement p = clipboard.placement();
		boolean selected = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement() == p;
		return (clipboard.atAim() ? "at " + shortPos(clipboard.spot().min()) + ".." + shortPos(clipboard.spot().max())
			: "away") + " "
			+ p.getMirror() + " " + p.getRotation() + (selected ? " selected " : " unselected ")
			+ clipboard.worldBlocks().size() + " blocks";
	}

	/**
	 * The colour of the box Building Planner draws for the copy at the aim or the build modes' locked positions, as
	 * {@code copycolour} gives it.
	 */
	private static String copyColour() throws Exception {
		if (session() == null) {
			return "nosession";
		}
		CopyOutline.Shown outline = BuildingPlannerClient.outlineShown();
		CopyColour.Shown shown = outline == null ? null : outline.colour();
		return shown == null ? "none" : shown.colour() + (shown.refusal() == null ? "" : " " + shown.refusal());
	}

	/**
	 * The copy's set: {@code none} while no set is fixed, otherwise how many copies it has, how many of
	 * Litematica's placements are the copy's or its set's, with the mirrors and rotations they have (one, when every copy
	 * is turned alike), then each copy's box, the first's first: {@code 2 copies, 2 placements NONE NONE:
	 * 4,-60,20..6,-59,21 ; 7,-60,20..9,-59,21}.
	 */
	private static String set() throws Exception {
		if (session() == null) {
			return "nosession";
		}
		Clipboard clipboard = clipboard();
		if (!clipboard.setFixed()) {
			return "none";
		}
		List<CopyPlacement.Spot> spots = clipboard.setSpots();
		List<SchematicPlacement> placements = DataManager.getSchematicPlacementManager().getAllSchematicsPlacements()
			.stream().filter(clipboard::holds).toList();
		String turns = placements.stream().map(p -> p.getMirror() + " " + p.getRotation()).distinct()
			.collect(Collectors.joining(","));
		return spots.size() + " copies, " + placements.size() + " placements " + turns + ": " + spots.stream()
			.map(spot -> shortPos(spot.min()) + ".." + shortPos(spot.max())).collect(Collectors.joining(" ; "));
	}

	private static String shortPos(BlockPos pos) {
		return pos.getX() + "," + pos.getY() + "," + pos.getZ();
	}

	private static BlockPos pos(String[] a, int i) {
		return new BlockPos(Integer.parseInt(a[i]), Integer.parseInt(a[i + 1]), Integer.parseInt(a[i + 2]));
	}

	/** The point a[i], a[i + 1], a[i + 2], which may be fractional. */
	private static Vec3 vec(String[] a, int i) {
		return new Vec3(Double.parseDouble(a[i]), Double.parseDouble(a[i + 1]), Double.parseDouble(a[i + 2]));
	}

	private static BlockState state(String id) throws Exception {
		return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, id, false).blockState();
	}
}
