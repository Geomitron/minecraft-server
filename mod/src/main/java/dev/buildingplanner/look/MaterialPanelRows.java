package dev.buildingplanner.look;

import dev.buildingplanner.sync.ListSort;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The material list panel's rows, as Litematica's material list screen shows the list: its rows in the screen's order
 * ({@code MaterialListSorter}), without the rows "Hide available" hides ({@code MaterialListBase.recreateFilteredList}),
 * and each count with the multiplier, as {@code WidgetMaterialListEntry} shows it: "Total" times the multiplier,
 * "Missing" the same whenever the multiplier isn't 1, and "Available" as it is. "Missing" has the screen's colours:
 * green at 0, gold when "Available" covers it, red otherwise. The panel shows at most {@link #MAX_ROWS} rows, Litematica's
 * own Info HUD limit, and counts the rest for "+N more"; {@link #within} cuts them further to the room the panel has.
 *
 * @param rows the rows to show, in order; empty for a list with no rows to show ("No blocks to list.")
 * @param more how many rows were left out after the last shown, for "+N more", or 0
 */
public record MaterialPanelRows<T>(List<Row<T>> rows, int more) {
	/** Litematica's own Info HUD limit ({@code materialListHudMaxLines}). */
	public static final int MAX_ROWS = 10;

	/** "Missing"'s colour on the screen. */
	public enum Colour {
		GREEN(0xFF55FF55),
		GOLD(0xFFFFAA00),
		RED(0xFFFF5555);

		/** The colour as ARGB, as the game's {@code ChatFormatting} has it. */
		public final int argb;

		Colour(int argb) {
			this.argb = argb;
		}
	}

	/**
	 * One of the list's rows as the screen keeps it: its item, the item's name, which sorts it and breaks ties, and its
	 * counts without the multiplier.
	 */
	public record Entry<T>(T item, String name, int total, int missing, int available) {
	}

	/** A row as the panel shows it: its item, and its three counts as the screen shows them. */
	public record Row<T>(T item, long total, long available, long missing, Colour missingColour) {
	}

	/**
	 * The rows for a list's {@code entries} (without the rows the player ignored), sorted by {@code sort}, reversed as
	 * {@code reverse} says, without the rows {@code hideAvailable} hides, at the {@code multiplier}.
	 */
	public static <T> MaterialPanelRows<T> of(List<Entry<T>> entries, ListSort sort, boolean reverse,
		boolean hideAvailable, int multiplier) {
		List<Entry<T>> shown = new ArrayList<>();
		for (Entry<T> entry : entries) {
			if (!hideAvailable || entry.available() < missing(entry, multiplier)) {
				shown.add(entry);
			}
		}
		shown.sort(order(sort, reverse));
		List<Row<T>> rows = new ArrayList<>();
		for (Entry<T> entry : shown.subList(0, Math.min(MAX_ROWS, shown.size()))) {
			long missing = missing(entry, multiplier);
			Colour colour = missing == 0 ? Colour.GREEN : entry.available() >= missing ? Colour.GOLD : Colour.RED;
			rows.add(new Row<>(entry.item(), (long) entry.total() * multiplier, entry.available(), missing, colour));
		}
		return new MaterialPanelRows<>(rows, shown.size() - rows.size());
	}

	/**
	 * These rows cut to the ones that fit in {@code height}, for a panel that has only so much room: {@code rowHeight} for
	 * each row, and, when any row is left out, {@code moreHeight} for the "+N more" line after the last, which must fit
	 * too. The rows that don't fit join {@code more}, so it counts every row the panel leaves out.
	 */
	public MaterialPanelRows<T> within(int height, int rowHeight, int moreHeight) {
		int total = rows.size() + more;
		int fit = Math.min(rows.size(), Math.max(0, height) / rowHeight);
		while (fit > 0 && fit < total && fit * rowHeight + moreHeight > height) {
			fit--;
		}
		return new MaterialPanelRows<>(rows.subList(0, fit), total - fit);
	}

	/** "Missing" as the screen shows it: as counted at a multiplier of 1, and "Total" times the multiplier otherwise. */
	private static long missing(Entry<?> entry, int multiplier) {
		return multiplier == 1 ? entry.missing() : (long) entry.total() * multiplier;
	}

	/**
	 * Litematica's {@code MaterialListSorter}: by the column's count, most first (fewest first in reverse), with equal
	 * counts by name; by name, Z to A, or A to Z in reverse. Its counts are the ones without the multiplier.
	 */
	private static <T> Comparator<Entry<T>> order(ListSort sort, boolean reverse) {
		return (a, b) -> {
			int names = a.name().compareTo(b.name());
			int countA;
			int countB;
			switch (sort) {
				case COUNT_TOTAL -> {
					countA = a.total();
					countB = b.total();
				}
				case COUNT_MISSING -> {
					countA = a.missing();
					countB = b.missing();
				}
				case COUNT_AVAILABLE -> {
					countA = a.available();
					countB = b.available();
				}
				default -> {
					return reverse ? names : -names;
				}
			}
			return countA == countB ? names : (countA > countB) != reverse ? -1 : 1;
		};
	}
}
