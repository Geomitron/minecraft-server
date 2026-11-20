package dev.buildingplanner.sync;

import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.layer.LayerColumn;
import dev.buildingplanner.layer.LayerSection;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;

/**
 * A player's game's copy of the layer: the columns of its dimension whose chunks it has loaded, kept equal to the
 * server's. The game says which chunks load and unload; once a tick, {@link #takeMessages} gives the {@code watch} and
 * {@code unwatch} to send. A column is held from its snapshot (in {@code columns}) until its chunk unloads or the
 * level changes; edits ({@code applied}) change only held columns, since until a column's snapshot arrives the
 * snapshot holds them. The {@link Listener} hears of every change to a held column, for drawing.
 *
 * <p>Not thread-safe: the game's main thread uses it, which is where its payload handlers run.
 */
public final class LayerReplica {
	/** Hears of each change to the columns held. */
	@FunctionalInterface
	public interface Listener {
		/**
		 * A column is about to change by an edit or a snapshot: {@code column} is what it holds now (null for none), and
		 * {@code changing} the edit's positions in it, or null for a snapshot. A column dropped hears of no change to come.
		 */
		default void columnChanging(int chunkX, int chunkZ, LayerColumn column, List<BlockPos> changing) {
		}

		/**
		 * A column changed: {@code column} is what it holds now (null for no ghost block or no longer held), and
		 * {@code changed} the positions of its blocks that changed, or null when the whole column was replaced.
		 */
		void columnChanged(int chunkX, int chunkZ, LayerColumn column, List<BlockPos> changed);
	}

	private final Listener listener;
	private Identifier dimension;
	private Layer layer = new Layer();
	/** Columns asked for, or about to be, and not unwatched since, by {@link Layer#key}. */
	private final LongSet watched = new LongOpenHashSet();
	/** Columns whose snapshot arrived. */
	private final LongSet held = new LongOpenHashSet();
	private final LongSet toWatch = new LongLinkedOpenHashSet();
	private final LongSet toUnwatch = new LongLinkedOpenHashSet();
	/** An {@code unwatch} of every column the previous level watched, until it's sent. */
	private final List<CustomPacketPayload> leftLevel = new ArrayList<>();

	/** {@code dimension} is the game's dimension now, or null before it has a level. */
	public LayerReplica(Identifier dimension, Listener listener) {
		this.dimension = dimension;
		this.listener = listener;
	}

	public Identifier dimension() {
		return dimension;
	}

	/** The ghost blocks of the columns held. */
	public Layer layer() {
		return layer;
	}

	public boolean holds(int chunkX, int chunkZ) {
		return held.contains(Layer.key(chunkX, chunkZ));
	}

	/** The columns held, by {@link Layer#key}. */
	public LongSet heldColumns() {
		return LongSets.unmodifiable(held);
	}

	/** The game loaded a chunk of its level: the next messages ask for its column. */
	public void chunkLoaded(int chunkX, int chunkZ) {
		long key = Layer.key(chunkX, chunkZ);
		// Asked again even while the server still watches it: the column was dropped when the chunk unloaded.
		toUnwatch.remove(key);
		toWatch.add(key);
		watched.add(key);
	}

	/** The game unloaded a chunk: its column is dropped, and the next messages unwatch it. */
	public void chunkUnloaded(int chunkX, int chunkZ) {
		long key = Layer.key(chunkX, chunkZ);
		if (!watched.remove(key)) {
			return;
		}
		if (!toWatch.remove(key)) {
			toUnwatch.add(key);
		}
		drop(key);
	}

	/**
	 * The game's level changed (another dimension, or a respawn): every column is dropped, and the next messages
	 * unwatch them in the old dimension, where the server may still watch them.
	 */
	public void levelChanged(Identifier newDimension) {
		LongSet old = new LongOpenHashSet(watched);
		old.removeAll(toWatch);
		if (dimension != null && !old.isEmpty()) {
			leftLevel.add(new Payloads.Unwatch(dimension, positions(old)));
		}
		for (long key : held) {
			listener.columnChanged(Layer.keyX(key), Layer.keyZ(key), null, null);
		}
		dimension = newDimension;
		layer = new Layer();
		watched.clear();
		held.clear();
		toWatch.clear();
		toUnwatch.clear();
	}

	/** The messages to send now, in order: the unwatches, then one watch of the columns loaded since the last call. */
	public List<CustomPacketPayload> takeMessages() {
		List<CustomPacketPayload> messages = new ArrayList<>(leftLevel);
		leftLevel.clear();
		if (dimension != null) {
			if (!toUnwatch.isEmpty()) {
				messages.add(new Payloads.Unwatch(dimension, positions(toUnwatch)));
			}
			if (!toWatch.isEmpty()) {
				messages.add(new Payloads.Watch(dimension, positions(toWatch)));
			}
		}
		toUnwatch.clear();
		toWatch.clear();
		return messages;
	}

	/** The server's snapshots: each replaces what the game held for that column. Columns no longer watched are skipped. */
	public void receive(Payloads.Columns columns) {
		if (!columns.dimension().equals(dimension)) {
			return;
		}
		for (Payloads.Column column : columns.columns()) {
			int x = column.pos().x();
			int z = column.pos().z();
			long key = Layer.key(x, z);
			if (watched.contains(key)) {
				listener.columnChanging(x, z, layer.column(x, z), null);
				layer.replaceColumn(x, z, column.sections());
				held.add(key);
				listener.columnChanged(x, z, layer.column(x, z), null);
			}
		}
	}

	/** An edit the server applied: its changes to the columns held go into the copy. Returns those changes. */
	public List<LayerChange> receive(Payloads.Applied applied) {
		if (!applied.dimension().equals(dimension)) {
			return List.of();
		}
		List<LayerChange> done = new ArrayList<>(applied.changes().size());
		Long2ObjectLinkedOpenHashMap<List<BlockPos>> byColumn = new Long2ObjectLinkedOpenHashMap<>();
		for (LayerChange change : applied.changes()) {
			BlockPos pos = change.pos();
			long key = Layer.key(pos.getX() >> LayerSection.SHIFT, pos.getZ() >> LayerSection.SHIFT);
			if (held.contains(key)) {
				byColumn.computeIfAbsent(key, k -> new ArrayList<>()).add(pos);
				done.add(change);
			}
		}
		byColumn.forEach((key, positions) ->
			listener.columnChanging(Layer.keyX(key), Layer.keyZ(key), layer.column(Layer.keyX(key), Layer.keyZ(key)),
				positions));
		for (LayerChange change : done) {
			layer.set(change.pos(), change.state());
		}
		byColumn.forEach((key, positions) -> {
			int x = Layer.keyX(key);
			int z = Layer.keyZ(key);
			listener.columnChanged(x, z, layer.column(x, z), positions);
		});
		layer.takeChangedAreas();
		return done;
	}

	private void drop(long key) {
		if (held.remove(key)) {
			int x = Layer.keyX(key);
			int z = Layer.keyZ(key);
			layer.replaceColumn(x, z, List.of());
			listener.columnChanged(x, z, null, null);
		}
	}

	private static List<ChunkPos> positions(LongSet keys) {
		List<ChunkPos> positions = new ArrayList<>(keys.size());
		for (long key : keys) {
			positions.add(new ChunkPos(Layer.keyX(key), Layer.keyZ(key)));
		}
		return positions;
	}
}
