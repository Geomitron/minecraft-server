package dev.buildingplanner.sync;

import java.util.List;
import java.util.Locale;

/**
 * A message players see: a translation key in {@code assets/buildingplanner/lang/en_us.json} and its arguments. Every
 * message the add-on sends is made here, so the keys and the language file stay in step (a test checks them).
 */
public record PlayerMessage(String key, List<String> args) {
	private static final String PREFIX = "buildingplanner.";

	public PlayerMessage {
		args = List.copyOf(args);
	}

	public static PlayerMessage tooManyBlocks(int blocks, int max) {
		return new PlayerMessage(PREFIX + "rejected.too_many_blocks", List.of(Integer.toString(blocks), Integer.toString(max)));
	}

	/** An edit to the layer in another dimension than the player's (sent just before they changed dimension, say). */
	public static PlayerMessage otherDimension() {
		return new PlayerMessage(PREFIX + "rejected.other_dimension", List.of());
	}

	/** An edit to the layer that reaches a column the player's game hasn't loaded. */
	public static PlayerMessage notLoaded() {
		return new PlayerMessage(PREFIX + "rejected.unloaded", List.of());
	}

	public static PlayerMessage outsideBuildHeight() {
		return new PlayerMessage(PREFIX + "rejected.build_height", List.of());
	}

	public static PlayerMessage outsideWorldBorder() {
		return new PlayerMessage(PREFIX + "rejected.world_border", List.of());
	}

	public static PlayerMessage nothingToUndo() {
		return new PlayerMessage(PREFIX + "undo.nothing", List.of());
	}

	/** After an undo (or a redo) that left blocks someone else changed since. */
	public static PlayerMessage blocksLeftAsTheyAre(int blocks) {
		return blocks == 1 ? new PlayerMessage(PREFIX + "undo.left_as_it_is", List.of())
			: new PlayerMessage(PREFIX + "undo.left_as_they_are", List.of(Integer.toString(blocks)));
	}

	public static PlayerMessage nothingToRedo() {
		return new PlayerMessage(PREFIX + "redo.nothing", List.of());
	}

	/** The player turned the block mix on in the picker; {@code key} is the picker key's name. */
	public static PlayerMessage mixOn(String key) {
		return new PlayerMessage(PREFIX + "mix.on", List.of(key));
	}

	/** The player turned the block mix off with the picker's "Clear Mix". */
	public static PlayerMessage mixOff() {
		return new PlayerMessage(PREFIX + "mix.off", List.of());
	}

	/** The box holds no ghost blocks and no real blocks other than air. */
	public static PlayerMessage nothingToCopy() {
		return new PlayerMessage(PREFIX + "copy.nothing", List.of());
	}

	/** The box holds more blocks than a paste may carry. */
	public static PlayerMessage copyTooLarge(int blocks, int max) {
		return new PlayerMessage(PREFIX + "copy.too_many", List.of(Integer.toString(blocks), Integer.toString(max)));
	}

	/** The player copied {@code blocks} blocks. */
	public static PlayerMessage copied(int blocks) {
		return blocks == 1 ? new PlayerMessage(PREFIX + "copy.done_one", List.of())
			: new PlayerMessage(PREFIX + "copy.done", List.of(Integer.toString(blocks)));
	}

	/** The player hid the ghost blocks with the show or hide key or button. */
	public static PlayerMessage ghostsHidden() {
		return new PlayerMessage(PREFIX + "ghosts.hidden", List.of());
	}

	/** The player showed the ghost blocks with the show or hide key or button. */
	public static PlayerMessage ghostsShown() {
		return new PlayerMessage(PREFIX + "ghosts.shown", List.of());
	}

	/**
	 * Ghost blocks were hidden and the player drew, which shows them again; {@code key} is the show or hide key's
	 * name.
	 */
	public static PlayerMessage ghostsShownAgain(String key) {
		return new PlayerMessage(PREFIX + "ghosts.shown_again", List.of(key));
	}

	/** The player pressed Paste with no copy since they joined. */
	public static PlayerMessage nothingToPaste() {
		return new PlayerMessage(PREFIX + "paste.nothing", List.of());
	}

	/** The player's Delete box erased {@code blocks} ghost blocks, the count written with digit grouping ("16,001"). */
	public static PlayerMessage erased(int blocks) {
		return blocks == 1 ? new PlayerMessage(PREFIX + "delete.done_one", List.of())
			: new PlayerMessage(PREFIX + "delete.done", List.of(String.format(Locale.US, "%,d", blocks)));
	}

	/** The player's Delete box holds no ghost blocks, so it erased nothing. */
	public static PlayerMessage nothingToErase() {
		return new PlayerMessage(PREFIX + "delete.nothing", List.of());
	}
}
