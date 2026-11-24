package dev.buildingplanner.input;

/**
 * Building Planner's modes that take over the mouse. While one is on, hooks M1 to M5 take the left, right and middle
 * clicks and the scroll wheel, and Effortless Building's clicks, its longer aim and its preview stay out (hooks E14,
 * E15 and E22). At most one is on at a time.
 */
public enum MouseMode {
	/**
	 * Drawing a box for Copy, Delete or the material list: three left clicks set a corner, the opposite corner on the
	 * same level and the height, and a right click leaves the mode. The middle click does nothing and the wheel changes
	 * the hotbar slot.
	 */
	BOX,
	/**
	 * The copy at the aim: a right click pastes it (in Zoop and Vertical, the first fixes a row and the second pastes the
	 * row), the scroll wheel turns it, a middle click mirrors it, and a left click puts it away, or drops a fixed row.
	 */
	COPY_AT_AIM,
	/**
	 * Delete by aim: holding Minecraft's sprint key marks the ghost blocks the aim sweeps over, and a right click clears
	 * the marks, or leaves the mode when there are none. The left click and the middle click do nothing, and the wheel
	 * changes the hotbar slot.
	 */
	DELETE_AIM;

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
	 * Whether the action ends this mode. Each action that ends it then does its own action (Copy, Delete and "Material
	 * list" start a box, Paste brings the copy back); undo and redo keep it, so a player can undo a paste and paste
	 * again. A box's own buttons, Copy, Delete and "Material list", don't end it here: the same button ends the box and
	 * another switches it to that purpose, keeping its corners. A box ends too at its third left click, which finishes
	 * it. Delete by aim ends at each of the three, and Delete then starts nothing; its right click ends it only once no
	 * ghost block is marked, which the session decides before asking. The copy at the aim ends at a left click, its
	 * right click being the paste, and the box and Delete by aim at a right click; a left click during a zoop's row
	 * drops the row instead, which the session decides before asking.
	 */
	public boolean endedBy(Action action) {
		return switch (action) {
			case CHOOSE_BUILD_MODE, SHOW_OR_HIDE, PASTE_BUTTON, BLOCK_MIX, LEAVE -> true;
			case COPY_BUTTON, DELETE_BUTTON, MATERIAL_LIST_BUTTON -> this != BOX;
			case LEFT_CLICK -> this == COPY_AT_AIM;
			case RIGHT_CLICK -> this != COPY_AT_AIM;
			case SCROLL, MIDDLE_CLICK, UNDO, REDO -> false;
		};
	}
}
