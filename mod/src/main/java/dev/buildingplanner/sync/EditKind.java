package dev.buildingplanner.sync;

/** What made an edit to the layer. */
public enum EditKind {
	/** A shape drawn with the place click, or a paste. */
	SHAPE,
	/** A shape drawn with the break click, whose changes are air. */
	ERASE
}
