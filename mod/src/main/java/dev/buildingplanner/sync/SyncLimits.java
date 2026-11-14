package dev.buildingplanner.sync;

/** The server's caps on edits, from its config file; the server sends them in {@code hello}. */
public record SyncLimits(int maxEditBlocks, int maxPlanSide) {
	public static final SyncLimits DEFAULT = new SyncLimits(50_000, 512);
	/** The most blocks an edit can carry on the wire (see {@link Payloads#MAX_EDIT_BYTES}). */
	public static final int WIRE_MAX_EDIT_BLOCKS = 200_000;

	public SyncLimits {
		if (maxEditBlocks < 1 || maxEditBlocks > WIRE_MAX_EDIT_BLOCKS) {
			throw new IllegalArgumentException("maxEditBlocks must be 1 to " + WIRE_MAX_EDIT_BLOCKS + ": " + maxEditBlocks);
		}
		if (maxPlanSide < 1) {
			throw new IllegalArgumentException("maxPlanSide must be at least 1: " + maxPlanSide);
		}
	}
}
