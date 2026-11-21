package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class KeyRepeatTest {
	/**
	 * The ticks, counted from 0, on which a key pressed on tick 0 and held for {@code ticksDown} ticks acts, with the
	 * computer's own key repeat adding a press every {@code osRepeat} ticks from tick 10 (0 for none).
	 */
	private static List<Integer> acts(KeyRepeat repeat, int ticksDown, int osRepeat) {
		List<Integer> ticks = new ArrayList<>();
		for (int tick = 0; tick < ticksDown; tick++) {
			int presses = tick == 0 || osRepeat > 0 && tick >= 10 && (tick - 10) % osRepeat == 0 ? 1 : 0;
			for (int i = repeat.tick(presses, true); i > 0; i--) {
				ticks.add(tick);
			}
		}
		return ticks;
	}

	@Test
	void aPressActsAtOnce() {
		assertEquals(1, new KeyRepeat().tick(1, true));
	}

	@Test
	void aHeldKeyActsAgainAfterAFifthOfASecondThenEvery4Ticks() {
		assertEquals(List.of(0, 4, 8, 12, 16), acts(new KeyRepeat(), 17, 0));
	}

	@Test
	void aKeyHeldASecondActs5Times() {
		assertEquals(5, acts(new KeyRepeat(), 20, 0).size());
		assertEquals(10, acts(new KeyRepeat(), 40, 0).size());
	}

	@Test
	void theComputersOwnKeyRepeatDoesntCount() {
		assertEquals(List.of(0, 4, 8, 12, 16, 20, 24), acts(new KeyRepeat(), 25, 1));
	}

	@Test
	void aQuickTapActsOnce() {
		KeyRepeat repeat = new KeyRepeat();
		assertEquals(1, repeat.tick(1, false));
		assertEquals(0, repeat.tick(0, false));
		assertEquals(1, repeat.tick(1, false));
	}

	@Test
	void eachPressAfterALetGoActsAtOnceAndWaitsAgain() {
		KeyRepeat repeat = new KeyRepeat();
		acts(repeat, 6, 0);
		assertEquals(0, repeat.tick(0, false));
		assertEquals(List.of(0, 4), acts(repeat, 7, 0));
	}

	@Test
	void afterAResetThePressActsAtOnce() {
		KeyRepeat repeat = new KeyRepeat();
		repeat.tick(1, true);
		repeat.reset();
		assertEquals(1, repeat.tick(1, true));
		assertEquals(0, repeat.tick(0, true));
	}
}
