package dev.buildingplanner.look;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RadialLineTest {
	@Test
	void onAScreen240HighTheLineIsARowAboveAHoveredModesDescription() {
		// The description is at 120 + 100.
		assertEquals(210, RadialLine.row(240));
	}

	@Test
	void onAScreen256HighItIsStillARowAboveTheDescription() {
		// 854 by 480 at GUI scale 2 is 240 high; 1366 by 768 at GUI scale 3 is 256.
		assertEquals(218, RadialLine.row(256));
	}

	@Test
	void onAScreen257HighItIsARowAboveTheCornersName() {
		assertEquals(237, RadialLine.row(257));
	}

	@Test
	void onAScreen270HighItIsARowAboveTheCornersName() {
		// 1920 by 1080 at GUI scale 4 is 270 high.
		assertEquals(250, RadialLine.row(270));
	}
}
