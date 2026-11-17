package dev.buildingplanner.client;

import dev.buildingplanner.input.RepeatSpeed;
import dev.buildingplanner.look.SmallText;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

/**
 * Building Planner's widgets in Effortless Building's radial menu (hooks E11 and E12), placed from the menu's middle
 * as its own buttons are. The menu's left-hand buttons (the wrench, undo and redo, and the row under them) are 20
 * across with their middles 26 apart, the leftmost 157 left of the menu's middle, in two rows 13 above and 13 below
 * it, so they span 167 to 95 left of the middle and end 23 below it. The two speed sliders sit just under them, 6 below
 * the lower row as that row is below the upper, and end above a mode's description, which the menu draws 100 below
 * the middle. Their labels draw on one line at {@link SmallText}'s size, full size at GUI scales 1 to 3, where the
 * longest label, "Break speed: 3.33 blocks/s", is 137 across: more than the 102 between the buttons' left edge and the
 * menu's ring (65 from the middle at most). So the sliders end 65 left of the middle, outside the ring, and are as wide
 * as the longest label either shows needs at the current size, plus {@link #PAD} on each side, reaching further left
 * than the buttons. Each is {@link #HEIGHT} high, and they end 55 below the middle.
 */
public final class RadialMenuWidgets {
	/** Where the sliders end, from the middle: just outside the menu's ring. */
	private static final int RIGHT = -65;
	private static final int SLIDERS_TOP = 29;
	private static final int PAD = 4;
	private static final int HEIGHT = 12;
	private static final int SLIDER_GAP = 2;
	private static final String BUILD = "buildingplanner.ui.radial.build_speed";
	private static final String BREAK = "buildingplanner.ui.radial.break_speed";

	private RadialMenuWidgets() {
	}

	/** The widgets for a radial menu of that size, made afresh each time it opens. */
	public static List<AbstractWidget> create(int width, int height) {
		int sliderWidth = widestLabel() + 2 * PAD;
		int x = width / 2 + RIGHT - sliderWidth;
		int top = height / 2 + SLIDERS_TOP;
		return List.of(
			new RepeatSlider(x, top, sliderWidth, HEIGHT, RepeatSpeed.PLACING, BUILD, BuildingPlannerClient.config()),
			new RepeatSlider(x, top + HEIGHT + SLIDER_GAP, sliderWidth, HEIGHT, RepeatSpeed.ERASING, BREAK,
				BuildingPlannerClient.config()));
	}

	/** The width the longest label either slider shows takes at the current size, in GUI pixels. */
	private static int widestLabel() {
		Font font = Minecraft.getInstance().font;
		int widest = 0;
		for (int ticks = RepeatSpeed.FASTEST; ticks <= RepeatSpeed.SLOWEST; ticks++) {
			for (String key : List.of(BUILD, BREAK)) {
				widest = Math.max(widest,
					font.width(Component.translatable(key, RepeatSpeed.blocksPerSecond(ticks))));
			}
		}
		return (int) Math.ceil(widest * SmallText.scale(Minecraft.getInstance().getWindow().getGuiScale()));
	}
}
