package dev.buildingplanner.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CopyRulesTest {
	private static final UUID HALL = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID BARN = UUID.fromString("00000000-0000-0000-0000-000000000002");

	@Test
	void refusesAnEmptyCopyAndOneOverTheServersCap() {
		assertEquals(Optional.of(PlayerMessage.nothingToCopy()), CopyRules.refusal(0, 50_000));
		assertEquals(Optional.of(PlayerMessage.copyTooLarge(50_001, 50_000)), CopyRules.refusal(50_001, 50_000));
		assertEquals(Optional.empty(), CopyRules.refusal(50_000, 50_000));
		assertEquals(Optional.empty(), CopyRules.refusal(1, 50_000));
	}

	@Test
	void theSourceIsThePlanMostBlocksCameFrom() {
		Map<UUID, Integer> counts = new LinkedHashMap<>();
		counts.put(HALL, 12);
		counts.put(BARN, 30);
		assertEquals(BARN, CopyRules.sourcePlan(counts));
		counts.put(HALL, 30);
		assertEquals(HALL, CopyRules.sourcePlan(counts), "the first plan on a tie");
		assertNull(CopyRules.sourcePlan(Map.of()));
	}

	@Test
	void aPasteGoesIntoThePlanAtTheCopysOriginOrElseItsSource() {
		assertEquals(Optional.of(BARN), CopyRules.pasteTarget(BARN, HALL, true));
		assertEquals(Optional.of(BARN), CopyRules.pasteTarget(BARN, HALL, false));
		assertEquals(Optional.of(HALL), CopyRules.pasteTarget(null, HALL, true));
		assertEquals(Optional.empty(), CopyRules.pasteTarget(null, HALL, false), "the source isn't loaded");
		assertEquals(Optional.empty(), CopyRules.pasteTarget(null, null, false));
	}
}
