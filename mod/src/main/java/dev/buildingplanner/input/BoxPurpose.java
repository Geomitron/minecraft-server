package dev.buildingplanner.input;

/** What a box Building Planner draws is for: Copy's and the material list's three-click box, or Delete's box. */
public enum BoxPurpose {
	/** The ghost blocks and real blocks in the box become the copy. */
	COPY,
	/** The ghost blocks in the box are marked, or unmarked, for Delete's erase. */
	DELETE,
	/** Litematica's material list opens for the blocks in the box. */
	MATERIAL_LIST
}
