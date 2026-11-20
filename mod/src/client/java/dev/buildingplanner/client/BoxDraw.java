package dev.buildingplanner.client;

import dev.buildingplanner.input.BoxCorners;
import dev.buildingplanner.layer.Layer;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Building Planner's box for Copy or the material list, one of its mouse modes: three left clicks set the first corner,
 * the opposite corner on the same level, and the height ({@link BoxCorners}). Between clicks the box follows the aim,
 * with the corners set so far fixed. A box past a limit draws red, and its click sets no corner. It uses none of
 * Effortless Building's classes.
 */
final class BoxDraw {
	/** What the box is for. */
	enum Purpose {
		/** The ghost blocks and real blocks in the box become the copy. */
		COPY,
		/** Litematica's material list opens for the blocks in the box. */
		MATERIAL_LIST
	}

	/**
	 * The box as it shows now: what it's for, how many corners are set (0 to 2), its lowest and highest corners for the
	 * aim now, whether the aim gives the next corner, and the limit it breaks, or null. With no corner set and the player
	 * aiming at nothing, {@code min} and {@code max} are null.
	 */
	record Shown(Purpose purpose, int corners, BlockPos min, BlockPos max, boolean aimed, BoxCorners.Limit limit) {
		/** Whether the next left click sets a corner. */
		boolean clickable() {
			return aimed && limit == null;
		}
	}

	/** A finished box, by its lowest and highest corners. */
	record Finished(Purpose purpose, BlockPos min, BlockPos max) {
	}

	private Purpose purpose;
	private BlockPos first;
	private BlockPos second;
	/** The box for the aim at the last update, or null while no box is drawn. */
	private Shown shown;
	/** The corner the aim gave at the last update, which the next left click sets. */
	private BlockPos next;

	/** Whether a box is drawn. */
	boolean on() {
		return purpose != null;
	}

	Purpose purpose() {
		return purpose;
	}

	/** Starts a box with no corners set. */
	void start(Purpose purpose) {
		this.purpose = purpose;
		first = null;
		second = null;
		shown = null;
	}

	/** The box is for the other purpose now, keeping the corners set so far. */
	void switchTo(Purpose purpose) {
		this.purpose = purpose;
		if (shown != null) {
			shown = new Shown(purpose, shown.corners(), shown.min(), shown.max(), shown.aimed(), shown.limit());
		}
	}

	/** Ends the box; it stops drawing. */
	void end() {
		purpose = null;
		first = null;
		second = null;
		shown = null;
	}

	/** The box as it shows, or null while none is drawn. */
	Shown shown() {
		return shown;
	}

	/**
	 * Follows the aim: the box from the corners set so far to the next corner the player aims at ({@link Aim}), aiming at ghost blocks
	 * in {@code layer} while they're {@code ghostsShown}. Runs once a client tick and before each left click.
	 */
	void update(Layer layer, boolean ghostsShown) {
		Minecraft mc = Minecraft.getInstance();
		Entity aimer = Aim.entity();
		ClientLevel level = mc.level;
		if (purpose == null || aimer == null || level == null) {
			shown = null;
			return;
		}
		Vec3 eye = aimer.getEyePosition();
		Vec3 look = aimer.getLookAngle();
		BlockPos from;
		BlockPos to;
		if (first == null) {
			BlockHitResult hit = GhostAim.aim(layer, ghostsShown, BoxCorners.REACH);
			from = hit == null ? null : BoxCorners.first(hit.getBlockPos(), hit.getDirection());
			to = from;
		} else if (second == null) {
			from = first;
			to = BoxCorners.second(eye, look, first);
		} else {
			from = first;
			OptionalInt height = BoxCorners.height(eye, look, second);
			to = height.isPresent() ? second.atY(height.getAsInt()) : null;
		}
		int corners = first == null ? 0 : second == null ? 1 : 2;
		boolean aimed = to != null;
		if (!aimed) {
			// Aiming at nothing shows the corners set so far.
			to = corners == 2 ? second : from;
		}
		if (from == null) {
			shown = new Shown(purpose, 0, null, null, false, null);
			return;
		}
		int minY = level.getMinY();
		int maxY = minY + level.getHeight() - 1;
		from = BoxCorners.withinHeight(from, minY, maxY);
		to = BoxCorners.withinHeight(to, minY, maxY);
		// ClientLevel.hasChunk is always true; the chunk cache holds only the chunks the game has loaded.
		ClientChunkCache chunks = level.getChunkSource();
		shown = new Shown(purpose, corners, BlockPos.min(from, to), BlockPos.max(from, to), aimed,
			BoxCorners.limit(from, to, (x, z) -> chunks.getChunk(x, z, ChunkStatus.FULL, false) != null));
		next = to;
	}

	/**
	 * The left click: sets the next corner where the aim gives one within the limits, and otherwise does nothing.
	 * Returns the finished box after the third, which ends the mode; null before.
	 */
	Finished click(Layer layer, boolean ghostsShown) {
		update(layer, ghostsShown);
		if (shown == null || !shown.clickable()) {
			return null;
		}
		switch (shown.corners()) {
			case 0 -> first = next;
			case 1 -> second = next;
			default -> {
				Finished finished = new Finished(purpose, shown.min(), shown.max());
				end();
				return finished;
			}
		}
		update(layer, ghostsShown);
		return null;
	}

	/** The box as the in-game checks read it: what it's for, the corners set, its corners and its colour, or none. */
	String describe() {
		if (shown == null) {
			return purpose == null ? "none" : purpose + " 0 unaimed";
		}
		if (shown.min() == null) {
			return shown.purpose() + " 0 unaimed";
		}
		return shown.purpose() + " " + shown.corners() + " " + pos(shown.min()) + ".." + pos(shown.max()) + " "
			+ (shown.limit() != null ? "red" : "white") + (shown.aimed() ? "" : " unaimed");
	}

	private static String pos(BlockPos pos) {
		return pos.getX() + "," + pos.getY() + "," + pos.getZ();
	}
}
