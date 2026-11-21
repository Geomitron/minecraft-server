package dev.buildingplanner.client;

import dev.buildingplanner.input.RepeatSpeed;
import dev.buildingplanner.sync.EditKind;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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
			case "pastehere" -> {
				// pastehere x1 y1 z1 x2 y2 z2: copies the box, as Copy does, and pastes the copy at once where it was
				// copied from, as a left click with the copy there would; the box's lowest corner must hold a block.
				session().copy(pos(a, 1), pos(a, 4));
				clipboard().moveTo(pos(a, 1));
				session().paste();
				return copy();
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
			case "holdlog" -> {
				// holdlog: ticks each mouse button was down, and the edits Building Planner sent since holdreset.
				return SentPackets.holdLog();
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
				// allowinteraction: whether Freecam's "Allow interaction" option is on.
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
				// screen whose text starts with <text>, or none.
				int n = Integer.parseInt(a[1]);
				String prefix = line.substring(a[0].length() + a[1].length() + 2);
				for (GuiEventListener child : mc.gui.screen().children()) {
					if (child instanceof AbstractWidget w && w.visible && w.getMessage().getString().startsWith(prefix)
						&& n-- == 0) {
						return scaled(mc, w.getX() + w.getWidth() / 2.0, w.getY() + w.getHeight() / 2.0);
					}
				}
				return "none";
			}
			case "sliderat" -> {
				// sliderat <ticks> <text>: the window position on the slider whose text starts with <text> that sets a gap
				// of <ticks> ticks, where a vanilla slider takes a click at x as (x - (left + 4)) / (width - 8).
				int ticks = Integer.parseInt(a[1]);
				String prefix = line.substring(a[0].length() + a[1].length() + 2);
				for (GuiEventListener child : mc.gui.screen().children()) {
					if (child instanceof AbstractWidget w && w.getMessage().getString().startsWith(prefix)) {
						return scaled(mc, w.getX() + 4 + RepeatSpeed.position(ticks) * (w.getWidth() - 8),
							w.getY() + w.getHeight() / 2.0);
					}
				}
				return "none";
			}
			case "box" -> {
				// box <text>: the window position, width and height of the first widget on the open screen whose text
				// starts with <text>, for outlining it in a screenshot.
				String prefix = line.substring(a[0].length() + 1);
				double scale = mc.getWindow().getGuiScale();
				for (GuiEventListener child : mc.gui.screen().children()) {
					if (child instanceof AbstractWidget w && w.getMessage().getString().startsWith(prefix)) {
						return scaled(mc, w.getX(), w.getY()) + " " + (int) (w.getWidth() * scale) + " "
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
			case "keybindlist" -> {
				// keybindlist: the key binds screen's headings, each with its keys in order: "Heading: key, key ; ...".
				return ScreenProbe.keyBinds(mc);
			}
			case "keybindscroll" -> {
				// keybindscroll <heading>: scrolls the key binds screen to that heading, then gives the window boxes
				// (left top right bottom) of its row and of the heading with its keys.
				return ScreenProbe.scrollKeyBinds(mc, line.substring("keybindscroll ".length()));
			}
			case "modehud" -> {
				// modehud: the line of the mouse mode's controls as the player reads it, or hidden.
				return modeHud().text();
			}
			case "modewrap" -> {
				// modewrap: the controls as they wrap on the screen now, lines separated by " / ", or hidden.
				return modeHud().wrappedText();
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
			case "radialclear" -> {
				// radialclear: "clear" when Building Planner's buttons in the open radial menu meet nothing of the menu's,
				// the sliders, each other or the screen's edges, else each meeting.
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
			case "toolitem" -> {
				// toolitem: whether Litematica's tool item is on.
				return String.valueOf(Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue());
			}
			case "repeat" -> {
				// repeat: the ticks between repeats that hold to repeat uses now, for placing and for erasing.
				return "place=" + RepeatSpeed.PLACING.ticks(BuildingPlannerClient.config())
					+ " erase=" + RepeatSpeed.ERASING.ticks(BuildingPlannerClient.config());
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
				return "ok";
			}
			default -> {
				return "unknown command";
			}
		}
	}

	/** The class of the screen open now, or {@code none}. */
	private static String screenClass() {
		Screen screen = Minecraft.getInstance().gui.screen();
		return screen == null ? "none" : screen.getClass().getName();
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
			case "sprint" -> mc.options.keySprint.isDown() == Boolean.parseBoolean(a[1]);
			case "noglow" -> glows().isEmpty();
			case "held" -> SentPackets.held(a[1].equals("use"));
			case "pressedafter" -> SentPackets.presses() > Integer.parseInt(a[1]);
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
				case "copy" -> copy();
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

	private static String shortPos(BlockPos pos) {
		return pos.getX() + "," + pos.getY() + "," + pos.getZ();
	}

	private static BlockPos pos(String[] a, int i) {
		return new BlockPos(Integer.parseInt(a[i]), Integer.parseInt(a[i + 1]), Integer.parseInt(a[i + 2]));
	}

	private static BlockState state(String id) throws Exception {
		return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, id, false).blockState();
	}
}
