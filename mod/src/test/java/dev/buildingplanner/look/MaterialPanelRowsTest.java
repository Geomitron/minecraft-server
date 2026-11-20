package dev.buildingplanner.look;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.look.MaterialPanelRows.Colour;
import dev.buildingplanner.look.MaterialPanelRows.Entry;
import dev.buildingplanner.look.MaterialPanelRows.Row;
import dev.buildingplanner.sync.ListSort;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MaterialPanelRowsTest {
	// The spec's example: 10 ghost oak planks, 6 built, 20 real stone, a player carrying 3 oak planks; and glass, all
	// missing, with 9 carried.
	private static final Entry<String> PLANKS = new Entry<>("planks", "Oak Planks", 10, 4, 3);
	private static final Entry<String> STONE = new Entry<>("stone", "Stone", 20, 0, 0);
	private static final Entry<String> GLASS = new Entry<>("glass", "Glass", 5, 5, 9);
	private static final List<Entry<String>> LIST = List.of(PLANKS, STONE, GLASS);

	private static List<String> items(MaterialPanelRows<String> panel) {
		return panel.rows().stream().map(Row::item).toList();
	}

	@Test
	void sortsAsTheScreenDoesByEachColumn() {
		assertEquals(List.of("stone", "planks", "glass"), items(MaterialPanelRows.of(LIST, ListSort.COUNT_TOTAL, false, false, 1)));
		assertEquals(List.of("glass", "planks", "stone"), items(MaterialPanelRows.of(LIST, ListSort.COUNT_TOTAL, true, false, 1)));
		assertEquals(List.of("glass", "planks", "stone"), items(MaterialPanelRows.of(LIST, ListSort.COUNT_MISSING, false, false, 1)));
		assertEquals(List.of("glass", "planks", "stone"), items(MaterialPanelRows.of(LIST, ListSort.COUNT_AVAILABLE, false, false, 1)));
		// Litematica sorts by name Z to A, and A to Z in reverse, which choosing the name column starts with.
		assertEquals(List.of("stone", "planks", "glass"), items(MaterialPanelRows.of(LIST, ListSort.NAME, false, false, 1)));
		assertEquals(List.of("glass", "planks", "stone"), items(MaterialPanelRows.of(LIST, ListSort.NAME, true, false, 1)));
	}

	@Test
	void equalCountsGoByName() {
		Entry<String> a = new Entry<>("a", "Andesite", 4, 0, 0);
		Entry<String> b = new Entry<>("b", "Brick", 4, 0, 0);
		assertEquals(List.of("a", "b"), items(MaterialPanelRows.of(List.of(b, a), ListSort.COUNT_TOTAL, false, false, 1)));
	}

	@Test
	void hideAvailableLeavesOutTheRowsTheInventoryCovers() {
		// Stone, with nothing missing, and glass, with 9 carried for 5 missing, are covered.
		assertEquals(List.of("planks"), items(MaterialPanelRows.of(LIST, ListSort.COUNT_TOTAL, false, true, 1)));
	}

	@Test
	void theMultiplierMultipliesTotalAndMakesMissingTheSame() {
		MaterialPanelRows<String> panel = MaterialPanelRows.of(List.of(PLANKS, STONE), ListSort.COUNT_TOTAL, false, false, 4);
		assertEquals(new Row<>("stone", 80, 0, 80, Colour.RED), panel.rows().get(0));
		assertEquals(new Row<>("planks", 40, 3, 40, Colour.RED), panel.rows().get(1));
		// With the multiplier, hiding what's available compares with the multiplied "Missing": stone is no longer covered.
		assertEquals(List.of("stone", "planks"), items(MaterialPanelRows.of(List.of(PLANKS, STONE), ListSort.COUNT_TOTAL,
			false, true, 4)));
	}

	@Test
	void missingIsGreenAtZeroGoldWhenCoveredAndRedOtherwise() {
		MaterialPanelRows<String> panel = MaterialPanelRows.of(LIST, ListSort.COUNT_TOTAL, false, false, 1);
		assertEquals(new Row<>("stone", 20, 0, 0, Colour.GREEN), panel.rows().get(0));
		assertEquals(new Row<>("planks", 10, 3, 4, Colour.RED), panel.rows().get(1));
		assertEquals(new Row<>("glass", 5, 9, 5, Colour.GOLD), panel.rows().get(2));
		assertEquals(0, panel.more());
	}

	@Test
	void showsTenRowsThenCountsTheRestForMore() {
		List<Entry<String>> many = new ArrayList<>();
		for (int i = 0; i < 13; i++) {
			many.add(new Entry<>("item" + i, "Item " + i, 100 - i, 0, 0));
		}
		MaterialPanelRows<String> panel = MaterialPanelRows.of(many, ListSort.COUNT_TOTAL, false, false, 1);
		assertEquals(MaterialPanelRows.MAX_ROWS, panel.rows().size());
		assertEquals("item0", panel.rows().getFirst().item());
		assertEquals("item9", panel.rows().getLast().item());
		assertEquals(3, panel.more());
	}

	private static MaterialPanelRows<String> many(int kinds) {
		List<Entry<String>> entries = new ArrayList<>();
		for (int i = 0; i < kinds; i++) {
			entries.add(new Entry<>("item" + i, "Item " + i, 100 - i, 0, 0));
		}
		return MaterialPanelRows.of(entries, ListSort.COUNT_TOTAL, false, false, 1);
	}

	@Test
	void withinKeepsTheRowsThatFitAndCountsTheRestForMore() {
		// 13 kinds, 10 rows to show; each row is 18 high and "+N more" is 11, as in the panel.
		MaterialPanelRows<String> fitted = many(13).within(182, 18, 11);
		assertEquals(9, fitted.rows().size());
		assertEquals("item8", fitted.rows().getLast().item());
		assertEquals(4, fitted.more(), "the 3 left out at 10 rows, and the one that no longer fits");
	}

	@Test
	void withinLeavesRoomForTheMoreLineToo() {
		// 10 rows take exactly 180, but with "+3 more" after them the panel needs 191.
		assertEquals(10, many(13).within(191, 18, 11).rows().size());
		assertEquals(9, many(13).within(190, 18, 11).rows().size());
		assertEquals(4, many(13).within(190, 18, 11).more());
	}

	@Test
	void withinNeedsNoMoreLineWhenEveryRowFits() {
		MaterialPanelRows<String> all = many(5).within(90, 18, 11);
		assertEquals(5, all.rows().size());
		assertEquals(0, all.more());
		// Less room than the five rows, and what is left out goes into "+N more", whose line then needs room too: 4 rows
		// and the line take 83, so 82 leaves 3 rows.
		assertEquals(4, many(5).within(83, 18, 11).rows().size());
		MaterialPanelRows<String> tight = many(5).within(82, 18, 11);
		assertEquals(3, tight.rows().size());
		assertEquals(2, tight.more());
	}

	@Test
	void withinChangesNothingWhenThereIsRoomAndNeverShowsMoreThanTheTenRows() {
		assertEquals(many(13), many(13).within(10_000, 18, 11));
		assertEquals(10, many(13).within(10_000, 18, 11).rows().size());
		assertEquals(3, many(13).within(10_000, 18, 11).more());
	}

	@Test
	void withinCountsEveryRowAsMoreWhenNoneFit() {
		MaterialPanelRows<String> none = many(13).within(5, 18, 11);
		assertTrue(none.rows().isEmpty());
		assertEquals(13, none.more());
		assertEquals(0, many(0).within(0, 18, 11).more());
	}

	@Test
	void anEmptyListHasNoRows() {
		MaterialPanelRows<String> empty = MaterialPanelRows.of(List.of(), ListSort.COUNT_TOTAL, false, false, 1);
		assertTrue(empty.rows().isEmpty());
		assertEquals(0, empty.more());
		// A list whose every row "Hide available" hides has none to show either.
		MaterialPanelRows<String> hidden = MaterialPanelRows.of(List.of(STONE), ListSort.COUNT_TOTAL, false, true, 1);
		assertTrue(hidden.rows().isEmpty());
		assertEquals(0, hidden.more());
	}
}
