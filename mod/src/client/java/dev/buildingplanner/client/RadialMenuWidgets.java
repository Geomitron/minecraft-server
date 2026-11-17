package dev.buildingplanner.client;

import dev.buildingplanner.input.RepeatSpeed;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;

/**
 * Building Planner's widgets in Effortless Building's radial menu (hooks E11 and E12), placed from the menu's middle
 * as its own buttons are. The menu's left-hand buttons (the wrench, undo and redo, and the row under them) are 20
 * across with their middles 26 apart, the leftmost 157 left of the menu's middle, in two rows 13 above and 13 below
 * it, so they span 167 to 95 left of the middle and end 23 below it. The two speed sliders sit just under them, their
 * left edges in line with the buttons', 6 below the lower row as that row is below the upper. At 102 across they end
 * 65 left of the middle, outside the menu's ring (65 from the middle at most), and they end 55 below the middle, above
 * a mode's description, which the menu draws 100 below it.
 */
public final class RadialMenuWidgets {
	/** The left edge of the menu's leftmost buttons, from its middle. */
	private static final int LEFT = -167;
	private static final int SLIDERS_TOP = 29;
	private static final int SLIDER_WIDTH = 102;
	private static final int SLIDER_HEIGHT = 12;
	private static final int SLIDER_GAP = 2;

	private RadialMenuWidgets() {
	}

	/** The widgets for a radial menu of that size, made afresh each time it opens. */
	public static List<AbstractWidget> create(int width, int height) {
		int x = width / 2 + LEFT;
		int top = height / 2 + SLIDERS_TOP;
		return List.of(
			new RepeatSlider(x, top, SLIDER_WIDTH, SLIDER_HEIGHT, RepeatSpeed.PLACING,
				"buildingplanner.ui.radial.build_speed", BuildingPlannerClient.config()),
			new RepeatSlider(x, top + SLIDER_HEIGHT + SLIDER_GAP, SLIDER_WIDTH, SLIDER_HEIGHT, RepeatSpeed.ERASING,
				"buildingplanner.ui.radial.break_speed", BuildingPlannerClient.config()));
	}
}
