package dev.buildingplanner.client;

import ch.endte.syncmatica.data.ServerPlacement;
import ch.endte.syncmatica.litematica.LitematicManager;
import ch.endte.syncmatica.litematica.gui.GuiSyncmaticaServerPlacementList;
import dev.buildingplanner.sync.BlockChange;
import dev.buildingplanner.sync.EditKind;
import dev.buildingplanner.sync.PlanReplicas;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.SchematicUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.gui.widgets.WidgetContainer;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Drives a dev client for the in-game checks in {@code clienttest/}. It runs the commands in {@code driver-in.txt} in
 * the game folder, one at a time as lines are added, and appends each with its result to {@code driver-out.txt}.
 * Without that file it does nothing. It's a mod of its own, on the dev clients' classpath only.
 */
public final class Driver implements ClientModInitializer {
	private static final int AWAIT_TICKS = 1200;

	private int done;
	private int wait;
	private String awaiting;
	private Supplier<String> condition;
	private int awaitedTicks;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
	}

	private void tick(Minecraft mc) {
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
			case "select" -> {
				AreaSelection area = DataManager.getSelectionManager().getCurrentSelection();
				area.setSelectedSubRegionCornerPos(pos(a, 1), PositionUtils.Corner.CORNER_1);
				area.setSelectedSubRegionCornerPos(pos(a, 4), PositionUtils.Corner.CORNER_2);
				return "ok";
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
			case "shape" -> {
				// shape <plan> x y z block: one block in the plan's first region, in plan coordinates, as a SHAPE edit
				// (the kind Effortless Building's capture sends), which can grow the plan.
				PlanReplicas replicas = replicas();
				ServerPlacement server = server(a[1]);
				String region = replicas.plan(server.getId()).orElseThrow().firstRegion().name();
				List<BlockChange> changes = new ArrayList<>();
				changes.add(new BlockChange(region, pos(a, 2), state(a[5])));
				return "seq " + replicas.edit(server.getId(), EditKind.SHAPE, changes);
			}
			case "area" -> {
				return area(a[1]);
			}
			case "glows" -> {
				return glows();
			}
			case "syncgui" -> {
				GuiBase.openGui(new GuiSyncmaticaServerPlacementList());
				return "ok";
			}
			case "button" -> {
				// button <plan> <label>: the window position of the centre of that button on the plan's row in the
				// open list, for a real click.
				return button(mc, a[1], line.substring(a[0].length() + a[1].length() + 2));
			}
			case "close" -> {
				mc.setScreenAndShow(null);
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

	/** The conditions {@code await} takes. */
	private static boolean holds(String what) throws Exception {
		String[] a = what.split(" ");
		Minecraft mc = Minecraft.getInstance();
		return switch (a[0]) {
			case "session" -> session() != null;
			case "screen" -> mc.gui.screen() != null
				&& mc.gui.screen().getTitle().getString().equals(what.substring(7));
			case "noscreen" -> mc.gui.screen() == null;
			case "placement" -> placement(a[1]) != null;
			case "noplacement" -> placement(a[1]) == null;
			case "shared" -> sharedNames().contains(what.substring(7));
			case "notshared" -> !sharedNames().contains(what.substring(10));
			case "block" -> blockId(pos(a, 1)).equals(a[4]);
			case "area" -> area(a[1]).equals(a[2]);
			case "glow" -> glows().contains(what.substring(5));
			case "noglow" -> glows().isEmpty();
			default -> throw new IllegalArgumentException(what);
		};
	}

	private static String describe(String awaitLine) {
		String[] a = awaitLine.split(" ");
		try {
			return switch (a[1]) {
				case "block" -> blockId(pos(a, 2));
				case "area" -> area(a[2]);
				case "glow" -> glows();
				case "shared", "notshared" -> sharedNames().toString();
				case "screen", "noscreen" -> String.valueOf(Minecraft.getInstance().gui.screen());
				default -> "";
			};
		} catch (Throwable t) {
			return t.toString();
		}
	}

	private static String blockId(BlockPos pos) {
		BlockState state = SchematicWorldHandler.getSchematicWorld().getBlockState(pos);
		return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
	}

	private static SchematicPlacement placement(String name) {
		for (SchematicPlacement p : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements()) {
			if (p.getName().equals(name) && LitematicManager.getInstance().getRenderingSyncmatic(p) != null) {
				return p;
			}
		}
		return null;
	}

	/** The plan's areas as {@code min..max;min..max}, in plan coordinates. */
	private static String area(String name) {
		List<String> boxes = new ArrayList<>();
		for (Box b : placement(name).getSchematic().getAreas().values()) {
			boxes.add(b.getPos1().toShortString().replace(" ", "") + ".." + b.getPos2().toShortString().replace(" ", ""));
		}
		return String.join(";", boxes);
	}

	private static List<String> sharedNames() {
		List<String> names = new ArrayList<>();
		for (ServerPlacement p : LitematicManager.getInstance().getActiveContext().getSyncmaticManager().getAll()) {
			names.add(p.getName());
		}
		return names;
	}

	private static ServerPlacement server(String name) {
		for (ServerPlacement p : LitematicManager.getInstance().getActiveContext().getSyncmaticManager().getAll()) {
			if (p.getName().equals(name)) {
				return p;
			}
		}
		throw new IllegalArgumentException("no shared plan " + name);
	}

	/** The glows showing now, as {@code editor:blocks} pairs. */
	private static String glows() throws Exception {
		Field glowField = BuildingPlannerClient.class.getDeclaredField("GLOW");
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

	private static String button(Minecraft mc, String plan, String label) throws Exception {
		Method getList = GuiListBase.class.getDeclaredMethod("getListWidget");
		getList.setAccessible(true);
		Object list = getList.invoke(mc.gui.screen());
		Field entries = WidgetListBase.class.getDeclaredField("listWidgets");
		entries.setAccessible(true);
		Field children = WidgetContainer.class.getDeclaredField("subWidgets");
		children.setAccessible(true);
		Field text = ButtonBase.class.getDeclaredField("displayString");
		text.setAccessible(true);
		for (Object entry : (List<?>) entries.get(list)) {
			if (!(((WidgetListEntryBase<?>) entry).getEntry() instanceof ServerPlacement p) || !p.getName().equals(plan)) {
				continue;
			}
			for (Object child : (List<?>) children.get(entry)) {
				if (child instanceof ButtonBase b && label.equals(text.get(b))) {
					WidgetBase w = b;
					double scale = mc.getWindow().getGuiScale();
					return (int) ((w.getX() + w.getWidth() / 2.0) * scale) + " "
						+ (int) ((w.getY() + w.getHeight() / 2.0) * scale);
				}
			}
		}
		return "none";
	}

	private static ClientSession session() throws Exception {
		Field f = BuildingPlannerClient.class.getDeclaredField("session");
		f.setAccessible(true);
		return (ClientSession) f.get(null);
	}

	private static PlanReplicas replicas() throws Exception {
		Field f = ClientSession.class.getDeclaredField("replicas");
		f.setAccessible(true);
		return (PlanReplicas) f.get(session());
	}

	private static BlockPos pos(String[] a, int i) {
		return new BlockPos(Integer.parseInt(a[i]), Integer.parseInt(a[i + 1]), Integer.parseInt(a[i + 2]));
	}

	private static BlockState state(String id) throws Exception {
		return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, id, false).blockState();
	}
}
