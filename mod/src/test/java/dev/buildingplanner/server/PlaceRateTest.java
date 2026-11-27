package dev.buildingplanner.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The cap of real blocks a second ("Place real blocks", "The cap"). */
class PlaceRateTest {
	private static final UUID ALEX = new UUID(0, 1);
	private static final UUID SAM = new UUID(0, 2);

	@Test
	void aPlaceGoesInWhileTheSecondBeforeStaysWithinTheCap() {
		PlaceRate rate = new PlaceRate(2_000);
		assertTrue(rate.allows(ALEX, 2_000, 0));
		assertFalse(rate.allows(ALEX, 2_001, 0));
		rate.placed(ALEX, 1_500, 100);
		assertTrue(rate.allows(ALEX, 500, 200));
		assertFalse(rate.allows(ALEX, 501, 200));
	}

	@Test
	void blocksPlacedASecondAgoNoLongerCount() {
		PlaceRate rate = new PlaceRate(2_000);
		rate.placed(ALEX, 2_000, 1_000);
		assertFalse(rate.allows(ALEX, 1, 1_999));
		assertTrue(rate.allows(ALEX, 1, 2_000));
		assertTrue(rate.allows(ALEX, 2_000, 2_000));
	}

	@Test
	void eachPlayerHasTheirOwnCountUntilTheyLeave() {
		PlaceRate rate = new PlaceRate(2_000);
		rate.placed(ALEX, 2_000, 0);
		assertTrue(rate.allows(SAM, 2_000, 0));
		assertFalse(rate.allows(ALEX, 1, 0));
		rate.left(ALEX);
		assertTrue(rate.allows(ALEX, 2_000, 0));
	}

	@Test
	void severalPlacesInASecondAddUp() {
		PlaceRate rate = new PlaceRate(2_000);
		rate.placed(ALEX, 800, 0);
		rate.placed(ALEX, 800, 400);
		assertTrue(rate.allows(ALEX, 400, 900));
		assertFalse(rate.allows(ALEX, 401, 900));
		assertTrue(rate.allows(ALEX, 1_200, 1_000));
	}
}
