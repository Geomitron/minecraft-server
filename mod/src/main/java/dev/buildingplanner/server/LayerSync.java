package dev.buildingplanner.server;

import dev.buildingplanner.layer.FillRule;
import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.layer.LayerColumn;
import dev.buildingplanner.layer.LayerSection;
import dev.buildingplanner.sync.LayerChange;
import dev.buildingplanner.sync.Payloads;
import dev.buildingplanner.sync.PlayerMessage;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The server's shared layer: each dimension's ghost blocks in memory, the columns each player watches, the order of
 * edits, and each player's undo.
 *
 * <p>Every method runs on the server's main thread, one call at a time, so edits apply in the order they arrive. Each
 * applied edit goes, through the {@link Outbox}, to every player who watches a column it touches, its author included.
 * Each block takes the value of the last edit, in this order, that wrote it; there are no locks and no owners.
 * Messages to one player keep their order, so nothing falls between a column's snapshot and the edits after it.
 *
 * <p>Ghost blocks real blocks fill ({@link FillRule}) leave the layer: hook S1 and the check at chunk load note them,
 * and the end of each server tick removes them as an edit by no player. Edits, undo and redo don't write a ghost block
 * over a real block that fills it.
 */
public final class LayerSync {
	/** How many chunks past the server's view distance a watched column may be. */
	static final int WATCH_MARGIN = 2;
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();

	/** Sends a payload to one player; the Fabric adapter sends it on that player's connection, in call order. */
	@FunctionalInterface
	public interface Outbox {
		void send(UUID player, CustomPacketPayload payload);
	}

	/** What the layer needs to know of the running game. */
	public interface World {
		/** The player's dimension and the column they stand in; empty when they aren't on the server. */
		Optional<Place> place(UUID player);

		/** The server's view distance, in chunks. */
		int viewDistance();

		/** Whether the y is within the dimension's build height. */
		boolean inBuildHeight(Identifier dimension, int y);

		/** Whether the position is inside the dimension's world border. */
		boolean insideBorder(Identifier dimension, BlockPos pos);

		/** The real block at the position, or null when the server hasn't loaded its chunk; never loads one. */
		BlockState realBlock(Identifier dimension, BlockPos pos);
	}

	/** One loaded chunk's real blocks, for the check at chunk load. */
	@FunctionalInterface
	public interface RealChunk {
		/** The section at that section y, or null when it holds only air (or lies outside the build height). */
		RealSection section(int sectionY);
	}

	/** One section's real blocks, by their coordinates within the section. */
	@FunctionalInterface
	public interface RealSection {
		BlockState get(int x, int y, int z);
	}

	/** Where a player is: their dimension and the column they stand in. */
	public record Place(Identifier dimension, ChunkPos column) {
	}

	private final int maxEditBlocks;
	private final Outbox outbox;
	private final World world;
	private final Map<Identifier, Layer> layers;
	/** Each player's watched columns, by {@link Layer#key}, all in the dimension they were in when they watched. */
	private final Map<UUID, Watched> watched = new HashMap<>();
	private final UndoHistory<Step> history = new UndoHistory<>();
	/** Positions, by {@link BlockPos#asLong}, whose ghost block a real block filled this tick, by dimension. */
	private final Map<Identifier, LongSet> filled = new LinkedHashMap<>();

	/** {@code layers} are the layers read from the files, by dimension; the sync owns them from here. */
	public LayerSync(int maxEditBlocks, Map<Identifier, Layer> layers, Outbox outbox, World world) {
		this.maxEditBlocks = maxEditBlocks;
		this.layers = new HashMap<>(layers);
		this.outbox = outbox;
		this.world = world;
	}

	/** Every dimension's layer, for saving. */
	public Map<Identifier, Layer> layers() {
		return Collections.unmodifiableMap(layers);
	}

	/** A dimension's ghost block at a position, air for none. */
	public BlockState get(Identifier dimension, BlockPos pos) {
		Layer layer = layers.get(dimension);
		return layer == null ? AIR : layer.get(pos);
	}

	/**
	 * Answers with one {@code columns} message holding a snapshot of each column asked for, and from then sends the
	 * player every edit that touches one. A watch of another dimension than the player's, and each column farther from
	 * them than the view distance plus {@value #WATCH_MARGIN} chunks, is ignored. Watching a new dimension drops the
	 * old one's columns.
	 */
	public void watch(UUID player, Identifier dimension, List<ChunkPos> columns) {
		Optional<Place> place = world.place(player);
		if (place.isEmpty() || !place.get().dimension().equals(dimension)) {
			return;
		}
		ChunkPos at = place.get().column();
		int range = world.viewDistance() + WATCH_MARGIN;
		Watched watching = watched.get(player);
		if (watching == null || !watching.dimension.equals(dimension)) {
			watching = new Watched(dimension);
			watched.put(player, watching);
		}
		LongSet asked = new LongLinkedOpenHashSet();
		for (ChunkPos column : columns) {
			if (Math.max(Math.abs(column.x() - at.x()), Math.abs(column.z() - at.z())) <= range) {
				asked.add(Layer.key(column.x(), column.z()));
			}
		}
		if (asked.isEmpty()) {
			return;
		}
		Layer layer = layers.get(dimension);
		List<Payloads.Column> snapshots = new ArrayList<>(asked.size());
		for (long key : asked) {
			watching.columns.add(key);
			LayerColumn column = layer == null ? null : layer.column(Layer.keyX(key), Layer.keyZ(key));
			snapshots.add(new Payloads.Column(new ChunkPos(Layer.keyX(key), Layer.keyZ(key)),
				column == null ? List.of() : column.pack()));
		}
		outbox.send(player, new Payloads.Columns(dimension, snapshots));
	}

	/** The player's game unloaded the columns: the server stops sending their edits. */
	public void unwatch(UUID player, Identifier dimension, List<ChunkPos> columns) {
		Watched watching = watched.get(player);
		if (watching != null && watching.dimension.equals(dimension)) {
			columns.forEach(column -> watching.columns.remove(Layer.key(column.x(), column.z())));
		}
	}

	/** The player changed dimension: their game dropped every column it watched. */
	public void changedDimension(UUID player) {
		watched.remove(player);
	}

	/** The player left. Their undo steps stay until the server stops. */
	public void disconnect(UUID player) {
		watched.remove(player);
	}

	/**
	 * Checks and applies one edit: the dimension is the player's own, the edit is within the block cap, and every
	 * position is in a column the player watches, within the build height and inside the world border. A failed check
	 * changes nothing and answers the player with {@code rejected}. A ghost block a real block already fills isn't
	 * written. An edit that changed a block goes on the player's undo list and clears their redo list.
	 */
	public void edit(UUID player, Identifier dimension, int clientSeq, List<LayerChange> changes) {
		Optional<PlayerMessage> problem = check(player, dimension, changes);
		if (problem.isPresent()) {
			outbox.send(player, new Payloads.Rejected(clientSeq, problem.get()));
			return;
		}
		Layer layer = layers.computeIfAbsent(dimension, key -> new Layer());
		List<LayerChange> applied = new ArrayList<>(changes.size());
		// Each block's state before the edit (its first write) and after it (its last).
		Map<BlockPos, StepBlock> step = new LinkedHashMap<>();
		for (LayerChange change : changes) {
			BlockState state = change.state().isAir() ? AIR : change.state();
			if (builtOver(dimension, change.pos(), state)) {
				continue;
			}
			BlockState before = layer.set(change.pos(), state);
			if (before != state) {
				applied.add(new LayerChange(change.pos(), state));
				step.merge(change.pos(), new StepBlock(change.pos(), before, state),
					(first, last) -> new StepBlock(first.pos(), first.before(), last.after()));
			}
		}
		Step done = new Step(dimension, step.values().stream().filter(block -> block.before() != block.after()).toList());
		if (!done.isEmpty()) {
			history.record(player, done);
		}
		Payloads.Applied message = new Payloads.Applied(dimension, player, clientSeq, applied);
		if (!broadcast(dimension, applied, message).contains(player)) {
			outbox.send(player, message);
		}
	}

	/**
	 * Takes back the player's newest step, in whichever dimension they made it: each block goes back to its state
	 * before the step only if the layer still holds the step's state there. Blocks someone has changed since stay as
	 * they are, and a notice says how many. The undo goes to watchers as an ordinary {@code applied} with no client
	 * sequence; one that changes no block sends none.
	 */
	public void undo(UUID player) {
		Optional<Step> step = history.takeUndo(player);
		if (step.isEmpty()) {
			outbox.send(player, new Payloads.Notice(PlayerMessage.nothingToUndo()));
			return;
		}
		history.undone(player, applyStep(player, step.get().reversed()).reversed());
	}

	/** Takes the player's newest undone step forward again, by the same rule as {@link #undo}. */
	public void redo(UUID player) {
		Optional<Step> step = history.takeRedo(player);
		if (step.isEmpty()) {
			outbox.send(player, new Payloads.Notice(PlayerMessage.nothingToRedo()));
			return;
		}
		history.redone(player, applyStep(player, step.get()));
	}

	/**
	 * Hook S1: a real block changed in a loaded chunk. When it fills the ghost block there, the position waits for
	 * {@link #removeFilled} at the end of the tick. One map lookup for a dimension without ghost blocks, and one for a
	 * column without any.
	 */
	public void realChanged(Identifier dimension, BlockPos pos, BlockState real) {
		if (real.isAir()) {
			return;
		}
		Layer layer = layers.get(dimension);
		if (layer != null && FillRule.fills(real, layer.get(pos))) {
			filled.computeIfAbsent(dimension, key -> new LongLinkedOpenHashSet()).add(pos.asLong());
		}
	}

	/**
	 * A chunk loaded: each of the column's sections that holds ghost blocks is compared with the chunk's real blocks,
	 * and the filled positions wait for {@link #removeFilled}, as S1's do.
	 */
	public void chunkLoaded(Identifier dimension, int chunkX, int chunkZ, RealChunk real) {
		Layer layer = layers.get(dimension);
		LayerColumn column = layer == null ? null : layer.column(chunkX, chunkZ);
		if (column == null) {
			return;
		}
		column.sections().forEach((sectionY, section) -> {
			RealSection blocks = real.section(sectionY);
			if (blocks == null) {
				return;
			}
			for (int y = 0; y < LayerSection.SIDE; y++) {
				for (int z = 0; z < LayerSection.SIDE; z++) {
					for (int x = 0; x < LayerSection.SIDE; x++) {
						BlockState ghost = section.get(LayerSection.index(x, y, z));
						if (!ghost.isAir() && FillRule.fills(blocks.get(x, y, z), ghost)) {
							filled.computeIfAbsent(dimension, key -> new LongLinkedOpenHashSet()).add(BlockPos.asLong(
								(chunkX << LayerSection.SHIFT) + x, (sectionY << LayerSection.SHIFT) + y,
								(chunkZ << LayerSection.SHIFT) + z));
						}
					}
				}
			}
		});
	}

	/**
	 * At the end of the server tick: removes the ghost blocks noted since the last call that real blocks still fill, as
	 * one edit for each dimension, split at the block cap. The edit belongs to no player, so no undo list gains a step,
	 * and it goes to each watcher as an {@code applied} by {@link Payloads#FILLED_AUTHOR} with no client sequence.
	 */
	public void removeFilled() {
		if (filled.isEmpty()) {
			return;
		}
		filled.forEach((dimension, positions) -> {
			Layer layer = layers.get(dimension);
			List<LayerChange> removed = new ArrayList<>();
			positions.forEach((long packed) -> {
				BlockPos pos = BlockPos.of(packed);
				if (builtOver(dimension, pos, layer.get(pos))) {
					layer.set(pos, AIR);
					removed.add(new LayerChange(pos, AIR));
				}
			});
			for (int from = 0; from < removed.size(); from += maxEditBlocks) {
				List<LayerChange> part = removed.subList(from, Math.min(removed.size(), from + maxEditBlocks));
				broadcast(dimension, part, new Payloads.Applied(dimension, Payloads.FILLED_AUTHOR, Payloads.NO_CLIENT_SEQ,
					part));
			}
		});
		filled.clear();
	}

	/** Whether the server has the position's chunk loaded and its real block fills the ghost block. */
	private boolean builtOver(Identifier dimension, BlockPos pos, BlockState ghost) {
		if (ghost.isAir()) {
			return false;
		}
		BlockState real = world.realBlock(dimension, pos);
		return real != null && FillRule.fills(real, ghost);
	}

	private Optional<PlayerMessage> check(UUID player, Identifier dimension, List<LayerChange> changes) {
		Optional<Place> place = world.place(player);
		if (place.isEmpty() || !place.get().dimension().equals(dimension)) {
			return Optional.of(PlayerMessage.otherDimension());
		}
		if (changes.size() > maxEditBlocks) {
			return Optional.of(PlayerMessage.tooManyBlocks(changes.size(), maxEditBlocks));
		}
		Watched watching = watched.get(player);
		for (LayerChange change : changes) {
			BlockPos pos = change.pos();
			if (watching == null || !watching.dimension.equals(dimension) || !watching.columns.contains(columnKey(pos))) {
				return Optional.of(PlayerMessage.notLoaded());
			}
			if (!world.inBuildHeight(dimension, pos.getY())) {
				return Optional.of(PlayerMessage.outsideBuildHeight());
			}
			if (!world.insideBorder(dimension, pos)) {
				return Optional.of(PlayerMessage.outsideWorldBorder());
			}
		}
		return Optional.empty();
	}

	/**
	 * Sets each block from its before-state to its after-state where the layer still holds the before-state, except
	 * where a real block already fills the after-state. The notice counts the blocks left as they are, leaving out those
	 * whose real block fills the before-state: built since, not changed by another player.
	 */
	private Step applyStep(UUID player, Step step) {
		Identifier dimension = step.dimension();
		Layer layer = layers.computeIfAbsent(dimension, key -> new Layer());
		List<StepBlock> done = new ArrayList<>(step.blocks().size());
		int left = 0;
		for (StepBlock block : step.blocks()) {
			if (layer.get(block.pos()) != block.before()) {
				if (!builtOver(dimension, block.pos(), block.before())) {
					left++;
				}
			} else if (!builtOver(dimension, block.pos(), block.after())) {
				layer.set(block.pos(), block.after());
				done.add(block);
			}
		}
		if (!done.isEmpty()) {
			List<LayerChange> changes = done.stream().map(block -> new LayerChange(block.pos(), block.after())).toList();
			broadcast(dimension, changes, new Payloads.Applied(dimension, player, Payloads.NO_CLIENT_SEQ, changes));
		}
		if (left > 0) {
			outbox.send(player, new Payloads.Notice(PlayerMessage.blocksLeftAsTheyAre(left)));
		}
		return new Step(step.dimension(), done);
	}

	/** Sends the message to every player who watches a column the changes touch, and returns who got it. */
	private List<UUID> broadcast(Identifier dimension, List<LayerChange> changes, CustomPacketPayload message) {
		LongSet touched = new LongOpenHashSet();
		for (LayerChange change : changes) {
			touched.add(columnKey(change.pos()));
		}
		List<UUID> sent = new ArrayList<>();
		watched.forEach((player, watching) -> {
			if (watching.dimension.equals(dimension) && touched.longStream().anyMatch(watching.columns::contains)) {
				outbox.send(player, message);
				sent.add(player);
			}
		});
		return sent;
	}

	private static long columnKey(BlockPos pos) {
		return Layer.key(pos.getX() >> LayerSection.SHIFT, pos.getZ() >> LayerSection.SHIFT);
	}

	private static final class Watched {
		final Identifier dimension;
		final LongSet columns = new LongOpenHashSet();

		Watched(Identifier dimension) {
			this.dimension = dimension;
		}
	}

	/** One block of a step: its world position and its state before and after. */
	private record StepBlock(BlockPos pos, BlockState before, BlockState after) {
		StepBlock reversed() {
			return new StepBlock(pos, after, before);
		}
	}

	/** One edit by one player, in one dimension. */
	private record Step(Identifier dimension, List<StepBlock> blocks) implements UndoHistory.Step {
		Step {
			blocks = List.copyOf(blocks);
		}

		Step reversed() {
			return new Step(dimension, blocks.stream().map(StepBlock::reversed).toList());
		}

		@Override
		public boolean isEmpty() {
			return blocks.isEmpty();
		}

		@Override
		public int blockCount() {
			return blocks.size();
		}
	}
}
