package dev.buildingplanner.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * What Effortless Building's preview drew in the last whole frame: the positions it drew as blocks to place or break,
 * as its {@code renderBoundingBoxFaces} gets them, for the driver's {@code preview}.
 */
public final class PreviewProbe {
	private static List<BlockPos> drawing = new ArrayList<>();
	private static List<BlockPos> drawn = List.of();

	private PreviewProbe() {
	}

	/** A frame begins: what the one before drew is the last whole frame's. */
	public static void beforeFrame() {
		drawn = drawing;
		drawing = new ArrayList<>();
	}

	/** The preview draws these positions in this frame. */
	public static void drawing(List<BlockPos> positions) {
		positions.forEach(pos -> drawing.add(pos.immutable()));
	}

	/** The positions the last whole frame's preview drew, as x,y,z in the order drawn, or none. */
	static String positions() {
		return drawn.isEmpty() ? "none" : String.join(" ",
			drawn.stream().map(pos -> pos.getX() + "," + pos.getY() + "," + pos.getZ()).toList());
	}
}
