package dev.buildingplanner.look;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;

/**
 * How the red frame for the left click reads as red on a ghost block (see "Locking and the first click": "The first
 * click's blocks"; 1391 Q1 B). Effortless Building draws its erase look while the world draws, so Litematica's light
 * blue overlay and the ghost blocks drawn through walls, drawn later, leave it dark red edges and faint red sides. On
 * the framed positions that hold a ghost block, a box in Effortless Building's erase red therefore draws after them:
 * sides at {@value #SIDES_ALPHA} alpha where the ghost block shows, standing {@value #SIDES_OUT} of a block out from
 * it, in front of Effortless Building's own sides at 0.002, and an opaque outline {@value #LINE_WIDTH} pixels wide,
 * twice the through-walls outline's. A real block keeps Effortless Building's look alone.
 */
public final class GhostFrame {
	/** Effortless Building's erase red, #FF0000. */
	public static final int RGB = 0xFF0000;
	public static final float SIDES_ALPHA = 0.5f;
	public static final float LINE_ALPHA = 1f;
	public static final float LINE_WIDTH = 4f;
	public static final float SIDES_OUT = 0.01f;

	private GhostFrame() {
	}

	/** The positions among {@code framed} that hold a ghost block ({@code ghost}), which the box draws on, in order. */
	public static List<BlockPos> on(List<BlockPos> framed, Predicate<BlockPos> ghost) {
		return framed.stream().filter(ghost).toList();
	}
}
