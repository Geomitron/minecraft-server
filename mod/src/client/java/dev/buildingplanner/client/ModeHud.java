package dev.buildingplanner.client;

import dev.buildingplanner.input.MouseMode;
import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The line of a mouse mode's controls, centred where Effortless Building draws its own line during a shape
 * ({@code RenderHandler.renderSubtitle}: 58 above the screen's bottom, with a shadow), in its colours: white, with each
 * action in dark aqua. A line too wide for the screen wraps onto lines above it. It shows while a mode is on, with no
 * screen open and the HUD shown (not hidden with F1).
 */
final class ModeHud implements HudElement {
	private static final int BOTTOM = 58;
	private static final int MARGIN = 4;
	private static final String COPY = "buildingplanner.ui.mode.copy";

	private final Supplier<MouseMode> mode;

	/** {@code mode} is the mouse mode now, or null. */
	ModeHud(Supplier<MouseMode> mode) {
		this.mode = mode;
	}

	/** The controls line now, or null when it doesn't show. */
	Component line() {
		Minecraft mc = Minecraft.getInstance();
		MouseMode now = mode.get();
		if (now == null || mc.gui.screen() != null || mc.gui.hud.isHidden()) {
			return null;
		}
		return switch (now) {
			case COPY_AT_AIM -> Component.translatable(COPY, action(COPY + ".paste"), action(COPY + ".turn"),
				action(COPY + ".mirror"), action(COPY + ".put_away")).withStyle(ChatFormatting.WHITE);
		};
	}

	/** The line's text as the player reads it, or {@code hidden}; for the in-game checks. */
	String text() {
		Component line = line();
		return line == null ? "hidden" : line.getString();
	}

	private static Component action(String key) {
		return Component.translatable(key).withStyle(ChatFormatting.DARK_AQUA);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Component line = line();
		if (line == null) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		List<FormattedCharSequence> lines = font.split(line, graphics.guiWidth() - 2 * MARGIN);
		int y = graphics.guiHeight() - BOTTOM - (lines.size() - 1) * font.lineHeight;
		for (FormattedCharSequence text : lines) {
			graphics.text(font, text, (graphics.guiWidth() - font.width(text)) / 2, y, -1, true);
			y += font.lineHeight;
		}
	}
}
