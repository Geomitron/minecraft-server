package dev.buildingplanner.client;

import dev.buildingplanner.input.RepeatSpeed;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;

/**
 * Building Planner's widgets in Effortless Building's radial menu (hooks E11 and E12), placed from the menu's middle
 * as its own buttons are. The menu's left-hand buttons are 20 across with their middles 26 apart, the leftmost 157
 * left of the menu's middle, in two rows 13 above and 13 below it. Under them go a row of five buttons (middles 39
 * below), a row of three while a copy exists (65 below), and then the two speed sliders, which stay in place whether
 * or not those rows show. The lowest slider ends 115 below the middle, inside the 120 a screen at least 240 high has.
 */
public final class RadialMenuWidgets {
	/** The left edge of the menu's leftmost buttons, from its middle. */
	private static final int LEFT = -167;
	private static final int SLIDERS_TOP = 81;
	private static final int SLIDER_WIDTH = 156;
	private static final int SLIDER_HEIGHT = 16;
	private static final int SLIDER_GAP = 2;

	private RadialMenuWidgets() {
	}

	/** The widgets for a radial menu of that size, made afresh each time it opens. */
	public static List<AbstractWidget> create(int width, int height) {
		int x = width / 2 + LEFT;
		int top = height / 2 + SLIDERS_TOP;
		return List.of(
			new RepeatSlider(x, top, SLIDER_WIDTH, SLIDER_HEIGHT, RepeatSpeed.PLACING,
				"buildingplanner.ui.radial.placing_speed", BuildingPlannerClient.config()),
			new RepeatSlider(x, top + SLIDER_HEIGHT + SLIDER_GAP, SLIDER_WIDTH, SLIDER_HEIGHT, RepeatSpeed.ERASING,
				"buildingplanner.ui.radial.erasing_speed", BuildingPlannerClient.config()));
	}
}
