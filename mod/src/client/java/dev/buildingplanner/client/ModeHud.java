package dev.buildingplanner.client;

import dev.buildingplanner.input.BoxCorners;
import dev.buildingplanner.input.MouseMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

/**
 * The line of a mouse mode's controls, centred where Effortless Building draws its own line during a shape
 * ({@code RenderHandler.renderSubtitle}: 58 above the screen's bottom, with a shadow), in its colours: white, with each
 * action in dark aqua. A box's line ends with its size, and a box past a limit has a red line above saying which. A
 * line too wide for the screen wraps onto lines above it. It shows while a mode is on, with no screen open and the HUD
 * shown (not hidden with F1).
 */
final class ModeHud implements HudElement {
	private static final int BOTTOM = 58;
	private static final int MARGIN = 4;
	private static final String COPY = "buildingplanner.ui.mode.copy";
	private static final String BOX = "buildingplanner.ui.mode.box";

	private final Supplier<MouseMode> mode;
	private final Supplier<BoxDraw.Shown> box;

	/** {@code mode} is the mouse mode now, or null, and {@code box} the box drawn now, or null. */
	ModeHud(Supplier<MouseMode> mode, Supplier<BoxDraw.Shown> box) {
		this.mode = mode;
		this.box = box;
	}

	/** The lines to show now, from the top, or none. */
	List<Component> lines() {
		Minecraft mc = Minecraft.getInstance();
		MouseMode now = mode.get();
		if (now == null || mc.gui.screen() != null || mc.gui.hud.isHidden()) {
			return List.of();
		}
		return switch (now) {
			case BOX -> boxLines(box.get());
			case COPY_AT_AIM -> List.of(Component.translatable(COPY, action(COPY + ".paste"), action(COPY + ".turn"),
				action(COPY + ".mirror"), action(COPY + ".put_away")).withStyle(ChatFormatting.WHITE));
		};
	}

	private static List<Component> boxLines(BoxDraw.Shown shown) {
		if (shown == null) {
			return List.of();
		}
		String purpose = switch (shown.purpose()) {
			case COPY -> BOX + ".copy";
			case MATERIAL_LIST -> BOX + ".material_list";
		};
		String next = switch (shown.corners()) {
			case 0 -> BOX + ".first";
			case 1 -> BOX + ".second";
			default -> BOX + ".height";
		};
		MutableComponent controls = Component.translatable(BOX, Component.translatable(purpose), action(next),
			action(BOX + ".cancel")).withStyle(ChatFormatting.WHITE);
		if (shown.corners() > 0 && shown.min() != null) {
			int[] size = BoxCorners.size(shown.min(), shown.max());
			controls.append(" ").append(Component.translatable(BOX + ".size", size[0], size[1], size[2]));
		}
		List<Component> lines = new ArrayList<>(2);
		if (shown.limit() != null) {
			MutableComponent limit = switch (shown.limit()) {
				case SIDE -> Component.translatable(BOX + ".too_large", BoxCorners.MAX_SIDE);
				case NOT_LOADED -> Component.translatable(BOX + ".not_loaded");
			};
			lines.add(limit.withStyle(ChatFormatting.RED));
		}
		lines.add(controls);
		return lines;
	}

	/** The lines' text as the player reads them, separated by " | ", or {@code hidden}; for the in-game checks. */
	String text() {
		List<Component> lines = lines();
		return lines.isEmpty() ? "hidden" : lines.stream().map(Component::getString).collect(Collectors.joining(" | "));
	}

	private static Component action(String key) {
		return Component.translatable(key).withStyle(ChatFormatting.DARK_AQUA);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		List<Component> lines = lines();
		if (lines.isEmpty()) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		List<FormattedCharSequence> wrapped = new ArrayList<>();
		for (Component line : lines) {
			wrapped.addAll(font.split(line, graphics.guiWidth() - 2 * MARGIN));
		}
		int y = graphics.guiHeight() - BOTTOM - (wrapped.size() - 1) * font.lineHeight;
		for (FormattedCharSequence text : wrapped) {
			graphics.text(font, text, (graphics.guiWidth() - font.width(text)) / 2, y, -1, true);
			y += font.lineHeight;
		}
	}
}
