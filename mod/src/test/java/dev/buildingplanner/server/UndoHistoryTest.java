package dev.buildingplanner.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The undo list's limits: its depth and its budget of blocks. */
class UndoHistoryTest {
	private static final UUID ALEX = UUID.randomUUID();
	private static final UUID BLAIR = UUID.randomUUID();

	private record Step(String name, int blockCount) implements UndoHistory.Step {
		@Override
		public boolean isEmpty() {
			return blockCount == 0;
		}
	}

	/** Takes every undo step the player has, newest first, by name. */
	private static List<String> undoable(UndoHistory<Step> history, UUID player) {
		List<String> names = new ArrayList<>();
		for (var step = history.takeUndo(player); step.isPresent(); step = history.takeUndo(player)) {
			names.add(step.get().name());
		}
		return names;
	}

	@Test
	void stepsHoldingTheWholeBudgetAreAllKept() {
		UndoHistory<Step> history = new UndoHistory<>(1_000);
		history.record(ALEX, new Step("old", 400));
		history.record(ALEX, new Step("new", 600));

		assertEquals(List.of("new", "old"), undoable(history, ALEX));
	}

	@Test
	void stepsOneBlockOverTheBudgetDropTheOldest() {
		UndoHistory<Step> history = new UndoHistory<>(1_000);
		history.record(ALEX, new Step("oldest", 400));
		history.record(ALEX, new Step("middle", 300));
		history.record(ALEX, new Step("newest", 301));

		assertEquals(List.of("newest", "middle"), undoable(history, ALEX));
	}

	@Test
	void aNewBigStepDropsSeveralOldOnesAndKeepsItself() {
		UndoHistory<Step> history = new UndoHistory<>(1_000);
		for (int i = 0; i < 5; i++) {
			history.record(ALEX, new Step("small " + i, 100));
		}
		history.record(ALEX, new Step("big", 900));

		assertEquals(List.of("big", "small 4"), undoable(history, ALEX));
	}

	@Test
	void theNewestStepStaysEvenWhenItAloneIsOverTheBudget() {
		UndoHistory<Step> history = new UndoHistory<>(1_000);
		history.record(ALEX, new Step("old", 10));
		history.record(ALEX, new Step("huge", 5_000));

		assertEquals(List.of("huge"), undoable(history, ALEX));
	}

	@Test
	void eachPlayerHasTheirOwnBudget() {
		UndoHistory<Step> history = new UndoHistory<>(1_000);
		history.record(ALEX, new Step("alex 1", 800));
		history.record(BLAIR, new Step("blair 1", 800));
		history.record(ALEX, new Step("alex 2", 800));

		assertEquals(List.of("alex 2"), undoable(history, ALEX));
		assertEquals(List.of("blair 1"), undoable(history, BLAIR));
	}

	@Test
	void theBudgetIsTwoMillionBlocksAndSteps200000BlocksEachFitTenOfThem() {
		UndoHistory<Step> history = new UndoHistory<>();
		for (int i = 0; i < 12; i++) {
			history.record(ALEX, new Step("step " + i, 200_000));
		}

		assertEquals(2_000_000, UndoHistory.BLOCK_BUDGET);
		assertEquals(List.of("step 11", "step 10", "step 9", "step 8", "step 7", "step 6", "step 5", "step 4", "step 3",
			"step 2"), undoable(history, ALEX));
	}

	@Test
	void theDepthStillLimitsSmallSteps() {
		UndoHistory<Step> history = new UndoHistory<>();
		for (int i = 0; i < UndoHistory.DEPTH + 1; i++) {
			history.record(ALEX, new Step("step " + i, 1));
		}

		List<String> kept = undoable(history, ALEX);
		assertEquals(UndoHistory.DEPTH, kept.size());
		assertEquals("step " + UndoHistory.DEPTH, kept.getFirst());
		assertEquals("step 1", kept.getLast());
	}

	@Test
	void anUndoneStepGoesToTheRedoListAndBackWithinTheBudget() {
		UndoHistory<Step> history = new UndoHistory<>(1_000);
		history.record(ALEX, new Step("old", 500));
		history.record(ALEX, new Step("new", 500));

		history.undone(ALEX, history.takeUndo(ALEX).orElseThrow());
		history.redone(ALEX, history.takeRedo(ALEX).orElseThrow());

		assertEquals(List.of("new", "old"), undoable(history, ALEX));
	}
}
