package dev.buildingplanner.look;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.buildingplanner.input.BoxCorners;
import dev.buildingplanner.input.BoxPurpose;
import dev.buildingplanner.input.CopyColour.Refusal;
import dev.buildingplanner.input.CopyMode;
import dev.buildingplanner.input.MouseMode;
import dev.buildingplanner.input.ShapeLock;
import dev.buildingplanner.look.ControlsLine.CopyState;
import dev.buildingplanner.look.ControlsLine.DeleteBox;
import dev.buildingplanner.look.ControlsLine.DeleteState;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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
	private static final String COPY_END = TURN_MIRROR + "[H: Lock (Off)][R: Mode (Off)][Esc/Left-click: close]";
	private static final String LOCKED_END = TURN_MIRROR + "[H: Lock (On)][R: Mode (Off)][Esc/Left-click: close]";
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
	void offFollowingTheAim() {
		assertEquals("[Right-click: paste]" + COPY_END, copy(state(CopyMode.OFF, false, 0, false, null), DEFAULT));
	}

	@Test
	void afterAPasteHoldingTheSprintKeyLinesUp() {
		assertEquals("[Right-click: paste]" + TURN_MIRROR + "[Hold Left Control: line up][H: Lock (Off)][R: Mode (Off)]"
			+ "[Esc/Left-click: close]", copy(state(CopyMode.OFF, false, 0, true, null), DEFAULT));
	}

	@Test
	void locked() {
		assertEquals("[Right-click: paste]" + NUDGES + LOCKED_END,
			copy(state(CopyMode.OFF, true, 0, false, null), DEFAULT));
	}

	@Test
	void lockedTheSprintKeyLinesNothingUpSoTheLineLeavesItOut() {
		assertEquals("[Right-click: paste]" + NUDGES + LOCKED_END,
			copy(state(CopyMode.OFF, true, 0, true, null), DEFAULT));
		assertFalse(copy(state(CopyMode.LINE, true, 0, true, null), DEFAULT).contains("line up"));
	}

	@Test
	void lineWallAndFloorBeforeTheFirstClick() {
		assertEquals("[Right-click: set the start]" + TURN_MIRROR
			+ "[H: Lock (Off)][R: Mode (Line)][Esc/Left-click: close]",
			copy(state(CopyMode.LINE, false, 0, false, null), DEFAULT));
		assertEquals("[Right-click: set the start]" + TURN_MIRROR
			+ "[H: Lock (Off)][R: Mode (Wall)][Esc/Left-click: close]",
			copy(state(CopyMode.WALL, false, 0, false, null), DEFAULT));
		assertEquals("[Right-click: set the start]" + TURN_MIRROR + "[Hold Left Control: line up]"
			+ "[H: Lock (Off)][R: Mode (Floor)][Esc/Left-click: close]",
			copy(state(CopyMode.FLOOR, false, 0, true, null), DEFAULT));
		assertEquals("[Right-click: set the start]" + NUDGES + TURN_MIRROR
			+ "[H: Lock (On)][R: Mode (Line)][Esc/Left-click: close]",
			copy(state(CopyMode.LINE, true, 0, true, null), DEFAULT));
	}

	@Test
	void afterTheFirstClickTheSetsPasteWithItsCount() {
		assertEquals("[Right-click: paste 4 copies]" + TURN_MIRROR
			+ "[H: Lock (Off)][R: Mode (Line)][Esc/Left-click: cancel]",
			copy(state(CopyMode.LINE, false, 4, true, null), DEFAULT));
		assertEquals("[Right-click: paste 1 copy]" + TURN_MIRROR
			+ "[H: Lock (Off)][R: Mode (Wall)][Esc/Left-click: cancel]",
			copy(state(CopyMode.WALL, false, 1, false, null), DEFAULT));
		assertEquals("[Right-click: paste 16 copies]" + NUDGES + TURN_MIRROR
			+ "[H: Lock (On)][R: Mode (Floor)][Esc/Left-click: cancel]",
			copy(state(CopyMode.FLOOR, true, 16, true, null), DEFAULT));
	}

	@Test
	void whileRedTheReasonTakesThePastesPlace() {
		assertEquals("[Can't paste: part of the copy is outside the world]" + COPY_END,
			copy(state(CopyMode.OFF, false, 0, false, Refusal.BUILD_HEIGHT), DEFAULT));
		assertEquals("[Can't paste: part of the copy is outside the world]" + NUDGES + LOCKED_END,
			copy(state(CopyMode.OFF, true, 0, false, Refusal.WORLD_BORDER), DEFAULT));
		assertEquals("[Can't paste: too many blocks]" + TURN_MIRROR
			+ "[H: Lock (Off)][R: Mode (Wall)][Esc/Left-click: cancel]",
			copy(state(CopyMode.WALL, false, 10, false, Refusal.TOO_MANY_BLOCKS), DEFAULT));
		// The first click still fixes the first copy while the copy is red, so it stays, after the reason.
		assertEquals("[Can't paste: too many blocks][Right-click: set the start]" + TURN_MIRROR
			+ "[H: Lock (Off)][R: Mode (Line)][Esc/Left-click: close]",
			copy(state(CopyMode.LINE, false, 0, false, Refusal.TOO_MANY_BLOCKS), DEFAULT));
		assertEquals("[Can't paste: part of the copy is outside the world][Right-click: set the start]"
			+ NUDGES + TURN_MIRROR + "[H: Lock (On)][R: Mode (Floor)][Esc/Left-click: close]",
			copy(state(CopyMode.FLOOR, true, 0, false, Refusal.WORLD_BORDER), DEFAULT));
	}

	@Test
	void theReasonIsARectangleInRedAndTheActionsAreActions() {
		Line line = ControlsLine.copy(state(CopyMode.OFF, false, 0, false, Refusal.BUILD_HEIGHT), DEFAULT);
		assertEquals(Look.WARNING, ((ControlsLine.Translated) line.rects().getFirst()).look());
		assertEquals(List.of("Can't paste: part of the copy is outside the world"), collect(line, Look.WARNING));
		assertEquals(List.of("turn", "mirror", "Lock (Off)", "Mode (Off)", "close"), collect(line, Look.ACTION));
	}

	@Test
	void eachControlIsARectangleOfItsOwnWithNoLabel() {
		Line line = ControlsLine.copy(state(CopyMode.LINE, true, 0, false, null), DEFAULT);
		assertNull(line.label());
		assertEquals(List.of("Right-click: set the start", "Arrow keys + Page Up/Down: nudge", "Scroll: turn",
			"Middle-click: mirror", "H: Lock (On)", "R: Mode (Line)", "Esc/Left-click: close"),
			line.rects().stream().map(ControlsLineTest::read).toList());
		assertEquals(7, line.all().size());
		assertNull(ControlsLine.copy(state(CopyMode.OFF, false, 0, false, null), DEFAULT).label());
	}

	@Test
	void reboundKeysAreNamedAsBound() {
		assertEquals("[Right-click: paste]" + TURN_MIRROR + "[Hold Left Alt: line up][K: Lock (Off)][G: Mode (Off)]"
			+ "[Esc/Left-click: close]", copy(state(CopyMode.OFF, false, 0, true, null), REBOUND));
		assertEquals("[Right-click: paste][I/J/U/O + Y/N: nudge]" + TURN_MIRROR
			+ "[K: Lock (On)][G: Mode (Off)][Esc/Left-click: close]", copy(state(CopyMode.OFF, true, 0, true, null), REBOUND));
	}

	@Test
	void theArrowKeysInAnotherOrderAreNamedOneByOne() {
		Keys swapped = new Keys("H", "R", List.of("Down Arrow", "Up Arrow", "Left Arrow", "Right Arrow"), false,
			"Page Up", "Page Down", "Left Control");
		assertTrue(copy(state(CopyMode.OFF, true, 0, false, null), swapped)
			.contains("[Down Arrow/Up Arrow/Left Arrow/Right Arrow + Page Up/Down: nudge]"));
	}

	@Test
	void unboundKeysAreLeftOut() {
		assertEquals("[Right-click: paste]" + TURN_MIRROR + "[Esc/Left-click: close]",
			copy(state(CopyMode.OFF, false, 0, true, null), UNBOUND));
		assertEquals("[Right-click: paste]" + TURN_MIRROR + "[Esc/Left-click: close]",
			copy(state(CopyMode.OFF, true, 0, true, null), UNBOUND));
		Keys some = new Keys("H", "R", Arrays.asList("Up Arrow", null, null, "Right Arrow"), false, null, "Page Down",
			"Left Control");
		assertTrue(copy(state(CopyMode.OFF, true, 0, false, null), some)
			.contains("[Up Arrow/Right Arrow + Page Down: nudge]"));
		Keys onlyAlong = new Keys("H", "R", List.of("Up Arrow", "Down Arrow", "Left Arrow", "Right Arrow"), true, null,
			null, "Left Control");
		assertTrue(copy(state(CopyMode.OFF, true, 0, false, null), onlyAlong).contains("[Arrow keys: nudge]"));
		Keys allUnbound = new Keys("H", "R", Arrays.asList(null, null, null, null), false, null, null, "Left Control");
		assertFalse(copy(state(CopyMode.OFF, true, 0, false, null), allUnbound).contains("nudge"));
	}

	@Test
	void theUpAndDownKeysShareTheirFirstWordOnlyOnce() {
		Keys keypad = new Keys("H", "R", List.of("Up Arrow", "Down Arrow", "Left Arrow", "Right Arrow"), true,
			"Keypad 8", "Keypad 2", "Left Control");
		assertTrue(copy(state(CopyMode.OFF, true, 0, false, null), keypad).contains("[Arrow keys + Keypad 8/2: nudge]"));
		Keys letters = new Keys("H", "R", List.of("Up Arrow", "Down Arrow", "Left Arrow", "Right Arrow"), true, "U", "J",
			"Left Control");
		assertTrue(copy(state(CopyMode.OFF, true, 0, false, null), letters).contains("[Arrow keys + U/J: nudge]"));
		Keys mixed = new Keys("H", "R", List.of("Up Arrow", "Down Arrow", "Left Arrow", "Right Arrow"), true,
			"Page Up", "Left Shift", "Left Control");
		assertTrue(copy(state(CopyMode.OFF, true, 0, false, null), mixed)
			.contains("[Arrow keys + Page Up/Left Shift: nudge]"));
	}

	@Test
	void reboundArrowKeysAreNamedOneByOneBesideTheUpAndDownKeys() {
		Keys rebound = new Keys("H", "R", List.of("W", "S", "A", "D"), false, "Page Up", "Page Down", "Left Control");
		assertTrue(copy(state(CopyMode.OFF, true, 0, false, null), rebound).contains("[W/S/A/D + Page Up/Down: nudge]"));
	}

	@Test
	void theBoxesLines() {
		assertEquals(List.of("[Left-click: set the first corner][Esc/Right-click: cancel]"),
			box(0, null, null));
		assertEquals(List.of("[Left-click: set the opposite corner][Esc/Right-click: cancel][12 × 1 × 8]"),
			box(1, new int[] {12, 1, 8}, null));
		assertEquals(List.of("[Left-click: set the height][Esc/Right-click: cancel][12 × 5 × 8]"),
			box(2, new int[] {12, 5, 8}, null));
		assertEquals(List.of("[Left-click: set the first corner][Esc/Right-click: cancel]"),
			box(0, null, null));
		assertEquals(List.of("Part of the box is too far away to read. Move closer.",
			"[Left-click: set the opposite corner][Esc/Right-click: cancel][3 × 1 × 2]"),
			box(1, new int[] {3, 1, 2}, BoxCorners.Limit.NOT_LOADED));
		assertEquals(List.of("set the first corner", "cancel"),
			collect(ControlsLine.box(0, null, null).getFirst(), Look.ACTION));
	}

	@Test
	void theToolsNameShowsBesideTheHotbarInTheBuildModesNamesPlace() {
		assertEquals("Copy", read(ControlsLine.toolName(MouseMode.BOX, BoxPurpose.COPY)));
		assertEquals("Material list", read(ControlsLine.toolName(MouseMode.BOX, BoxPurpose.MATERIAL_LIST)));
		assertEquals("Delete", read(ControlsLine.toolName(MouseMode.DELETE, BoxPurpose.COPY)));
		assertEquals("Paste", read(ControlsLine.toolName(MouseMode.COPY_AT_AIM, BoxPurpose.COPY)));
		// No mouse mode leaves the build mode's name.
		assertNull(ControlsLine.toolName(null, null));
	}

	@Test
	void aWarningAboveTheControlsIsALineOfItsOwnInRed() {
		Line warning = ControlsLine.box(1, new int[] {261, 1, 1}, BoxCorners.Limit.SIDE).getFirst();
		assertEquals(List.of(), warning.rects());
		assertEquals(Look.WARNING, ((ControlsLine.Translated) warning.label()).look());
	}

	@Test
	void deleteWithNoMarks() {
		assertEquals(List.of("[Hold Left Control: toggle mark][Hold Left Control + Left-click: mark a box]"
			+ "[Esc/Right-click: close]"), delete(new DeleteState(false, false, false, null), DEFAULT));
	}

	@Test
	void deleteWithMarks() {
		assertEquals(List.of("[Hold Left-click: erase][Hold Left Control: toggle mark][Hold Left Control + Left-click: "
			+ "mark a box][Esc/Right-click: clear]"), delete(new DeleteState(true, false, false, null), DEFAULT));
	}

	@Test
	void aHoldThatUnmarksSaysUnmark() {
		assertEquals(List.of("[Hold Left-click: erase][Hold Left Control: unmark][Hold Left Control + Left-click: "
			+ "unmark a box][Esc/Right-click: clear]"), delete(new DeleteState(true, true, false, null), DEFAULT));
		// A hold that has unmarked every mark still unmarks until the key is let go.
		assertEquals(List.of("[Hold Left Control: unmark][Hold Left Control + Left-click: unmark a box]"
			+ "[Esc/Right-click: close]"), delete(new DeleteState(false, true, false, null), DEFAULT));
	}

	@Test
	void whileTheBoxIsDrawnItsLineNamesTheReleaseEscAndItsSize() {
		assertEquals(List.of("[Release Left-click: mark the box][Esc: cancel][12 × 1 × 8]"),
			delete(new DeleteState(true, false, false, new DeleteBox(false, new int[] {12, 1, 8}, null)), DEFAULT));
		assertEquals(List.of("[Release Left-click: unmark the box][Esc: cancel][10 × 4 × 10]"),
			delete(new DeleteState(true, true, false, new DeleteBox(true, new int[] {10, 4, 10}, null)), DEFAULT));
		assertEquals(List.of("Too many blocks", "[Release Left-click: mark the box][Esc: cancel][80 × 53 × 50]"),
			delete(new DeleteState(true, false, false,
				new DeleteBox(false, new int[] {80, 53, 50}, BoxCorners.Limit.TOO_MANY_BLOCKS)), DEFAULT));
		assertEquals(List.of("Part of the box is too far away to read. Move closer.",
			"[Release Left-click: mark the box][Esc: cancel][3 × 1 × 300]"),
			delete(new DeleteState(false, false, false,
				new DeleteBox(false, new int[] {3, 1, 300}, BoxCorners.Limit.NOT_LOADED)), DEFAULT));
	}

	@Test
	void atTheCapAWarningGoesAbove() {
		assertEquals(List.of("Too many blocks", "[Hold Left-click: erase][Hold Left Control: toggle mark]"
			+ "[Hold Left Control + Left-click: mark a box][Esc/Right-click: clear]"),
			delete(new DeleteState(true, false, true, null), DEFAULT));
		assertEquals(List.of("Too many blocks"),
			collect(ControlsLine.delete(new DeleteState(true, false, true, null), DEFAULT).getFirst(), Look.WARNING));
	}

	@Test
	void deletesActionsAreActions() {
		assertEquals(List.of("erase", "toggle mark", "mark a box", "clear"),
			collect(ControlsLine.delete(new DeleteState(true, false, false, null), DEFAULT).getFirst(), Look.ACTION));
		assertEquals(List.of("mark the box", "cancel"), collect(ControlsLine.delete(new DeleteState(true, false, false,
			new DeleteBox(false, new int[] {1, 1, 1}, null)), DEFAULT).getFirst(), Look.ACTION));
	}

	@Test
	void deleteNamesTheSprintKeyAsBoundAndLeavesItOutUnbound() {
		assertEquals(List.of("[Hold Left-click: erase][Hold Left Alt: toggle mark][Hold Left Alt + Left-click: mark a "
			+ "box][Esc/Right-click: clear]"), delete(new DeleteState(true, false, false, null), REBOUND));
		assertEquals(List.of("[Hold Left-click: erase][Esc/Right-click: clear]"),
			delete(new DeleteState(true, false, false, null), UNBOUND));
	}

	@Test
	void theTurningLineHoldsTheSprintKeyToTurnOrFlip() {
		assertEquals("[Hold Left Control: turn or flip]", turning(new TurnState(true, true, false), "Left Control"));
		assertEquals("[Hold Left Control: turn]", turning(new TurnState(true, false, false), "Left Control"));
		assertEquals("[Hold Left Control: flip]", turning(new TurnState(false, true, false), "Left Control"));
	}

	@Test
	void withTheSprintKeyHeldTheTurningLineNamesTheWheelAndTheMiddleClick() {
		assertEquals("[Scroll: turn][Middle-click: flip]", turning(new TurnState(true, true, true), "Left Control"));
		assertEquals("[Scroll: turn]", turning(new TurnState(true, false, true), "Left Control"));
		assertEquals("[Middle-click: flip]", turning(new TurnState(false, true, true), "Left Control"));
	}

	@Test
	void theTurningLineNamesTheSprintKeyAsBoundAndItsActionsAreActions() {
		assertEquals("[Hold Right Control: turn or flip]",
			turning(new TurnState(true, true, false), "Right Control"));
		assertEquals(List.of("turn", "flip"),
			collect(ControlsLine.turning(new TurnState(true, true, true), "Left Control"), Look.ACTION));
	}

	@Test
	void noTurningLineForABlockThatNeitherTurnsNorFlipsOrWithTheSprintKeyUnbound() {
		assertNull(ControlsLine.turning(new TurnState(false, false, false), "Left Control"));
		assertNull(ControlsLine.turning(new TurnState(false, false, true), "Left Control"));
		assertNull(ControlsLine.turning(new TurnState(true, true, false), null));
	}

	@Test
	void withoutTheSprintKeyDisablesLineIsTheTurningLine() {
		assertEquals("[Hold Left Control: turn or flip]", disable(new TurnState(true, true, false), false, true));
		assertEquals("[Hold Left Control: turn]", disable(new TurnState(true, false, false), false, true));
		assertEquals("[Hold Left Control: flip]", disable(new TurnState(false, true, false), false, true));
		assertEquals("[Hold Right Control: turn or flip]", read(ControlsLine.disable(
			new DisableState(new TurnState(true, true, false), false, true), "Right Control")));
	}

	@Test
	void withTheSprintKeyHeldDisablesLinePlacesAndNamesTheWheelAndTheMiddleClick() {
		assertEquals("[Right-click: place][Scroll: turn][Middle-click: flip]",
			disable(new TurnState(true, true, true), false, true));
		assertEquals("[Right-click: place][Scroll: turn]", disable(new TurnState(true, false, true), false, true));
		assertEquals("[Right-click: place][Middle-click: flip]",
			disable(new TurnState(false, true, true), false, true));
		assertEquals("[Right-click: place][Scroll: turn][Middle-click: flip]",
			disable(new TurnState(true, true, true), false, true));
	}

	@Test
	void disablesLineSaysInRedWhereTheTurnedBlockCantBePlaced() {
		assertEquals("[Can't place here][Scroll: turn]", disable(new TurnState(true, false, true), true, false));
		assertEquals(List.of("Can't place here"), collect(ControlsLine.disable(
			new DisableState(new TurnState(true, true, true), false, false), "Left Control"), Look.WARNING));
		assertEquals(List.of("place", "turn", "flip"), collect(ControlsLine.disable(
			new DisableState(new TurnState(true, true, true), false, true), "Left Control"), Look.ACTION));
	}

	@Test
	void noDisableLineWithoutTheSprintKeyForABlockThatHangsOnTheBlockBehindIt() {
		assertNull(ControlsLine.disable(new DisableState(new TurnState(true, false, false), true, true),
			"Left Control"));
		assertEquals("[Right-click: place][Scroll: turn]", disable(new TurnState(true, false, true), true, true));
		assertNull(ControlsLine.disable(new DisableState(new TurnState(false, false, true), false, true),
			"Left Control"));
		assertNull(ControlsLine.disable(new DisableState(new TurnState(true, true, true), false, true), null));
	}

	/** Stairs, which turn and flip, without the sprint key held. */
	private static final TurnState STAIRS = new TurnState(true, true, false);
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
		assertEquals("[Right-click: place][Arrow keys + Page Up/Down: nudge][Left-click: erase]"
			+ "[Hold Left Control: turn or flip][H: Lock (On)][Esc: exit mode]",
			shape(new ShapeState(true, true, false, Underway.NONE, STAIRS), DEFAULT));
		assertEquals("[Right-click: start placing][Arrow keys + Page Up/Down: nudge][Left-click: start erasing]"
			+ "[Hold Left Control: turn or flip][H: Lock (On)][Esc: exit mode]",
			shape(new ShapeState(false, true, false, Underway.NONE, STAIRS), DEFAULT));
	}

	@Test
	void theBuildModesLineNamesTheWheelAndTheMiddleClickWithTheSprintKeyHeldForTheBlocksTurnPhrases() {
		String end = "[H: Lock (Off)]" + ESC;
		String start = "[Right-click: start placing][Left-click: start erasing]";
		assertEquals(start + "[Scroll: turn][Middle-click: flip]" + end,
			shape(new ShapeState(false, false, false, Underway.NONE, new TurnState(true, true, true)), DEFAULT));
		assertEquals(start + "[Hold Left Control: turn]" + end,
			shape(new ShapeState(false, false, false, Underway.NONE, new TurnState(true, false, false)), DEFAULT));
		assertEquals(start + "[Scroll: turn]" + end,
			shape(new ShapeState(false, false, false, Underway.NONE, new TurnState(true, false, true)), DEFAULT));
		assertEquals(start + "[Hold Left Control: flip]" + end,
			shape(new ShapeState(false, false, false, Underway.NONE, new TurnState(false, true, false)), DEFAULT));
		assertEquals(start + "[Middle-click: flip]" + end,
			shape(new ShapeState(false, false, false, Underway.NONE, new TurnState(false, true, true)), DEFAULT));
		assertEquals(start + end,
			shape(new ShapeState(false, false, false, Underway.NONE, new TurnState(false, false, false)), DEFAULT));
	}

	@Test
	void whileTheLockedPositionIsRedItsReasonTakesTheRightClicksPlace() {
		assertEquals("[Can't place: outside the world]" + NUDGES + "[Left-click: erase]" + HOLD + "[H: Lock (On)]"
			+ ESC, shape(new ShapeState(true, true, true, Underway.NONE, STAIRS), DEFAULT));
		assertEquals("[Can't place: outside the world]" + NUDGES + "[Left-click: start erasing]" + HOLD
			+ "[H: Lock (On)]" + ESC, shape(new ShapeState(false, true, true, Underway.NONE, STAIRS), DEFAULT));
		assertEquals(List.of("Can't place: outside the world"),
			collect(ControlsLine.shape(new ShapeState(false, true, true, Underway.NONE, STAIRS), DEFAULT), Look.WARNING));
	}

	@Test
	void aShapeOfRealBlocksReadsAsAShapeOfGhostBlocks() {
		// ControlsLine takes no "Place real blocks" setting: the lines read the same with it on (1411 [c2], [c3]).
		assertEquals("[Right-click: place][Left-click: erase]" + HOLD + "[H: Lock (Off)]" + ESC,
			shape(new ShapeState(true, false, false, Underway.NONE, STAIRS), DEFAULT));
		assertEquals("[Right-click: start placing][Left-click: start erasing]" + HOLD + "[H: Lock (Off)]" + ESC,
			shape(new ShapeState(false, false, false, Underway.NONE, STAIRS), DEFAULT));
		assertEquals("[Hold Left Control: turn or flip][H: Lock (Off)]",
			shape(new ShapeState(false, false, false, Underway.PLACING, STAIRS), DEFAULT));
	}

	@Test
	void aPasteOfRealBlocksReadsAsAPasteOfGhostBlocks() {
		assertEquals("[Right-click: paste]" + COPY_END, copy(new CopyState(CopyMode.OFF, false, 0, false, null), DEFAULT));
		assertEquals("[Right-click: paste]" + NUDGES + LOCKED_END,
			copy(new CopyState(CopyMode.OFF, true, 0, false, null), DEFAULT));
		assertEquals("[Right-click: paste 3 copies]" + TURN_MIRROR + "[H: Lock (Off)][R: Mode (Line)][Esc/Left-click: cancel]",
			copy(new CopyState(CopyMode.LINE, false, 3, true, null), DEFAULT));
		assertEquals("[Right-click: paste 1 copy]" + TURN_MIRROR + "[H: Lock (Off)][R: Mode (Wall)][Esc/Left-click: cancel]",
			copy(new CopyState(CopyMode.WALL, false, 1, false, null), DEFAULT));
	}

	@Test
	void aCopyTheInventoryDoesntCoverSaysWhyInTheRightClicksPlace() {
		assertEquals("[Can't paste: not enough blocks]" + COPY_END,
			copy(new CopyState(CopyMode.OFF, false, 0, false, Refusal.NOT_ENOUGH_BLOCKS), DEFAULT));
		assertEquals("[Can't paste: not enough blocks]" + TURN_MIRROR
			+ "[H: Lock (Off)][R: Mode (Floor)][Esc/Left-click: cancel]",
			copy(new CopyState(CopyMode.FLOOR, false, 25, false, Refusal.NOT_ENOUGH_BLOCKS), DEFAULT));
		assertEquals("[Can't paste: not enough blocks][Right-click: set the start]" + TURN_MIRROR
			+ "[H: Lock (Off)][R: Mode (Line)][Esc/Left-click: close]",
			copy(new CopyState(CopyMode.LINE, false, 0, false, Refusal.NOT_ENOUGH_BLOCKS), DEFAULT));
		assertEquals(List.of("Can't paste: not enough blocks"), collect(ControlsLine.copy(
			new CopyState(CopyMode.OFF, false, 0, false, Refusal.NOT_ENOUGH_BLOCKS), DEFAULT), Look.WARNING));
	}

	@Test
	void duringAShapeDrawnWithThePlaceClickOnlyTheLockAndTheTurningControls() {
		assertEquals("[Hold Left Control: turn or flip][H: Lock (Off)]",
			shape(new ShapeState(false, false, false, Underway.PLACING, STAIRS), DEFAULT));
		assertEquals("[Arrow keys + Page Up/Down: nudge][Hold Left Control: turn or flip][H: Lock (On)]",
			shape(new ShapeState(false, true, false, Underway.PLACING, STAIRS), DEFAULT));
	}

	@Test
	void afterEachClickTheNextStepsLineStartsAtLockOff() {
		ShapeLock lock = new ShapeLock();
		lock.lock(new BlockPos(0, -60, 0),
			new BlockHitResult(new Vec3(0.5, -60, 0.5), Direction.UP, new BlockPos(0, -61, 0), false));
		assertEquals("[Right-click: start placing][Arrow keys + Page Up/Down: nudge][Left-click: start erasing]"
			+ "[Hold Left Control: turn or flip][H: Lock (On)][Esc: exit mode]",
			shape(new ShapeState(false, lock.on(0), false, Underway.NONE, STAIRS), DEFAULT));
		String off = "[Hold Left Control: turn or flip][H: Lock (Off)]";
		String on = "[Arrow keys + Page Up/Down: nudge][Hold Left Control: turn or flip][H: Lock (On)]";
		assertEquals(off, shape(new ShapeState(false, lock.on(1), false, Underway.PLACING, STAIRS), DEFAULT));
		lock.lockStep(1, new BlockPos(3, -60, 2));
		assertEquals(on, shape(new ShapeState(false, lock.on(1), false, Underway.PLACING, STAIRS), DEFAULT));
		assertEquals(off, shape(new ShapeState(false, lock.on(2), false, Underway.PLACING, STAIRS), DEFAULT));
		lock.lockStep(2, new BlockPos(3, -58, 2));
		assertEquals(on, shape(new ShapeState(false, lock.on(2), false, Underway.PLACING, STAIRS), DEFAULT));
	}

	@Test
	void duringAShapeDrawnWithTheBreakClickTheLockWithoutTheTurningControl() {
		assertEquals("[H: Lock (Off)]", shape(new ShapeState(false, false, false, Underway.BREAKING, STAIRS), DEFAULT));
		assertEquals("[Arrow keys + Page Up/Down: nudge][H: Lock (On)]",
			shape(new ShapeState(false, true, false, Underway.BREAKING, new TurnState(true, true, false)), DEFAULT));
	}

	@Test
	void theBuildModesLineNamesTheKeysAsBoundAndLeavesOutAKeyWithNone() {
		assertEquals("[Right-click: place][I/J/U/O + Y/N: nudge][Left-click: erase][Hold Left Alt: turn or flip]"
			+ "[K: Lock (On)][Esc: exit mode]", shape(new ShapeState(true, true, false, Underway.NONE, STAIRS), REBOUND));
		assertEquals("[Right-click: place][Left-click: erase][Esc: exit mode]",
			shape(new ShapeState(true, true, false, Underway.NONE, STAIRS), UNBOUND));
	}

	/**
	 * Every controls line Geo approved for 0.7.0, exactly (1313: revision 1's texts, with revision 2's lines without
	 * status labels, and with the tools' names beside the hotbar), with Delete's "toggle mark" as approved for 0.8.0
	 * (1391), each from the state the request shows it in.
	 */
	@Test
	void everyApprovedLine() {
		// Single's lines / Following the aim
		assertEquals("[Right-click: place][Left-click: erase][Hold Left Control: turn or flip][H: Lock (Off)][Esc: exit mode]",
			shape(new ShapeState(true, false, false, Underway.NONE, new TurnState(true, true, false)), DEFAULT));
		// Single's lines / Following the aim, Left Ctrl held
		assertEquals("[Right-click: place][Left-click: erase][Scroll: turn][Middle-click: flip][H: Lock (Off)][Esc: exit mode]",
			shape(new ShapeState(true, false, false, Underway.NONE, new TurnState(true, true, true)), DEFAULT));
		// Single's lines / A block that only turns (a chest)
		assertEquals("[Right-click: place][Left-click: erase][Hold Left Control: turn][H: Lock (Off)][Esc: exit mode]",
			shape(new ShapeState(true, false, false, Underway.NONE, new TurnState(true, false, false)), DEFAULT));
		// Single's lines / A block that only turns, Left Ctrl held
		assertEquals("[Right-click: place][Left-click: erase][Scroll: turn][H: Lock (Off)][Esc: exit mode]",
			shape(new ShapeState(true, false, false, Underway.NONE, new TurnState(true, false, true)), DEFAULT));
		// Single's lines / A block that only flips (a slab)
		assertEquals("[Right-click: place][Left-click: erase][Hold Left Control: flip][H: Lock (Off)][Esc: exit mode]",
			shape(new ShapeState(true, false, false, Underway.NONE, new TurnState(false, true, false)), DEFAULT));
		// Single's lines / A block that only flips, Left Ctrl held
		assertEquals("[Right-click: place][Left-click: erase][Middle-click: flip][H: Lock (Off)][Esc: exit mode]",
			shape(new ShapeState(true, false, false, Underway.NONE, new TurnState(false, true, true)), DEFAULT));
		// Single's lines / A block that neither turns nor flips (stone)
		assertEquals("[Right-click: place][Left-click: erase][H: Lock (Off)][Esc: exit mode]",
			shape(new ShapeState(true, false, false, Underway.NONE, new TurnState(false, false, false)), DEFAULT));
		// Single's lines / Turned
		assertEquals("[Right-click: place][Left-click: erase][Hold Left Control: turn or flip][H: Lock (Off)][Esc: exit mode]",
			shape(new ShapeState(true, false, false, Underway.NONE, new TurnState(true, true, false)), DEFAULT));
		// Single's lines / Locked
		assertEquals("[Right-click: place][Arrow keys + Page Up/Down: nudge][Left-click: erase][Hold Left Control: turn or flip][H: Lock (On)][Esc: exit mode]",
			shape(new ShapeState(true, true, false, Underway.NONE, new TurnState(true, true, false)), DEFAULT));
		// Single's lines / Locked and turned
		assertEquals("[Right-click: place][Arrow keys + Page Up/Down: nudge][Left-click: erase][Hold Left Control: turn or flip][H: Lock (On)][Esc: exit mode]",
			shape(new ShapeState(true, true, false, Underway.NONE, new TurnState(true, true, false)), DEFAULT));
		// Single's lines / Red, locked (the reason in red)
		assertEquals("[Can't place: outside the world][Arrow keys + Page Up/Down: nudge][Left-click: erase][Hold Left Control: turn or flip][H: Lock (On)][Esc: exit mode]",
			shape(new ShapeState(true, true, true, Underway.NONE, new TurnState(true, true, false)), DEFAULT));
		// The other build modes' lines / Before the first click
		assertEquals("[Right-click: start placing][Left-click: start erasing][Hold Left Control: turn or flip][H: Lock (Off)][Esc: exit mode]",
			shape(new ShapeState(false, false, false, Underway.NONE, new TurnState(true, true, false)), DEFAULT));
		// The other build modes' lines / Before the first click, a block that only turns (a chest)
		assertEquals("[Right-click: start placing][Left-click: start erasing][Hold Left Control: turn][H: Lock (Off)][Esc: exit mode]",
			shape(new ShapeState(false, false, false, Underway.NONE, new TurnState(true, false, false)), DEFAULT));
		// The other build modes' lines / Before the first click, a block that only flips (a slab)
		assertEquals("[Right-click: start placing][Left-click: start erasing][Hold Left Control: flip][H: Lock (Off)][Esc: exit mode]",
			shape(new ShapeState(false, false, false, Underway.NONE, new TurnState(false, true, false)), DEFAULT));
		// The other build modes' lines / Before the first click, Left Ctrl held (one part for a block that only turns or only flips)
		assertEquals("[Right-click: start placing][Left-click: start erasing][Scroll: turn][Middle-click: flip][H: Lock (Off)][Esc: exit mode]",
			shape(new ShapeState(false, false, false, Underway.NONE, new TurnState(true, true, true)), DEFAULT));
		// The other build modes' lines / Before the first click, a block that neither turns nor flips (stone)
		assertEquals("[Right-click: start placing][Left-click: start erasing][H: Lock (Off)][Esc: exit mode]",
			shape(new ShapeState(false, false, false, Underway.NONE, new TurnState(false, false, false)), DEFAULT));
		// The other build modes' lines / Before the first click, turned
		assertEquals("[Right-click: start placing][Left-click: start erasing][Hold Left Control: turn or flip][H: Lock (Off)][Esc: exit mode]",
			shape(new ShapeState(false, false, false, Underway.NONE, new TurnState(true, true, false)), DEFAULT));
		// The other build modes' lines / Before the first click, turned, Left Ctrl held
		assertEquals("[Right-click: start placing][Left-click: start erasing][Scroll: turn][Middle-click: flip][H: Lock (Off)][Esc: exit mode]",
			shape(new ShapeState(false, false, false, Underway.NONE, new TurnState(true, true, true)), DEFAULT));
		// The other build modes' lines / Locked, before the first click
		assertEquals("[Right-click: start placing][Arrow keys + Page Up/Down: nudge][Left-click: start erasing][Hold Left Control: turn or flip][H: Lock (On)][Esc: exit mode]",
			shape(new ShapeState(false, true, false, Underway.NONE, new TurnState(true, true, false)), DEFAULT));
		// The other build modes' lines / Red, locked before the first click (the reason in red)
		assertEquals("[Can't place: outside the world][Arrow keys + Page Up/Down: nudge][Left-click: start erasing][Hold Left Control: turn or flip][H: Lock (On)][Esc: exit mode]",
			shape(new ShapeState(false, true, true, Underway.NONE, new TurnState(true, true, false)), DEFAULT));
		// The other build modes' lines / During a shape placed with Right-click, above Effortless Building's line
		assertEquals("[Hold Left Control: turn or flip][H: Lock (Off)]",
			shape(new ShapeState(false, false, false, Underway.PLACING, new TurnState(true, true, false)), DEFAULT));
		// The other build modes' lines / During a shape placed with Right-click, turned
		assertEquals("[Hold Left Control: turn or flip][H: Lock (Off)]",
			shape(new ShapeState(false, false, false, Underway.PLACING, new TurnState(true, true, false)), DEFAULT));
		// The other build modes' lines / During a shape placed with Right-click, locked
		assertEquals("[Arrow keys + Page Up/Down: nudge][Hold Left Control: turn or flip][H: Lock (On)]",
			shape(new ShapeState(false, true, false, Underway.PLACING, new TurnState(true, true, false)), DEFAULT));
		// The other build modes' lines / During a shape broken with Left-click
		assertEquals("[H: Lock (Off)]",
			shape(new ShapeState(false, false, false, Underway.BREAKING, new TurnState(true, true, false)), DEFAULT));
		// The other build modes' lines / During a shape broken with Left-click, locked
		assertEquals("[Arrow keys + Page Up/Down: nudge][H: Lock (On)]",
			shape(new ShapeState(false, true, false, Underway.BREAKING, new TurnState(true, true, false)), DEFAULT));
		// The copy's lines / Following the aim
		assertEquals("[Right-click: paste][Scroll: turn][Middle-click: mirror][H: Lock (Off)][R: Mode (Off)][Esc/Left-click: close]",
			copy(state(CopyMode.OFF, false, 0, false, null), DEFAULT));
		// The copy's lines / Following the aim, after the first paste in the dimension
		assertEquals("[Right-click: paste][Scroll: turn][Middle-click: mirror][Hold Left Control: line up][H: Lock (Off)][R: Mode (Off)][Esc/Left-click: close]",
			copy(state(CopyMode.OFF, false, 0, true, null), DEFAULT));
		// The copy's lines / Locked
		assertEquals("[Right-click: paste][Arrow keys + Page Up/Down: nudge][Scroll: turn][Middle-click: mirror][H: Lock (On)][R: Mode (Off)][Esc/Left-click: close]",
			copy(state(CopyMode.OFF, true, 0, false, null), DEFAULT));
		// The copy's lines / Line before its first click (Zoop in 0.6.0)
		assertEquals("[Right-click: set the start][Scroll: turn][Middle-click: mirror][H: Lock (Off)][R: Mode (Line)][Esc/Left-click: close]",
			copy(state(CopyMode.LINE, false, 0, false, null), DEFAULT));
		// The copy's lines / Wall before its first click
		assertEquals("[Right-click: set the start][Scroll: turn][Middle-click: mirror][H: Lock (Off)][R: Mode (Wall)][Esc/Left-click: close]",
			copy(state(CopyMode.WALL, false, 0, false, null), DEFAULT));
		// The copy's lines / Floor before its first click, after a paste
		assertEquals("[Right-click: set the start][Scroll: turn][Middle-click: mirror][Hold Left Control: line up][H: Lock (Off)][R: Mode (Floor)][Esc/Left-click: close]",
			copy(state(CopyMode.FLOOR, false, 0, true, null), DEFAULT));
		// The copy's lines / Line locked, before its first click (Zoop in 0.6.0)
		assertEquals("[Right-click: set the start][Arrow keys + Page Up/Down: nudge][Scroll: turn][Middle-click: mirror][H: Lock (On)][R: Mode (Line)][Esc/Left-click: close]",
			copy(state(CopyMode.LINE, true, 0, true, null), DEFAULT));
		// The copy's lines / A Line of copies after its first click (a Zoop row in 0.6.0)
		assertEquals("[Right-click: paste 4 copies][Scroll: turn][Middle-click: mirror][H: Lock (Off)][R: Mode (Line)][Esc/Left-click: cancel]",
			copy(state(CopyMode.LINE, false, 4, true, null), DEFAULT));
		// The copy's lines / A set of one copy (a row of one in 0.6.0)
		assertEquals("[Right-click: paste 1 copy][Scroll: turn][Middle-click: mirror][H: Lock (Off)][R: Mode (Wall)][Esc/Left-click: cancel]",
			copy(state(CopyMode.WALL, false, 1, true, null), DEFAULT));
		// The copy's lines / A Wall of copies
		assertEquals("[Right-click: paste 6 copies][Scroll: turn][Middle-click: mirror][H: Lock (Off)][R: Mode (Wall)][Esc/Left-click: cancel]",
			copy(state(CopyMode.WALL, false, 6, true, null), DEFAULT));
		// The copy's lines / A Floor of copies
		assertEquals("[Right-click: paste 16 copies][Scroll: turn][Middle-click: mirror][H: Lock (Off)][R: Mode (Floor)][Esc/Left-click: cancel]",
			copy(state(CopyMode.FLOOR, false, 16, true, null), DEFAULT));
		// The copy's lines / Wall locked, before its first click
		assertEquals("[Right-click: set the start][Arrow keys + Page Up/Down: nudge][Scroll: turn][Middle-click: mirror][H: Lock (On)][R: Mode (Wall)][Esc/Left-click: close]",
			copy(state(CopyMode.WALL, true, 0, true, null), DEFAULT));
		// The copy's lines / Floor locked, before its first click
		assertEquals("[Right-click: set the start][Arrow keys + Page Up/Down: nudge][Scroll: turn][Middle-click: mirror][H: Lock (On)][R: Mode (Floor)][Esc/Left-click: close]",
			copy(state(CopyMode.FLOOR, true, 0, true, null), DEFAULT));
		// The copy's lines / A Floor of copies, locked
		assertEquals("[Right-click: paste 16 copies][Arrow keys + Page Up/Down: nudge][Scroll: turn][Middle-click: mirror][H: Lock (On)][R: Mode (Floor)][Esc/Left-click: cancel]",
			copy(state(CopyMode.FLOOR, true, 16, true, null), DEFAULT));
		// The copy's lines / Red, following the aim (the reason in red)
		assertEquals("[Can't paste: part of the copy is outside the world][Scroll: turn][Middle-click: mirror][H: Lock (Off)][R: Mode (Off)][Esc/Left-click: close]",
			copy(state(CopyMode.OFF, false, 0, false, Refusal.BUILD_HEIGHT), DEFAULT));
		// The copy's lines / Red, locked
		assertEquals("[Can't paste: part of the copy is outside the world][Arrow keys + Page Up/Down: nudge][Scroll: turn][Middle-click: mirror][H: Lock (On)][R: Mode (Off)][Esc/Left-click: close]",
			copy(state(CopyMode.OFF, true, 0, false, Refusal.WORLD_BORDER), DEFAULT));
		// The copy's lines / Red, Floor locked before its first click (Zoop in 0.6.0)
		assertEquals("[Can't paste: part of the copy is outside the world][Right-click: set the start][Arrow keys + Page Up/Down: nudge][Scroll: turn][Middle-click: mirror][H: Lock (On)][R: Mode (Floor)][Esc/Left-click: close]",
			copy(state(CopyMode.FLOOR, true, 0, false, Refusal.WORLD_BORDER), DEFAULT));
		// The copy's lines / Red, Line locked before its first click
		assertEquals("[Can't paste: part of the copy is outside the world][Right-click: set the start][Arrow keys + Page Up/Down: nudge][Scroll: turn][Middle-click: mirror][H: Lock (On)][R: Mode (Line)][Esc/Left-click: close]",
			copy(state(CopyMode.LINE, true, 0, false, Refusal.BUILD_HEIGHT), DEFAULT));
		// The copy's lines / Red, Wall locked before its first click
		assertEquals("[Can't paste: part of the copy is outside the world][Right-click: set the start][Arrow keys + Page Up/Down: nudge][Scroll: turn][Middle-click: mirror][H: Lock (On)][R: Mode (Wall)][Esc/Left-click: close]",
			copy(state(CopyMode.WALL, true, 0, false, Refusal.BUILD_HEIGHT), DEFAULT));
		// The copy's lines / Red, Line before its first click, a copy over the cap
		assertEquals("[Can't paste: too many blocks][Right-click: set the start][Scroll: turn][Middle-click: mirror][H: Lock (Off)][R: Mode (Line)][Esc/Left-click: close]",
			copy(state(CopyMode.LINE, false, 0, false, Refusal.TOO_MANY_BLOCKS), DEFAULT));
		// The copy's lines / A Wall of copies over the cap (a row in 0.6.0)
		assertEquals("[Can't paste: too many blocks][Scroll: turn][Middle-click: mirror][H: Lock (Off)][R: Mode (Wall)][Esc/Left-click: cancel]",
			copy(state(CopyMode.WALL, false, 10, false, Refusal.TOO_MANY_BLOCKS), DEFAULT));
		// The copy's lines / A Line of copies over the cap (a row in 0.6.0)
		assertEquals("[Can't paste: too many blocks][Scroll: turn][Middle-click: mirror][H: Lock (Off)][R: Mode (Line)][Esc/Left-click: cancel]",
			copy(state(CopyMode.LINE, false, 20, false, Refusal.TOO_MANY_BLOCKS), DEFAULT));
		// The Copy and Material list boxes' lines / A box for Copy, the first corner
		assertEquals("[Left-click: set the first corner][Esc/Right-click: cancel]",
			box(0, null, null).getLast());
		// The Copy and Material list boxes' lines / The opposite corner, with the box's size
		assertEquals("[Left-click: set the opposite corner][Esc/Right-click: cancel][3 × 1 × 2]",
			box(1, new int[] {3, 1, 2}, null).getLast());
		// The Copy and Material list boxes' lines / The height, with the box's size
		assertEquals("[Left-click: set the height][Esc/Right-click: cancel][3 × 2 × 2]",
			box(2, new int[] {3, 2, 2}, null).getLast());
		// The Copy and Material list boxes' lines / A box for the material list, each corner as Copy's (the opposite corner shown)
		assertEquals("[Left-click: set the opposite corner][Esc/Right-click: cancel][3 × 1 × 2]",
			box(1, new int[] {3, 1, 2}, null).getLast());
		// Delete's lines and messages / Nothing marked (Aim in 0.6.0)
		assertEquals("[Hold Left Control: toggle mark][Hold Left Control + Left-click: mark a box][Esc/Right-click: close]",
			delete(new DeleteState(false, false, false, null), DEFAULT).getLast());
		// Delete's lines and messages / With marks
		assertEquals("[Hold Left-click: erase][Hold Left Control: toggle mark][Hold Left Control + Left-click: mark a box][Esc/Right-click: clear]",
			delete(new DeleteState(true, false, false, null), DEFAULT).getLast());
		// Delete's lines and messages / A sweep or a box that unmarks
		assertEquals("[Hold Left-click: erase][Hold Left Control: unmark][Hold Left Control + Left-click: unmark a box][Esc/Right-click: clear]",
			delete(new DeleteState(true, true, false, null), DEFAULT).getLast());
		// Delete's lines and messages / Holding the left button to erase (a bar above the line fills over the second)
		assertEquals("[Hold Left-click: erase][Hold Left Control: toggle mark][Hold Left Control + Left-click: mark a box][Esc/Right-click: clear]",
			delete(new DeleteState(true, false, false, null), DEFAULT).getLast());
		// Delete's lines and messages / A box being drawn, with its size
		assertEquals("[Release Left-click: mark the box][Esc: cancel][10 × 4 × 10]",
			delete(new DeleteState(true, false, false, new DeleteBox(false, new int[] {10, 4, 10}, null)), DEFAULT).getLast());
		// Delete's lines and messages / A box that unmarks, being drawn
		assertEquals("[Release Left-click: unmark the box][Esc: cancel][5 × 4 × 5]",
			delete(new DeleteState(true, true, false, new DeleteBox(true, new int[] {5, 4, 5}, null)), DEFAULT).getLast());
	}

	/** Effortless Building's line while placing, as {@code RenderHandler.PLACING_TEXT} holds it. */
	private static final List<Part> PLACING = List.of(new Part("Left-click to ", "white"), new Part("cancel", "dark_aqua"),
		new Part(", Right-click to ", "white"), new Part("place", "dark_aqua"));

	@Test
	void effortlessBuildingsLineIsAControlForEachClauseInItsOwnWords() {
		assertEquals("[Esc/Left-click: cancel][Right-click: place]", read(ControlsLine.effortless(PLACING)));
		assertEquals("[Esc/Left-click: cancel][Right-click: interact]", read(ControlsLine.effortless(List.of(
			new Part("Left-click to ", "white"), new Part("cancel", "dark_aqua"), new Part(", Right-click to ", "white"),
			new Part("interact", "dark_aqua")))));
		assertNull(ControlsLine.effortless(PLACING).label());
	}

	@Test
	void effortlessBuildingsActionsKeepTheirColoursAndItsKeysAreWhite() {
		assertEquals(List.of("cancel", "place"), collect(ControlsLine.effortless(PLACING), Look.ACTION));
		Line breaking = ControlsLine.effortless(List.of(new Part("Left-click to ", "white"), new Part("break", "red"),
			new Part(", Right-click to ", "white"), new Part("cancel", "red")));
		assertEquals("[Left-click: break][Esc/Right-click: cancel]", read(breaking));
		assertEquals(List.of("break", "cancel"), collect(breaking, Look.WARNING));
		assertEquals(List.of(), collect(breaking, Look.ACTION));
		// A key with no colour of its own is drawn white, as Effortless Building draws it.
		assertEquals("[Esc/Left-click: cancel]", read(ControlsLine.effortless(List.of(new Part("Left-click to ", null),
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

	private static List<String> delete(DeleteState state, Keys keys) {
		return ControlsLine.delete(state, keys).stream().map(ControlsLineTest::read).toList();
	}

	private static CopyState state(CopyMode mode, boolean locked, int set, boolean pasted, Refusal refusal) {
		return new CopyState(mode, locked, set, pasted, refusal);
	}

	private static String copy(CopyState state, Keys keys) {
		return read(ControlsLine.copy(state, keys));
	}

	private static List<String> box(int corners, int[] size, BoxCorners.Limit limit) {
		return ControlsLine.box(corners, size, limit).stream().map(ControlsLineTest::read).toList();
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
