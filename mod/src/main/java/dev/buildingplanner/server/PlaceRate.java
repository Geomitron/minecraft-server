package dev.buildingplanner.server;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The real blocks each player's shapes and pastes placed in the last second, for the cap a second (see "Place real
 * blocks", "The cap"): a {@code place} goes in only while the blocks it would place and those placed in the 1,000 ms
 * before it come to at most the cap. Read and written on the server thread.
 */
final class PlaceRate {
	static final long WINDOW_MILLIS = 1_000;

	private final int max;
	private final Map<UUID, Deque<Placed>> placed = new HashMap<>();

	PlaceRate(int max) {
		this.max = max;
	}

	private record Placed(long at, int blocks) {
	}

	/** Whether {@code blocks} more real blocks at {@code now} keep the player within the cap for the second before. */
	boolean allows(UUID player, int blocks, long now) {
		Deque<Placed> recent = placed.get(player);
		long total = blocks;
		if (recent != null) {
			drop(recent, now);
			for (Placed p : recent) {
				total += p.blocks;
			}
		}
		return total <= max;
	}

	/** The player's {@code place} put in {@code blocks} real blocks, finishing at {@code now}. */
	void placed(UUID player, int blocks, long now) {
		if (blocks <= 0) {
			return;
		}
		Deque<Placed> recent = placed.computeIfAbsent(player, id -> new ArrayDeque<>());
		drop(recent, now);
		recent.addLast(new Placed(now, blocks));
	}

	/** The player left: their count goes. */
	void left(UUID player) {
		placed.remove(player);
	}

	private static void drop(Deque<Placed> recent, long now) {
		while (!recent.isEmpty() && recent.peekFirst().at <= now - WINDOW_MILLIS) {
			recent.removeFirst();
		}
	}
}
