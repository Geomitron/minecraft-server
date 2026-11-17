package dev.buildingplanner.layer;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One dimension's ghost blocks: a map from chunk column to the column's sections that hold ghost blocks. A section
 * left with no ghost block is freed, and so is a column left with no section, so memory follows the ghost blocks.
 *
 * <p>Columns are grouped into areas of {@value #AREA_SIDE} by {@value #AREA_SIDE} columns, one file each
 * ({@link LayerFiles}); the layer notes each area a {@link #set} changes until {@link #takeChangedAreas}. Columns and
 * areas are keyed by {@link #key}. Not thread-safe: one thread uses a layer.
 */
public final class Layer {
	public static final int AREA_SHIFT = 5;
	public static final int AREA_SIDE = 1 << AREA_SHIFT;

	private final Long2ObjectOpenHashMap<LayerColumn> columns = new Long2ObjectOpenHashMap<>();
	private final LongOpenHashSet changedAreas = new LongOpenHashSet();

	/** A key for a column's or an area's two coordinates. */
	public static long key(int x, int z) {
		return (long) z << Integer.SIZE | x & 0xFFFFFFFFL;
	}

	public static int keyX(long key) {
		return (int) key;
	}

	public static int keyZ(long key) {
		return (int) (key >> Integer.SIZE);
	}

	public BlockState get(BlockPos pos) {
		LayerColumn column = columns.get(key(pos.getX() >> LayerSection.SHIFT, pos.getZ() >> LayerSection.SHIFT));
		return column == null ? LayerSection.AIR : column.get(pos.getX(), pos.getY(), pos.getZ());
	}

	/** Sets a ghost block (air for none) and returns the block it replaced. */
	public BlockState set(BlockPos pos, BlockState state) {
		int chunkX = pos.getX() >> LayerSection.SHIFT;
		int chunkZ = pos.getZ() >> LayerSection.SHIFT;
		long columnKey = key(chunkX, chunkZ);
		LayerColumn column = columns.get(columnKey);
		if (column == null) {
			if (state.isAir()) {
				return LayerSection.AIR;
			}
			column = new LayerColumn();
			columns.put(columnKey, column);
		}
		BlockState replaced = column.set(pos.getX(), pos.getY(), pos.getZ(), state);
		if (column.isEmpty()) {
			columns.remove(columnKey);
		}
		if (replaced != state && !(replaced.isAir() && state.isAir())) {
			changedAreas.add(key(chunkX >> AREA_SHIFT, chunkZ >> AREA_SHIFT));
		}
		return replaced;
	}

	/** A column holding ghost blocks, or null. */
	public LayerColumn column(int chunkX, int chunkZ) {
		return columns.get(key(chunkX, chunkZ));
	}

	/** Every column holding ghost blocks, by {@link #key}. */
	public Long2ObjectMap<LayerColumn> columns() {
		return Long2ObjectMaps.unmodifiable(columns);
	}

	/**
	 * Replaces a column with its packed sections, or removes it when none holds a ghost block. Not a change to save:
	 * this loads a column from a file or a message. A damaged section is an error and changes nothing.
	 */
	public void replaceColumn(int chunkX, int chunkZ, List<PackedSection> sections) {
		putColumn(chunkX, chunkZ, LayerColumn.unpack(sections));
	}

	void putColumn(int chunkX, int chunkZ, LayerColumn column) {
		if (column.isEmpty()) {
			columns.remove(key(chunkX, chunkZ));
		} else {
			columns.put(key(chunkX, chunkZ), column);
		}
	}

	public boolean isEmpty() {
		return columns.isEmpty();
	}

	/** The areas changed since the last call, by {@link #key}; the layer forgets them. */
	public LongSet takeChangedAreas() {
		LongSet taken = new LongOpenHashSet(changedAreas);
		changedAreas.clear();
		return taken;
	}
}
