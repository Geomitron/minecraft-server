package dev.buildingplanner.input;

/**
 * The copy's mode, which "Change copy mode" (R) cycles in this order, as R cycles Satisfactory's build modes: Off pastes
 * one copy a click; Line, Wall and Floor fix the first copy with a click and paste a set of copies laid as Effortless
 * Building's Line, Wall and Floor lay blocks with the next ({@link CopyGrid}).
 */
public enum CopyMode {
	OFF,
	LINE,
	WALL,
	FLOOR;

	/** The mode R switches to from this one. */
	public CopyMode next() {
		return values()[(ordinal() + 1) % values().length];
	}
}
