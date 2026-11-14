package dev.buildingplanner;

/**
 * Player-experience choices still open with Geo in inbox request 420 (see docs/building-planner.md), each behind one
 * setting here until Geo answers.
 */
public final class OpenChoices {
	/**
	 * Q6: whether undone steps can be redone. The server's undo history keeps a redo list only while this is on, and
	 * the {@code redo} payload is registered only then. No key sends undo or redo yet.
	 */
	public static final boolean REDO = false;

	/**
	 * Q9: whether server operators can delete any plan, as well as the player who shared it. The delete check
	 * (hook S1) lets operators through only while this is on.
	 */
	public static final boolean OPERATORS_DELETE_ANY_PLAN = false;

	private OpenChoices() {
	}
}
