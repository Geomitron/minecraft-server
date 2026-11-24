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
import dev.buildingplanner.look.ControlsLine.Part;
import dev.buildingplanner.look.ControlsLine.ShapeState;
import dev.buildingplanner.look.ControlsLine.Text;
import dev.buildingplanner.look.ControlsLine.TurnState;
import dev.buildingplanner.look.ControlsLine.Underway;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Each line as the in-game checks read it: its label, then each other rectangle in [ ], in English from en_us.json; a
 * warning above the controls is its text alone.
 */
class ControlsLineTest {
	/** The copy's keys as they are by default, with their names as the game gives them. */
	private static final Keys DEFAULT = new Keys("H", "R", List.of("Up Arrow", "Down Arrow", "Left Arrow", "Right Arrow"),
		true, "Page Up", "Page Down", "Left Control");
	/** The keys as a player rebound them. */
	private static final Keys REBOUND = new Keys("K", "G", List.of("I", "J", "U", "O"), false, "Y", "N", "Left Alt");
	/** Every key unbound. */
	private static final Keys UNBOUND = new Keys(null, null, Arrays.asList(null, null, null, null), false, null, null,
		null);
	private static final String TURN_MIRROR = "[Scroll: turn][Middle-click: mirror]";
	private static final String COPY_END = TURN_MIRROR + "[H: lock][R: Zoop mode][Esc/Left-click: close]";
	private static final String LOCKED_END = TURN_MIRROR + "[H: unlock][R: Zoop mode][Esc/Left-click: close]";
	private static final String NUDGES = "[Arrow keys + Page Up/Down: nudge]";

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
		assertEquals("[Right-click: paste]" + COPY_END, copy(state(CopyMode.SINGLE, false, 0, false, null), DEFAULT));
	}

	@Test
	void afterAPasteHoldingTheSprintKeyLinesUp() {
		assertEquals("[Right-click: paste]" + TURN_MIRROR + "[Hold Left Control: line up][H: lock][R: Zoop mode]"
			+ "[Esc/Left-click: close]", copy(state(CopyMode.SINGLE, false, 0, true, null), DEFAULT));
	}

	@Test
	void locked() {
		assertEquals("Locked: [Right-click: paste]" + NUDGES + LOCKED_END,
			copy(state(CopyMode.SINGLE, true, 0, false, null), DEFAULT));
	}

	@Test
	void lockedTheSprintKeyLinesNothingUpSoTheLineLeavesItOut() {
		assertEquals("Locked: [Right-click: paste]" + NUDGES + LOCKED_END,
			copy(state(CopyMode.SINGLE, true, 0, true, null), DEFAULT));
		assertFalse(copy(state(CopyMode.ZOOP, true, 0, true, null), DEFAULT).contains("line up"));
	}

	@Test
	void zoopAndVerticalBeforeTheFirstClick() {
		assertEquals("Zoop: [Right-click: set the start]" + COPY_END,
			copy(state(CopyMode.ZOOP, false, 0, false, null), DEFAULT));
		assertEquals("Vertical: [Right-click: set the start]" + COPY_END,
			copy(state(CopyMode.VERTICAL, false, 0, false, null), DEFAULT));
		assertEquals("Zoop, locked: [Right-click: set the start]" + NUDGES + LOCKED_END,
			copy(state(CopyMode.ZOOP, true, 0, false, null), DEFAULT));
		assertEquals("Vertical, locked: [Right-click: set the start]" + NUDGES + LOCKED_END,
			copy(state(CopyMode.VERTICAL, true, 0, false, null), DEFAULT));
	}

	@Test
	void afterTheFirstClickTheRowsPasteWithItsCount() {
		assertEquals("Zoop: [Right-click: paste 4 copies][Esc/Left-click: cancel]",
			copy(state(CopyMode.ZOOP, false, 4, true, null), DEFAULT));
		assertEquals("Zoop: [Right-click: paste 1 copy][Esc/Left-click: cancel]",
			copy(state(CopyMode.ZOOP, false, 1, true, null), DEFAULT));
		assertEquals("Vertical: [Right-click: paste 1 copy][Esc/Left-click: cancel]",
			copy(state(CopyMode.VERTICAL, true, 1, false, null), DEFAULT));
	}

	@Test
	void whileRedTheReasonTakesThePastesPlace() {
		assertEquals("[Can't paste: part of the copy is outside the world]" + COPY_END,
			copy(state(CopyMode.SINGLE, false, 0, false, Refusal.BUILD_HEIGHT), DEFAULT));
		assertEquals("Locked: [Can't paste: part of the copy is outside the world]" + NUDGES + LOCKED_END,
			copy(state(CopyMode.SINGLE, true, 0, false, Refusal.WORLD_BORDER), DEFAULT));
		assertEquals("Zoop: [Can't paste: too many blocks][Esc/Left-click: cancel]",
			copy(state(CopyMode.ZOOP, false, 10, false, Refusal.TOO_MANY_BLOCKS), DEFAULT));
		// A zoop's first click still fixes the row while the copy is red, so it stays, after the reason.
		assertEquals("Zoop: [Can't paste: part of the copy is outside the world][Right-click: set the start]" + COPY_END,
			copy(state(CopyMode.ZOOP, false, 0, false, Refusal.BUILD_HEIGHT), DEFAULT));
		assertEquals("Zoop, locked: [Can't paste: part of the copy is outside the world][Right-click: set the start]"
			+ NUDGES + LOCKED_END, copy(state(CopyMode.ZOOP, true, 0, false, Refusal.WORLD_BORDER), DEFAULT));
		assertEquals("Vertical: [Can't paste: too many blocks][Esc/Left-click: cancel]",
			copy(state(CopyMode.VERTICAL, true, 6, false, Refusal.TOO_MANY_BLOCKS), DEFAULT));
		assertEquals("Vertical, locked: [Can't paste: too many blocks][Right-click: set the start]" + NUDGES + LOCKED_END,
			copy(state(CopyMode.VERTICAL, true, 0, false, Refusal.TOO_MANY_BLOCKS), DEFAULT));
	}

	@Test
	void theReasonIsARectangleInRedAndTheActionsAreActions() {
		Line line = ControlsLine.copy(state(CopyMode.SINGLE, false, 0, false, Refusal.BUILD_HEIGHT), DEFAULT);
		assertEquals(Look.WARNING, ((ControlsLine.Translated) line.rects().getFirst()).look());
		assertEquals(List.of("Can't paste: part of the copy is outside the world"), collect(line, Look.WARNING));
		assertEquals(List.of("turn", "mirror", "lock", "Zoop mode", "close"), collect(line, Look.ACTION));
	}

	@Test
	void eachControlIsARectangleOfItsOwnAfterTheLabel() {
		Line line = ControlsLine.copy(state(CopyMode.ZOOP, true, 0, false, null), DEFAULT);
		assertEquals("Zoop, locked:", read(line.label()));
		assertEquals(List.of("Right-click: set the start", "Arrow keys + Page Up/Down: nudge", "Scroll: turn",
			"Middle-click: mirror", "H: unlock", "R: Zoop mode", "Esc/Left-click: close"),
			line.rects().stream().map(ControlsLineTest::read).toList());
		assertEquals(8, line.all().size());
		assertNull(ControlsLine.copy(state(CopyMode.SINGLE, false, 0, false, null), DEFAULT).label());
	}

	@Test
	void reboundKeysAreNamedAsBound() {
		assertEquals("[Right-click: paste]" + TURN_MIRROR + "[Hold Left Alt: line up][K: lock][G: Zoop mode]"
			+ "[Esc/Left-click: close]", copy(state(CopyMode.SINGLE, false, 0, true, null), REBOUND));
		assertEquals("Locked: [Right-click: paste][I/J/U/O + Y/N: nudge]" + TURN_MIRROR
			+ "[K: unlock][G: Zoop mode][Esc/Left-click: close]", copy(state(CopyMode.SINGLE, true, 0, true, null), REBOUND));
	}

	@Test
	void theArrowKeysInAnotherOrderAreNamedOneByOne() {
		Keys swapped = new Keys("H", "R", List.of("Down Arrow", "Up Arrow", "Left Arrow", "Right Arrow"), false,
			"Page Up", "Page Down", "Left Control");
		assertTrue(copy(state(CopyMode.SINGLE, true, 0, false, null), swapped)
			.contains("[Down Arrow/Up Arrow/Left Arrow/Right Arrow + Page Up/Down: nudge]"));
	}

	@Test
	void unboundKeysAreLeftOut() {
		assertEquals("[Right-click: paste]" + TURN_MIRROR + "[Esc/Left-click: close]",
			copy(state(CopyMode.SINGLE, false, 0, true, null), UNBOUND));
		assertEquals("Locked: [Right-click: paste]" + TURN_MIRROR + "[Esc/Left-click: close]",
			copy(state(CopyMode.SINGLE, true, 0, true, null), UNBOUND));
		Keys some = new Keys("H", "R", Arrays.asList("Up Arrow", null, null, "Right Arrow"), false, null, "Page Down",
			"Left Control");
		assertTrue(copy(state(CopyMode.SINGLE, true, 0, false, null), some)
			.contains("[Up Arrow/Right Arrow + Page Down: nudge]"));
		Keys onlyAlong = new Keys("H", "R", List.of("Up Arrow", "Down Arrow", "Left Arrow", "Right Arrow"), true, null,
			null, "Left Control");
		assertTrue(copy(state(CopyMode.SINGLE, true, 0, false, null), onlyAlong).contains("[Arrow keys: nudge]"));
		Keys allUnbound = new Keys("H", "R", Arrays.asList(null, null, null, null), false, null, null, "Left Control");
		assertFalse(copy(state(CopyMode.SINGLE, true, 0, false, null), allUnbound).contains("nudge"));
	}

	@Test
	void theUpAndDownKeysShareTheirFirstWordOnlyOnce() {
		Keys keypad = new Keys("H", "R", List.of("Up Arrow", "Down Arrow", "Left Arrow", "Right Arrow"), true,
			"Keypad 8", "Keypad 2", "Left Control");
		assertTrue(copy(state(CopyMode.SINGLE, true, 0, false, null), keypad).contains("[Arrow keys + Keypad 8/2: nudge]"));
		Keys letters = new Keys("H", "R", List.of("Up Arrow", "Down Arrow", "Left Arrow", "Right Arrow"), true, "U", "J",
			"Left Control");
		assertTrue(copy(state(CopyMode.SINGLE, true, 0, false, null), letters).contains("[Arrow keys + U/J: nudge]"));
		Keys mixed = new Keys("H", "R", List.of("Up Arrow", "Down Arrow", "Left Arrow", "Right Arrow"), true,
			"Page Up", "Left Shift", "Left Control");
		assertTrue(copy(state(CopyMode.SINGLE, true, 0, false, null), mixed)
			.contains("[Arrow keys + Page Up/Left Shift: nudge]"));
	}

	@Test
	void reboundArrowKeysAreNamedOneByOneBesideTheUpAndDownKeys() {
		Keys rebound = new Keys("H", "R", List.of("W", "S", "A", "D"), false, "Page Up", "Page Down", "Left Control");
		assertTrue(copy(state(CopyMode.SINGLE, true, 0, false, null), rebound).contains("[W/S/A/D + Page Up/Down: nudge]"));
	}

	@Test
	void theDeleteBoxsLines() {
		assertEquals(List.of("Delete: [Left-click: set the first corner][R: Delete mode][Right-click: cancel]"),
			box("DELETE", 0, null, null));
		assertEquals(List.of("Delete: [Left-click: set the opposite corner][R: Delete mode][Right-click: cancel]"
			+ "[3 × 1 × 2]"), box("DELETE", 1, new int[] {3, 1, 2}, null));
		assertEquals(List.of("Delete: [Left-click: set the height][R: Delete mode][Right-click: cancel][40 × 10 × 40]"),
			box("DELETE", 2, new int[] {40, 10, 40}, null));
		assertEquals(List.of("Too many blocks",
			"Delete: [Left-click: set the height][R: Delete mode][Right-click: cancel][80 × 53 × 50]"),
			box("DELETE", 2, new int[] {80, 53, 50}, BoxCorners.Limit.TOO_MANY_BLOCKS));
	}

	@Test
	void theOtherBoxesLines() {
		assertEquals(List.of("Copy: [Left-click: set the first corner][Right-click: cancel]"),
			box("COPY", 0, null, null));
		assertEquals(List.of("Copy: [Left-click: set the opposite corner][Right-click: cancel][12 × 1 × 8]"),
			box("COPY", 1, new int[] {12, 1, 8}, null));
		assertEquals(List.of("Copy: [Left-click: set the height][Right-click: cancel][12 × 5 × 8]"),
			box("COPY", 2, new int[] {12, 5, 8}, null));
		assertEquals(List.of("Material list: [Left-click: set the first corner][Right-click: cancel]"),
			box("MATERIAL_LIST", 0, null, null));
		assertEquals(List.of("Part of the box is too far away to read. Move closer.",
			"Material list: [Left-click: set the opposite corner][Right-click: cancel][3 × 1 × 2]"),
			box("MATERIAL_LIST", 1, new int[] {3, 1, 2}, BoxCorners.Limit.NOT_LOADED));
		assertEquals(2, ControlsLine.box("COPY", 0, null, null, "R").getFirst().rects().size());
	}

	@Test
	void aWarningAboveTheControlsIsALineOfItsOwnInRed() {
		Line warning = ControlsLine.box("COPY", 1, new int[] {261, 1, 1}, BoxCorners.Limit.SIDE, "R").getFirst();
		assertEquals(List.of(), warning.rects());
		assertEquals(Look.WARNING, ((ControlsLine.Translated) warning.label()).look());
	}

	@Test
	void theDeleteBoxNamesTheModeKeyAsBoundAndLeavesItOutUnbound() {
		assertEquals(List.of("Delete: [Left-click: set the first corner][G: Delete mode][Right-click: cancel]"),
			box("DELETE", 0, null, null, "G"));
		assertEquals(List.of("Delete: [Left-click: set the first corner][Right-click: cancel]"),
			box("DELETE", 0, null, null, null));
		assertEquals(List.of("Copy: [Left-click: set the first corner][Right-click: cancel]"),
			box("COPY", 0, null, null, "R"));
		assertEquals(List.of("set the first corner", "Delete mode", "cancel"),
			collect(ControlsLine.box("DELETE", 0, null, null, "R").getFirst(), Look.ACTION));
	}

	@Test
	void aimWithNoMarks() {
		assertEquals(List.of("Delete, Aim: [Hold Left Control: mark][R: Delete mode][Right-click: close]"),
			aim(new AimState(false, false, -1, false), DEFAULT));
	}

	@Test
	void aimWithMarks() {
		assertEquals(List.of("Delete, Aim: [Hold Left-click: erase][Hold Left Control: mark][R: Delete mode]"
			+ "[Right-click: clear]"), aim(new AimState(true, false, -1, false), DEFAULT));
	}

	@Test
	void aSweepThatUnmarksSaysUnmark() {
		assertEquals(List.of("Delete, Aim: [Hold Left-click: erase][Hold Left Control: unmark][R: Delete mode]"
			+ "[Right-click: clear]"), aim(new AimState(true, true, -1, false), DEFAULT));
		// A sweep that has unmarked every mark still unmarks until the key is let go.
		assertEquals(List.of("Delete, Aim: [Hold Left Control: unmark][R: Delete mode][Right-click: close]"),
			aim(new AimState(false, true, -1, false), DEFAULT));
	}

	@Test
	void theHoldsProgressTakesTheErasesPlace() {
		assertEquals(List.of("Delete, Aim: [Hold Left-click: erasing 0%][Hold Left Control: mark][R: Delete mode]"
			+ "[Right-click: clear]"), aim(new AimState(true, false, 0, false), DEFAULT));
		assertEquals(List.of("Delete, Aim: [Hold Left-click: erasing 50%][Hold Left Control: mark][R: Delete mode]"
			+ "[Right-click: clear]"), aim(new AimState(true, false, 50, false), DEFAULT));
		assertEquals(List.of("Delete, Aim: [Hold Left-click: erasing 60%][Hold Left Control: mark][R: Delete mode]"
			+ "[Right-click: clear]"), aim(new AimState(true, false, 60, false), DEFAULT));
		assertEquals(List.of("Delete, Aim: [Hold Left-click: erasing 95%][Hold Left Control: mark][R: Delete mode]"
			+ "[Right-click: clear]"), aim(new AimState(true, false, 95, false), DEFAULT));
	}

	@Test
	void atTheCapAWarningGoesAbove() {
		assertEquals(List.of("Too many blocks", "Delete, Aim: [Hold Left-click: erase][Hold Left Control: mark]"
			+ "[R: Delete mode][Right-click: clear]"), aim(new AimState(true, false, -1, true), DEFAULT));
		assertEquals(List.of("Too many blocks"),
			collect(ControlsLine.deleteAim(new AimState(true, false, -1, true), DEFAULT).getFirst(), Look.WARNING));
	}

	@Test
	void aimsActionsAreActions() {
		assertEquals(List.of("erase", "mark", "Delete mode", "clear"),
			collect(ControlsLine.deleteAim(new AimState(true, false, -1, false), DEFAULT).getFirst(), Look.ACTION));
		assertEquals(List.of("erasing 60%", "mark", "Delete mode", "clear"),
			collect(ControlsLine.deleteAim(new AimState(true, false, 60, false), DEFAULT).getFirst(), Look.ACTION));
	}

	@Test
	void aimNamesTheKeysAsBoundAndLeavesOutUnboundOnes() {
		assertEquals(List.of("Delete, Aim: [Hold Left-click: erase][Hold Left Alt: mark][G: Delete mode]"
			+ "[Right-click: clear]"), aim(new AimState(true, false, -1, false), REBOUND));
		assertEquals(List.of("Delete, Aim: [Hold Left-click: erase][Right-click: clear]"),
			aim(new AimState(true, false, -1, false), UNBOUND));
	}

	@Test
	void theTurningLineHoldsTheSprintKeyToTurnOrFlip() {
		assertEquals("[Hold Left Control: turn or flip]", turning(new TurnState(true, true, false, false), "Left Control"));
		assertEquals("[Hold Left Control: turn]", turning(new TurnState(true, false, false, false), "Left Control"));
		assertEquals("[Hold Left Control: flip]", turning(new TurnState(false, true, false, false), "Left Control"));
	}

	@Test
	void withTheSprintKeyHeldTheTurningLineNamesTheWheelAndTheMiddleClick() {
		assertEquals("[Scroll: turn][Middle-click: flip]", turning(new TurnState(true, true, true, false), "Left Control"));
		assertEquals("[Scroll: turn]", turning(new TurnState(true, false, true, false), "Left Control"));
		assertEquals("[Middle-click: flip]", turning(new TurnState(false, true, true, false), "Left Control"));
	}

	@Test
	void onceTurnedTheTurningLineStartsTurned() {
		assertEquals("Turned: [Hold Left Control: turn or flip]",
			turning(new TurnState(true, true, false, true), "Left Control"));
		assertEquals("Turned: [Scroll: turn][Middle-click: flip]",
			turning(new TurnState(true, true, true, true), "Left Control"));
	}

	@Test
	void theTurningLineNamesTheSprintKeyAsBoundAndItsActionsAreActions() {
		assertEquals("[Hold Right Control: turn or flip]",
			turning(new TurnState(true, true, false, false), "Right Control"));
		assertEquals(List.of("turn", "flip"),
			collect(ControlsLine.turning(new TurnState(true, true, true, true), "Left Control"), Look.ACTION));
	}

	@Test
	void noTurningLineForABlockThatNeitherTurnsNorFlipsOrWithTheSprintKeyUnbound() {
		assertNull(ControlsLine.turning(new TurnState(false, false, false, false), "Left Control"));
		assertNull(ControlsLine.turning(new TurnState(false, false, true, true), "Left Control"));
		assertNull(ControlsLine.turning(new TurnState(true, true, false, false), null));
	}

	@Test
	void withoutTheSprintKeyDisablesLineIsTheTurningLineWithNoTurned() {
		assertEquals("[Hold Left Control: turn or flip]", disable(new TurnState(true, true, false, true), false, true));
		assertEquals("[Hold Left Control: turn]", disable(new TurnState(true, false, false, false), false, true));
		assertEquals("[Hold Left Control: flip]", disable(new TurnState(false, true, false, false), false, true));
		assertEquals("[Hold Right Control: turn or flip]", read(ControlsLine.disable(
			new DisableState(new TurnState(true, true, false, false), false, true), "Right Control")));
	}

	@Test
	void withTheSprintKeyHeldDisablesLinePlacesAndNamesTheWheelAndTheMiddleClick() {
		assertEquals("[Right-click: place][Scroll: turn][Middle-click: flip]",
			disable(new TurnState(true, true, true, false), false, true));
		assertEquals("[Right-click: place][Scroll: turn]", disable(new TurnState(true, false, true, false), false, true));
		assertEquals("[Right-click: place][Middle-click: flip]",
			disable(new TurnState(false, true, true, false), false, true));
		assertEquals("Turned: [Right-click: place][Scroll: turn][Middle-click: flip]",
			disable(new TurnState(true, true, true, true), false, true));
	}

	@Test
	void disablesLineSaysInRedWhereTheTurnedBlockCantBePlaced() {
		assertEquals("Turned: [Can't place here][Scroll: turn]", disable(new TurnState(true, false, true, true), true, false));
		assertEquals(List.of("Can't place here"), collect(ControlsLine.disable(
			new DisableState(new TurnState(true, true, true, true), false, false), "Left Control"), Look.WARNING));
		assertEquals(List.of("place", "turn", "flip"), collect(ControlsLine.disable(
			new DisableState(new TurnState(true, true, true, true), false, true), "Left Control"), Look.ACTION));
	}

	@Test
	void noDisableLineWithoutTheSprintKeyForABlockThatHangsOnTheBlockBehindIt() {
		assertNull(ControlsLine.disable(new DisableState(new TurnState(true, false, false, false), true, true),
			"Left Control"));
		assertEquals("[Right-click: place][Scroll: turn]", disable(new TurnState(true, false, true, false), true, true));
		assertNull(ControlsLine.disable(new DisableState(new TurnState(false, false, true, false), false, true),
			"Left Control"));
		assertNull(ControlsLine.disable(new DisableState(new TurnState(true, true, true, false), false, true), null));
	}

	/** Stairs, which turn and flip, without the sprint key held and not turned. */
	private static final TurnState STAIRS = new TurnState(true, true, false, false);
	private static final String HOLD = "[Hold Left Control: turn or flip]";

	private static final String ESC = "[Esc: exit mode]";

	@Test
	void singleBeforeAClickPlacesErasesTurnsLocksAndExits() {
		assertEquals("[Right-click: place][Left-click: erase][Hold Left Control: turn or flip][H: Lock (Off)][Esc: exit mode]",
			shape(new ShapeState(true, false, false, Underway.NONE, STAIRS), DEFAULT));
	}

	@Test
	void theOtherModesBeforeTheFirstClickStartPlacingAndErasing() {
		assertEquals("[Right-click: start placing][Left-click: start erasing][Hold Left Control: turn or flip]"
			+ "[H: Lock (Off)][Esc: exit mode]", shape(new ShapeState(false, false, false, Underway.NONE, STAIRS), DEFAULT));
	}

	@Test
	void lockedBeforeTheFirstClickNamesTheNudgesAndTheLockOn() {
		assertEquals("Locked: [Right-click: place][Arrow keys + Page Up/Down: nudge][Left-click: erase]"
			+ "[Hold Left Control: turn or flip][H: Lock (On)][Esc: exit mode]",
			shape(new ShapeState(true, true, false, Underway.NONE, STAIRS), DEFAULT));
		assertEquals("Locked: [Right-click: start placing][Arrow keys + Page Up/Down: nudge][Left-click: start erasing]"
			+ "[Hold Left Control: turn or flip][H: Lock (On)][Esc: exit mode]",
			shape(new ShapeState(false, true, false, Underway.NONE, STAIRS), DEFAULT));
	}

	@Test
	void theBuildModesLineNamesTheWheelAndTheMiddleClickWithTheSprintKeyHeldForTheBlocksTurnPhrases() {
		String end = "[H: Lock (Off)]" + ESC;
		String start = "[Right-click: start placing][Left-click: start erasing]";
		assertEquals(start + "[Scroll: turn][Middle-click: flip]" + end,
			shape(new ShapeState(false, false, false, Underway.NONE, new TurnState(true, true, true, false)), DEFAULT));
		assertEquals(start + "[Hold Left Control: turn]" + end,
			shape(new ShapeState(false, false, false, Underway.NONE, new TurnState(true, false, false, false)), DEFAULT));
		assertEquals(start + "[Scroll: turn]" + end,
			shape(new ShapeState(false, false, false, Underway.NONE, new TurnState(true, false, true, false)), DEFAULT));
		assertEquals(start + "[Hold Left Control: flip]" + end,
			shape(new ShapeState(false, false, false, Underway.NONE, new TurnState(false, true, false, false)), DEFAULT));
		assertEquals(start + "[Middle-click: flip]" + end,
			shape(new ShapeState(false, false, false, Underway.NONE, new TurnState(false, true, true, false)), DEFAULT));
		assertEquals(start + end,
			shape(new ShapeState(false, false, false, Underway.NONE, new TurnState(false, false, false, false)), DEFAULT));
	}

	@Test
	void onceTurnedTheBuildModesLineSaysSoAfterTheLock() {
		TurnState turned = new TurnState(true, true, false, true);
		assertEquals("Turned: [Right-click: start placing][Left-click: start erasing]" + HOLD + "[H: Lock (Off)]" + ESC,
			shape(new ShapeState(false, false, false, Underway.NONE, turned), DEFAULT));
		assertEquals("Locked, turned: [Right-click: place]" + NUDGES + "[Left-click: erase]" + HOLD + "[H: Lock (On)]"
			+ ESC, shape(new ShapeState(true, true, false, Underway.NONE, turned), DEFAULT));
	}

	@Test
	void whileTheLockedPositionIsRedItsReasonTakesTheRightClicksPlace() {
		assertEquals("Locked: [Can't place: outside the world]" + NUDGES + "[Left-click: erase]" + HOLD + "[H: Lock (On)]"
			+ ESC, shape(new ShapeState(true, true, true, Underway.NONE, STAIRS), DEFAULT));
		assertEquals("Locked: [Can't place: outside the world]" + NUDGES + "[Left-click: start erasing]" + HOLD
			+ "[H: Lock (On)]" + ESC, shape(new ShapeState(false, true, true, Underway.NONE, STAIRS), DEFAULT));
		assertEquals(List.of("Can't place: outside the world"),
			collect(ControlsLine.shape(new ShapeState(false, true, true, Underway.NONE, STAIRS), DEFAULT), Look.WARNING));
	}

	@Test
	void duringAShapeDrawnWithThePlaceClickOnlyTheLockAndTheTurningControls() {
		assertEquals("[Hold Left Control: turn or flip][H: Lock (Off)]",
			shape(new ShapeState(false, false, false, Underway.PLACING, STAIRS), DEFAULT));
		assertEquals("Locked: [Arrow keys + Page Up/Down: nudge][Hold Left Control: turn or flip][H: Lock (On)]",
			shape(new ShapeState(false, true, false, Underway.PLACING, STAIRS), DEFAULT));
	}

	@Test
	void duringAShapeDrawnWithTheBreakClickTheLockWithoutTheTurningControl() {
		assertEquals("[H: Lock (Off)]", shape(new ShapeState(false, false, false, Underway.BREAKING, STAIRS), DEFAULT));
		assertEquals("Locked: [Arrow keys + Page Up/Down: nudge][H: Lock (On)]",
			shape(new ShapeState(false, true, false, Underway.BREAKING, new TurnState(true, true, false, true)), DEFAULT));
	}

	@Test
	void theBuildModesLineNamesTheKeysAsBoundAndLeavesOutAKeyWithNone() {
		assertEquals("Locked: [Right-click: place][I/J/U/O + Y/N: nudge][Left-click: erase][Hold Left Alt: turn or flip]"
			+ "[K: Lock (On)][Esc: exit mode]", shape(new ShapeState(true, true, false, Underway.NONE, STAIRS), REBOUND));
		assertEquals("Locked: [Right-click: place][Left-click: erase][Esc: exit mode]",
			shape(new ShapeState(true, true, false, Underway.NONE, STAIRS), UNBOUND));
	}

	/** Effortless Building's line while placing, as {@code RenderHandler.PLACING_TEXT} holds it. */
	private static final List<Part> PLACING = List.of(new Part("Left-click to ", "white"), new Part("cancel", "dark_aqua"),
		new Part(", Right-click to ", "white"), new Part("place", "dark_aqua"));

	@Test
	void effortlessBuildingsLineIsAControlForEachClauseInItsOwnWords() {
		assertEquals("[Left-click: cancel][Right-click: place]", read(ControlsLine.effortless(PLACING)));
		assertEquals("[Left-click: cancel][Right-click: interact]", read(ControlsLine.effortless(List.of(
			new Part("Left-click to ", "white"), new Part("cancel", "dark_aqua"), new Part(", Right-click to ", "white"),
			new Part("interact", "dark_aqua")))));
		assertNull(ControlsLine.effortless(PLACING).label());
	}

	@Test
	void effortlessBuildingsActionsKeepTheirColoursAndItsKeysAreWhite() {
		assertEquals(List.of("cancel", "place"), collect(ControlsLine.effortless(PLACING), Look.ACTION));
		Line breaking = ControlsLine.effortless(List.of(new Part("Left-click to ", "white"), new Part("break", "red"),
			new Part(", Right-click to ", "white"), new Part("cancel", "red")));
		assertEquals("[Left-click: break][Right-click: cancel]", read(breaking));
		assertEquals(List.of("break", "cancel"), collect(breaking, Look.WARNING));
		assertEquals(List.of(), collect(breaking, Look.ACTION));
		// A key with no colour of its own is drawn white, as Effortless Building draws it.
		assertEquals("[Left-click: cancel]", read(ControlsLine.effortless(List.of(new Part("Left-click to ", null),
			new Part("cancel", "dark_aqua")))));
	}

	@Test
	void effortlessBuildingKeepsALineOfAnotherFormOrInOtherColours() {
		assertNull(ControlsLine.effortless(List.of()));
		assertNull(ControlsLine.effortless(List.of(new Part("Building...", "white"))));
		assertNull(ControlsLine.effortless(List.of(new Part("Left-click to ", "white"), new Part("cancel", "dark_aqua"),
			new Part(", Right-click", "white"))));
		assertNull(ControlsLine.effortless(List.of(new Part("Left-click to ", "white"), new Part("cancel", "gold"))));
		assertNull(ControlsLine.effortless(List.of(new Part("Left-click to ", "red"), new Part("cancel", "dark_aqua"))));
		assertNull(ControlsLine.effortless(List.of(new Part("Left-click to ", "white"), new Part("can", "dark_aqua"),
			new Part("cel", "red"))));
	}

	private static String shape(ShapeState state, Keys keys) {
		return read(ControlsLine.shape(state, keys));
	}

	private static String disable(TurnState turn, boolean hangs, boolean stays) {
		return read(ControlsLine.disable(new DisableState(turn, hangs, stays), "Left Control"));
	}

	private static String turning(TurnState state, String sprint) {
		return read(ControlsLine.turning(state, sprint));
	}

	private static List<String> aim(AimState state, Keys keys) {
		return ControlsLine.deleteAim(state, keys).stream().map(ControlsLineTest::read).toList();
	}

	private static CopyState state(CopyMode mode, boolean locked, int row, boolean pasted, Refusal refusal) {
		return new CopyState(mode, locked, row, pasted, refusal);
	}

	private static String copy(CopyState state, Keys keys) {
		return read(ControlsLine.copy(state, keys));
	}

	private static List<String> box(String purpose, int corners, int[] size, BoxCorners.Limit limit) {
		return box(purpose, corners, size, limit, "R");
	}

	private static List<String> box(String purpose, int corners, int[] size, BoxCorners.Limit limit, String mode) {
		return ControlsLine.box(purpose, corners, size, limit, mode).stream().map(ControlsLineTest::read).toList();
	}

	/** The line as the in-game checks read it: its label, then each other rectangle in [ ]. */
	private static String read(Line line) {
		StringBuilder text = new StringBuilder();
		if (line.label() != null) {
			text.append(read(line.label())).append(line.rects().isEmpty() ? "" : " ");
		}
		line.rects().forEach(rect -> text.append('[').append(read(rect)).append(']'));
		return text.toString();
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
		};
	}

	/** The texts of the line's parts in that look, outermost first. */
	private static List<String> collect(Line line, Look look) {
		List<String> found = new ArrayList<>();
		line.all().forEach(rect -> collect(rect, look, found));
		return found;
	}

	private static void collect(Text text, Look look, List<String> found) {
		switch (text) {
			case ControlsLine.Translated translated -> {
				if (translated.look() == look) {
					found.add(read(translated));
				}
				translated.args().forEach(arg -> collect(arg, look, found));
			}
			case ControlsLine.Literal literal -> {
				if (literal.look() == look && look != Look.PLAIN) {
					found.add(literal.text());
				}
			}
		}
	}
}
