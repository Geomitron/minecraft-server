package dev.buildingplanner.input;

/**
 * Delete's two modes (see "Delete by aim"): a box, whose third click erases the ghost blocks in it, and Aim, where the
 * ghost blocks the player sweeps the aim over are marked to erase. R switches between them.
 */
public enum DeleteMode {
	BOX,
	AIM;

	/** The mode R switches to. */
	public DeleteMode next() {
		return this == BOX ? AIM : BOX;
	}
}
