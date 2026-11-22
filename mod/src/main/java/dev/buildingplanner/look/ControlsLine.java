package dev.buildingplanner.look;

import dev.buildingplanner.input.BoxCorners;
import dev.buildingplanner.input.CopyColour;
import dev.buildingplanner.input.CopyMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The lines of a mouse mode's controls (see "Building Planner's mouse modes"), as texts from the language file with
 * the keys' names in them, worked out from the mode's state and the keys as the player has them bound. The game turns
 * them into its own components ({@code ModeHud}): white, with each action in dark aqua and a reason the copy can't be
 * pasted in red.
 */
public final class ControlsLine {
	private static final String BOX = "buildingplanner.ui.mode.box";
	private static final String COPY = "buildingplanner.ui.mode.copy";
	private static final String DELETE = "buildingplanner.ui.mode.delete";
	private static final String TURN = "buildingplanner.ui.mode.turn";
	private static final String SINGLE = "buildingplanner.ui.mode.single";

	private ControlsLine() {
	}

	/** How a part of a line looks: as the line, as an action, or as a warning. */
	public enum Look {
		PLAIN, ACTION, WARNING
	}

	/** A part of a line. */
	public sealed interface Text permits Translated, Literal, Joined {
	}

	/** A text from the language file, with its arguments in its {@code %s} places. */
	public record Translated(String key, Look look, List<Text> args) implements Text {
		static Translated of(String key, Look look, Text... args) {
			return new Translated(key, look, List.of(args));
		}
	}

	/** A key's name as the game gives it. */
	public record Literal(String text) implements Text {
	}

	/** Parts separated by ", ". */
	public record Joined(List<Text> parts) implements Text {
	}

	/** A line of the controls, with the box's size at its end, which never breaks across lines, or null. */
	public record Line(Text text, Text size) {
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
		List<Text> parts = new ArrayList<>(3);
		parts.add(click(action(next)));
		if (purpose.equals("DELETE") && mode != null) {
			parts.add(modeFor(mode, DELETE + ".aim"));
		}
		parts.add(Translated.of(COPY + ".right", Look.PLAIN, action(BOX + ".cancel")));
		Text controls = Translated.of(COPY + ".titled", Look.PLAIN,
			Translated.of(BOX + "." + purpose.toLowerCase(Locale.ROOT), Look.PLAIN), new Joined(List.copyOf(parts)));
		Text sides = size == null ? null : Translated.of(BOX + ".size", Look.PLAIN, new Literal(String.valueOf(size[0])),
			new Literal(String.valueOf(size[1])), new Literal(String.valueOf(size[2])));
		List<Line> lines = new ArrayList<>(2);
		if (limit != null) {
			String reason = switch (limit) {
				case SIDE, TOO_MANY_BLOCKS -> BOX + ".too_large";
				case NOT_LOADED -> BOX + ".not_loaded";
			};
			lines.add(new Line(Translated.of(reason, Look.WARNING), null));
		}
		lines.add(new Line(controls, sides));
		return lines;
	}

	/**
	 * Delete by aim's lines: "Too many blocks" once the marks reach the cap, then its controls: with marks, the hold that
	 * erases them, or its progress while the left button is held; the sprint key that marks, or unmarks during a sweep
	 * that unmarks; the mode key, which switches to a Delete box; and the right click, which clears the marks, or with
	 * none closes Aim. A key with no key bound is left out.
	 */
	public static List<Line> deleteAim(AimState state, Keys keys) {
		List<Text> parts = new ArrayList<>();
		if (state.marked()) {
			parts.add(state.erasing() >= 0
				? Translated.of(DELETE + ".erasing", Look.ACTION, new Literal(String.valueOf(state.erasing())))
				: Translated.of(DELETE + ".hold_click", Look.PLAIN, action(DELETE + ".erase")));
		}
		if (keys.sprint() != null) {
			parts.add(Translated.of(COPY + ".hold", Look.PLAIN, new Literal(keys.sprint()),
				action(state.unmarking() ? DELETE + ".unmark" : DELETE + ".mark")));
		}
		if (keys.mode() != null) {
			parts.add(modeFor(keys.mode(), DELETE + ".box"));
		}
		parts.add(Translated.of(COPY + ".right", Look.PLAIN,
			action(state.marked() ? DELETE + ".clear" : COPY + ".put_away")));
		List<Line> lines = new ArrayList<>(2);
		if (state.full()) {
			lines.add(new Line(Translated.of(BOX + ".too_large", Look.WARNING), null));
		}
		lines.add(new Line(Translated.of(COPY + ".titled", Look.PLAIN,
			Translated.of(DELETE + ".titled", Look.PLAIN, Translated.of(DELETE + ".aim", Look.PLAIN)),
			new Joined(List.copyOf(parts))), null));
		return lines;
	}

	/**
	 * The block in hand as the turning line tells it: whether it turns and whether it flips (or a block of the mix
	 * does), whether Minecraft's sprint key is held, and whether the block has been turned or flipped.
	 */
	public record TurnState(boolean turns, boolean flips, boolean held, boolean turned) {
	}

	/**
	 * The turning line (see "One ghost block": "Controls line"): with the sprint key held, the wheel that turns and the
	 * middle click that flips, otherwise the sprint key held to turn or flip; each part only for a block that turns or
	 * flips, and "Turned:" before them once the block is turned or flipped. Null for a block that does neither, and
	 * while the sprint key has no key bound.
	 */
	public static Line turning(TurnState state, String sprint) {
		List<Text> parts = turnParts(state, sprint);
		return parts.isEmpty() ? null : titled(state.turned() ? Translated.of(TURN + ".turned", Look.PLAIN) : null, parts);
	}

	/**
	 * The turning line's parts: with the sprint key held, the wheel that turns and the middle click that flips, otherwise
	 * the sprint key held to turn or flip, each only for a block that turns or flips; none for a block that does neither
	 * or while the sprint key has no key bound.
	 */
	private static List<Text> turnParts(TurnState state, String sprint) {
		if (!state.turns() && !state.flips() || sprint == null) {
			return List.of();
		}
		List<Text> parts = new ArrayList<>(2);
		if (state.held()) {
			if (state.turns()) {
				parts.add(Translated.of(COPY + ".scroll", Look.PLAIN, action(COPY + ".turn")));
			}
			if (state.flips()) {
				parts.add(Translated.of(COPY + ".middle", Look.PLAIN, action(TURN + ".flip")));
			}
		} else {
			String verb = !state.flips() ? COPY + ".turn" : !state.turns() ? TURN + ".flip" : TURN + ".turn_or_flip";
			parts.add(Translated.of(COPY + ".hold", Look.PLAIN, new Literal(sprint), action(verb)));
		}
		return parts;
	}

	/**
	 * The one ghost block as its line tells it: its own mode, whether it's locked, the number of blocks in a Zoop or
	 * Vertical row fixed by its first click (0 before it), whether the block or the row is red (outside the world's build
	 * height or its border), and the block in hand as the turning line tells it.
	 */
	public record SingleState(CopyMode mode, boolean locked, int row, boolean red, TurnState turn) {
	}

	/**
	 * Single's line (see "One ghost block": "Controls line"). With a row fixed: the mode, the right click that places the
	 * row with its count (or in red why it can't), the turning parts, and the left click that cancels the row, locked or
	 * not. Otherwise: the mode, "Locked" or both, the right click (placing, or in red why it can't; in Zoop and Vertical
	 * the reason if there is one, then setting the row's start), the nudge keys while it's locked, the left click that
	 * erases, the turning parts, the lock key and the mode key. ", turned" follows the mode and the lock once the block
	 * is turned or flipped, or "Turned" alone in Single. A key with no key bound is left out.
	 */
	public static Line single(SingleState state, Keys keys) {
		List<Text> turning = turnParts(state.turn(), keys.sprint());
		List<Text> parts = new ArrayList<>();
		Text title;
		if (state.row() > 0) {
			title = modeName(state.mode());
			parts.add(state.red() ? Translated.of(SINGLE + ".outside", Look.WARNING)
				: Translated.of(COPY + ".right", Look.PLAIN, state.row() == 1
					? Translated.of(SINGLE + ".place_row_one", Look.ACTION)
					: Translated.of(SINGLE + ".place_row", Look.ACTION, new Literal(String.valueOf(state.row())))));
			parts.addAll(turning);
			parts.add(click(action(COPY + ".cancel")));
			return titled(turned(title, state.turn().turned()), parts);
		}
		if (state.locked()) {
			Text locked = Translated.of(COPY + ".locked", Look.PLAIN);
			title = state.mode() == CopyMode.SINGLE ? locked
				: Translated.of(COPY + ".mode_locked", Look.PLAIN, modeName(state.mode()));
		} else {
			title = state.mode() == CopyMode.SINGLE ? null : modeName(state.mode());
		}
		if (state.mode() != CopyMode.SINGLE) {
			// The first click still fixes the row's start while the block is red, so it stays, after the reason.
			if (state.red()) {
				parts.add(Translated.of(SINGLE + ".outside", Look.WARNING));
			}
			parts.add(Translated.of(COPY + ".right", Look.PLAIN, action(COPY + ".start")));
		} else {
			parts.add(state.red() ? Translated.of(SINGLE + ".outside", Look.WARNING)
				: Translated.of(COPY + ".right", Look.PLAIN, action(TURN + ".place")));
		}
		if (state.locked()) {
			Text nudge = nudge(keys);
			if (nudge != null) {
				parts.add(key(nudge, COPY + ".nudge"));
			}
		}
		parts.add(click(action(DELETE + ".erase")));
		parts.addAll(turning);
		if (keys.lock() != null) {
			parts.add(key(new Literal(keys.lock()), state.locked() ? COPY + ".unlock" : COPY + ".lock"));
		}
		if (keys.mode() != null) {
			parts.add(modeFor(keys.mode(), modeKey(state.mode().next())));
		}
		return titled(turned(title, state.turn().turned()), parts);
	}

	/** The line's start with ", turned" after it once the block is turned or flipped, or "Turned" alone with none. */
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
	 * turning line's parts, after "Turned:" once the block is turned or flipped. Null as for the turning line.
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
		List<Text> parts = new ArrayList<>(3);
		parts.add(state.stays() ? Translated.of(COPY + ".right", Look.PLAIN, action(TURN + ".place"))
			: Translated.of(TURN + ".cant_place", Look.WARNING));
		if (turn.turns()) {
			parts.add(Translated.of(COPY + ".scroll", Look.PLAIN, action(COPY + ".turn")));
		}
		if (turn.flips()) {
			parts.add(Translated.of(COPY + ".middle", Look.PLAIN, action(TURN + ".flip")));
		}
		return titled(turn.turned() ? Translated.of(TURN + ".turned", Look.PLAIN) : null, parts);
	}

	/** "R for Aim": the mode key, by its name, and the mode it switches to, as an action. */
	private static Text modeFor(String key, String mode) {
		return Translated.of(COPY + ".for", Look.PLAIN, new Literal(key), Translated.of(mode, Look.ACTION));
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
		List<Text> parts = new ArrayList<>();
		Text title;
		if (state.row() > 0) {
			title = modeName(state.mode());
			parts.add(state.refusal() != null ? refusal(state.refusal())
				: click(state.row() == 1 ? Translated.of(COPY + ".paste_row_one", Look.ACTION)
					: Translated.of(COPY + ".paste_row", Look.ACTION, new Literal(String.valueOf(state.row())))));
			parts.add(Translated.of(COPY + ".right", Look.PLAIN, action(COPY + ".cancel")));
			return titled(title, parts);
		}
		if (state.locked()) {
			Text locked = Translated.of(COPY + ".locked", Look.PLAIN);
			title = state.mode() == CopyMode.SINGLE ? locked
				: Translated.of(COPY + ".mode_locked", Look.PLAIN, modeName(state.mode()));
		} else {
			title = state.mode() == CopyMode.SINGLE ? null : modeName(state.mode());
		}
		if (state.mode() != CopyMode.SINGLE) {
			// A zoop's first click still fixes the row while the copy is red, so it stays, after the reason.
			if (state.refusal() != null) {
				parts.add(refusal(state.refusal()));
			}
			parts.add(click(action(COPY + ".start")));
		} else {
			parts.add(state.refusal() != null ? refusal(state.refusal()) : click(action(COPY + ".paste")));
		}
		if (state.locked()) {
			Text nudge = nudge(keys);
			if (nudge != null) {
				parts.add(key(nudge, COPY + ".nudge"));
			}
		}
		parts.add(Translated.of(COPY + ".scroll", Look.PLAIN, action(COPY + ".turn")));
		parts.add(Translated.of(COPY + ".middle", Look.PLAIN, action(COPY + ".mirror")));
		if (!state.locked() && state.pasted() && keys.sprint() != null) {
			parts.add(Translated.of(COPY + ".hold", Look.PLAIN, new Literal(keys.sprint()), action(COPY + ".line_up")));
		}
		if (keys.lock() != null) {
			parts.add(key(new Literal(keys.lock()), state.locked() ? COPY + ".unlock" : COPY + ".lock"));
		}
		if (keys.mode() != null) {
			parts.add(modeFor(keys.mode(), modeKey(state.mode().next())));
		}
		parts.add(Translated.of(COPY + ".right", Look.PLAIN, action(COPY + ".put_away")));
		return titled(title, parts);
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
	 * The keys of the one phrase "... to nudge": the keys along the ground and the keys up and down joined by " + ", or
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

	private static Line titled(Text title, List<Text> parts) {
		Text joined = new Joined(List.copyOf(parts));
		return new Line(title == null ? joined : Translated.of(COPY + ".titled", Look.PLAIN, title, joined), null);
	}

	private static Text refusal(CopyColour.Refusal refusal) {
		return Translated.of(switch (refusal) {
			case TOO_MANY_BLOCKS -> COPY + ".too_many";
			case BUILD_HEIGHT, WORLD_BORDER -> COPY + ".outside";
		}, Look.WARNING);
	}

	private static Text click(Text action) {
		return Translated.of(COPY + ".click", Look.PLAIN, action);
	}

	private static Text key(Text key, String verb) {
		return Translated.of(COPY + ".key", Look.PLAIN, key, action(verb));
	}

	private static Text modeName(CopyMode mode) {
		return Translated.of(modeKey(mode), Look.PLAIN);
	}

	private static String modeKey(CopyMode mode) {
		return COPY + "." + mode.name().toLowerCase(Locale.ROOT);
	}

	private static Text action(String key) {
		return Translated.of(key, Look.ACTION);
	}
}
