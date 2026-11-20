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
