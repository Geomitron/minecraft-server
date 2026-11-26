package dev.buildingplanner.client;

import dev.buildingplanner.input.BoxCorners;
import dev.buildingplanner.input.BoxPurpose;
import dev.buildingplanner.layer.DeleteMarks;
import dev.buildingplanner.layer.Layer;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Delete's box (see "Delete": "Marking with a box"): pressing the left button with the sprint key held sets its first
 * corner at the block the crosshair is on, ghost or real, as hook E9 aims ({@link GhostAim}); the box then follows the
 * aim, the block the crosshair is on giving its second corner, which stays where it was while the aim meets nothing;
 * and letting the left button go marks every ghost block in it, or unmarks them when the hold of the sprint key unmarks
 * ({@link DeleteMarks#box}). It has a Copy box's limits and, while it marks, the cap on the marks; past one it draws
 * red, and letting go marks nothing.
 */
final class MarkBox {
	private BlockPos first;
	private BlockPos second;
	private boolean unmarks;
	private BoxDraw.Shown shown;
	/**
	 * The box last checked against the cap, by its corners, with the marks' and the layer's versions then, and whether
	 * it fitted: the check runs again only when one of them changes.
	 */
	private BlockPos cappedMin;
	private BlockPos cappedMax;
	private long cappedMarks;
	private long cappedLayer;
	private boolean fits;

	/** Whether the box is drawn. */
	boolean on() {
		return first != null;
	}

	/** Whether letting go unmarks the ghost blocks in the box rather than marking them. */
	boolean unmarks() {
		return unmarks;
	}

	/** The box as it shows, in Delete's purpose with one corner set, or null while none is drawn. */
	BoxDraw.Shown shown() {
		return shown;
	}

	/**
	 * The left button with the sprint key held: starts the box at the block the player aims at, which decides whether
	 * the hold marks or unmarks unless it has already decided ({@link DeleteMarks#startBox}). Aiming at nothing starts
	 * no box; returns whether one started.
	 */
	boolean start(DeleteMarks marks, Layer layer, boolean ghostsShown) {
		BlockHitResult hit = GhostAim.aim(layer, ghostsShown, BoxCorners.REACH);
		if (hit == null) {
			return false;
		}
		first = hit.getBlockPos().immutable();
		second = first;
		unmarks = marks.startBox(first);
		cappedMin = null;
		update(marks, layer, ghostsShown);
		return true;
	}

	/** Follows the aim: the block the player aims at is the second corner. Runs once a client tick and at the release. */
	void update(DeleteMarks marks, Layer layer, boolean ghostsShown) {
		ClientLevel level = Minecraft.getInstance().level;
		if (first == null || level == null) {
			shown = null;
			return;
		}
		BlockHitResult hit = GhostAim.aim(layer, ghostsShown, BoxCorners.REACH);
		boolean aimed = hit != null;
		if (aimed) {
			int minY = level.getMinY();
			second = BoxCorners.withinHeight(hit.getBlockPos(), minY, minY + level.getHeight() - 1).immutable();
		}
		BlockPos min = BlockPos.min(first, second);
		BlockPos max = BlockPos.max(first, second);
		// ClientLevel.hasChunk is always true; the chunk cache holds only the chunks the game has loaded.
		ClientChunkCache chunks = level.getChunkSource();
		BoxCorners.Limit limit = BoxCorners.limit(first, second,
			(x, z) -> chunks.getChunk(x, z, ChunkStatus.FULL, false) != null);
		if (limit == null && !unmarks && !fits(marks, layer, min, max)) {
			limit = BoxCorners.Limit.TOO_MANY_BLOCKS;
		}
		shown = new BoxDraw.Shown(BoxPurpose.DELETE, 1, min, max, aimed, limit, null);
	}

	private boolean fits(DeleteMarks marks, Layer layer, BlockPos min, BlockPos max) {
		if (!min.equals(cappedMin) || !max.equals(cappedMax) || marks.version() != cappedMarks
			|| layer.version() != cappedLayer) {
			cappedMin = min;
			cappedMax = max;
			cappedMarks = marks.version();
			cappedLayer = layer.version();
			fits = marks.fits(layer, min, max);
		}
		return fits;
	}

	/**
	 * The left button let go: marks or unmarks the ghost blocks in the box unless it breaks a limit, and ends it.
	 * Returns whether it marked or unmarked.
	 */
	boolean release(DeleteMarks marks, Layer layer, boolean ghostsShown) {
		update(marks, layer, ghostsShown);
		boolean done = shown != null && shown.limit() == null && marks.box(shown.min(), shown.max(), layer, unmarks);
		end();
		return done;
	}

	/** Ends the box, marking nothing. */
	void end() {
		first = null;
		second = null;
		shown = null;
	}

	/** The box as the in-game checks read it: "mark" or "unmark", its corners and its colour, or none. */
	String describe() {
		if (shown == null) {
			return "none";
		}
		return (unmarks ? "unmark " : "mark ") + pos(shown.min()) + ".." + pos(shown.max()) + " "
			+ shown.colour().name().toLowerCase(Locale.ROOT) + (shown.aimed() ? "" : " unaimed");
	}

	private static String pos(BlockPos pos) {
		return pos.getX() + "," + pos.getY() + "," + pos.getZ();
	}
}
