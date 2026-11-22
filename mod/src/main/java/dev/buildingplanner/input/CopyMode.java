package dev.buildingplanner.input;

/**
 * The copy's build mode, which "Change Zoop or Delete mode" (R) cycles in this order, as R cycles Satisfactory's build modes:
 * Single pastes one copy a click; Zoop and Vertical fix the first copy with a click and paste a row of copies with the
 * next ({@link CopyRow}).
 */
public enum CopyMode {
	SINGLE,
	ZOOP,
	VERTICAL;

	/** The mode R switches to from this one. */
	public CopyMode next() {
		return values()[(ordinal() + 1) % values().length];
	}
}
