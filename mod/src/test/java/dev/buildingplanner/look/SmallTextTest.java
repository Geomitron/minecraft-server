package dev.buildingplanner.look;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SmallTextTest {
	@Test
	void fullSizeAtGuiScalesOneToThree() {
		assertEquals(1f, SmallText.scale(1));
		assertEquals(1f, SmallText.scale(2));
		assertEquals(1f, SmallText.scale(3));
	}

	@Test
	void theSmallestSizeAtOrAboveSeventyPercentWhereEachFontPixelIsWholeScreenPixels() {
		assertEquals(0.75f, SmallText.scale(4));
		assertEquals(0.8f, SmallText.scale(5));
		assertEquals(5f / 6, SmallText.scale(6));
	}
}
