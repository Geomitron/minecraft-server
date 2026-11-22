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
import dev.buildingplanner.look.ControlsLine.AimState;
import dev.buildingplanner.look.ControlsLine.CopyState;
import dev.buildingplanner.look.ControlsLine.DisableState;
import dev.buildingplanner.look.ControlsLine.Keys;
import dev.buildingplanner.look.ControlsLine.Line;
import dev.buildingplanner.look.ControlsLine.Look;
import dev.buildingplanner.look.ControlsLine.Text;
import dev.buildingplanner.look.ControlsLine.TurnState;
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
	private static final String NUDGES = "Arrow keys + Page Up/Down to nudge";

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
		assertEquals("Locked: Left-click to paste, I/J/U/O + Y/N to nudge, Scroll to turn, "
			+ "Middle-click to mirror, K to unlock, G for Zoop, Right-click to close",
			copy(state(CopyMode.SINGLE, true, 0, true, null), REBOUND));
	}

	@Test
	void theArrowKeysInAnotherOrderAreNamedOneByOne() {
		Keys swapped = new Keys("H", "R", List.of("Down Arrow", "Up Arrow", "Left Arrow", "Right Arrow"), false,
			"Page Up", "Page Down", "Left Control");
		assertTrue(copy(state(CopyMode.SINGLE, true, 0, false, null), swapped)
			.contains(", Down Arrow/Up Arrow/Left Arrow/Right Arrow + Page Up/Down to nudge, "));
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
			.contains(", Up Arrow/Right Arrow + Page Down to nudge, "));
		Keys onlyAlong = new Keys("H", "R", List.of("Up Arrow", "Down Arrow", "Left Arrow", "Right Arrow"), true, null,
			null, "Left Control");
		assertTrue(copy(state(CopyMode.SINGLE, true, 0, false, null), onlyAlong)
			.contains(", Arrow keys to nudge, "));
		Keys allUnbound = new Keys("H", "R", Arrays.asList(null, null, null, null), false, null, null, "Left Control");
		assertFalse(copy(state(CopyMode.SINGLE, true, 0, false, null), allUnbound).contains("nudge"));
	}

	@Test
	void theUpAndDownKeysShareTheirFirstWordOnlyOnce() {
		Keys keypad = new Keys("H", "R", List.of("Up Arrow", "Down Arrow", "Left Arrow", "Right Arrow"), true,
			"Keypad 8", "Keypad 2", "Left Control");
		assertTrue(copy(state(CopyMode.SINGLE, true, 0, false, null), keypad)
			.contains(", Arrow keys + Keypad 8/2 to nudge, "));
		Keys letters = new Keys("H", "R", List.of("Up Arrow", "Down Arrow", "Left Arrow", "Right Arrow"), true, "U", "J",
			"Left Control");
		assertTrue(copy(state(CopyMode.SINGLE, true, 0, false, null), letters)
			.contains(", Arrow keys + U/J to nudge, "));
		Keys mixed = new Keys("H", "R", List.of("Up Arrow", "Down Arrow", "Left Arrow", "Right Arrow"), true,
			"Page Up", "Left Shift", "Left Control");
		assertTrue(copy(state(CopyMode.SINGLE, true, 0, false, null), mixed)
			.contains(", Arrow keys + Page Up/Left Shift to nudge, "));
	}

	@Test
	void reboundArrowKeysAreNamedOneByOneBesideTheUpAndDownKeys() {
		Keys rebound = new Keys("H", "R", List.of("W", "S", "A", "D"), false, "Page Up", "Page Down", "Left Control");
		assertTrue(copy(state(CopyMode.SINGLE, true, 0, false, null), rebound)
			.contains(", W/S/A/D + Page Up/Down to nudge, "));
	}

	@Test
	void theDeleteBoxsLines() {
		assertEquals(List.of("Delete: Left-click to set the first corner, R for Aim, Right-click to cancel"),
			box("DELETE", 0, null, null));
		assertEquals(List.of("Delete: Left-click to set the opposite corner, R for Aim, Right-click to cancel (3 × 1 × 2)"),
			box("DELETE", 1, new int[] {3, 1, 2}, null));
		assertEquals(List.of("Delete: Left-click to set the height, R for Aim, Right-click to cancel (40 × 10 × 40)"),
			box("DELETE", 2, new int[] {40, 10, 40}, null));
		assertEquals(List.of("Too many blocks",
			"Delete: Left-click to set the height, R for Aim, Right-click to cancel (80 × 53 × 50)"),
			box("DELETE", 2, new int[] {80, 53, 50}, BoxCorners.Limit.TOO_MANY_BLOCKS));
	}

	@Test
	void theOtherBoxesLines() {
		assertEquals(List.of("Copy: Left-click to set the first corner, Right-click to cancel"),
			box("COPY", 0, null, null));
		assertEquals(List.of("Part of the box is too far away to read. Move closer.",
			"Material list: Left-click to set the opposite corner, Right-click to cancel (3 × 1 × 2)"),
			box("MATERIAL_LIST", 1, new int[] {3, 1, 2}, BoxCorners.Limit.NOT_LOADED));
		assertNull(ControlsLine.box("COPY", 0, null, null, "R").getFirst().size());
	}

	@Test
	void theDeleteBoxNamesTheModeKeyAsBoundAndLeavesItOutUnbound() {
		assertEquals(List.of("Delete: Left-click to set the first corner, G for Aim, Right-click to cancel"),
			box("DELETE", 0, null, null, "G"));
		assertEquals(List.of("Delete: Left-click to set the first corner, Right-click to cancel"),
			box("DELETE", 0, null, null, null));
		assertEquals(List.of("Copy: Left-click to set the first corner, Right-click to cancel"),
			box("COPY", 0, null, null, "R"));
		Line line = ControlsLine.box("DELETE", 0, null, null, "R").getFirst();
		List<String> actions = new ArrayList<>();
		collect(line.text(), Look.ACTION, actions);
		assertEquals(List.of("set the first corner", "Aim", "cancel"), actions);
	}

	@Test
	void aimWithNoMarks() {
		assertEquals(List.of("Delete, Aim: Hold Left Control to mark, R for Box, Right-click to close"),
			aim(new AimState(false, false, -1, false), DEFAULT));
	}

	@Test
	void aimWithMarks() {
		assertEquals(List.of("Delete, Aim: Hold Left-click to erase, Hold Left Control to mark, R for Box, Right-click to "
			+ "clear"), aim(new AimState(true, false, -1, false), DEFAULT));
	}

	@Test
	void aSweepThatUnmarksSaysUnmark() {
		assertEquals(List.of("Delete, Aim: Hold Left-click to erase, Hold Left Control to unmark, R for Box, Right-click "
			+ "to clear"), aim(new AimState(true, true, -1, false), DEFAULT));
		// A sweep that has unmarked every mark still unmarks until the key is let go.
		assertEquals(List.of("Delete, Aim: Hold Left Control to unmark, R for Box, Right-click to close"),
			aim(new AimState(false, true, -1, false), DEFAULT));
	}

	@Test
	void theHoldsProgressTakesTheErasesPlace() {
		assertEquals(List.of("Delete, Aim: Erasing 0%, Hold Left Control to mark, R for Box, Right-click to clear"),
			aim(new AimState(true, false, 0, false), DEFAULT));
		assertEquals(List.of("Delete, Aim: Erasing 50%, Hold Left Control to mark, R for Box, Right-click to clear"),
			aim(new AimState(true, false, 50, false), DEFAULT));
		assertEquals(List.of("Delete, Aim: Erasing 95%, Hold Left Control to mark, R for Box, Right-click to clear"),
			aim(new AimState(true, false, 95, false), DEFAULT));
	}

	@Test
	void atTheCapAWarningGoesAbove() {
		assertEquals(List.of("Too many blocks", "Delete, Aim: Hold Left-click to erase, Hold Left Control to mark, R for "
			+ "Box, Right-click to clear"), aim(new AimState(true, false, -1, true), DEFAULT));
		List<String> warnings = new ArrayList<>();
		collect(ControlsLine.deleteAim(new AimState(true, false, -1, true), DEFAULT).getFirst().text(), Look.WARNING,
			warnings);
		assertEquals(List.of("Too many blocks"), warnings);
	}

	@Test
	void aimsActionsAreActions() {
		List<String> actions = new ArrayList<>();
		collect(ControlsLine.deleteAim(new AimState(true, false, -1, false), DEFAULT).getFirst().text(), Look.ACTION,
			actions);
		assertEquals(List.of("erase", "mark", "Box", "clear"), actions);
		actions.clear();
		collect(ControlsLine.deleteAim(new AimState(true, false, 60, false), DEFAULT).getFirst().text(), Look.ACTION,
			actions);
		assertEquals(List.of("Erasing 60%", "mark", "Box", "clear"), actions);
	}

	@Test
	void aimNamesTheKeysAsBoundAndLeavesOutUnboundOnes() {
		assertEquals(List.of("Delete, Aim: Hold Left-click to erase, Hold Left Alt to mark, G for Box, Right-click to "
			+ "clear"), aim(new AimState(true, false, -1, false), REBOUND));
		assertEquals(List.of("Delete, Aim: Hold Left-click to erase, Right-click to clear"),
			aim(new AimState(true, false, -1, false), UNBOUND));
	}

	@Test
	void theTurningLineHoldsTheSprintKeyToTurnOrFlip() {
		assertEquals("Hold Left Control to turn or flip", turning(new TurnState(true, true, false, false), "Left Control"));
		assertEquals("Hold Left Control to turn", turning(new TurnState(true, false, false, false), "Left Control"));
		assertEquals("Hold Left Control to flip", turning(new TurnState(false, true, false, false), "Left Control"));
	}

	@Test
	void withTheSprintKeyHeldTheTurningLineNamesTheWheelAndTheMiddleClick() {
		assertEquals("Scroll to turn, Middle-click to flip", turning(new TurnState(true, true, true, false), "Left Control"));
		assertEquals("Scroll to turn", turning(new TurnState(true, false, true, false), "Left Control"));
		assertEquals("Middle-click to flip", turning(new TurnState(false, true, true, false), "Left Control"));
	}

	@Test
	void onceTurnedTheTurningLineStartsTurned() {
		assertEquals("Turned: Hold Left Control to turn or flip",
			turning(new TurnState(true, true, false, true), "Left Control"));
		assertEquals("Turned: Scroll to turn, Middle-click to flip",
			turning(new TurnState(true, true, true, true), "Left Control"));
	}

	@Test
	void theTurningLineNamesTheSprintKeyAsBoundAndItsActionsAreActions() {
		assertEquals("Hold Right Control to turn or flip", turning(new TurnState(true, true, false, false), "Right Control"));
		List<String> actions = new ArrayList<>();
		collect(ControlsLine.turning(new TurnState(true, true, true, true), "Left Control").text(), Look.ACTION, actions);
		assertEquals(List.of("turn", "flip"), actions);
	}

	@Test
	void noTurningLineForABlockThatNeitherTurnsNorFlipsOrWithTheSprintKeyUnbound() {
		assertNull(ControlsLine.turning(new TurnState(false, false, false, false), "Left Control"));
		assertNull(ControlsLine.turning(new TurnState(false, false, true, true), "Left Control"));
		assertNull(ControlsLine.turning(new TurnState(true, true, false, false), null));
	}

	@Test
	void withoutTheSprintKeyDisablesLineIsTheTurningLineWithNoTurned() {
		assertEquals("Hold Left Control to turn or flip", disable(new TurnState(true, true, false, true), false, true));
		assertEquals("Hold Left Control to turn", disable(new TurnState(true, false, false, false), false, true));
		assertEquals("Hold Left Control to flip", disable(new TurnState(false, true, false, false), false, true));
		assertEquals("Hold Right Control to turn or flip", read(ControlsLine.disable(
			new DisableState(new TurnState(true, true, false, false), false, true), "Right Control").text()));
	}

	@Test
	void withTheSprintKeyHeldDisablesLinePlacesAndNamesTheWheelAndTheMiddleClick() {
		assertEquals("Right-click to place, Scroll to turn, Middle-click to flip",
			disable(new TurnState(true, true, true, false), false, true));
		assertEquals("Right-click to place, Scroll to turn", disable(new TurnState(true, false, true, false), false, true));
		assertEquals("Right-click to place, Middle-click to flip",
			disable(new TurnState(false, true, true, false), false, true));
		assertEquals("Turned: Right-click to place, Scroll to turn, Middle-click to flip",
			disable(new TurnState(true, true, true, true), false, true));
	}

	@Test
	void disablesLineSaysInRedWhereTheTurnedBlockCantBePlaced() {
		assertEquals("Turned: Can't place here, Scroll to turn", disable(new TurnState(true, false, true, true), true, false));
		Line line = ControlsLine.disable(new DisableState(new TurnState(true, true, true, true), false, false),
			"Left Control");
		List<String> warnings = new ArrayList<>();
		collect(line.text(), Look.WARNING, warnings);
		assertEquals(List.of("Can't place here"), warnings);
		List<String> actions = new ArrayList<>();
		collect(ControlsLine.disable(new DisableState(new TurnState(true, true, true, true), false, true),
			"Left Control").text(), Look.ACTION, actions);
		assertEquals(List.of("place", "turn", "flip"), actions);
	}

	@Test
	void noDisableLineWithoutTheSprintKeyForABlockThatHangsOnTheBlockBehindIt() {
		assertNull(ControlsLine.disable(new DisableState(new TurnState(true, false, false, false), true, true),
			"Left Control"));
		assertEquals("Right-click to place, Scroll to turn", disable(new TurnState(true, false, true, false), true, true));
		assertNull(ControlsLine.disable(new DisableState(new TurnState(false, false, true, false), false, true),
			"Left Control"));
		assertNull(ControlsLine.disable(new DisableState(new TurnState(true, true, true, false), false, true), null));
	}

	private static String disable(TurnState turn, boolean hangs, boolean stays) {
		Line line = ControlsLine.disable(new DisableState(turn, hangs, stays), "Left Control");
		assertNull(line.size());
		return read(line.text());
	}

	private static String turning(TurnState state, String sprint) {
		Line line = ControlsLine.turning(state, sprint);
		assertNull(line.size());
		return read(line.text());
	}

	private static List<String> aim(AimState state, Keys keys) {
		return ControlsLine.deleteAim(state, keys).stream().map(line -> {
			assertNull(line.size());
			return read(line.text());
		}).toList();
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
		return box(purpose, corners, size, limit, "R");
	}

	private static List<String> box(String purpose, int corners, int[] size, BoxCorners.Limit limit, String mode) {
		return ControlsLine.box(purpose, corners, size, limit, mode).stream()
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
