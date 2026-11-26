package dev.buildingplanner.input;

import net.minecraft.core.Direction;

/**
 * The six "Nudge" keys, which move the locked copy, or the build modes' locked positions, one block, as Satisfactory's nudge
 * keys move a locked hologram: forward away from the view, back toward it, left and right to the view's sides, along
 * the compass direction nearest the way the view faces, and up and down.
 */
public enum CopyNudge {
	FORWARD("forward"),
	BACK("back"),
	LEFT("left"),
	RIGHT("right"),
	UP("up"),
	DOWN("down");

	private final String name;

	CopyNudge(String name) {
		this.name = name;
	}

	/** The key's translation key, {@code key.buildingplanner.nudge_<name>}. */
	public String translationKey() {
		return "key.buildingplanner.nudge_" + name;
	}

	/**
	 * The way this key moves the copy for a view turned {@code yRot} degrees (Minecraft's yaw: 0 faces south, 90 west),
	 * whatever its pitch: the compass direction nearest the view's facing, its opposite or its sides; up or down.
	 */
	public Direction direction(float yRot) {
		Direction facing = Direction.fromYRot(yRot);
		return switch (this) {
			case FORWARD -> facing;
			case BACK -> facing.getOpposite();
			case LEFT -> facing.getCounterClockWise();
			case RIGHT -> facing.getClockWise();
			case UP -> Direction.UP;
			case DOWN -> Direction.DOWN;
		};
	}
}
