package dev.buildingplanner.look;

import dev.buildingplanner.input.BoxPurpose;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * How Building Planner's box looks while it's drawn: its outline and see-through sides in the colour of its purpose
 * (white for Copy, the copy's blue #00D4FF for the material list, the marks' red #FF7070 for Delete), or red while it
 * breaks a limit; and, before its first corner is set, the top face of the block the player aims at shaded black, so
 * the player sees which block the corner sits on.
 */
public final class BoxLook {
	/** The shaded face's alpha. */
	public static final float SHADE_ALPHA = 0.35f;
	/** How far above the block's top the shaded square is drawn, so it never fights with the block's own face. */
	static final double SHADE_LIFT = 0.005;

	private BoxLook() {
	}

	/** A box colour: its outline's red, green and blue as 0xRRGGBB, drawn opaque, and its sides' alpha. */
	public enum Colour {
		WHITE(0xFFFFFF, 0.15f),
		BLUE(0x00D4FF, 0.15f),
		RED(0xFF7070, 0.2f),
		/** A box past a limit, of any purpose. */
		LIMIT(0xFF3333, 0.2f);

		private final int rgb;
		private final float sidesAlpha;

		Colour(int rgb, float sidesAlpha) {
			this.rgb = rgb;
			this.sidesAlpha = sidesAlpha;
		}

		public int rgb() {
			return rgb;
		}

		public float sidesAlpha() {
			return sidesAlpha;
		}
	}

	/** The box's colour for its purpose, or {@link Colour#LIMIT} while it breaks a limit. */
	public static Colour of(BoxPurpose purpose, boolean pastLimit) {
		if (pastLimit) {
			return Colour.LIMIT;
		}
		return switch (purpose) {
			case COPY -> Colour.WHITE;
			case MATERIAL_LIST -> Colour.BLUE;
			case DELETE -> Colour.RED;
		};
	}

	/** A horizontal square in world coordinates: from {@code minX}, {@code minZ} to {@code maxX}, {@code maxZ} at y. */
	public record Square(double minX, double y, double minZ, double maxX, double maxZ) {
	}

	/**
	 * The shaded square for the player aiming at {@code face} of the block at {@code block} before the box's first
	 * corner is set: the block's top face, just above it, when the face is the top, where the corner is the space above
	 * the block; none for the other five faces, where the corner is the block itself.
	 */
	public static Optional<Square> shade(BlockPos block, Direction face) {
		if (face != Direction.UP) {
			return Optional.empty();
		}
		return Optional.of(new Square(block.getX(), block.getY() + 1 + SHADE_LIFT, block.getZ(), block.getX() + 1,
			block.getZ() + 1));
	}
}
