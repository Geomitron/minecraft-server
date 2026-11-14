package dev.buildingplanner.server;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Each player's undo steps, across plans, newest first, kept in memory until the server stops. With redo on, an undone
 * step waits on a redo list until the player's next new edit clears it.
 */
final class UndoHistory {
	/** Effortless Building's own undo depth. */
	static final int DEPTH = 50;

	/** One block of a step: its state before and after, in plan coordinates. */
	record StepBlock(String region, BlockPos pos, BlockState before, BlockState after) {
		StepBlock reversed() {
			return new StepBlock(region, pos, after, before);
		}
	}

	/** One edit by one player: a shape, an erase, or one tick of Litematica writes. */
	record Step(UUID plan, List<StepBlock> blocks) {
		Step {
			blocks = List.copyOf(blocks);
		}

		Step reversed() {
			return new Step(plan, blocks.stream().map(StepBlock::reversed).toList());
		}
	}

	private final boolean redo;
	private final Map<UUID, Deque<Step>> undoSteps = new HashMap<>();
	private final Map<UUID, Deque<Step>> redoSteps = new HashMap<>();

	UndoHistory(boolean redo) {
		this.redo = redo;
	}

	boolean redoEnabled() {
		return redo;
	}

	/** A new edit: its step goes on the undo list, and the redo list empties. */
	void record(UUID player, Step step) {
		push(undoSteps, player, step);
		redoSteps.remove(player);
	}

	Optional<Step> takeUndo(UUID player) {
		return Optional.ofNullable(undoSteps.getOrDefault(player, new ArrayDeque<>()).pollFirst());
	}

	/** The part of an undone step that was undone, ready to redo. Does nothing while redo is off. */
	void undone(UUID player, Step step) {
		if (redo) {
			push(redoSteps, player, step);
		}
	}

	Optional<Step> takeRedo(UUID player) {
		return Optional.ofNullable(redoSteps.getOrDefault(player, new ArrayDeque<>()).pollFirst());
	}

	/** The part of a step that was redone goes back on the undo list, keeping the rest of the redo list. */
	void redone(UUID player, Step step) {
		push(undoSteps, player, step);
	}

	/** A deleted plan's steps go with it. */
	void dropPlan(UUID plan) {
		for (Map<UUID, Deque<Step>> lists : List.of(undoSteps, redoSteps)) {
			lists.values().forEach(steps -> steps.removeIf(step -> step.plan().equals(plan)));
		}
	}

	/** An empty step (every block changed by someone else since) has nothing left to take back or forward. */
	private static void push(Map<UUID, Deque<Step>> lists, UUID player, Step step) {
		if (step.blocks().isEmpty()) {
			return;
		}
		Deque<Step> steps = lists.computeIfAbsent(player, p -> new ArrayDeque<>());
		steps.addFirst(step);
		while (steps.size() > DEPTH) {
			steps.removeLast();
		}
	}
}
