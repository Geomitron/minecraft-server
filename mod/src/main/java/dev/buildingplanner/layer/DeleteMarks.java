package dev.buildingplanner.layer;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The ghost blocks marked in Delete (see "Delete"): while the sprint key is held, a sweep of the aim marks each ghost
 * block it passes over, and a box marks every ghost block in it; or, when the hold's first ghost block (the sweep's
 * first, or the box's first corner) is marked, the hold unmarks them instead. Only the ghost block itself is marked,
 * not the other half of a two-block ghost block, which the erase takes with it ({@link OtherHalf}); the marks stop at
 * the server's cap for one edit, counting those other halves. A mark drops when its ghost block leaves the layer
 * ({@link #retain}).
 */
public final class DeleteMarks {
	/** The most a sweep's aimed points are apart, in blocks, so a quick sweep leaves no gap. */
	public static final double STEP = 0.5;
	/** The most aims a sweep takes in a frame. */
	public static final int MAX_STEPS = 64;
	/** The most changed positions kept for {@link #takeChanged}; past it, all count as changed. */
	private static final int MOST_CHANGES = 1 << 16;

	/** Where an aim starts and the way it looks, a unit vector. */
	public record View(Vec3 eye, Vec3 look) {
	}

	/**
	 * What an aim meets first: the ghost block it meets, or null when a real block comes first or it meets nothing, and
	 * how far from the eyes it met it, or its reach when it met nothing.
	 */
	public record Hit(BlockPos ghost, double distance) {
	}

	/** Aims from an eye along a look. */
	public interface Tracer {
		Hit trace(Vec3 eye, Vec3 look);
	}

	private final int cap;
	private final LongOpenHashSet marks = new LongOpenHashSet();
	/** Each position the erase would take, by {@link BlockPos#asLong}, with how many marks take it. */
	private final Long2IntOpenHashMap erased = new Long2IntOpenHashMap();
	/** The other half each mark of a two-block ghost block takes with it. */
	private final Long2LongOpenHashMap halves = new Long2LongOpenHashMap();
	private boolean sweeping;
	/** Whether the sweep under way unmarks; null until it meets its first ghost block. */
	private Boolean unmarking;
	private long version;
	/** The positions marked or unmarked since the last {@link #takeChanged}, unless all count as changed. */
	private final LongOpenHashSet changed = new LongOpenHashSet();
	private boolean allChanged = true;

	/** {@code cap} is the server's cap on the blocks of one edit, which the erase of the marks may not pass. */
	public DeleteMarks(int cap) {
		this.cap = cap;
	}

	/**
	 * The ghost blocks a frame of a sweep passes over, in order: from the view {@code from} at the last frame (null at
	 * the sweep's first) to {@code to} at this one, aiming along the look directions between them, from eyes between
	 * the two, in as many steps as keep the aimed points at most {@link #STEP} apart at the farther of the two aims'
	 * distances, up to {@link #MAX_STEPS}. The first frame aims once.
	 */
	public static List<BlockPos> swept(View from, View to, Tracer tracer) {
		Hit end = tracer.trace(to.eye(), to.look());
		List<BlockPos> ghosts = new ArrayList<>();
		if (from == null) {
			if (end.ghost() != null) {
				ghosts.add(end.ghost());
			}
			return ghosts;
		}
		Hit start = tracer.trace(from.eye(), from.look());
		double reach = Math.max(start.distance(), end.distance());
		Vec3 first = from.eye().add(from.look().scale(reach));
		Vec3 last = to.eye().add(to.look().scale(reach));
		int steps = (int) Math.min(MAX_STEPS, Math.max(1, Math.ceil(first.distanceTo(last) / STEP)));
		for (int i = 1; i <= steps; i++) {
			Hit hit;
			if (i == steps) {
				hit = end;
			} else {
				double t = (double) i / steps;
				Vec3 look = from.look().lerp(to.look(), t);
				hit = tracer.trace(from.eye().lerp(to.eye(), t), look.lengthSqr() < 1.0E-8 ? to.look() : look.normalize());
			}
			if (hit.ghost() != null) {
				ghosts.add(hit.ghost());
			}
		}
		return ghosts;
	}

	/**
	 * The sprint key went down: a hold starts, which decides at its first ghost block whether it marks or unmarks.
	 */
	public void startSweep() {
		sweeping = true;
		unmarking = null;
	}

	/** The sprint key came up, or the hold can't go on: the next hold decides again. */
	public void endSweep() {
		sweeping = false;
		unmarking = null;
	}

	/** Whether a hold of the sprint key is under way. */
	public boolean sweeping() {
		return sweeping;
	}

	/** Whether the hold under way unmarks what it passes over and its boxes take in. */
	public boolean unmarking() {
		return Boolean.TRUE.equals(unmarking);
	}

	/** A frame of the sweep under way: marks or unmarks each ghost block it passes over ({@link #swept}). */
	public void sweep(View from, View to, Tracer tracer, Layer layer) {
		for (BlockPos ghost : swept(from, to, tracer)) {
			pass(ghost, layer);
		}
	}

	/**
	 * The sweep under way passes over the ghost block at {@code pos}: the first it meets decides whether it marks or
	 * unmarks; then a mark is added, unless the erase would pass the cap, or taken away.
	 */
	public void pass(BlockPos pos, Layer layer) {
		long key = pos.asLong();
		if (unmarking == null) {
			unmarking = marks.contains(key);
		}
		if (unmarking) {
			unmark(key);
		} else {
			mark(pos, layer);
		}
	}

	/**
	 * A box's first corner, at {@code corner}, during the hold under way (starting one if none is): the hold decides
	 * there whether it marks or unmarks, unless its sweep has already met a ghost block. Returns whether the box
	 * unmarks.
	 */
	public boolean startBox(BlockPos corner) {
		sweeping = true;
		if (unmarking == null) {
			unmarking = marks.contains(corner.asLong());
		}
		return unmarking;
	}

	/**
	 * A box from {@code min} to {@code max} let go: unmarks each mark in it when {@code unmark}, and otherwise marks every
	 * ghost block in it, or none of them when that would take the erase past the cap, other halves counted. Returns
	 * whether it marked or unmarked.
	 */
	public boolean box(BlockPos min, BlockPos max, Layer layer, boolean unmark) {
		if (unmark) {
			List<Long> inBox = new ArrayList<>();
			for (LongIterator it = marks.iterator(); it.hasNext();) {
				long key = it.nextLong();
				if (inside(key, min, max)) {
					inBox.add(key);
				}
			}
			inBox.forEach(this::unmark);
			return true;
		}
		List<long[]> adding = new ArrayList<>();
		LongOpenHashSet more = new LongOpenHashSet();
		layer.forEachIn(min, max, (pos, ghost) -> {
			long key = pos.asLong();
			if (marks.contains(key)) {
				return;
			}
			long other = otherHalf(pos, ghost, layer);
			adding.add(new long[] {key, other});
			if (!erased.containsKey(key)) {
				more.add(key);
			}
			if (!erased.containsKey(other)) {
				more.add(other);
			}
		});
		if (erased.size() + more.size() > cap) {
			return false;
		}
		for (long[] mark : adding) {
			add(mark[0], mark[1]);
			changed(mark[0]);
		}
		if (!adding.isEmpty()) {
			version++;
		}
		return true;
	}

	/**
	 * Whether a box from {@code min} to {@code max} may mark its ghost blocks within the cap, for its colour while it's
	 * drawn: its ghost blocks, less the positions already to erase in it, and the positions already to erase, come to
	 * no more than the cap. Other halves outside the box count only when it's let go ({@link #box}).
	 */
	public boolean fits(Layer layer, BlockPos min, BlockPos max) {
		int inBox = 0;
		for (LongIterator it = erased.keySet().iterator(); it.hasNext();) {
			if (inside(it.nextLong(), min, max)) {
				inBox++;
			}
		}
		return !GhostErase.overCap(layer, min, max, cap - erased.size() + inBox);
	}

	private static boolean inside(long key, BlockPos min, BlockPos max) {
		int x = BlockPos.getX(key);
		int y = BlockPos.getY(key);
		int z = BlockPos.getZ(key);
		return x >= min.getX() && x <= max.getX() && y >= min.getY() && y <= max.getY() && z >= min.getZ()
			&& z <= max.getZ();
	}

	private void mark(BlockPos pos, Layer layer) {
		long key = pos.asLong();
		if (marks.contains(key)) {
			return;
		}
		BlockState ghost = layer.get(pos);
		if (ghost.isAir()) {
			return;
		}
		long other = otherHalf(pos, ghost, layer);
		int adding = (erased.containsKey(key) ? 0 : 1) + (other != key && !erased.containsKey(other) ? 1 : 0);
		if (erased.size() + adding > cap) {
			return;
		}
		add(key, other);
		changed(key);
		version++;
	}

	private void add(long key, long other) {
		marks.add(key);
		erased.addTo(key, 1);
		if (other != key) {
			halves.put(key, other);
			erased.addTo(other, 1);
		}
	}

	private void unmark(long key) {
		if (!marks.remove(key)) {
			return;
		}
		release(key);
		if (halves.containsKey(key)) {
			release(halves.remove(key));
		}
		changed(key);
		version++;
	}

	private void changed(long key) {
		if (allChanged) {
			return;
		}
		changed.add(key);
		if (changed.size() > MOST_CHANGES) {
			allChanged = true;
			changed.clear();
		}
	}

	private void release(long key) {
		if (erased.addTo(key, -1) <= 1) {
			erased.remove(key);
		}
	}

	/** The other half the erase takes with the ghost block at {@code pos}, or {@code pos} itself for a block of one. */
	private static long otherHalf(BlockPos pos, BlockState ghost, Layer layer) {
		BlockPos other = OtherHalf.of(pos, ghost);
		return other != null && OtherHalf.matches(ghost, layer.get(other)) ? other.asLong() : pos.asLong();
	}

	/**
	 * Drops each mark whose ghost block has left {@code layer}, for any reason (another player's edit, an undo, a real
	 * block filling it, its column unloading), and counts the other halves the erase would take again.
	 */
	public void retain(Layer layer) {
		LongOpenHashSet kept = new LongOpenHashSet(marks.size());
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (LongIterator it = marks.iterator(); it.hasNext();) {
			long key = it.nextLong();
			if (!layer.get(pos.set(key)).isAir()) {
				kept.add(key);
			} else {
				changed(key);
			}
		}
		int marked = marks.size();
		int erasing = erased.size();
		marks.clear();
		erased.clear();
		halves.clear();
		for (LongIterator it = kept.iterator(); it.hasNext();) {
			long key = it.nextLong();
			BlockPos at = BlockPos.of(key);
			add(key, otherHalf(at, layer.get(at), layer));
		}
		if (marks.size() != marked || erased.size() != erasing) {
			version++;
		}
	}

	/** Takes away every mark. */
	public void clear() {
		if (!marks.isEmpty()) {
			marks.clear();
			erased.clear();
			halves.clear();
			changed.clear();
			allChanged = true;
			version++;
		}
	}

	/**
	 * The positions marked or unmarked since the last call, by {@link BlockPos#asLong}, for a reader that redraws only
	 * what changed; or null when every mark is to count as changed (the first call, after {@link #clear}, or after more
	 * changes than are kept), and the reader starts again from {@link #positions}.
	 */
	public LongSet takeChanged() {
		if (allChanged) {
			allChanged = false;
			changed.clear();
			return null;
		}
		LongOpenHashSet taken = new LongOpenHashSet(changed);
		changed.clear();
		return taken;
	}

	/** Whether the ghost block at {@code pos} is marked. */
	public boolean contains(BlockPos pos) {
		return marks.contains(pos.asLong());
	}

	/** The marked positions, by {@link BlockPos#asLong}. */
	public LongSet positions() {
		return LongSets.unmodifiable(marks);
	}

	/** How many ghost blocks are marked. */
	public int size() {
		return marks.size();
	}

	/** How many positions the erase of the marks would take: the marks and the other halves they take with them. */
	public int eraseSize() {
		return erased.size();
	}

	/** Whether the marks have reached the cap, so a sweep marks nothing more. */
	public boolean full() {
		return erased.size() >= cap;
	}

	/** A number that changes whenever the marks do, so a reader can tell whether what it worked out still holds. */
	public long version() {
		return version;
	}
}
