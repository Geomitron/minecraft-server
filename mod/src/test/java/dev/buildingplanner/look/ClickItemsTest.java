package dev.buildingplanner.look;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.buildingplanner.look.ClickItems.Entry;
import dev.buildingplanner.look.ClickItems.Order;
import dev.buildingplanner.look.MaterialPanelRows.Colour;
import dev.buildingplanner.look.MaterialPanelRows.Row;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClickItemsTest {
	// A click of 24 oak planks with 30 held, 12 stone with 4 held, 6 glass with none and 6 andesite with 6.
	private static final Entry<String> PLANKS = new Entry<>("planks", "minecraft:oak_planks", 24, 30);
	private static final Entry<String> STONE = new Entry<>("stone", "minecraft:stone", 12, 4);
	private static final Entry<String> GLASS = new Entry<>("glass", "minecraft:glass", 6, 0);
	private static final Entry<String> ANDESITE = new Entry<>("andesite", "minecraft:andesite", 6, 6);
	private static final List<Entry<String>> CLICK = List.of(PLANKS, STONE, GLASS, ANDESITE);

	private static List<String> items(MaterialPanelRows<String> rows) {
		return rows.rows().stream().map(Row::item).toList();
	}

	@Test
	void eachRowIsUsesHoldsAndMissingRedUnlessNoneAre() {
		MaterialPanelRows<String> rows = ClickItems.rows(CLICK, Order.MOST_FIRST, 10);
		assertEquals(new Row<>("planks", 24, 30, 0, Colour.GREEN), rows.rows().get(0));
		assertEquals(new Row<>("stone", 12, 4, 8, Colour.RED), rows.rows().get(1));
		assertEquals(0, rows.more());
	}

	@Test
	void shortFirstPutsTheMissingItemsFirstEachMostUsedFirst() {
		assertEquals(List.of("stone", "glass", "planks", "andesite"), items(ClickItems.rows(CLICK, Order.SHORT_FIRST, 10)));
	}

	@Test
	void mostFirstGoesByUsesAndEqualCountsByName() {
		assertEquals(List.of("planks", "stone", "andesite", "glass"), items(ClickItems.rows(CLICK, Order.MOST_FIRST, 10)));
	}

	@Test
	void theRowsPastTheMostGoToMore() {
		MaterialPanelRows<String> rows = ClickItems.rows(CLICK, Order.SHORT_FIRST, 2);
		assertEquals(List.of("stone", "glass"), items(rows));
		assertEquals(2, rows.more());
		// The room the panel has cuts them further, as the material list panel's.
		MaterialPanelRows<String> cut = rows.within(18, 18, 11);
		assertEquals(List.of(), items(cut));
		assertEquals(4, cut.more());
	}
}
