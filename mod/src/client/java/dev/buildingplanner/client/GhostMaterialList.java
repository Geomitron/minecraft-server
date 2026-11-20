package dev.buildingplanner.client;

import dev.buildingplanner.layer.MaterialCount;
import dev.buildingplanner.sync.ListData;
import dev.buildingplanner.sync.ListSort;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.util.BlockUtils;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.GuiUtils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * Litematica's material list for the blocks in a box the player drew: each block's total, ghost and real blocks alike,
 * and for the ghost blocks how many are still missing in the world and how many have the wrong block there. It counts
 * the player's copy of the layer, which holds the columns the game has loaded, against the player's world, by the rule
 * Litematica's own list uses, so the copy, which Litematica also draws, never counts. While the player's ghost blocks
 * are hidden, it lists the real blocks only. Litematica's Refresh button counts again.
 *
 * <p>A list the server kept ({@link #kept}) shows its counts as kept, with "Available" counted from the inventory,
 * until Refresh counts its box again. Refresh counts only while the game holds the whole box (see {@link #count}), and
 * otherwise says so on the screen, since a chunk the game doesn't hold would count as air.
 */
final class GhostMaterialList extends MaterialListBase {
	private final Identifier dimension;
	private final BlockPos min;
	private final BlockPos max;
	/** The saved list's name, for a list opened from one. */
	private final Optional<String> name;
	/** When the list was saved, or last counted. */
	private OptionalLong time;
	/** Whether the list counted its box since {@link #takeCounted} last asked. */
	private boolean counted;

	/** The list for the box from {@code min} to {@code max} in {@code dimension}, counted at once. */
	GhostMaterialList(Identifier dimension, BlockPos min, BlockPos max) {
		this(dimension, min, max, Optional.empty(), OptionalLong.empty());
		count();
	}

	private GhostMaterialList(Identifier dimension, BlockPos min, BlockPos max, Optional<String> name,
		OptionalLong time) {
		this.dimension = dimension;
		this.min = min.immutable();
		this.max = max.immutable();
		this.name = name;
		this.time = time;
	}

	/** A list the server kept, with its counts and the screen's settings as kept, and "Available" counted now. */
	static GhostMaterialList kept(ListData data) {
		GhostMaterialList list = new GhostMaterialList(data.dimension(), data.min(), data.max(), data.name(), data.time());
		list.multiplier = Math.max(1, data.multiplier());
		list.sortCriteria = SortCriteria.valueOf(data.sort().name());
		list.reverse = data.reverse();
		list.hideAvailable = data.hideAvailable();
		List<MaterialListEntry> entries = new ArrayList<>();
		for (ListData.Row row : data.rows()) {
			entries.add(new MaterialListEntry(new ItemStack(row.item()), row.total(), row.missing(), row.wrong(), 0));
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			MaterialListUtils.updateAvailableCounts(entries, mc.player);
		}
		list.setMaterialListEntries(entries);
		list.recreateFilteredList();
		return list;
	}

	@Override
	public String getName() {
		return getTitle();
	}

	@Override
	public String getTitle() {
		return Component.translatable("buildingplanner.ui.material_list.title").getString();
	}

	/**
	 * Litematica's Refresh button: counts the box again when the game can (see {@link #count}), and otherwise keeps the
	 * counts and says so on the list's screen.
	 */
	@Override
	public void reCreateMaterialList() {
		if (!count() && GuiUtils.getCurrentScreen() instanceof GuiMaterialList gui && gui.getMaterialList() == this) {
			gui.addMessage(MessageType.WARNING, "buildingplanner.ui.material_list.move_closer");
		}
	}

	/**
	 * Counts the box again while Building Planner has a session, the player is in the box's dimension, and the game
	 * holds every chunk the box touches, as a box needs when it's drawn; returns whether it counted.
	 */
	boolean count() {
		ClientSession session = BuildingPlannerClient.session();
		Minecraft mc = Minecraft.getInstance();
		if (session == null || mc.level == null || !dimension.equals(session.replica().dimension()) || !held(mc.level)) {
			return false;
		}
		MaterialCount count = MaterialCount.of(session.replica().layer(), min, max, BoxList.realSections(mc.level),
			GhostMaterialList::built, session.ghostsShown());
		setMaterialListEntries(MaterialListUtils.getMaterialList(count.total(), count.missing(), count.wrong(),
			new Object2IntOpenHashMap<>(), new Object2IntOpenHashMap<>(), new Object2IntOpenHashMap<>(), mc.player));
		time = OptionalLong.of(System.currentTimeMillis());
		counted = true;
		return true;
	}

	/** Whether the game holds every chunk the box touches. {@code ClientLevel.hasChunk} is always true. */
	private boolean held(ClientLevel level) {
		ClientChunkCache chunks = level.getChunkSource();
		for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++) {
			for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++) {
				if (chunks.getChunk(x, z, ChunkStatus.FULL, false) == null) {
					return false;
				}
			}
		}
		return true;
	}

	/** Whether the list counted its box since the last call, which makes it "Last counted". */
	boolean takeCounted() {
		boolean was = counted;
		counted = false;
		return was;
	}

	/**
	 * The list as the server keeps it: its box, the screen's multiplier, sort, reverse and "Hide available", and one row
	 * for each item, with its name and time when it has them.
	 */
	ListData data() {
		Map<Item, int[]> rows = new LinkedHashMap<>();
		for (MaterialListEntry entry : getMaterialsAll()) {
			int[] counts = rows.computeIfAbsent(entry.getStack().getItem(), item -> new int[3]);
			counts[0] += entry.getCountTotal();
			counts[1] += entry.getCountMissing();
			counts[2] += entry.getCountMismatched();
		}
		List<ListData.Row> list = new ArrayList<>(rows.size());
		rows.forEach((item, counts) -> list.add(new ListData.Row(item, counts[0], counts[1], counts[2])));
		return new ListData(name, time, dimension, min, max, getMultiplier(), ListSort.valueOf(getSortCriteria().name()),
			getSortInReverse(), getHideAvailable(), list);
	}

	/**
	 * Whether the real block is the ghost block built, as Litematica's list decides (its options "materialListIgnoreState"
	 * and "ignoreCropAge").
	 */
	private static boolean built(BlockState ghost, BlockState real) {
		return real == ghost
			|| Configs.Generic.MATERIAL_LIST_IGNORE_STATE.getBooleanValue() && real.getBlock() == ghost.getBlock()
			|| Configs.Visuals.IGNORE_CROP_AGE.getBooleanValue() && BlockUtils.areStatesEqualIgnoringAge(ghost, real);
	}
}
