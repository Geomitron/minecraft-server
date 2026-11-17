package dev.buildingplanner.look;

import dev.buildingplanner.ClientConfig;
import java.io.IOException;

/**
 * Whether ghost blocks show for this player: the show or hide switch, kept in the player's {@link ClientConfig} across
 * game runs. They show until the player hides them.
 */
public final class GhostsShown {
	static final String KEY = "ghost-blocks-shown";

	private GhostsShown() {
	}

	/** Whether ghost blocks show: anything but {@code false} in the config, a missing value included, means they do. */
	public static boolean get(ClientConfig config) {
		return !Boolean.FALSE.toString().equals(config.get(KEY));
	}

	/** Keeps the switch's state in the config. */
	public static void set(ClientConfig config, boolean shown) throws IOException {
		config.set(KEY, Boolean.toString(shown));
	}
}
