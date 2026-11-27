package dev.buildingplanner.look;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.buildingplanner.look.ClickItems.Entry;
import dev.buildingplanner.look.MaterialPanelRows.Colour;
import dev.buildingplanner.look.MaterialPanelRows.Row;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
	void theTitleAndHeadingsReadAsGeoWroteThem() throws Exception {
		JsonObject lang;
		try (var reader = new InputStreamReader(
			ClickItemsTest.class.getResourceAsStream("/assets/buildingplanner/lang/en_us.json"), StandardCharsets.UTF_8)) {
			lang = JsonParser.parseReader(reader).getAsJsonObject();
		}
		assertEquals("Required blocks", lang.get(ClickItems.TITLE_KEY).getAsString());
		assertEquals(List.of("Uses", "Hold", "Need"),
			ClickItems.HEADING_KEYS.stream().map(key -> lang.get(key).getAsString()).toList());
	}

	@Test
	void eachRowIsUsesHoldsAndMissingRedUnlessNoneAre() {
		MaterialPanelRows<String> rows = ClickItems.rows(CLICK);
		assertEquals(new Row<>("stone", 12, 4, 8, Colour.RED), rows.rows().get(0));
		assertEquals(new Row<>("planks", 24, 30, 0, Colour.GREEN), rows.rows().get(2));
		assertEquals(0, rows.more());
	}

	@Test
	void theMissingItemsComeFirstThenTheRestEachMostUsedFirstAndEqualCountsByName() {
		assertEquals(List.of("stone", "glass", "planks", "andesite"), items(ClickItems.rows(CLICK)));
	}

	@Test
	void theRowsPastTenGoToMore() {
		List<Entry<String>> entries = new ArrayList<>(CLICK);
		for (int i = 0; i < 8; i++) {
			entries.add(new Entry<>("block" + i, "minecraft:block" + i, 1, 1));
		}
		MaterialPanelRows<String> rows = ClickItems.rows(entries);
		assertEquals(10, rows.rows().size());
		assertEquals(List.of("stone", "glass", "planks", "andesite"), items(rows).subList(0, 4));
		assertEquals(2, rows.more());
		// The room the panel has cuts them further, as the material list panel's.
		MaterialPanelRows<String> cut = rows.within(18, 18, 11);
		assertEquals(List.of(), items(cut));
		assertEquals(12, cut.more());
	}
}
