package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SingleRuleTest {
	@Test
	void turningActiveInEveryModeButDisable() {
		assertTrue(SingleRule.turning(true, false, false, true, false));
		assertFalse(SingleRule.turning(true, true, false, true, false));
	}

	@Test
	void turningNotActiveWithoutASessionAMouseModeOrABlockInHand() {
		assertFalse(SingleRule.turning(false, false, false, true, false));
		assertFalse(SingleRule.turning(true, false, true, true, false));
		assertFalse(SingleRule.turning(true, false, false, false, false));
	}

	@Test
	void turningNotActiveDuringAShapeDrawnWithTheBreakClick() {
		assertFalse(SingleRule.turning(true, false, false, true, true));
	}
}
