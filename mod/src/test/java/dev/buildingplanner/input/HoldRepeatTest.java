package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class HoldRepeatTest {
	/** The ticks, counted from 0, on which the click repeats while the button is down for {@code ticksDown} ticks. */
	private static List<Integer> repeats(HoldRepeat hold, int period, int ticksDown) {
		List<Integer> ticks = new ArrayList<>();
		for (int tick = 0; tick < ticksDown; tick++) {
			if (hold.tick(true, period)) {
				ticks.add(tick);
			}
		}
		return ticks;
	}

	@Test
	void theTickTheButtonGoesDownIsNotARepeat() {
		assertFalse(new HoldRepeat().tick(true, 1));
	}

	@Test
	void aHeldClickRepeatsEveryPeriod() {
		assertEquals(List.of(4, 8, 12, 16), repeats(new HoldRepeat(), 4, 17));
		assertEquals(List.of(6, 12, 18), repeats(new HoldRepeat(), 6, 19));
		assertEquals(List.of(1, 2, 3), repeats(new HoldRepeat(), 1, 4));
		assertEquals(List.of(20), repeats(new HoldRepeat(), 20, 21));
	}

	@Test
	void aClickReleasedBeforeTheFirstRepeatNeverRepeats() {
		assertEquals(List.of(), repeats(new HoldRepeat(), 4, 4));
	}

	@Test
	void releasingStartsTheCountAgain() {
		HoldRepeat hold = new HoldRepeat();
		repeats(hold, 4, 3);
		assertFalse(hold.tick(false, 4));
		assertEquals(List.of(4, 8), repeats(hold, 4, 9));
	}

	@Test
	void aPeriodBelowOneIsRefused() {
		assertThrows(IllegalArgumentException.class, () -> new HoldRepeat().tick(true, 0));
	}
}
