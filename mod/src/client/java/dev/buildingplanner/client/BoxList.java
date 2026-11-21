package dev.buildingplanner.client;

import dev.buildingplanner.layer.BoxCount;
import dev.buildingplanner.layer.DeleteMarks;
import dev.buildingplanner.layer.GhostErase;
import dev.buildingplanner.layer.Layer;
import fi.dy.masa.litematica.materials.MaterialCache;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * The blocks in the box being drawn, for the list under the crosshair: each block's item, as Litematica's material list
 * gives it ({@code MaterialCache}), with its count, most numerous first, counted by the copy's rule ({@link BoxCount}),
 * or for a Delete box its ghost blocks alone ({@link GhostErase#counts}), since Delete leaves real blocks as they are.
 * It counts again when a corner moves or the box changes purpose, at most once every {@value #EVERY} ticks. A box past
 * a limit has no list. In Delete by aim it lists the marked ghost blocks instead, counted again when the marks or the
 * layer change, as often.
 */
final class BoxList {
	private static final int EVERY = 5;

	/** A kind of block in the box: its item, and how many of that item the box's blocks come to. */
	record Entry(ItemStack item, int count) {
	}

	private BoxDraw.Purpose purpose;
	private BlockPos min;
	private BlockPos max;
	/** The marks last counted, and their version and the layer's then. */
	private DeleteMarks marks;
	private long marksVersion;
	private long layerVersion;
	private int sinceCount = EVERY;
	private List<Entry> entries = List.of();

	/** The kinds of block in the box, most numerous first; none while no box shows within its limits. */
	List<Entry> entries() {
		return entries;
	}

	/** Once a client tick: counts the box as it shows now when its corners moved and the last count is old enough. */
	void update(BoxDraw.Shown box, Layer layer, boolean ghostsShown) {
		sinceCount++;
		ClientLevel level = Minecraft.getInstance().level;
		boolean wasMarks = marks != null;
		marks = null;
		if (box == null || box.min() == null || box.limit() != null || level == null) {
			purpose = null;
			min = null;
			max = null;
			entries = List.of();
			return;
		}
		if (!wasMarks && (box.purpose() == purpose && box.min().equals(min) && box.max().equals(max)
			|| sinceCount < EVERY)) {
			return;
		}
		sinceCount = 0;
		purpose = box.purpose();
		min = box.min();
		max = box.max();
		entries = entries(purpose == BoxDraw.Purpose.DELETE ? GhostErase.counts(layer, min, max)
			: BoxCount.of(min, max, layer, realSections(level), ghostsShown));
	}

	/**
	 * Once a client tick in Delete by aim: counts the marked ghost blocks when the marks or the layer changed and the
	 * last count is old enough.
	 */
	void update(DeleteMarks marked, Layer layer) {
		sinceCount++;
		purpose = null;
		min = null;
		max = null;
		if (marked == marks && (marked.version() == marksVersion && layer.version() == layerVersion
			|| sinceCount < EVERY)) {
			return;
		}
		sinceCount = 0;
		marks = marked;
		marksVersion = marked.version();
		layerVersion = layer.version();
		Object2IntOpenHashMap<BlockState> blocks = new Object2IntOpenHashMap<>();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (LongIterator it = marked.positions().iterator(); it.hasNext();) {
			BlockState ghost = layer.get(pos.set(it.nextLong()));
			if (!ghost.isAir()) {
				blocks.addTo(ghost, 1);
			}
		}
		entries = entries(blocks);
	}

	/** Each block's item with its count, most numerous first. */
	private static List<Entry> entries(Object2IntOpenHashMap<BlockState> blocks) {
		Object2IntOpenHashMap<Item> items = new Object2IntOpenHashMap<>();
		MaterialCache cache = MaterialCache.getInstance();
		for (Object2IntMap.Entry<BlockState> block : blocks.object2IntEntrySet()) {
			ItemStack item = cache.getRequiredBuildItemForState(block.getKey());
			if (!item.isEmpty()) {
				items.addTo(item.getItem(), item.getCount() * block.getIntValue());
			}
		}
		return items.object2IntEntrySet().stream()
			.sorted(Comparator.<Object2IntMap.Entry<Item>>comparingInt(Object2IntMap.Entry::getIntValue).reversed()
				.thenComparing(entry -> BuiltInRegistries.ITEM.getKey(entry.getKey()).toString()))
			.map(entry -> new Entry(new ItemStack(entry.getKey()), entry.getIntValue()))
			.toList();
	}

	/**
	 * The real blocks of the chunks the game has loaded, by section; none elsewhere. {@code ClientLevel.hasChunk} is
	 * always true, so the chunk cache says which it holds.
	 */
	static BoxCount.Sections realSections(ClientLevel level) {
		ClientChunkCache chunks = level.getChunkSource();
		return (x, y, z) -> {
			LevelChunk chunk = chunks.getChunk(x, z, ChunkStatus.FULL, false);
			if (chunk == null) {
				return null;
			}
			int index = chunk.getSectionIndexFromSectionY(y);
			return index < 0 || index >= chunk.getSectionsCount() ? null : chunk.getSection(index);
		};
	}
}
