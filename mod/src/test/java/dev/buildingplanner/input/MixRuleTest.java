package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MixRuleTest {
	@Test
	void appliesWithTheMixOnABlockInHandAndAShapeMode() {
		assertTrue(MixRule.applies(true, true, false));
	}

	@Test
	void doesNotApplyWithTheMixOff() {
		assertFalse(MixRule.applies(false, true, false));
	}

	@Test
	void doesNotApplyWithAToolOrAnyOtherItemInHand() {
		assertFalse(MixRule.applies(true, false, false));
	}

	@Test
	void doesNotApplyInDisable() {
		assertFalse(MixRule.applies(true, true, true));
	}
}
