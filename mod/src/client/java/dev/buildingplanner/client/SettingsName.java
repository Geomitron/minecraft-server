package dev.buildingplanner.client;

/**
 * Hook E39: Effortless Building's settings screen and the radial menu's button that opens it are named "Settings"
 * (see "Settings"), Building Planner's text in place of Effortless Building's two keys, both "Visual Settings".
 */
public final class SettingsName {
	/** Building Planner's name for the screen and the button. */
	public static final String KEY = "buildingplanner.ui.settings";
	private static final String SCREEN = "effortlessbuilding.screen.client_config";
	private static final String BUTTON = "effortlessbuilding.action.open_client_config";

	private SettingsName() {
	}

	/**
	 * The key a text of Effortless Building's settings screen or radial menu reads: Building Planner's for the screen's
	 * title and the button's name, and otherwise {@code key}.
	 */
	public static String key(String key) {
		return key.equals(SCREEN) || key.equals(BUTTON) ? KEY : key;
	}
}
