package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class HoldRepeatTest {
	/** The ticks, counted from 0, on which the click repeats while the button is down for {@code ticksDown} ticks. */
	private static List<Integer> repeats(HoldRepeat hold, int ticksDown) {
		List<Integer> ticks = new ArrayList<>();
		for (int tick = 0; tick < ticksDown; tick++) {
			if (hold.tick(true)) {
				ticks.add(tick);
			}
		}
		return ticks;
	}

	@Test
	void theTickTheButtonGoesDownIsNotARepeat() {
		HoldRepeat hold = new HoldRepeat(HoldRepeat.USE_PERIOD);
		assertFalse(hold.tick(true));
	}

	@Test
	void aHeldUseClickRepeatsEveryFourTicks() {
		assertEquals(List.of(4, 8, 12, 16), repeats(new HoldRepeat(HoldRepeat.USE_PERIOD), 17));
	}

	@Test
	void aHeldAttackClickRepeatsEverySixTicks() {
		assertEquals(List.of(6, 12, 18), repeats(new HoldRepeat(HoldRepeat.ATTACK_PERIOD), 19));
	}

	@Test
	void aClickReleasedBeforeTheFirstRepeatNeverRepeats() {
		assertEquals(List.of(), repeats(new HoldRepeat(HoldRepeat.USE_PERIOD), 4));
	}

	@Test
	void releasingStartsTheCountAgain() {
		HoldRepeat hold = new HoldRepeat(HoldRepeat.USE_PERIOD);
		repeats(hold, 3);
		assertFalse(hold.tick(false));
		assertEquals(List.of(4, 8), repeats(hold, 9));
	}

	@Test
	void aPeriodBelowOneIsRefused() {
		assertThrows(IllegalArgumentException.class, () -> new HoldRepeat(0));
	}
}
