package dev.buildingplanner.look;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.buildingplanner.look.PanelRoom.Box;
import dev.buildingplanner.look.PanelRoom.Room;
import java.util.List;
import org.junit.jupiter.api.Test;

class PanelRoomTest {
	// A 427 by 240 screen (854 by 480 at GUI scale 2): the panel's column at the right edge, 100 wide.
	private static final int LEFT = 323;
	private static final int RIGHT = 423;
	private static final int BOTTOM = 240 - 39;

	@Test
	void withNothingInTheColumnThePanelHasItAllDownToTheBottom() {
		assertEquals(new Room(4, BOTTOM), PanelRoom.below(LEFT, RIGHT, 4, BOTTOM, List.of()));
		// The material list panel in the top-left corner isn't in the right edge's column.
		assertEquals(new Room(4, BOTTOM), PanelRoom.below(LEFT, RIGHT, 4, BOTTOM, List.of(new Box(4, 4, 90, 120))));
	}

	@Test
	void thePanelStartsBelowWhatReachesItsTopAndEndsAboveWhatIsUnder() {
		// The effect icons, two rows of them, then the mix panel, centred down the right edge.
		Box effects = new Box(377, 0, 427, 51);
		Box mix = new Box(380, 90, 425, 150);
		assertEquals(new Room(55, 86), PanelRoom.below(LEFT, RIGHT, 4, BOTTOM, List.of(effects, mix)));
	}

	@Test
	void boxesThatOverlapPushThePanelPastBoth() {
		Box first = new Box(300, 0, 427, 20);
		Box second = new Box(300, 18, 427, 40);
		assertEquals(new Room(44, BOTTOM), PanelRoom.below(LEFT, RIGHT, 4, BOTTOM, List.of(second, first)));
	}

	@Test
	void theControlsLinesEndTheRoomAndNoRoomIsNoHeight() {
		Box controls = new Box(0, 170, 427, 240);
		assertEquals(new Room(4, 166), PanelRoom.below(LEFT, RIGHT, 4, BOTTOM, List.of(controls)));
		Box full = new Box(0, 2, 427, 3);
		assertEquals(0, PanelRoom.below(LEFT, RIGHT, 4, BOTTOM, List.of(new Box(0, 6, 427, 240), full)).height());
	}
}
