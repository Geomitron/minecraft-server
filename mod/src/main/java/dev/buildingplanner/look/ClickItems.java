package dev.buildingplanner.look;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The rows of the click's panel (see "Place real blocks": "The click's panel"): for each item a click of real blocks
 * uses, how many it uses, how many the player holds, and how many are missing, the rows the material list panel draws
 * ({@link MaterialPanelRows.Row}: uses in its first column, holds in its second, missing in its third, green at 0 and
 * red otherwise). The items the player is short of come first, then the rest, each most used first and equal counts by
 * name, at most {@link MaterialPanelRows#MAX_ROWS} rows (1411 revision 3, Q2 A and Q3 A).
 */
public final class ClickItems {
	private ClickItems() {
	}

	/** The language key of the panel's title, "Required blocks". */
	public static final String TITLE_KEY = "buildingplanner.ui.click_panel.title";
	/** The language keys of the panel's headings, "Uses", "Hold" and "Need", one for each column. */
	public static final List<String> HEADING_KEYS = List.of("buildingplanner.ui.click_panel.uses",
		"buildingplanner.ui.click_panel.holds", "buildingplanner.ui.click_panel.missing");

	/** An item the click uses: the item, its name, which breaks ties, how many the click uses and how many are held. */
	public record Entry<T>(T item, String name, int uses, int holds) {
		int missing() {
			return Math.max(0, uses - holds);
		}
	}

	/**
	 * The panel's rows for {@code entries}, at most {@link MaterialPanelRows#MAX_ROWS}, with the rest counted for "+N
	 * more"; {@link MaterialPanelRows#within} cuts them further to the room the panel has.
	 */
	public static <T> MaterialPanelRows<T> rows(List<Entry<T>> entries) {
		List<Entry<T>> sorted = new ArrayList<>(entries);
		sorted.sort(Comparator.<Entry<T>, Boolean>comparing(entry -> entry.missing() == 0)
			.thenComparing(Comparator.<Entry<T>>comparingInt(Entry::uses).reversed())
			.thenComparing(Entry::name));
		List<MaterialPanelRows.Row<T>> rows = new ArrayList<>();
		for (Entry<T> entry : sorted.subList(0, Math.min(MaterialPanelRows.MAX_ROWS, sorted.size()))) {
			int missing = entry.missing();
			rows.add(new MaterialPanelRows.Row<>(entry.item(), entry.uses(), entry.holds(), missing,
				missing == 0 ? MaterialPanelRows.Colour.GREEN : MaterialPanelRows.Colour.RED));
		}
		return new MaterialPanelRows<>(rows, sorted.size() - rows.size());
	}
}
