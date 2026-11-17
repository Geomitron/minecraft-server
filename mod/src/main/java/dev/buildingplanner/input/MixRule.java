package dev.buildingplanner.input;

/**
 * When the block mix applies to Effortless Building's shapes (hooks E5 and E6): while it's on, the main hand holds a
 * block, and Effortless Building's mode isn't Disable, where every click is vanilla's. With a tool or any other item
 * in hand, clicks act as they would without the mix.
 */
public final class MixRule {
	private MixRule() {
	}

	/** Whether the mix applies, given the player's mix, the main hand and Effortless Building's mode. */
	public static boolean applies(boolean mixOn, boolean holdsBlock, boolean modeDisabled) {
		return mixOn && holdsBlock && !modeDisabled;
	}
}
