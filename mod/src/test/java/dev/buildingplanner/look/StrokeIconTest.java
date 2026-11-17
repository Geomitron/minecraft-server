package dev.buildingplanner.look;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class StrokeIconTest {
	private static final int SIZE = 24;

	private static int at(int[] coverage, int x, int y) {
		return coverage[y * SIZE + x];
	}

	@Test
	void aLineCoversTheStrokeAroundIt() {
		int[] line = StrokeIcon.rasterize(List.of("<path d=\"M3 12h18\" />"), SIZE);
		// The stroke runs from y 11 to 13, so it fills the pixel rows 11 and 12.
		assertEquals(255, at(line, 12, 11));
		assertEquals(255, at(line, 12, 12));
		assertEquals(0, at(line, 12, 10));
		assertEquals(0, at(line, 12, 13));
		// Its round ends reach 1 past each end, and no further.
		assertTrue(at(line, 2, 11) > 0);
		assertEquals(0, at(line, 1, 11));
		assertEquals(0, at(line, 22, 12));
	}

	@Test
	void aCircleCoversItsRingAndNotItsMiddle() {
		int[] circle = StrokeIcon.rasterize(List.of("<circle cx=\"12\" cy=\"12\" r=\"3\" />"), SIZE);
		assertEquals(0, at(circle, 11, 11));
		assertEquals(0, at(circle, 12, 12));
		assertTrue(at(circle, 14, 11) > 200);
		assertTrue(at(circle, 11, 14) > 200);
		assertEquals(0, at(circle, 17, 12));
	}

	@Test
	void aRoundedRectanglesCornersAreRound() {
		int[] rect = StrokeIcon.rasterize(List.of("<rect width=\"18\" height=\"18\" x=\"3\" y=\"3\" rx=\"2\" />"), SIZE);
		assertEquals(255, at(rect, 12, 2));
		assertEquals(255, at(rect, 3, 12));
		assertEquals(0, at(rect, 12, 12));
		// The corner's curve runs inside the square's corner, leaving its tip all but clear.
		assertTrue(at(rect, 2, 2) < 64);
	}

	@Test
	void arcsComeOutWhereTheirEndsAndFlagsPutThem() {
		// Lucide's eye is the same on both sides, so a mistake in the arcs' centres or sweeps shows as a difference.
		int[] eye = StrokeIcon.rasterize(List.of(
			"<path d=\"M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0\" />",
			"<circle cx=\"12\" cy=\"12\" r=\"3\" />"), SIZE);
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE / 2; x++) {
				assertTrue(Math.abs(at(eye, x, y) - at(eye, SIZE - 1 - x, y)) <= 32, "at " + x + "," + y);
			}
		}
		// The outline's top and bottom arcs pass 7 above and below the middle (at y 5 and 19), and its ends are at x 2
		// and 22.
		assertTrue(at(eye, 12, 4) > 128);
		assertTrue(at(eye, 12, 19) > 128);
		assertEquals(0, at(eye, 12, 7));
		assertTrue(at(eye, 1, 12) > 0);
		assertEquals(0, at(eye, 12, 1));
	}

	@Test
	void numbersMayRunOnWithoutSpaces() {
		List<double[]> segments = new ArrayList<>();
		StrokeIcon.PathReader reader = new StrokeIcon.PathReader("1.1.9-2-.5e1", segments);
		assertEquals(1.1, reader.number());
		assertEquals(0.9, reader.number());
		assertEquals(-2, reader.number());
		assertEquals(-5, reader.number());
	}

	@Test
	void relativeCommandsMoveFromTheLastPoint() {
		List<double[]> segments = StrokeIcon.segments(List.of("<path d=\"m3 7 2 2 4-4\" />"));
		assertEquals(2, segments.size());
		assertEquals(List.of(3.0, 7.0, 5.0, 9.0), List.of(segments.get(0)[0], segments.get(0)[1], segments.get(0)[2],
			segments.get(0)[3]));
		assertEquals(9.0, segments.get(1)[2]);
		assertEquals(5.0, segments.get(1)[3]);
	}

	@Test
	void anElementItCantDrawIsRefused() {
		assertThrows(IllegalArgumentException.class, () -> StrokeIcon.segments(List.of("<ellipse rx=\"2\" ry=\"3\" />")));
	}
}
