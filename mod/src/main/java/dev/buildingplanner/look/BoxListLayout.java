package dev.buildingplanner.look;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the list of the blocks in a box goes under the crosshair, in GUI pixels: cells 20 apart from 10 right of the
 * screen's middle and 8 above it, as Effortless Building's own list starts ({@code RenderHandler.drawBreakingStacks}),
 * each an item 16 wide with its count below its right edge, 18 by 18 with the text's shadow. Where the next cell would
 * pass 4 from the screen's right edge, the list goes on in a new row 20 below, starting at the same place. The rows stop
 * above the controls line; when the list has more kinds than cells, the last cell is "+N" for the kinds left out. Each
 * row sits on the controls' dark background ({@link #backgrounds}), so icons and counts stay readable over bright
 * blocks.
 *
 * @param cells each cell's top left corner, in order; with {@code more} above 0, the last is the "+N" cell
 * @param more the kinds the "+N" cell stands for, or 0 when every kind has its cell
 */
public record BoxListLayout(List<Cell> cells, int more) {
	public static final int STEP = 20;
	public static final int CELL = 18;
	private static final int RIGHT_OF_MIDDLE = 10;
	private static final int ABOVE_MIDDLE = 8;
	private static final int EDGE = 4;

	/** How far a row's background reaches past its cells at the left and right. */
	public static final int BACKGROUND_SIDE = 2;
	/** How far it reaches above and below them: half the gap between rows, so the rows' backgrounds meet. */
	public static final int BACKGROUND_END = (STEP - CELL) / 2;

	/** A cell's top left corner. */
	public record Cell(int x, int y) {
	}

	/** A rectangle from its left top corner to its right bottom one, exclusive. */
	public record Rect(int left, int top, int right, int bottom) {
	}

	/**
	 * The cells for {@code kinds} kinds of block on a screen {@code width} by {@code height} GUI pixels, in rows that end
	 * at or above {@code above}, the top of the controls line.
	 */
	public static BoxListLayout of(int width, int height, int kinds, int above) {
		int left = width / 2 + RIGHT_OF_MIDDLE;
		int top = height / 2 - ABOVE_MIDDLE;
		int perRow = Math.max(0, Math.floorDiv(width - EDGE - CELL - left, STEP) + 1);
		int rows = Math.max(0, Math.floorDiv(above - CELL - top, STEP) + 1);
		int capacity = perRow * rows;
		int count = Math.min(kinds, capacity);
		int more = kinds > capacity && capacity > 0 ? kinds - capacity + 1 : 0;
		List<Cell> cells = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			cells.add(new Cell(left + i % perRow * STEP, top + i / perRow * STEP));
		}
		return new BoxListLayout(List.copyOf(cells), more);
	}

	/**
	 * The dark background under each row of cells, top row first: from {@value #BACKGROUND_SIDE} left of its first cell
	 * to as far right of its last, and {@link #BACKGROUND_END} above and below its cells, so the rows' backgrounds meet
	 * without overlapping.
	 */
	public List<Rect> backgrounds() {
		List<Rect> rects = new ArrayList<>();
		for (int i = 0; i < cells.size(); i++) {
			Cell cell = cells.get(i);
			if (i + 1 < cells.size() && cells.get(i + 1).y() == cell.y()) {
				continue;
			}
			int first = i;
			while (first > 0 && cells.get(first - 1).y() == cell.y()) {
				first--;
			}
			rects.add(new Rect(cells.get(first).x() - BACKGROUND_SIDE, cell.y() - BACKGROUND_END,
				cell.x() + CELL + BACKGROUND_SIDE, cell.y() + CELL + BACKGROUND_END));
		}
		return List.copyOf(rects);
	}

	/**
	 * A count as it shows in a cell, at most three characters so it stays within its cell's 20: up to 999 as it is, then
	 * in thousands ("12k") and millions ("3M"), rounded.
	 */
	public static String count(int count) {
		if (count < 1000) {
			return Integer.toString(count);
		}
		long thousands = Math.round(count / 1e3);
		return thousands < 1000 ? thousands + "k" : Math.round(count / 1e6) + "M";
	}
}
