package dev.buildingplanner.look;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the rectangles of the controls lines go across the screen, in GUI pixels (see "Building Planner's mouse
 * modes"). Each rectangle is its text with 2 more at each side, one line high; a gap of 4 separates the rectangles in a
 * row, and 2 separates the rows. Each line starts a row, and a line too wide for the screen, less 4 at each side, goes
 * on in a row below, moving whole rectangles, never splitting one. Each row is centred.
 */
public final class ControlsRows {
	public static final int PADDING = 2;
	public static final int GAP = 4;
	public static final int ROW_GAP = 2;
	public static final int MARGIN = 4;

	private ControlsRows() {
	}

	/** A rectangle: its line, its place in the line (its label first), and its background's left edge and width. */
	public record Rect(int line, int index, int left, int width) {
	}

	/**
	 * The rows, top first, for lines whose rectangles' texts are {@code widths} wide, each line's in order, on a screen
	 * {@code screen} wide.
	 */
	public static List<List<Rect>> of(List<List<Integer>> widths, int screen) {
		int room = screen - 2 * MARGIN;
		List<List<Rect>> rows = new ArrayList<>();
		for (int line = 0; line < widths.size(); line++) {
			List<Integer> row = new ArrayList<>();
			int start = 0;
			int used = 0;
			List<Integer> texts = widths.get(line);
			for (int i = 0; i < texts.size(); i++) {
				int width = texts.get(i) + 2 * PADDING;
				if (!row.isEmpty() && used + GAP + width > room) {
					rows.add(centred(line, start, row, used, screen));
					start = i;
					row = new ArrayList<>();
					used = 0;
				}
				used += (row.isEmpty() ? 0 : GAP) + width;
				row.add(width);
			}
			if (!row.isEmpty()) {
				rows.add(centred(line, start, row, used, screen));
			}
		}
		return rows;
	}

	private static List<Rect> centred(int line, int start, List<Integer> widths, int used, int screen) {
		List<Rect> rects = new ArrayList<>(widths.size());
		int left = (screen - used) / 2;
		for (int i = 0; i < widths.size(); i++) {
			rects.add(new Rect(line, start + i, left, widths.get(i)));
			left += widths.get(i) + GAP;
		}
		return List.copyOf(rects);
	}

	/** The distance from one row's top to the next's, for text {@code lineHeight} high. */
	public static int pitch(int lineHeight) {
		return lineHeight + ROW_GAP;
	}

	/** The height of {@code rows} rows, from the first's top to the last's bottom, for text {@code lineHeight} high. */
	public static int height(int rows, int lineHeight) {
		return rows * lineHeight + Math.max(rows - 1, 0) * ROW_GAP;
	}
}
