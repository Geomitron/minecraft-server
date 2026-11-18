package dev.buildingplanner.client;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.ClientConfig;
import dev.buildingplanner.input.RepeatSpeed;
import dev.buildingplanner.look.SmallText;
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
 * {@link SmallText}'s size. Its right end and its top keep their distance from the menu's middle times the menu's
 * opening factor ({@link RadialMenuWidgets#factor}), and its size stays.
 */
final class RepeatSlider extends AbstractSliderButton implements RadialMenuWidgets.Placed {
	private static final Identifier TRACK = Identifier.withDefaultNamespace("widget/slider");
	private static final Identifier TRACK_FOCUSED = Identifier.withDefaultNamespace("widget/slider_highlighted");
	private static final Identifier HANDLE = Identifier.withDefaultNamespace("widget/slider_handle");
	private static final Identifier HANDLE_HOVERED = Identifier.withDefaultNamespace("widget/slider_handle_highlighted");
	private static final int HANDLE_WIDTH = 8;

	private final int right;
	private final int top;
	private final RepeatSpeed speed;
	private final String labelKey;
	private final ClientConfig config;
	private int ticks;

	/** A slider whose right end is {@code right} and top {@code top} from the menu's middle, in GUI pixels. */
	RepeatSlider(int right, int top, int width, int height, RepeatSpeed speed, String labelKey, ClientConfig config) {
		super(0, 0, width, height, Component.empty(), RepeatSpeed.position(speed.ticks(config)));
		this.right = right;
		this.top = top;
		this.speed = speed;
		this.labelKey = labelKey;
		this.config = config;
		this.ticks = speed.ticks(config);
		updateMessage();
	}

	/** Cut to whole GUI pixels as the menu cuts its texts' places. */
	@Override
	public void place(double menuX, double menuY, double factor) {
		setX((int) (menuX + right * factor) - getWidth());
		setY((int) (menuY + top * factor));
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
		Minecraft mc = Minecraft.getInstance();
		graphics.pose().pushMatrix();
		graphics.pose().translate(getX() + getWidth() / 2f, getY() + getHeight() / 2f);
		graphics.pose().scale(SmallText.scale(mc.getWindow().getGuiScale()));
		graphics.centeredText(mc.font, getMessage(), 0, -mc.font.lineHeight / 2, color);
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
