package dev.buildingplanner.input;

/** What Building Planner's box is for. */
public enum BoxPurpose {
	/** The ghost blocks and real blocks in the box become the copy. */
	COPY,
	/** The ghost blocks in the box are erased, as one edit. */
	DELETE,
	/** Litematica's material list opens for the blocks in the box. */
	MATERIAL_LIST
}
