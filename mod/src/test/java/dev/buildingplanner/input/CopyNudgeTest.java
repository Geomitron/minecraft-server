package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class CopyNudgeTest {
	@Test
	void forwardIsTheWayTheViewFaces() {
		assertEquals(Direction.SOUTH, CopyNudge.FORWARD.direction(0));
		assertEquals(Direction.WEST, CopyNudge.FORWARD.direction(90));
		assertEquals(Direction.NORTH, CopyNudge.FORWARD.direction(180));
		assertEquals(Direction.NORTH, CopyNudge.FORWARD.direction(-180));
		assertEquals(Direction.EAST, CopyNudge.FORWARD.direction(-90));
		assertEquals(Direction.EAST, CopyNudge.FORWARD.direction(270));
	}

	@Test
	void theViewTakesTheNearestCompassDirection() {
		assertEquals(Direction.SOUTH, CopyNudge.FORWARD.direction(44));
		assertEquals(Direction.WEST, CopyNudge.FORWARD.direction(46));
		assertEquals(Direction.NORTH, CopyNudge.FORWARD.direction(-150));
		assertEquals(Direction.SOUTH, CopyNudge.FORWARD.direction(-30));
		// Yaw keeps growing as the player turns round and round.
		assertEquals(Direction.WEST, CopyNudge.FORWARD.direction(90 + 720));
	}

	@Test
	void backIsTowardTheView() {
		assertEquals(Direction.NORTH, CopyNudge.BACK.direction(0));
		assertEquals(Direction.EAST, CopyNudge.BACK.direction(90));
		assertEquals(Direction.SOUTH, CopyNudge.BACK.direction(170));
	}

	@Test
	void leftAndRightAreTheViewsSides() {
		// Facing north, west is on the left and east on the right.
		assertEquals(Direction.WEST, CopyNudge.LEFT.direction(180));
		assertEquals(Direction.EAST, CopyNudge.RIGHT.direction(180));
		// Facing south, the other way round.
		assertEquals(Direction.EAST, CopyNudge.LEFT.direction(0));
		assertEquals(Direction.WEST, CopyNudge.RIGHT.direction(0));
		// Facing east: north on the left, south on the right.
		assertEquals(Direction.NORTH, CopyNudge.LEFT.direction(-90));
		assertEquals(Direction.SOUTH, CopyNudge.RIGHT.direction(-90));
	}

	@Test
	void upAndDownGoWhateverTheFacing() {
		for (float yRot : new float[] {0, 45, 90, -135, 300}) {
			assertEquals(Direction.UP, CopyNudge.UP.direction(yRot));
			assertEquals(Direction.DOWN, CopyNudge.DOWN.direction(yRot));
		}
	}

	@Test
	void eachKeyHasItsTranslationKey() {
		assertEquals("key.buildingplanner.nudge_forward", CopyNudge.FORWARD.translationKey());
		assertEquals("key.buildingplanner.nudge_down", CopyNudge.DOWN.translationKey());
	}
}
