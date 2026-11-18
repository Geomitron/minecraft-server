package dev.buildingplanner.look;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BoxListLayoutTest {
	// 854 by 480 at GUI scale 2, with the controls line wrapped onto two lines of 9: its top is 58 + 9 above the bottom.
	private static final int WIDTH = 427;
	private static final int HEIGHT = 240;
	private static final int CONTROLS_TOP = HEIGHT - 58 - 9;

	@Test
	void wrapsIntoRowsOfTenAndEndsTheLastRowAboveTheControlsWithAPlusCell() {
		BoxListLayout layout = BoxListLayout.of(WIDTH, HEIGHT, 40, CONTROLS_TOP);

		assertEquals(30, layout.cells().size());
		assertEquals(11, layout.more());
		assertEquals(new BoxListLayout.Cell(223, 112), layout.cells().getFirst());
		assertEquals(new BoxListLayout.Cell(403, 112), layout.cells().get(9));
		assertEquals(new BoxListLayout.Cell(223, 132), layout.cells().get(10));
		assertEquals(new BoxListLayout.Cell(403, 152), layout.cells().getLast());
		for (BoxListLayout.Cell cell : layout.cells()) {
			assertTrue(cell.x() + BoxListLayout.CELL <= WIDTH - 4, "inside the screen: " + cell);
			assertTrue(cell.y() + BoxListLayout.CELL <= CONTROLS_TOP, "above the controls line: " + cell);
		}
	}

	@Test
	void aShortListStaysInOneRowWithNoPlusCell() {
		BoxListLayout layout = BoxListLayout.of(WIDTH, HEIGHT, 3, CONTROLS_TOP);

		assertEquals(List.of(new BoxListLayout.Cell(223, 112), new BoxListLayout.Cell(243, 112),
			new BoxListLayout.Cell(263, 112)), layout.cells());
		assertEquals(0, layout.more());
	}

	@Test
	void aListThatJustFitsHasNoPlusCellAndOneMoreKindTakesTheLastCellForTwo() {
		assertEquals(0, BoxListLayout.of(WIDTH, HEIGHT, 30, CONTROLS_TOP).more());
		BoxListLayout over = BoxListLayout.of(WIDTH, HEIGHT, 31, CONTROLS_TOP);
		assertEquals(30, over.cells().size());
		assertEquals(2, over.more());
	}

	@Test
	void aRedLineAboveTheControlsLeavesTwoRows() {
		BoxListLayout layout = BoxListLayout.of(WIDTH, HEIGHT, 40, CONTROLS_TOP - 9);

		assertEquals(20, layout.cells().size());
		assertEquals(21, layout.more());
	}

	@Test
	void countsKeepToThreeCharacters() {
		assertEquals("7", BoxListLayout.count(7));
		assertEquals("999", BoxListLayout.count(999));
		assertEquals("1k", BoxListLayout.count(1000));
		assertEquals("2k", BoxListLayout.count(1500));
		assertEquals("66k", BoxListLayout.count(65_536));
		assertEquals("999k", BoxListLayout.count(999_499));
		assertEquals("1M", BoxListLayout.count(999_500));
		assertEquals("25M", BoxListLayout.count(256 * 256 * 384));
	}
}
