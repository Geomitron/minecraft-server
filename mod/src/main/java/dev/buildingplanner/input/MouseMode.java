package dev.buildingplanner.input;

/**
 * Building Planner's modes that take over the mouse. While one is on, hooks M1 to M5 take the left, right and middle
 * clicks and the scroll wheel, and Effortless Building's clicks, its longer aim and its preview stay out (hooks E14,
 * E15 and E22). At most one is on at a time.
 */
public enum MouseMode {
	/**
	 * Drawing a box for Copy or the material list: three left clicks set a corner, the opposite corner on the same level
	 * and the height, and a right click leaves the mode. The middle click does nothing and the wheel changes the hotbar
	 * slot.
	 */
	BOX,
	/**
	 * The copy at the aim: a right click pastes it (in Line, Wall and Floor, the first fixes the first copy and the second
	 * pastes the set), the scroll wheel turns it, a middle click mirrors it, and a left click puts it away, or drops a
	 * fixed set.
	 */
	COPY_AT_AIM,
	/**
	 * Delete: holding Minecraft's sprint key marks the ghost blocks the aim sweeps over, and with the left button too
	 * draws a box that marks the ghost blocks in it; holding the left button alone for a second erases the marks; and a
	 * right click clears the marks, or leaves the mode when there are none. The middle click does nothing, and the wheel
	 * changes the hotbar slot.
	 */
	DELETE;

	/** What the player does in Building Planner or Effortless Building while a mode is on. */
	public enum Action {
		LEFT_CLICK,
		SCROLL,
		MIDDLE_CLICK,
		RIGHT_CLICK,
		/** Choosing a mode in Effortless Building's radial menu, even the one the player had. */
		CHOOSE_BUILD_MODE,
		SHOW_OR_HIDE,
		COPY_BUTTON,
		DELETE_BUTTON,
		MATERIAL_LIST_BUTTON,
		PASTE_BUTTON,
		/** The block mix key, or the radial menu's "Block mix" button. */
		BLOCK_MIX,
		/** Leaving the server or changing dimension. */
		LEAVE,
		UNDO,
		REDO
	}

	/**
	 * Whether the action ends this mode. Each action that ends it then does its own action (Copy and "Material list"
	 * start a box, Delete starts Delete, Paste brings the copy back); undo and redo keep it, so a player can undo a paste
	 * and paste again. A box's own buttons, Copy and "Material list", don't end it here: the same button ends the box and
	 * the other switches it to that purpose, keeping its corners. A box ends too at its third left click, which finishes
	 * it. The Delete button ends Delete, which then starts nothing; Delete's right click ends it only once no ghost block
	 * is marked and no box is drawn, which the session decides before asking. The copy at the aim ends at a left click,
	 * its right click being the paste, and the box and Delete at a right click; a left click during a set drops the set
	 * instead, which the session decides before asking.
	 */
	public boolean endedBy(Action action) {
		return switch (action) {
			case CHOOSE_BUILD_MODE, SHOW_OR_HIDE, PASTE_BUTTON, BLOCK_MIX, LEAVE -> true;
			case DELETE_BUTTON -> true;
			case COPY_BUTTON, MATERIAL_LIST_BUTTON -> this != BOX;
			case LEFT_CLICK -> this == COPY_AT_AIM;
			case RIGHT_CLICK -> this != COPY_AT_AIM;
			case SCROLL, MIDDLE_CLICK, UNDO, REDO -> false;
		};
	}
}
