package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EraseHoldTest {
	/** The ticks, counted from 1, on which a hold pressed before tick 1 and held {@code ticks} ticks erases. */
	private static List<Integer> erases(EraseHold hold, int ticks) {
		hold.press();
		List<Integer> erased = new ArrayList<>();
		for (int tick = 1; tick <= ticks; tick++) {
			if (hold.tick(true)) {
				erased.add(tick);
			}
		}
		return erased;
	}

	@Test
	void nineteenTicksEraseNothing() {
		assertEquals(List.of(), erases(new EraseHold(), 19));
	}

	@Test
	void theTwentiethErasesOnceAndHoldingOnErasesNothingMore() {
		assertEquals(List.of(20), erases(new EraseHold(), 100));
	}

	@Test
	void aReleaseStartsTheCountAgain() {
		EraseHold hold = new EraseHold();
		assertEquals(List.of(), erases(hold, 15));
		assertFalse(hold.tick(false));
		assertFalse(hold.holding());
		assertEquals(List.of(20), erases(hold, 20));
	}

	@Test
	void afterAnEraseTheNextPressCountsAgain() {
		EraseHold hold = new EraseHold();
		assertEquals(List.of(20), erases(hold, 25));
		hold.tick(false);
		assertEquals(List.of(20), erases(hold, 20));
	}

	@Test
	void ticksWithNoPressEraseNothing() {
		EraseHold hold = new EraseHold();
		for (int tick = 0; tick < 40; tick++) {
			assertFalse(hold.tick(true));
		}
		assertEquals(-1, hold.percent());
	}

	@Test
	void aCancelledHoldErasesNothing() {
		EraseHold hold = new EraseHold();
		assertEquals(List.of(), erases(hold, 10));
		hold.cancel();
		for (int tick = 0; tick < 20; tick++) {
			assertFalse(hold.tick(true));
		}
	}

	@Test
	void theProgressShowsInStepsOfFivePercent() {
		EraseHold hold = new EraseHold();
		assertEquals(-1, hold.percent());
		hold.press();
		assertTrue(hold.holding());
		assertEquals(0, hold.percent());
		List<Integer> shown = new ArrayList<>();
		for (int tick = 1; tick < EraseHold.TICKS; tick++) {
			hold.tick(true);
			shown.add(hold.percent());
		}
		assertEquals(List.of(5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95), shown);
		hold.tick(true);
		assertEquals(-1, hold.percent());
	}
}
