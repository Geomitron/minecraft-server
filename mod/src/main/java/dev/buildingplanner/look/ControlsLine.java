package dev.buildingplanner.look;

import dev.buildingplanner.input.BoxCorners;
import dev.buildingplanner.input.CopyColour;
import dev.buildingplanner.input.CopyMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The lines of a mouse mode's controls (see "Building Planner's mouse modes"), as texts from the language file with
 * the keys' names in them, worked out from the mode's state and the keys as the player has them bound. Each line is a
 * row of rectangles: its label (the mode and its state, such as "Delete:"), if it has one, then each control as "key:
 * action", a red reason in place of a control that can't act, and a box's size. The game turns them into its own
 * components and lays the rectangles out ({@code ModeHud}, {@link ControlsRows}): white, with each action in dark aqua
 * and each reason in red.
 */
public final class ControlsLine {
	private static final String MODE = "buildingplanner.ui.mode";
	private static final String BOX = MODE + ".box";
	private static final String COPY = MODE + ".copy";
	private static final String DELETE = MODE + ".delete";
	private static final String TURN = MODE + ".turn";
	private static final String SINGLE = MODE + ".single";
	private static final String SHAPE = MODE + ".shape";

	private ControlsLine() {
	}

	/** A part of a line. */
	public sealed interface Text permits Translated, Literal {
	}

	/** A text from the language file, with its arguments in its {@code %s} places. */
	public record Translated(String key, Look look, List<Text> args) implements Text {
		static Translated of(String key, Look look, Text... args) {
			return new Translated(key, look, List.of(args));
		}
	}

	/** A key's name as the game gives it, a number, or a word of Effortless Building's own line, in its look. */
	public record Literal(String text, Look look) implements Text {
		public Literal(String text) {
			this(text, Look.PLAIN);
		}
	}

	private static final Text LEFT = Translated.of(MODE + ".left_click", Look.PLAIN);
	private static final Text RIGHT = Translated.of(MODE + ".right_click", Look.PLAIN);
	private static final Text MIDDLE = Translated.of(MODE + ".middle_click", Look.PLAIN);
	private static final Text SCROLL = Translated.of(MODE + ".scroll", Look.PLAIN);

	/** How a part of a line looks: as the line, as an action, or as a warning. */
	public enum Look {
		PLAIN, ACTION, WARNING
	}

	/**
	 * A line of the controls: its label's rectangle, or null, then the other rectangles in order. A warning above the
	 * controls ("Too many blocks") is a line of its label alone.
	 */
	public record Line(Text label, List<Text> rects) {
		/** Every rectangle of the line, its label first. */
		public List<Text> all() {
			if (label == null) {
				return rects;
			}
			List<Text> all = new ArrayList<>(rects.size() + 1);
			all.add(label);
			all.addAll(rects);
			return all;
		}
	}

	/**
	 * The copy's keys, each by its name as the game gives it, or null while it has no key: "Lock or unlock", "Change
	 * Zoop or Delete mode", the four nudges along the ground (forward, back, left and right), whether those four are on
	 * the arrow keys (Up, Down, Left and Right in that order), the nudges up and down, and Minecraft's sprint key, which
	 * lines the copy up.
	 */
	public record Keys(String lock, String mode, List<String> nudges, boolean nudgesOnArrows, String up, String down,
		String sprint) {
	}

	/**
	 * The copy at the aim as its line tells it: its mode, whether it's locked, the number of copies in a zoop's fixed
	 * row (0 before the first click), whether the player has pasted in this dimension (so the copy can line up with the
	 * latest paste), and why the server would refuse its paste, or null.
	 */
	public record CopyState(CopyMode mode, boolean locked, int row, boolean pasted, CopyColour.Refusal refusal) {
	}

	/**
	 * Delete by aim as its line tells it: whether any ghost block is marked, whether the sweep under way unmarks, the
	 * hold's progress toward the erase in percent ({@code erasing}, -1 while the left button isn't held to erase), and
	 * whether the marks have reached the cap.
	 */
	public record AimState(boolean marked, boolean unmarking, int erasing, boolean full) {
	}

	/**
	 * A box's lines: a warning naming the limit the aimed corner breaks, if one does, then its controls for the next
	 * click, with its size once a corner is set ({@code size} is null before). {@code purpose} is the box's purpose's
	 * name (COPY, DELETE or MATERIAL_LIST); a Delete box names the mode key, {@code mode}, which switches it to Aim,
	 * unless it has no key (null).
	 */
	public static List<Line> box(String purpose, int corners, int[] size, BoxCorners.Limit limit, String mode) {
		String next = switch (corners) {
			case 0 -> BOX + ".first";
			case 1 -> BOX + ".second";
			default -> BOX + ".height";
		};
		List<Text> rects = new ArrayList<>(4);
		rects.add(hint(LEFT, next));
		if (purpose.equals("DELETE") && mode != null) {
			rects.add(hint(new Literal(mode), MODE + ".delete_mode"));
		}
		rects.add(hint(RIGHT, BOX + ".cancel"));
		if (size != null) {
			rects.add(Translated.of(BOX + ".size", Look.PLAIN, new Literal(String.valueOf(size[0])),
				new Literal(String.valueOf(size[1])), new Literal(String.valueOf(size[2]))));
		}
		List<Line> lines = new ArrayList<>(2);
		if (limit != null) {
			String reason = switch (limit) {
				case SIDE, TOO_MANY_BLOCKS -> BOX + ".too_large";
				case NOT_LOADED -> BOX + ".not_loaded";
			};
			lines.add(warning(reason));
		}
		lines.add(labelled(Translated.of(BOX + "." + purpose.toLowerCase(Locale.ROOT), Look.PLAIN), rects));
		return lines;
	}

	/**
	 * Delete by aim's lines: "Too many blocks" once the marks reach the cap, then its controls: with marks, the hold that
	 * erases them, or its progress while the left button is held; the sprint key that marks, or unmarks during a sweep
	 * that unmarks; the mode key, which switches to a Delete box; and the right click, which clears the marks, or with
	 * none closes Aim. A key with no key bound is left out.
	 */
	public static List<Line> deleteAim(AimState state, Keys keys) {
		List<Text> rects = new ArrayList<>();
		if (state.marked()) {
			Text hold = Translated.of(MODE + ".hold", Look.PLAIN, LEFT);
			rects.add(state.erasing() >= 0
				? hint(hold, Translated.of(DELETE + ".erasing", Look.ACTION, new Literal(String.valueOf(state.erasing()))))
				: hint(hold, DELETE + ".erase"));
		}
		if (keys.sprint() != null) {
			rects.add(hint(held(keys.sprint()), state.unmarking() ? DELETE + ".unmark" : DELETE + ".mark"));
		}
		if (keys.mode() != null) {
			rects.add(hint(new Literal(keys.mode()), MODE + ".delete_mode"));
		}
		rects.add(hint(RIGHT, state.marked() ? DELETE + ".clear" : COPY + ".put_away"));
		List<Line> lines = new ArrayList<>(2);
		if (state.full()) {
			lines.add(warning(BOX + ".too_large"));
		}
		lines.add(labelled(Translated.of(DELETE + ".titled", Look.PLAIN, Translated.of(DELETE + ".aim", Look.PLAIN)),
			rects));
		return lines;
	}

	/**
	 * The block in hand as the turning line tells it: whether it turns and whether it flips (or a block of the mix
	 * does), whether Minecraft's sprint key is held, and whether the block has been turned or flipped.
	 */
	public record TurnState(boolean turns, boolean flips, boolean held, boolean turned) {
	}

	/**
	 * The turning line, Disable's line without the sprint key held (see "Turning real blocks in Disable": "Controls
	 * line"): with the sprint key held, the wheel that turns and the middle click that flips, otherwise the sprint key
	 * held to turn or flip; each part only for a block that turns or flips, and "Turned:" before them once the block is
	 * turned or flipped. Null for a block that does neither, and while the sprint key has no key bound.
	 */
	public static Line turning(TurnState state, String sprint) {
		List<Text> rects = turnParts(state, sprint);
		return rects.isEmpty() ? null
			: labelled(state.turned() ? Translated.of(TURN + ".turned", Look.PLAIN) : null, rects);
	}

	/**
	 * The turning line's controls: with the sprint key held, the wheel that turns and the middle click that flips,
	 * otherwise the sprint key held to turn or flip, each only for a block that turns or flips; none for a block that
	 * does neither or while the sprint key has no key bound.
	 */
	private static List<Text> turnParts(TurnState state, String sprint) {
		if (!state.turns() && !state.flips() || sprint == null) {
			return List.of();
		}
		List<Text> rects = new ArrayList<>(2);
		if (state.held()) {
			if (state.turns()) {
				rects.add(hint(SCROLL, COPY + ".turn"));
			}
			if (state.flips()) {
				rects.add(hint(MIDDLE, TURN + ".flip"));
			}
		} else {
			String verb = !state.flips() ? COPY + ".turn" : !state.turns() ? TURN + ".flip" : TURN + ".turn_or_flip";
			rects.add(hint(held(sprint), verb));
		}
		return rects;
	}

	/** Which of Effortless Building's shapes is under way: none, one drawn with the place click, or with the break click. */
	public enum Underway {
		NONE, PLACING, BREAKING
	}

	/**
	 * A build mode as its line tells it (see "Locking and the first click": "Controls line"): whether the mode is Single,
	 * whether a position is locked, whether the locked position is red (outside the world's build height or its border),
	 * which shape is under way, and the block in hand as the turning line tells it.
	 */
	public record ShapeState(boolean single, boolean locked, boolean red, Underway underway, TurnState turn) {
	}

	/**
	 * A build mode's line, in every mode but Disable (see "Locking and the first click": "Controls line"). Before the
	 * first click: "Locked" or "Turned" or both as the label, the right click (placing in Single, starting a shape in the
	 * others, or in red why the locked position can't place), the nudge keys while it's locked, the left click (erasing in
	 * Single, starting an erasing shape in the others), the turning controls, the lock key with the lock's state, and Esc,
	 * which leaves the mode. During a shape, beside Effortless Building's own line, only the lock and the turning controls:
	 * "Locked" or "Turned" or both, the nudge keys while it's locked, the turning controls during a shape drawn with the
	 * place click, and the lock key. A key with no key bound is left out.
	 */
	public static Line shape(ShapeState state, Keys keys) {
		boolean breaking = state.underway() == Underway.BREAKING;
		List<Text> turning = breaking ? List.of() : turnParts(state.turn(), keys.sprint());
		boolean turned = !breaking && state.turn().turned();
		List<Text> rects = new ArrayList<>();
		boolean before = state.underway() == Underway.NONE;
		if (before) {
			rects.add(state.red() ? reason(SINGLE + ".outside")
				: hint(RIGHT, state.single() ? TURN + ".place" : SHAPE + ".start_placing"));
		}
		if (state.locked()) {
			Text nudge = nudge(keys);
			if (nudge != null) {
				rects.add(hint(nudge, COPY + ".nudge"));
			}
		}
		if (before) {
			rects.add(hint(LEFT, state.single() ? DELETE + ".erase" : SHAPE + ".start_erasing"));
		}
		rects.addAll(turning);
		if (keys.lock() != null) {
			rects.add(hint(new Literal(keys.lock()), state.locked() ? SHAPE + ".lock_on" : SHAPE + ".lock_off"));
		}
		if (before) {
			rects.add(hint(Translated.of(SHAPE + ".esc", Look.PLAIN), SHAPE + ".exit_mode"));
		}
		return labelled(turned(state.locked() ? Translated.of(COPY + ".locked", Look.PLAIN) : null, turned), rects);
	}

	/** The label's text with ", turned" after it once the block is turned or flipped, or "Turned" alone with none. */
	private static Text turned(Text title, boolean turned) {
		if (!turned) {
			return title;
		}
		return title == null ? Translated.of(TURN + ".turned", Look.PLAIN)
			: Translated.of(SINGLE + ".turned_title", Look.PLAIN, title);
	}

	/**
	 * The block in hand as Disable's line tells it: the turning line's state ({@link TurnState}), whether the block a
	 * right click would place hangs on the block behind it, and whether the turned block can stay where it would go.
	 */
	public record DisableState(TurnState turn, boolean hangs, boolean stays) {
	}

	/**
	 * Disable's line (see "Turning real blocks in Disable": "Controls line"): without the sprint key held, the turning
	 * line with no "Turned:", since a right click then places as in Minecraft, and no line for a block that hangs on the
	 * block behind it; with it held, the right click that places the turned block, or in red why it can't, then the
	 * turning line's controls, after "Turned:" once the block is turned or flipped. Null as for the turning line.
	 */
	public static Line disable(DisableState state, String sprint) {
		TurnState turn = state.turn();
		if (!turn.held()) {
			return state.hangs() ? null : turning(new TurnState(turn.turns(), turn.flips(), false, false), sprint);
		}
		Line turning = turning(turn, sprint);
		if (turning == null) {
			return null;
		}
		List<Text> rects = new ArrayList<>(3);
		rects.add(state.stays() ? hint(RIGHT, TURN + ".place") : reason(TURN + ".cant_place"));
		rects.addAll(turning.rects());
		return new Line(turning.label(), List.copyOf(rects));
	}

	/**
	 * The copy at the aim's line. With a zoop's row fixed: the mode, the click that pastes the row (or why it can't),
	 * and the right click that drops the row. Otherwise: the mode, or "Locked", the left click (paste, or why it can't,
	 * in Single; in Zoop and Vertical the reason if there is one, then setting the row's start), the nudge keys while
	 * it's locked, the wheel, the middle click, the sprint key while it's unlocked and there's a paste to line up with
	 * (a locked copy doesn't line up), the lock key, the mode key, and the right click that closes it. A key with no key
	 * bound is left out.
	 */
	public static Line copy(CopyState state, Keys keys) {
		List<Text> rects = new ArrayList<>();
		if (state.row() > 0) {
			rects.add(state.refusal() != null ? refusal(state.refusal())
				: hint(LEFT, state.row() == 1 ? Translated.of(COPY + ".paste_row_one", Look.ACTION)
					: Translated.of(COPY + ".paste_row", Look.ACTION, new Literal(String.valueOf(state.row())))));
			rects.add(hint(RIGHT, COPY + ".cancel"));
			return labelled(modeName(state.mode()), rects);
		}
		if (state.mode() != CopyMode.SINGLE) {
			// A zoop's first click still fixes the row while the copy is red, so it stays, after the reason.
			if (state.refusal() != null) {
				rects.add(refusal(state.refusal()));
			}
			rects.add(hint(LEFT, COPY + ".start"));
		} else {
			rects.add(state.refusal() != null ? refusal(state.refusal()) : hint(LEFT, COPY + ".paste"));
		}
		if (state.locked()) {
			Text nudge = nudge(keys);
			if (nudge != null) {
				rects.add(hint(nudge, COPY + ".nudge"));
			}
		}
		rects.add(hint(SCROLL, COPY + ".turn"));
		rects.add(hint(MIDDLE, COPY + ".mirror"));
		if (!state.locked() && state.pasted() && keys.sprint() != null) {
			rects.add(hint(held(keys.sprint()), COPY + ".line_up"));
		}
		if (keys.lock() != null) {
			rects.add(hint(new Literal(keys.lock()), state.locked() ? COPY + ".unlock" : COPY + ".lock"));
		}
		if (keys.mode() != null) {
			rects.add(hint(new Literal(keys.mode()), MODE + ".zoop_mode"));
		}
		rects.add(hint(RIGHT, COPY + ".put_away"));
		return labelled(title(state.mode(), state.locked()), rects);
	}

	/** A run of Effortless Building's line in one colour: its text and the colour's name ("dark_aqua"), or null. */
	public record Part(String text, String colour) {
	}

	/**
	 * Effortless Building's own line during a shape (hook E31), as Building Planner's rectangles: each of its clauses,
	 * "key to action", separated by ", ", becomes the control "[key: action]", in Effortless Building's own words and
	 * colours: "Left-click to cancel, Right-click to place" is "[Left-click: cancel][Right-click: place]". The key and
	 * the colon are white; the action stays dark aqua, or red as Effortless Building draws a shape that breaks blocks.
	 * Null for a text of another form, or in other colours (a key not white, an action neither dark aqua nor red, or in
	 * more than one colour), which Effortless Building then draws as it is.
	 */
	public static Line effortless(List<Part> parts) {
		StringBuilder text = new StringBuilder();
		List<String> colours = new ArrayList<>();
		for (Part part : parts) {
			text.append(part.text());
			for (int i = 0; i < part.text().length(); i++) {
				colours.add(part.colour());
			}
		}
		List<Text> rects = new ArrayList<>(2);
		int start = 0;
		while (start < text.length()) {
			int end = text.indexOf(", ", start);
			if (end < 0) {
				end = text.length();
			}
			int to = text.indexOf(" to ", start);
			if (to <= start || to + 4 >= end) {
				return null;
			}
			int action = to + 4;
			Look look = switch (String.valueOf(colours.get(action))) {
				case "dark_aqua" -> Look.ACTION;
				case "red" -> Look.WARNING;
				default -> null;
			};
			if (look == null || !colours.subList(start, to).stream().allMatch(c -> c == null || c.equals("white"))
				|| !colours.subList(action, end).stream().allMatch(colours.get(action)::equals)) {
				return null;
			}
			rects.add(hint(new Literal(text.substring(start, to)), new Literal(text.substring(action, end), look)));
			start = end + 2;
		}
		return rects.isEmpty() ? null : new Line(null, List.copyOf(rects));
	}

	/** The label of a line that follows the aim: the mode outside Single, "Locked" or both, or none. */
	private static Text title(CopyMode mode, boolean locked) {
		if (locked) {
			return mode == CopyMode.SINGLE ? Translated.of(COPY + ".locked", Look.PLAIN)
				: Translated.of(COPY + ".mode_locked", Look.PLAIN, modeName(mode));
		}
		return mode == CopyMode.SINGLE ? null : modeName(mode);
	}

	/** The four nudges along the ground: "Arrow keys" while they're on them, otherwise their keys' names. */
	private static Text along(Keys keys) {
		if (keys.nudgesOnArrows()) {
			return Translated.of(COPY + ".arrow_keys", Look.PLAIN);
		}
		List<String> bound = keys.nudges().stream().filter(name -> name != null).toList();
		return bound.isEmpty() ? null : new Literal(String.join("/", bound));
	}

	/**
	 * The keys of the one control "...: nudge": the keys along the ground and the keys up and down joined by " + ", or
	 * whichever of the two has a key, or null while no nudge has one.
	 */
	private static Text nudge(Keys keys) {
		Text along = along(keys);
		Text upDown = upDown(keys);
		if (along != null && upDown != null) {
			return Translated.of(COPY + ".plus", Look.PLAIN, along, upDown);
		}
		return along != null ? along : upDown;
	}

	/**
	 * The nudges up and down, by their keys' names, with the second name leaving out the first word the two names share
	 * ("Page Up/Down", "Keypad 8/2"; "U/J" when they share none), or the one of them with a key.
	 */
	private static Text upDown(Keys keys) {
		if (keys.up() != null && keys.down() != null) {
			String down = keys.down();
			int upSpace = keys.up().indexOf(' ');
			int downSpace = down.indexOf(' ');
			if (upSpace > 0 && downSpace > 0 && keys.up().substring(0, upSpace).equals(down.substring(0, downSpace))) {
				down = down.substring(downSpace + 1);
			}
			return Translated.of(COPY + ".up_down", Look.PLAIN, new Literal(keys.up()), new Literal(down));
		}
		String one = keys.up() != null ? keys.up() : keys.down();
		return one == null ? null : new Literal(one);
	}

	/** A line with its label ("Delete:"), or with none while {@code title} is null. */
	private static Line labelled(Text title, List<Text> rects) {
		return new Line(title == null ? null : Translated.of(MODE + ".label", Look.PLAIN, title), List.copyOf(rects));
	}

	/** A warning above the controls, in red, alone on its line. */
	private static Line warning(String key) {
		return new Line(Translated.of(key, Look.WARNING), List.of());
	}

	/** A control: its key, then its action in dark aqua ("Left-click: paste"). */
	private static Text hint(Text key, Text action) {
		return Translated.of(MODE + ".hint", Look.PLAIN, key, action);
	}

	private static Text hint(Text key, String action) {
		return hint(key, Translated.of(action, Look.ACTION));
	}

	/** "Hold Left Control": a key held down, by its name. */
	private static Text held(String key) {
		return Translated.of(MODE + ".hold", Look.PLAIN, new Literal(key));
	}

	/** A reason in red in place of a control that can't act. */
	private static Text reason(String key) {
		return Translated.of(key, Look.WARNING);
	}

	private static Text refusal(CopyColour.Refusal refusal) {
		return reason(switch (refusal) {
			case TOO_MANY_BLOCKS -> COPY + ".too_many";
			case BUILD_HEIGHT, WORLD_BORDER -> COPY + ".outside";
		});
	}

	private static Text modeName(CopyMode mode) {
		return Translated.of(COPY + "." + mode.name().toLowerCase(Locale.ROOT), Look.PLAIN);
	}
}
