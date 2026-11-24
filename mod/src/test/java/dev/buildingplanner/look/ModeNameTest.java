package dev.buildingplanner.look;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.buildingplanner.look.ModeName.Box;
import org.junit.jupiter.api.Test;

class ModeNameTest {
	// A screen of 854 by 480 at GUI scale 2: 427 by 240, the hotbar from 122 to 304 and from 218 to 240.

	@Test
	void theNameEnds4LeftOfTheHotbarCentredOnItsHeight() {
		// A name 20 wide: its rectangle 24 wide, ending at 118; 9 high, centred on the hotbar's 22, from 224 to 233.
		assertEquals(new Box(94, 224, 118, 233), ModeName.box(20, 9, 427, 240, false));
	}

	@Test
	void withAnItemInTheOffHandOnTheLeftTheNameEnds4LeftOfItsSlot() {
		// The off hand's slot from 93 to 122.
		assertEquals(new Box(65, 224, 89, 233), ModeName.box(20, 9, 427, 240, true));
	}

	@Test
	void theItemsNameEnds2AboveTheTopOfTheHighestRow() {
		// In survival mode the game draws the name from 181; a line of one row starts at 182, so the name ends at 180.
		assertEquals(-10, ModeName.itemNameShift(181, 9, 182));
		// A line of two rows starts 11 higher, at 171.
		assertEquals(-21, ModeName.itemNameShift(181, 9, 171));
		// In creative mode the game draws the name from 195.
		assertEquals(-24, ModeName.itemNameShift(195, 9, 182));
	}
}
