package dev.buildingplanner.input;

import dev.buildingplanner.input.CopyPlacement.Spot;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

/**
 * The boxes of the player's most recent pastes in this dimension, whatever has happened to their blocks since, which
 * the copy at the aim snaps to, as a Satisfactory foundation snaps edge to edge to the one it's aimed at.
 */
public final class PasteBoxes {
	/** How many of the most recent pastes the copy snaps to. */
	public static final int KEPT = 16;

	/** Most recent first. */
	private final Deque<Spot> boxes = new ArrayDeque<>();

	/** A paste's box, as the most recent. */
	public void add(Spot box) {
		boxes.addFirst(box);
		while (boxes.size() > KEPT) {
			boxes.removeLast();
		}
	}

	/** Forgets every paste, on leaving the server or changing dimension. */
	public void clear() {
		boxes.clear();
	}

	/** The most recent paste's box, or null before the first. */
	public Spot latest() {
		return boxes.peekFirst();
	}

	/**
	 * Where a copy of {@code size} blocks, as it's turned, sits for the player's aim at the face {@code face} of the
	 * block {@code aimed}: against that face; flush against the face of the most recent paste's box that holds the aimed
	 * block, when one does; and, while {@code inLine} (the sprint key held), in line with the most recent paste.
	 */
	public Spot place(BlockPos aimed, Direction face, Vec3i size, boolean inLine) {
		Spot spot = CopyPlacement.againstFace(aimed, face, size);
		for (Spot box : boxes) {
			if (box.contains(aimed)) {
				spot = flush(box, aimed, face, size);
				break;
			}
		}
		Spot latest = latest();
		return inLine && latest != null ? inLine(latest, spot) : spot;
	}

	/**
	 * A copy of {@code size} flush against the face {@code face} of {@code box}, outside it, with its edges in line with
	 * the box's edges at the corner of that face nearest {@code aimed}; a tie takes the lower coordinates.
	 */
	static Spot flush(Spot box, BlockPos aimed, Direction face, Vec3i size) {
		BlockPos boxMax = box.max();
		int[] min = new int[3];
		for (Direction.Axis axis : Direction.Axis.values()) {
			int low = axis.choose(box.min().getX(), box.min().getY(), box.min().getZ());
			int high = axis.choose(boxMax.getX(), boxMax.getY(), boxMax.getZ());
			int length = axis.choose(size.getX(), size.getY(), size.getZ());
			int at = axis.choose(aimed.getX(), aimed.getY(), aimed.getZ());
			int i = axis.ordinal();
			if (axis == face.getAxis()) {
				min[i] = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? high + 1 : low - length;
			} else {
				min[i] = at - low <= high - at ? low : high - length + 1;
			}
		}
		return new Spot(new BlockPos(min[0], min[1], min[2]), size);
	}

	/**
	 * {@code spot} moved into line with {@code latest}: on its level, and on its row (along x) or its column (along z),
	 * whichever the copy's middle is nearer, the copy's edge in line with the paste's on the side the copy's middle is
	 * on; a tie takes the row, and the paste's lower edge.
	 */
	static Spot inLine(Spot latest, Spot spot) {
		BlockPos at = spot.middle();
		BlockPos paste = latest.middle();
		Vec3i size = spot.size();
		int y = latest.min().getY();
		int x = spot.min().getX();
		int z = spot.min().getZ();
		if (Math.abs(at.getZ() - paste.getZ()) <= Math.abs(at.getX() - paste.getX())) {
			z = at.getZ() <= paste.getZ() ? latest.min().getZ() : latest.max().getZ() - size.getZ() + 1;
		} else {
			x = at.getX() <= paste.getX() ? latest.min().getX() : latest.max().getX() - size.getX() + 1;
		}
		return new Spot(new BlockPos(x, y, z), size);
	}
}
