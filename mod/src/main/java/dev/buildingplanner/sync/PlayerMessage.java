package dev.buildingplanner.sync;

import java.util.List;

/**
 * A message players see: a translation key in {@code assets/buildingplanner/lang/en_us.json} and its arguments. Every
 * message the add-on sends is made here, so the keys and the language file stay in step (a test checks them).
 */
public record PlayerMessage(String key, List<String> args) {
	private static final String PREFIX = "buildingplanner.";

	public PlayerMessage {
		args = List.copyOf(args);
	}

	public static PlayerMessage planGone() {
		return new PlayerMessage(PREFIX + "rejected.plan_gone", List.of());
	}

	public static PlayerMessage planNotLoaded() {
		return new PlayerMessage(PREFIX + "rejected.not_loaded", List.of());
	}

	public static PlayerMessage tooManyBlocks(int blocks, int max) {
		return new PlayerMessage(PREFIX + "rejected.too_many_blocks", List.of(Integer.toString(blocks), Integer.toString(max)));
	}

	public static PlayerMessage planTooLarge(int side, int max) {
		return new PlayerMessage(PREFIX + "rejected.too_large", List.of(Integer.toString(side), Integer.toString(max)));
	}

	/** An edit no client should send: a region the plan lacks, or a Litematica write outside the plan's box. */
	public static PlayerMessage editDoesNotFit() {
		return new PlayerMessage(PREFIX + "rejected.does_not_fit", List.of());
	}

	public static PlayerMessage nothingToUndo() {
		return new PlayerMessage(PREFIX + "undo.nothing", List.of());
	}

	/** After an undo (or a redo) that left blocks someone else changed since. */
	public static PlayerMessage blocksLeftAsTheyAre(int blocks) {
		return new PlayerMessage(PREFIX + "undo.left_as_they_are", List.of(Integer.toString(blocks)));
	}

	public static PlayerMessage nothingToRedo() {
		return new PlayerMessage(PREFIX + "redo.nothing", List.of());
	}

	/** The player pressed the key to start a plan without an area marked with Litematica's area tool. */
	public static PlayerMessage noAreaMarked() {
		return new PlayerMessage(PREFIX + "start.no_area", List.of());
	}

	/** The player drew a shape outside every plan with no plan selected in Litematica. */
	public static PlayerMessage noPlanForShape() {
		return new PlayerMessage(PREFIX + "shape.no_plan", List.of());
	}

	/** The player turned the block mix on in the picker; {@code key} is the picker key's name. */
	public static PlayerMessage mixOn(String key) {
		return new PlayerMessage(PREFIX + "mix.on", List.of(key));
	}

	/** The player turned the block mix off with the picker key. */
	public static PlayerMessage mixOff() {
		return new PlayerMessage(PREFIX + "mix.off", List.of());
	}

	/** The delete check stopped a player deleting a plan someone else shared. */
	public static PlayerMessage notYourPlan(String owner) {
		return new PlayerMessage(PREFIX + "delete.not_yours", List.of(owner));
	}
}
