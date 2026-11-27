package dev.buildingplanner.look;

import java.util.List;

/**
 * The room a panel has on the screen, in GUI pixels, clear of what else the screen shows there: the click's panel (see
 * "Place real blocks": "The click's panel") keeps out of the material list panel, the mix panel, the effect icons,
 * Litematica's lines about the ghost block at the crosshair, the crosshair and Effortless Building's icons beside it,
 * chat, and the controls lines and hotbar at the bottom.
 */
public final class PanelRoom {
	/** Between the panel and anything beside it, in GUI pixels. */
	public static final int GAP = 4;

	private PanelRoom() {
	}

	/** Something the screen shows, as its box in GUI pixels: from left and top to right and bottom, exclusive. */
	public record Box(int left, int top, int right, int bottom) {
		boolean acrossColumn(int columnLeft, int columnRight) {
			return left < columnRight && columnLeft < right;
		}
	}

	/** The rows of the screen a panel may take, from {@code top} to {@code bottom}. */
	public record Room(int top, int bottom) {
		public int height() {
			return bottom - top;
		}
	}

	/**
	 * The room a panel has in the column from {@code left} to {@code right}, starting at {@code top} and going down at
	 * most to {@code bottom}: below each box in the column that reaches the panel's top, {@link #GAP} under it (the
	 * effect icons in the top-right corner, the material list panel in the top-left), and down to {@link #GAP} above the
	 * first box in the column below that.
	 */
	public static Room below(int left, int right, int top, int bottom, List<Box> boxes) {
		int start = top;
		boolean moved = true;
		while (moved) {
			moved = false;
			for (Box box : boxes) {
				if (box.acrossColumn(left, right) && box.top() <= start && start < box.bottom() + GAP) {
					start = box.bottom() + GAP;
					moved = true;
				}
			}
		}
		int end = bottom;
		for (Box box : boxes) {
			if (box.acrossColumn(left, right) && box.top() > start) {
				end = Math.min(end, box.top() - GAP);
			}
		}
		return new Room(start, Math.max(start, end));
	}
}
