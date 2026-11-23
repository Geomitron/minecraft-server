package dev.buildingplanner.look;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.look.ControlsRows.Rect;
import java.util.List;
import org.junit.jupiter.api.Test;

class ControlsRowsTest {
	@Test
	void aLineThatFitsIsOneCentredRowWithAGapBetweenItsRectangles() {
		// Texts 20, 30 and 10 wide: rectangles 24, 34 and 14, with two gaps of 4, 80 in all, centred on 200.
		List<List<Rect>> rows = ControlsRows.of(List.of(List.of(20, 30, 10)), 200);
		assertEquals(List.of(List.of(new Rect(0, 0, 60, 24), new Rect(0, 1, 88, 34), new Rect(0, 2, 126, 14))), rows);
	}

	@Test
	void aLineTooWideGoesOnInARowBelowMovingWholeRectangles() {
		// 100 wide less 4 at each side leaves 92: 44 + 4 + 44 fits, a third 44 doesn't.
		List<List<Rect>> rows = ControlsRows.of(List.of(List.of(40, 40, 40)), 100);
		assertEquals(List.of(List.of(new Rect(0, 0, 4, 44), new Rect(0, 1, 52, 44)), List.of(new Rect(0, 2, 28, 44))),
			rows);
	}

	@Test
	void eachLineStartsARowAndARectangleWiderThanTheScreenHasARowOfItsOwn() {
		List<List<Rect>> rows = ControlsRows.of(List.of(List.of(30), List.of(10, 200, 10)), 100);
		assertEquals(4, rows.size());
		assertEquals(List.of(new Rect(0, 0, 33, 34)), rows.get(0));
		assertEquals(List.of(new Rect(1, 0, 43, 14)), rows.get(1));
		assertEquals(List.of(new Rect(1, 1, -52, 204)), rows.get(2));
		assertEquals(List.of(new Rect(1, 2, 43, 14)), rows.get(3));
	}

	@Test
	void noRectangleOverlapsAnotherAndEachRowStaysOnTheScreen() {
		List<Integer> widths = List.of(60, 120, 80, 95, 70, 110, 40, 150, 66);
		for (int screen : List.of(213, 427, 480, 960)) {
			for (List<Rect> row : ControlsRows.of(List.of(widths), screen)) {
				assertTrue(row.getFirst().left() >= ControlsRows.MARGIN, "inside the screen: " + row);
				assertTrue(row.getLast().left() + row.getLast().width() <= screen - ControlsRows.MARGIN,
					"inside the screen: " + row);
				for (int i = 1; i < row.size(); i++) {
					Rect before = row.get(i - 1);
					assertEquals(before.left() + before.width() + ControlsRows.GAP, row.get(i).left());
				}
			}
		}
	}

	@Test
	void theRowsHeightCountsTheGapsBetweenThem() {
		assertEquals(9, ControlsRows.height(1, 9));
		assertEquals(31, ControlsRows.height(3, 9));
		assertEquals(11, ControlsRows.pitch(9));
	}
}
