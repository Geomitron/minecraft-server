package dev.buildingplanner.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class CopyRulesTest {
	@Test
	void refusesAnEmptyCopyAndOneOverTheServersCap() {
		assertEquals(Optional.of(PlayerMessage.nothingToCopy()), CopyRules.refusal(0, 50_000));
		assertEquals(Optional.of(PlayerMessage.copyTooLarge(50_001, 50_000)), CopyRules.refusal(50_001, 50_000));
		assertEquals(Optional.empty(), CopyRules.refusal(50_000, 50_000));
		assertEquals(Optional.empty(), CopyRules.refusal(1, 50_000));
	}

}
