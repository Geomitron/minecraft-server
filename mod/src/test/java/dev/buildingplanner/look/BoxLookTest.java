package dev.buildingplanner.look;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.buildingplanner.input.BoxPurpose;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class BoxLookTest {
	private static final BlockPos BLOCK = new BlockPos(4, -61, -3);

	@Test
	void eachPurposeHasItsColour() {
		assertEquals(BoxLook.Colour.WHITE, BoxLook.of(BoxPurpose.COPY, false));
		assertEquals(BoxLook.Colour.BLUE, BoxLook.of(BoxPurpose.MATERIAL_LIST, false));
		assertEquals(BoxLook.Colour.RED, BoxLook.of(BoxPurpose.DELETE, false));
	}

	@Test
	void theColoursAreWhiteTheCopysBlueAndTheMarksRed() {
		assertEquals(0xFFFFFF, BoxLook.Colour.WHITE.rgb());
		assertEquals(0x00D4FF, BoxLook.Colour.BLUE.rgb());
		assertEquals(0xFF7070, BoxLook.Colour.RED.rgb());
		assertEquals(0.2f, BoxLook.Colour.RED.sidesAlpha());
	}

	@Test
	void aBoxPastALimitIsTheLimitsRedWhateverItsPurpose() {
		for (BoxPurpose purpose : BoxPurpose.values()) {
			assertEquals(BoxLook.Colour.LIMIT, BoxLook.of(purpose, true), purpose.name());
		}
	}

	@Test
	void theTopFaceShadesJustAboveTheBlock() {
		assertEquals(Optional.of(new BoxLook.Square(4, -60 + BoxLook.SHADE_LIFT, -3, 5, -2)),
			BoxLook.shade(BLOCK, Direction.UP));
	}

	@Test
	void theOtherFiveFacesShadeNothing() {
		for (Direction face : new Direction[] {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST,
			Direction.EAST}) {
			assertEquals(Optional.empty(), BoxLook.shade(BLOCK, face), face.name());
		}
	}
}
