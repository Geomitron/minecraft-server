package dev.buildingplanner.sync;

/** What made an edit, which decides how the server treats blocks outside the plan's box. */
public enum EditKind {
	/** An Effortless Building shape: grows the plan to fit. */
	SHAPE,
	/** A shape drawn with the break click: blocks outside the box are already empty, so they're skipped. */
	ERASE,
	/** One client tick of Litematica's Edit Schematic writes: always inside the box, never grows it. */
	LITEMATICA
}
