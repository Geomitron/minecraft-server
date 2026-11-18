package dev.buildingplanner.input;

/**
 * Building Planner's modes that take over the mouse. While one is on, hooks M1 to M5 take the left, right and middle
 * clicks and the scroll wheel, and Effortless Building's clicks, its longer aim and its preview stay out (hooks E14,
 * E15 and E22). At most one is on at a time.
 */
public enum MouseMode {
	/**
	 * The copy at the aim: a left click pastes it, the scroll wheel turns it, a middle click mirrors it, and a right
	 * click puts it away.
	 */
	COPY_AT_AIM;

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
		MATERIAL_LIST_BUTTON,
		PASTE_BUTTON,
		/** The block mix key. */
		BLOCK_MIX,
		/** Leaving the server or changing dimension. */
		LEAVE,
		UNDO,
		REDO
	}

	/**
	 * Whether the action ends this mode. Each action that ends it then does its own action (Copy and "Material list"
	 * start a box, Paste brings the copy back); undo and redo keep it, so a player can undo a paste and paste again.
	 */
	public boolean endedBy(Action action) {
		return switch (action) {
			case RIGHT_CLICK, CHOOSE_BUILD_MODE, SHOW_OR_HIDE, COPY_BUTTON, MATERIAL_LIST_BUTTON, PASTE_BUTTON, BLOCK_MIX,
				LEAVE -> true;
			case LEFT_CLICK, SCROLL, MIDDLE_CLICK, UNDO, REDO -> false;
		};
	}
}
