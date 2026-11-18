package dev.buildingplanner.server;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Each player's undo steps, newest first, kept in memory until the server stops: the newest {@value #DEPTH} for each
 * player, and fewer while they hold more than {@value #BLOCK_BUDGET} blocks in all. An undone step waits on a redo list
 * until the player's next new edit clears it.
 */
final class UndoHistory<S extends UndoHistory.Step> {
	/** Effortless Building's own undo depth. */
	static final int DEPTH = 50;
	/**
	 * The most blocks a player's steps hold in all, about 128 MB of the server's memory at 64 bytes a block. The redo
	 * list needs no budget of its own: it only holds steps taken from the undo list, which a new edit clears.
	 */
	static final int BLOCK_BUDGET = 2_000_000;

	/** One edit by one player, with each block's state before and after it. */
	interface Step {
		/** True when every block has been changed by someone else since, so nothing is left to take back or forward. */
		boolean isEmpty();

		/** How many blocks the step holds, which count against the player's budget. */
		int blockCount();
	}

	private final int blockBudget;
	private final Map<UUID, Deque<S>> undoSteps = new HashMap<>();
	private final Map<UUID, Deque<S>> redoSteps = new HashMap<>();

	UndoHistory() {
		this(BLOCK_BUDGET);
	}

	UndoHistory(int blockBudget) {
		this.blockBudget = blockBudget;
	}

	/** A new edit: its step goes on the undo list, and the redo list empties. */
	void record(UUID player, S step) {
		push(undoSteps, player, step);
		redoSteps.remove(player);
	}

	Optional<S> takeUndo(UUID player) {
		return Optional.ofNullable(undoSteps.getOrDefault(player, new ArrayDeque<>()).pollFirst());
	}

	/** The part of an undone step that was undone, ready to redo. */
	void undone(UUID player, S step) {
		push(redoSteps, player, step);
	}

	Optional<S> takeRedo(UUID player) {
		return Optional.ofNullable(redoSteps.getOrDefault(player, new ArrayDeque<>()).pollFirst());
	}

	/** The part of a step that was redone goes back on the undo list, keeping the rest of the redo list. */
	void redone(UUID player, S step) {
		push(undoSteps, player, step);
	}

	/** Forgets every player's steps that match, on both lists. */
	void removeIf(Predicate<S> filter) {
		for (Map<UUID, Deque<S>> lists : List.of(undoSteps, redoSteps)) {
			lists.values().forEach(steps -> steps.removeIf(filter));
		}
	}

	/** Adds the step as the newest, then drops the oldest steps past the depth and the block budget, never the newest. */
	private void push(Map<UUID, Deque<S>> lists, UUID player, S step) {
		if (step.isEmpty()) {
			return;
		}
		Deque<S> steps = lists.computeIfAbsent(player, p -> new ArrayDeque<>());
		steps.addFirst(step);
		long blocks = steps.stream().mapToLong(Step::blockCount).sum();
		while (steps.size() > DEPTH || (blocks > blockBudget && steps.size() > 1)) {
			blocks -= steps.removeLast().blockCount();
		}
	}
}
