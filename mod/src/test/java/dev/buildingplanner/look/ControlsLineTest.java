package dev.buildingplanner.look;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.buildingplanner.input.BoxCorners;
import dev.buildingplanner.input.CopyColour.Refusal;
import dev.buildingplanner.input.CopyMode;
import dev.buildingplanner.look.ControlsLine.CopyState;
import dev.buildingplanner.look.ControlsLine.Keys;
import dev.buildingplanner.look.ControlsLine.Line;
import dev.buildingplanner.look.ControlsLine.Look;
import dev.buildingplanner.look.ControlsLine.Text;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ControlsLineTest {
	/** The copy's keys as they are by default, with their names as the game gives them. */
	private static final Keys DEFAULT = new Keys("H", "R", List.of("Up Arrow", "Down Arrow", "Left Arrow", "Right Arrow"),
		true, "Page Up", "Page Down", "Left Control");
	/** The keys as a player rebound them. */
	private static final Keys REBOUND = new Keys("K", "G", List.of("I", "J", "U", "O"), false, "Y", "N", "Left Alt");
	/** Every key unbound. */
	private static final Keys UNBOUND = new Keys(null, null, Arrays.asList(null, null, null, null), false, null, null,
		null);
	private static final String SINGLE_END = "Scroll to turn, Middle-click to mirror, H to lock, R for Zoop, "
		+ "Right-click to close";
	private static final String NUDGES = "Arrow keys to nudge, Page Up/Page Down to raise or lower";

	private static JsonObject lang;

	@BeforeAll
	static void readLanguage() throws Exception {
		try (var reader = new InputStreamReader(
			ControlsLineTest.class.getResourceAsStream("/assets/buildingplanner/lang/en_us.json"),
			StandardCharsets.UTF_8)) {
			lang = JsonParser.parseReader(reader).getAsJsonObject();
		}
	}

	@Test
	void singleFollowingTheAim() {
		assertEquals("Left-click to paste, " + SINGLE_END, copy(state(CopyMode.SINGLE, false, 0, false, null), DEFAULT));
	}

	@Test
	void afterAPasteHoldingTheSprintKeyLinesUp() {
		assertEquals("Left-click to paste, Scroll to turn, Middle-click to mirror, Hold Left Control to line up, H to "
			+ "lock, R for Zoop, Right-click to close", copy(state(CopyMode.SINGLE, false, 0, true, null), DEFAULT));
	}

	@Test
	void locked() {
		assertEquals("Locked: Left-click to paste, " + NUDGES + ", Scroll to turn, Middle-click to mirror, H to unlock, "
			+ "R for Zoop, Right-click to close", copy(state(CopyMode.SINGLE, true, 0, false, null), DEFAULT));
	}

	@Test
	void lockedTheSprintKeyLinesNothingUpSoTheLineLeavesItOut() {
		assertEquals("Locked: Left-click to paste, " + NUDGES + ", Scroll to turn, Middle-click to mirror, H to unlock, "
			+ "R for Zoop, Right-click to close", copy(state(CopyMode.SINGLE, true, 0, true, null), DEFAULT));
		assertFalse(copy(state(CopyMode.ZOOP, true, 0, true, null), DEFAULT).contains("line up"));
	}

	@Test
	void zoopAndVerticalBeforeTheFirstClick() {
		assertEquals("Zoop: Left-click to set the start, Scroll to turn, Middle-click to mirror, H to lock, R for "
			+ "Vertical, Right-click to close", copy(state(CopyMode.ZOOP, false, 0, false, null), DEFAULT));
		assertEquals("Vertical: Left-click to set the start, Scroll to turn, Middle-click to mirror, H to lock, R for "
			+ "Single, Right-click to close", copy(state(CopyMode.VERTICAL, false, 0, false, null), DEFAULT));
		assertEquals("Zoop, locked: Left-click to set the start, " + NUDGES + ", Scroll to turn, Middle-click to "
			+ "mirror, H to unlock, R for Vertical, Right-click to close",
			copy(state(CopyMode.ZOOP, true, 0, false, null), DEFAULT));
		assertEquals("Vertical, locked: Left-click to set the start, " + NUDGES + ", Scroll to turn, Middle-click to "
			+ "mirror, H to unlock, R for Single, Right-click to close",
			copy(state(CopyMode.VERTICAL, true, 0, false, null), DEFAULT));
	}

	@Test
	void afterTheFirstClickTheRowsPasteWithItsCount() {
		assertEquals("Zoop: Left-click to paste 4 copies, Right-click to cancel",
			copy(state(CopyMode.ZOOP, false, 4, true, null), DEFAULT));
		assertEquals("Vertical: Left-click to paste 1 copy, Right-click to cancel",
			copy(state(CopyMode.VERTICAL, true, 1, false, null), DEFAULT));
	}

	@Test
	void whileRedTheReasonTakesThePastesPlace() {
		assertEquals("Can't paste: part of the copy is outside the world, " + SINGLE_END,
			copy(state(CopyMode.SINGLE, false, 0, false, Refusal.BUILD_HEIGHT), DEFAULT));
		assertEquals("Locked: Can't paste: part of the copy is outside the world, " + NUDGES + ", Scroll to turn, "
			+ "Middle-click to mirror, H to unlock, R for Zoop, Right-click to close",
			copy(state(CopyMode.SINGLE, true, 0, false, Refusal.WORLD_BORDER), DEFAULT));
		assertEquals("Zoop: Can't paste: too many blocks, Right-click to cancel",
			copy(state(CopyMode.ZOOP, false, 10, false, Refusal.TOO_MANY_BLOCKS), DEFAULT));
		// A zoop's first click still fixes the row while the copy is red, so it stays, after the reason.
		assertEquals("Zoop: Can't paste: part of the copy is outside the world, Left-click to set the start, Scroll to "
			+ "turn, Middle-click to mirror, H to lock, R for Vertical, Right-click to close",
			copy(state(CopyMode.ZOOP, false, 0, false, Refusal.BUILD_HEIGHT), DEFAULT));
		assertEquals("Vertical, locked: Can't paste: too many blocks, Left-click to set the start, " + NUDGES
			+ ", Scroll to turn, Middle-click to mirror, H to unlock, R for Single, Right-click to close",
			copy(state(CopyMode.VERTICAL, true, 0, false, Refusal.TOO_MANY_BLOCKS), DEFAULT));
	}

	@Test
	void theReasonIsAWarningAndTheActionsAreActions() {
		Line line = ControlsLine.copy(state(CopyMode.SINGLE, false, 0, false, Refusal.BUILD_HEIGHT), DEFAULT);
		List<String> warnings = new ArrayList<>();
		List<String> actions = new ArrayList<>();
		collect(line.text(), Look.WARNING, warnings);
		collect(line.text(), Look.ACTION, actions);
		assertEquals(List.of("Can't paste: part of the copy is outside the world"), warnings);
		assertEquals(List.of("turn", "mirror", "lock", "Zoop", "close"), actions);
	}

	@Test
	void reboundKeysAreNamedAsBound() {
		assertEquals("Left-click to paste, Scroll to turn, Middle-click to mirror, Hold Left Alt to line up, K to lock, "
			+ "G for Zoop, Right-click to close", copy(state(CopyMode.SINGLE, false, 0, true, null), REBOUND));
		assertEquals("Locked: Left-click to paste, I/J/U/O to nudge, Y/N to raise or lower, Scroll to turn, "
			+ "Middle-click to mirror, K to unlock, G for Zoop, Right-click to close",
			copy(state(CopyMode.SINGLE, true, 0, true, null), REBOUND));
	}

	@Test
	void theArrowKeysInAnotherOrderAreNamedOneByOne() {
		Keys swapped = new Keys("H", "R", List.of("Down Arrow", "Up Arrow", "Left Arrow", "Right Arrow"), false,
			"Page Up", "Page Down", "Left Control");
		assertTrue(copy(state(CopyMode.SINGLE, true, 0, false, null), swapped)
			.contains(", Down Arrow/Up Arrow/Left Arrow/Right Arrow to nudge, "));
	}

	@Test
	void unboundKeysAreLeftOut() {
		assertEquals("Left-click to paste, Scroll to turn, Middle-click to mirror, Right-click to close",
			copy(state(CopyMode.SINGLE, false, 0, true, null), UNBOUND));
		assertEquals("Locked: Left-click to paste, Scroll to turn, Middle-click to mirror, Right-click to close",
			copy(state(CopyMode.SINGLE, true, 0, true, null), UNBOUND));
		Keys some = new Keys("H", "R", Arrays.asList("Up Arrow", null, null, "Right Arrow"), false, null, "Page Down",
			"Left Control");
		assertTrue(copy(state(CopyMode.SINGLE, true, 0, false, null), some)
			.contains(", Up Arrow/Right Arrow to nudge, Page Down to raise or lower, "));
	}

	@Test
	void theDeleteBoxsLines() {
		assertEquals(List.of("Delete: Left-click to set the first corner, Right-click to cancel"),
			box("DELETE", 0, null, null));
		assertEquals(List.of("Delete: Left-click to set the opposite corner, Right-click to cancel (3 × 1 × 2)"),
			box("DELETE", 1, new int[] {3, 1, 2}, null));
		assertEquals(List.of("Delete: Left-click to set the height, Right-click to cancel (40 × 10 × 40)"),
			box("DELETE", 2, new int[] {40, 10, 40}, null));
		assertEquals(List.of("Too many blocks", "Delete: Left-click to set the height, Right-click to cancel (80 × 53 × 50)"),
			box("DELETE", 2, new int[] {80, 53, 50}, BoxCorners.Limit.TOO_MANY_BLOCKS));
	}

	@Test
	void theOtherBoxesLines() {
		assertEquals(List.of("Copy: Left-click to set the first corner, Right-click to cancel"),
			box("COPY", 0, null, null));
		assertEquals(List.of("Part of the box is too far away to read. Move closer.",
			"Material list: Left-click to set the opposite corner, Right-click to cancel (3 × 1 × 2)"),
			box("MATERIAL_LIST", 1, new int[] {3, 1, 2}, BoxCorners.Limit.NOT_LOADED));
		assertNull(ControlsLine.box("COPY", 0, null, null).getFirst().size());
	}

	private static CopyState state(CopyMode mode, boolean locked, int row, boolean pasted, Refusal refusal) {
		return new CopyState(mode, locked, row, pasted, refusal);
	}

	private static String copy(CopyState state, Keys keys) {
		Line line = ControlsLine.copy(state, keys);
		assertNull(line.size());
		return read(line.text());
	}

	private static List<String> box(String purpose, int corners, int[] size, BoxCorners.Limit limit) {
		return ControlsLine.box(purpose, corners, size, limit).stream()
			.map(line -> line.size() == null ? read(line.text()) : read(line.text()) + " " + read(line.size()))
			.toList();
	}

	/** The text as the game shows it in English, every key in it from en_us.json. */
	private static String read(Text text) {
		return switch (text) {
			case ControlsLine.Translated translated -> {
				assertTrue(lang.has(translated.key()), "en_us.json has " + translated.key());
				yield String.format(lang.get(translated.key()).getAsString(),
					translated.args().stream().map(ControlsLineTest::read).toArray());
			}
			case ControlsLine.Literal literal -> literal.text();
			case ControlsLine.Joined joined -> joined.parts().stream().map(ControlsLineTest::read)
				.collect(Collectors.joining(", "));
		};
	}

	/** The texts of the parts in that look, outermost first. */
	private static void collect(Text text, Look look, List<String> found) {
		switch (text) {
			case ControlsLine.Translated translated -> {
				if (translated.look() == look) {
					found.add(read(translated));
				}
				translated.args().forEach(arg -> collect(arg, look, found));
			}
			case ControlsLine.Literal literal -> {
			}
			case ControlsLine.Joined joined -> joined.parts().forEach(part -> collect(part, look, found));
		}
	}
}
