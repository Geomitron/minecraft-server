package dev.buildingplanner.layer;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * The comparisons of the game's self-check for one chunk column ("Overlapping shapes"): the game's copy of the layer
 * outside an edit's own positions, before and after the edit, and the copy against the blocks Litematica holds. Each
 * reads only the sections it needs: the sections an edit's positions fall in, and the tile's sections.
 */
public final class ColumnCheck {
	private static final int MASK = LayerSection.SIDE - 1;

	/** Ghost blocks outside an edit's positions, before and after it, and how many of those positions changed. */
	public record Outside(int before, int after, int changed) {
	}

	/**
	 * Positions outside other placements' boxes: those holding a ghost block in the game's copy of the layer, those
	 * where Litematica holds a block, and those where the two differ.
	 */
	public record Held(int layer, int held, int differing) {
	}

	/**
	 * A column before an edit: a copy of each section the edit's positions fall in, and the column's other sections as
	 * they are, with how many ghost blocks each holds, since the edit changes nothing in them.
	 */
	public static final class Before {
		private final Int2ObjectMap<BlockState[]> touched = new Int2ObjectOpenHashMap<>();
		private final Int2ObjectMap<LayerSection> others = new Int2ObjectOpenHashMap<>();
		private final Int2IntOpenHashMap otherBlocks = new Int2IntOpenHashMap();

		private Before() {
		}
	}

	private ColumnCheck() {
	}

	/** The column ({@code null} for none) before an edit of {@code own}, the edit's positions in it. */
	public static Before before(LayerColumn column, Collection<BlockPos> own) {
		Before before = new Before();
		for (BlockPos pos : own) {
			int height = pos.getY() >> LayerSection.SHIFT;
			if (!before.touched.containsKey(height)) {
				LayerSection section = column == null ? null : column.sections().get(height);
				BlockState[] states = new BlockState[LayerSection.VOLUME];
				for (int index = 0; index < LayerSection.VOLUME; index++) {
					states[index] = section == null ? LayerSection.AIR : section.get(index);
				}
				before.touched.put(height, states);
			}
		}
		if (column != null) {
			for (Int2ObjectMap.Entry<LayerSection> entry : column.sections().int2ObjectEntrySet()) {
				if (!before.touched.containsKey(entry.getIntKey())) {
					before.others.put(entry.getIntKey(), entry.getValue());
					before.otherBlocks.put(entry.getIntKey(), entry.getValue().blocks());
				}
			}
		}
		return before;
	}

	/**
	 * The column's ghost blocks outside the edit's positions {@code own}, as {@link #before} took them and in
	 * {@code after} ({@code null} for none), the column once the edit applied.
	 */
	public static Outside outside(Before before, LayerColumn after, Collection<BlockPos> own) {
		Int2ObjectMap<BitSet> ownIndices = new Int2ObjectOpenHashMap<>();
		for (BlockPos pos : own) {
			ownIndices.computeIfAbsent(pos.getY() >> LayerSection.SHIFT, height -> new BitSet(LayerSection.VOLUME))
				.set(LayerSection.index(pos.getX(), pos.getY(), pos.getZ()));
		}
		int was = 0;
		int is = 0;
		int changed = 0;
		for (Int2ObjectMap.Entry<BlockState[]> entry : before.touched.int2ObjectEntrySet()) {
			BitSet skip = ownIndices.get(entry.getIntKey());
			BlockState[] states = entry.getValue();
			LayerSection now = after == null ? null : after.sections().get(entry.getIntKey());
			for (int index = 0; index < LayerSection.VOLUME; index++) {
				if (skip.get(index)) {
					continue;
				}
				BlockState then = states[index];
				BlockState state = now == null ? LayerSection.AIR : now.get(index);
				was += then.isAir() ? 0 : 1;
				is += state.isAir() ? 0 : 1;
				changed += differ(then, state) ? 1 : 0;
			}
		}
		for (Int2ObjectMap.Entry<LayerSection> entry : before.others.int2ObjectEntrySet()) {
			LayerSection then = entry.getValue();
			int thenBlocks = before.otherBlocks.get(entry.getIntKey());
			LayerSection now = after == null ? null : after.sections().get(entry.getIntKey());
			was += thenBlocks;
			if (now == then && now.blocks() == thenBlocks) {
				is += thenBlocks;
				continue;
			}
			int nowBlocks = now == null ? 0 : now.blocks();
			is += nowBlocks;
			if (now == null) {
				changed += thenBlocks;
				continue;
			}
			int differing = 0;
			for (int index = 0; index < LayerSection.VOLUME; index++) {
				differing += differ(then.get(index), now.get(index)) ? 1 : 0;
			}
			// A section changed in place shows only in its count.
			changed += Math.max(differing, Math.abs(nowBlocks - thenBlocks));
		}
		if (after != null) {
			for (Int2ObjectMap.Entry<LayerSection> entry : after.sections().int2ObjectEntrySet()) {
				if (!before.touched.containsKey(entry.getIntKey()) && !before.others.containsKey(entry.getIntKey())) {
					is += entry.getValue().blocks();
					changed += entry.getValue().blocks();
				}
			}
		}
		return new Outside(was, is, changed);
	}

	/**
	 * The game's copy of the column {@code layer} at {@code chunkX}, {@code chunkZ} against what Litematica holds there
	 * ({@code litematica}, by section), over the column's sections from its lowest to its highest with ghost blocks, the
	 * tile's height. Positions inside {@code skip}, other placements' boxes, which draw over the tile, are left out.
	 */
	public static Held held(LayerColumn layer, int chunkX, int chunkZ, BoxCount.Sections litematica,
		List<BoundingBox> skip) {
		int inLayer = 0;
		int inLitematica = 0;
		int differing = 0;
		int lowest = layer.sections().firstIntKey();
		int highest = layer.sections().lastIntKey();
		int x0 = chunkX << LayerSection.SHIFT;
		int z0 = chunkZ << LayerSection.SHIFT;
		for (int height = lowest; height <= highest; height++) {
			LayerSection ghosts = layer.sections().get(height);
			LevelChunkSection held = litematica.at(chunkX, height, chunkZ);
			boolean noneHeld = held == null || held.hasOnlyAir();
			if (ghosts == null && noneHeld) {
				continue;
			}
			int y0 = height << LayerSection.SHIFT;
			BoundingBox section = new BoundingBox(x0, y0, z0, x0 + MASK, y0 + MASK, z0 + MASK);
			List<BoundingBox> cuts = skip.stream().filter(box -> box.intersects(section)).toList();
			if (cuts.isEmpty() && noneHeld) {
				inLayer += ghosts.blocks();
				differing += ghosts.blocks();
				continue;
			}
			for (int index = 0; index < LayerSection.VOLUME; index++) {
				int x = index & MASK;
				int z = index >> LayerSection.SHIFT & MASK;
				int y = index >> 2 * LayerSection.SHIFT;
				if (inside(cuts, x0 + x, y0 + y, z0 + z)) {
					continue;
				}
				BlockState ghost = ghosts == null ? LayerSection.AIR : ghosts.get(index);
				BlockState block = noneHeld ? LayerSection.AIR : held.getBlockState(x, y, z);
				if (block.is(Blocks.STRUCTURE_VOID)) {
					block = LayerSection.AIR;
				}
				inLayer += ghost.isAir() ? 0 : 1;
				inLitematica += block.isAir() ? 0 : 1;
				differing += differ(ghost, block) ? 1 : 0;
			}
		}
		return new Held(inLayer, inLitematica, differing);
	}

	private static boolean inside(List<BoundingBox> boxes, int x, int y, int z) {
		for (BoundingBox box : boxes) {
			if (box.isInside(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	private static boolean differ(BlockState a, BlockState b) {
		return a.isAir() ? !b.isAir() : a != b;
	}

	/**
	 * Litematica's rebuilds of each column, by {@link Layer#key}, as the self-check needs them: whether one that began
	 * at or past a change's mark has finished, and whether one is running or began while the self-check read the
	 * column. A running rebuild swaps an empty chunk in for the column's (it unloads the chunk and loads a new one) and
	 * puts the filled chunk in only as it ends, so the column can be judged only between rebuilds. Rebuilds run on
	 * Litematica's threads and the self-check on the game's, so every method is safe from any thread.
	 */
	public static final class Rebuilds {
		/** A column's rebuilds: how many began, whether one runs and its number, and the newest a finished one had. */
		private static final class Column {
			long began;
			long running = -1;
			long finished = -1;
		}

		private final Long2ObjectOpenHashMap<Column> columns = new Long2ObjectOpenHashMap<>();

		/** A rebuild of the column begins, numbered {@code number}: no task made after that number was made yet. */
		public synchronized void begin(long column, long number) {
			Column state = columns.computeIfAbsent(column, key -> new Column());
			state.began++;
			state.running = number;
		}

		/** The rebuild of the column that began last has ended; {@code finished} unless it failed with an error. */
		public synchronized void end(long column, boolean finished) {
			Column state = columns.get(column);
			if (finished) {
				state.finished = Math.max(state.finished, state.running);
			}
			state.running = -1;
		}

		/**
		 * Whether the column may be judged now: no rebuild of it runs, and, unless {@code anyRebuild}, one that began at
		 * or past {@code mark} has finished. Returns a token for {@link #unchanged}, or -1 when it may not.
		 */
		public synchronized long settled(long column, long mark, boolean anyRebuild) {
			Column state = columns.get(column);
			if (state == null) {
				return anyRebuild ? 0 : -1;
			}
			if (state.running >= 0 || !anyRebuild && state.finished < mark) {
				return -1;
			}
			return state.began;
		}

		/** Whether no rebuild of the column began since {@link #settled} gave {@code token}, so what was read stands. */
		public synchronized boolean unchanged(long column, long token) {
			Column state = columns.get(column);
			return (state == null ? 0 : state.began) == token && (state == null || state.running < 0);
		}
	}

	/** At most one warning a minute for each column. */
	public static final class Limit {
		public static final long INTERVAL_MILLIS = 60_000;
		/** Above this many columns, those whose minute is over are forgotten. */
		private static final int KEPT = 256;

		private final Long2LongOpenHashMap last = new Long2LongOpenHashMap();

		/** Whether a warning for the column ({@link Layer#key}) may go to the log at {@code now}; if so, it counts. */
		public boolean allows(long column, long now) {
			if (last.containsKey(column) && now - last.get(column) < INTERVAL_MILLIS) {
				return false;
			}
			if (last.size() >= KEPT) {
				last.long2LongEntrySet().removeIf(entry -> now - entry.getLongValue() >= INTERVAL_MILLIS);
			}
			last.put(column, now);
			return true;
		}
	}
}
