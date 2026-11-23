package dev.buildingplanner.client;

import dev.buildingplanner.look.ControlsLine;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/**
 * Effortless Building's own line during a shape ("Left-click to cancel, Right-click to place"), drawn in Building
 * Planner's rectangles (hook E31, {@link ControlsLine#effortless}).
 */
public final class EffortlessLine {
	private EffortlessLine() {
	}

	/**
	 * Draws Effortless Building's {@code text} as rectangles, centred 58 above the screen's bottom, where Effortless
	 * Building draws it; false, drawing nothing, for a text of another form, which the caller then draws as it is.
	 */
	public static boolean draw(GuiGraphicsExtractor graphics, Font font, Component text) {
		List<ControlsLine.Part> parts = new ArrayList<>();
		text.visit((style, string) -> {
			parts.add(new ControlsLine.Part(string, style.getColor() == null ? null : style.getColor().serialize()));
			return Optional.empty();
		}, Style.EMPTY);
		return ModeHud.drawEffortless(graphics, font, parts);
	}
}
