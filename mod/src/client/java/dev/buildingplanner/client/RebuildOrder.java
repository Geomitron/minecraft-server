package dev.buildingplanner.client;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Hooks L8 and L9: keeps Litematica's rebuilds of each chunk column in order, so the rebuild queued after a change to
 * a tile draws it. Litematica's background threads rebuild a column from its placements' blocks as they are when the
 * rebuild runs, and nothing stops two rebuilds of one column from running at once: an older one that read the tile
 * before a change can finish last and replace the newer one's result, and every rebuild, as it ends, drops the queued
 * rebuilds of its column, even one queued after it read the tile. Either leaves the change undrawn until something
 * else rebuilds the column.
 *
 * <p>Here the rebuilds of one column run one at a time, so the last to run reads the tile after the change; and a
 * running rebuild drops only the queued rebuilds made before it began, whose changes it reads.
 */
public final class RebuildOrder {
	/** The number of the newest task made: Litematica's tasks are numbered in the order they're made. */
	private static final AtomicLong MADE = new AtomicLong();
	/** One lock for the columns whose position hashes to it; two columns may share one, which only orders them. */
	private static final Object[] LOCKS = new Object[64];
	/** On a thread that runs a rebuild, the number of the newest task made before it began. */
	private static final ThreadLocal<Long> RUNNING = new ThreadLocal<>();

	static {
		for (int i = 0; i < LOCKS.length; i++) {
			LOCKS[i] = new Object();
		}
	}

	private RebuildOrder() {
	}

	/** A task of Litematica's placement manager, with its column and its number. */
	public interface Numbered {
		int buildingplanner$chunkX();

		int buildingplanner$chunkZ();

		long buildingplanner$number();
	}

	/** A new task's number. */
	public static long next() {
		return MADE.incrementAndGet();
	}

	/** Runs a rebuild of the column after any other rebuild of it that's running has ended. */
	public static void run(int chunkX, int chunkZ, Runnable rebuild) {
		synchronized (LOCKS[Math.floorMod(31 * chunkX + chunkZ, LOCKS.length)]) {
			RUNNING.set(MADE.get());
			try {
				rebuild.run();
			} finally {
				RUNNING.remove();
			}
		}
	}

	/** Whether this thread runs a rebuild. */
	public static boolean inRebuild() {
		return RUNNING.get() != null;
	}

	/** Whether the rebuild running on this thread may drop the queued task: a task of its column made before it began. */
	public static boolean drops(Object task, int chunkX, int chunkZ) {
		return task instanceof Numbered numbered && numbered.buildingplanner$chunkX() == chunkX
			&& numbered.buildingplanner$chunkZ() == chunkZ && numbered.buildingplanner$number() <= RUNNING.get();
	}
}
