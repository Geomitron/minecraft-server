package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.buildingplanner.input.MouseMode.Action;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MouseModeTest {
	/** Pasting, turning, mirroring, undo and redo keep the copy at the aim; everything else puts it away. */
	@Test
	void theCopyAtTheAimEndsWithARightClickAndTheModsOtherActions() {
		assertEquals(EnumSet.of(Action.RIGHT_CLICK, Action.CHOOSE_BUILD_MODE, Action.SHOW_OR_HIDE, Action.COPY_BUTTON,
			Action.DELETE_BUTTON, Action.MATERIAL_LIST_BUTTON, Action.PASTE_BUTTON, Action.BLOCK_MIX, Action.LEAVE),
			ending(MouseMode.COPY_AT_AIM));
	}

	/**
	 * A box keeps going through its left clicks, the wheel, the middle click, undo and redo, and through Copy, Delete
	 * and "Material list", which end or switch it themselves; everything else ends it.
	 */
	@Test
	void aBoxEndsWithARightClickAndTheModsOtherActionsButItsOwnButtons() {
		assertEquals(EnumSet.of(Action.RIGHT_CLICK, Action.CHOOSE_BUILD_MODE, Action.SHOW_OR_HIDE, Action.PASTE_BUTTON,
			Action.BLOCK_MIX, Action.LEAVE), ending(MouseMode.BOX));
	}

	private static Set<Action> ending(MouseMode mode) {
		Set<Action> ending = EnumSet.noneOf(Action.class);
		for (Action action : Action.values()) {
			if (mode.endedBy(action)) {
				ending.add(action);
			}
		}
		return ending;
	}
}
