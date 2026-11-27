package dev.buildingplanner.look;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The rows of the click's panel (see "Place real blocks": "The click's panel"): for each item a click of real blocks
 * uses, how many it uses, how many the player holds, and how many are missing, the rows the material list panel draws
 * ({@link MaterialPanelRows.Row}: uses in its first column, holds in its second, missing in its third, green at 0 and
 * red otherwise). {@link Layout} holds the choices still open to Geo (1411 revision 3), each with its recommendation as
 * the default.
 */
public final class ClickItems {
	private ClickItems() {
	}

	/** The order of the rows. */
	public enum Order {
		/** The items the player is short of first, then the rest, each most used first, equal counts by name. */
		SHORT_FIRST,
		/** Most used first, equal counts by name, as the material list panel's "All" sorts at first. */
		MOST_FIRST
	}

	/** Where the panel goes (see {@link PanelRoom}). */
	public enum Spot {
		/** The top-right corner, 4 from each edge, below the effect icons while any show. */
		TOP_RIGHT,
		/** The middle of the screen's top edge, 4 from it, above the crosshair. */
		TOP_MIDDLE,
		/** The top-left corner, under the material list panel while it shows. */
		TOP_LEFT
	}

	/**
	 * The panel's layout: where it goes, the order of its rows, the most rows it shows before "+N more", and whether
	 * Effortless Building's icons beside the crosshair still show while it does. Geo picks each in 1411 revision 3; the
	 * in-game checks switch them for that request's shots.
	 */
	public record Layout(Spot spot, Order order, int maxRows, boolean iconsShow) {
		/** The recommendation for each choice. */
		public static final Layout RECOMMENDED = new Layout(Spot.TOP_RIGHT, Order.SHORT_FIRST, MaterialPanelRows.MAX_ROWS,
			false);
	}

	/** An item the click uses: the item, its name, which breaks ties, how many the click uses and how many are held. */
	public record Entry<T>(T item, String name, int uses, int holds) {
		int missing() {
			return Math.max(0, uses - holds);
		}
	}

	/**
	 * The panel's rows for {@code entries} in {@code order}, at most {@code maxRows}, with the rest counted for "+N more";
	 * {@link MaterialPanelRows#within} cuts them further to the room the panel has.
	 */
	public static <T> MaterialPanelRows<T> rows(List<Entry<T>> entries, Order order, int maxRows) {
		List<Entry<T>> sorted = new ArrayList<>(entries);
		Comparator<Entry<T>> mostFirst = Comparator.<Entry<T>>comparingInt(Entry::uses).reversed()
			.thenComparing(Entry::name);
		sorted.sort(order == Order.SHORT_FIRST
			? Comparator.<Entry<T>, Boolean>comparing(entry -> entry.missing() == 0).thenComparing(mostFirst) : mostFirst);
		List<MaterialPanelRows.Row<T>> rows = new ArrayList<>();
		for (Entry<T> entry : sorted.subList(0, Math.min(maxRows, sorted.size()))) {
			int missing = entry.missing();
			rows.add(new MaterialPanelRows.Row<>(entry.item(), entry.uses(), entry.holds(), missing,
				missing == 0 ? MaterialPanelRows.Colour.GREEN : MaterialPanelRows.Colour.RED));
		}
		return new MaterialPanelRows<>(rows, sorted.size() - rows.size());
	}
}
