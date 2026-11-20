package dev.buildingplanner.input;

/**
 * Which entity the player aims from when editing ghost blocks (see "Editing from Freecam"): the game's camera entity,
 * which is Freecam's camera while that is on and the player otherwise, except while Freecam's "Player control" is on,
 * which moves the player and keeps the camera still.
 */
public final class AimSource {
	private AimSource() {
	}

	/**
	 * The entity to aim from: the camera entity, or the player while {@code playerControl} is on or the game has no
	 * camera entity.
	 */
	public static <E> E of(E camera, E player, boolean playerControl) {
		return camera == null || playerControl ? player : camera;
	}
}
