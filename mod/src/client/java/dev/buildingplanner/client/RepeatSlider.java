package dev.buildingplanner.client;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.ClientConfig;
import dev.buildingplanner.input.RepeatSpeed;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/**
 * A speed slider in Effortless Building's radial menu: how fast a held button keeps placing or erasing, from 1 block
 * a second at the left to 20 at the right, in steps of one tick between repeats. Each step it reaches is kept in the
 * player's config at once, and hold to repeat reads it from there. It draws as the game's sliders do, with its text at
 * {@link #TEXT_SCALE}, so the slider fits beside the menu's ring.
 */
final class RepeatSlider extends AbstractSliderButton {
	/** The text's size beside the game's own; at 0.7 the widest label, "Break speed: 3.33 blocks/s", is 96 across. */
	private static final float TEXT_SCALE = 0.7f;
	private static final Identifier TRACK = Identifier.withDefaultNamespace("widget/slider");
	private static final Identifier TRACK_FOCUSED = Identifier.withDefaultNamespace("widget/slider_highlighted");
	private static final Identifier HANDLE = Identifier.withDefaultNamespace("widget/slider_handle");
	private static final Identifier HANDLE_HOVERED = Identifier.withDefaultNamespace("widget/slider_handle_highlighted");
	private static final int HANDLE_WIDTH = 8;

	private final RepeatSpeed speed;
	private final String labelKey;
	private final ClientConfig config;
	private int ticks;

	RepeatSlider(int x, int y, int width, int height, RepeatSpeed speed, String labelKey, ClientConfig config) {
		super(x, y, width, height, Component.empty(), RepeatSpeed.position(speed.ticks(config)));
		this.speed = speed;
		this.labelKey = labelKey;
		this.config = config;
		this.ticks = speed.ticks(config);
		updateMessage();
	}

	@Override
	protected void updateMessage() {
		setMessage(Component.translatable(labelKey, RepeatSpeed.blocksPerSecond(RepeatSpeed.ticksAt(value))));
	}

	@Override
	public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		int color = ARGB.white(alpha);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, isFocused() ? TRACK_FOCUSED : TRACK, getX(), getY(),
			getWidth(), getHeight(), color);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, isHoveredOrFocused() ? HANDLE_HOVERED : HANDLE,
			getX() + (int) (value * (getWidth() - HANDLE_WIDTH)), getY(), HANDLE_WIDTH, getHeight(), color);
		var font = Minecraft.getInstance().font;
		graphics.pose().pushMatrix();
		graphics.pose().translate(getX() + getWidth() / 2f, getY() + getHeight() / 2f);
		graphics.pose().scale(TEXT_SCALE);
		graphics.centeredText(font, getMessage(), 0, -font.lineHeight / 2, color);
		graphics.pose().popMatrix();
		handleCursor(graphics);
	}

	@Override
	protected void applyValue() {
		int now = RepeatSpeed.ticksAt(value);
		if (now == ticks) {
			return;
		}
		ticks = now;
		try {
			speed.set(config, ticks);
		} catch (IOException e) {
			BuildingPlanner.LOGGER.warn("Couldn't save the {} speed", speed, e);
		}
	}

	/** Letting go puts the handle on the step it chose, where it shows next time. */
	@Override
	public void onRelease(MouseButtonEvent event) {
		value = RepeatSpeed.position(ticks);
		super.onRelease(event);
	}
}
