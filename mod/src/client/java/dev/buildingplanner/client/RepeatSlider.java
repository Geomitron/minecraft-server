package dev.buildingplanner.client;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.ClientConfig;
import dev.buildingplanner.input.RepeatSpeed;
import java.io.IOException;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/**
 * A speed slider in Effortless Building's radial menu: how fast a held button keeps placing or erasing, from 1 block
 * a second at the left to 20 at the right, in steps of one tick between repeats. Each step it reaches is kept in the
 * player's config at once, and hold to repeat reads it from there. It sits in a section of a mode's options, as
 * Effortless Building 4.3's {@code RadialMenu} places them, under its heading ("Build speed" or "Break speed"), and
 * shows only its speed ("5 blocks/s"), at full size. It draws as the game's sliders do, and shows only while
 * {@code shown} holds, read each frame.
 *
 * <p>Effortless Building draws a section's heading with shadow in its {@code optionTextColor}, left-aligned at
 * {@code (int) (middleX + 105 * scale - 9)} and {@code (int) middleY - 37 + 39 * section}, and the section's buttons 20
 * across, their middles 26 apart from 105 right of the middle, in a row whose middle is 13 above the middle and 39
 * lower for each next section. The heading's left and the buttons sit at the menu's opening factor
 * ({@link RadialMenuWidgets#factor}) of their distance from the middle, and the heading's top stays. The slider's left
 * end is the row's first button's, 95 right of the middle, and it's 20 high, centred on the row; its size stays. Like
 * the menu's button backgrounds, it draws where that falls, between GUI pixels too, and its box, which takes the
 * pointer, is cut to whole GUI pixels; its text is cut to whole GUI pixels as the menu cuts its texts'.
 */
final class RepeatSlider extends AbstractSliderButton implements RadialMenuWidgets.Placed {
	private static final Identifier TRACK = Identifier.withDefaultNamespace("widget/slider");
	private static final Identifier TRACK_FOCUSED = Identifier.withDefaultNamespace("widget/slider_highlighted");
	private static final Identifier HANDLE = Identifier.withDefaultNamespace("widget/slider_handle");
	private static final Identifier HANDLE_HOVERED = Identifier.withDefaultNamespace("widget/slider_handle_highlighted");
	private static final int HANDLE_WIDTH = 8;
	/** Effortless Building's {@code optionTextColor}, as its radial menu passes it to {@code text}. */
	private static final int HEADING_COLOR = 0xeeeeeeff;
	static final int HEIGHT = 20;
	/** From the menu's middle: where a section's first button starts, and the first section's row's middle. */
	private static final int LEFT = 95;
	private static final int FIRST_ROW = -13;
	private static final int SECTION_GAP = 39;
	/** From the menu's middle: where a section's heading starts, and the first section's heading's top. */
	private static final int HEADING_LEFT = 105;
	private static final int HEADING_SHIFT = 9;
	private static final int FIRST_HEADING = -37;

	private final int section;
	private final RepeatSpeed speed;
	private final ClientConfig config;
	private final BooleanSupplier shown;
	private int ticks;
	private double left;
	private double top;
	private int headingX;
	private int headingY;

	/** A slider in the menu's {@code section} (0 for the first) of {@code width} that shows while {@code shown}. */
	RepeatSlider(int section, int width, RepeatSpeed speed, ClientConfig config, BooleanSupplier shown) {
		super(0, 0, width, HEIGHT, Component.empty(), RepeatSpeed.position(speed.ticks(config)));
		this.section = section;
		this.speed = speed;
		this.config = config;
		this.shown = shown;
		this.ticks = speed.ticks(config);
		updateMessage();
	}

	/** The heading's place is cut to whole GUI pixels as the menu cuts its texts' places. */
	@Override
	public void place(double menuX, double menuY, double factor) {
		visible = shown.getAsBoolean();
		left = menuX + LEFT * factor;
		top = menuY + (FIRST_ROW + SECTION_GAP * section) * factor - HEIGHT / 2.0;
		setX((int) Math.floor(left));
		setY((int) Math.floor(top));
		headingX = (int) (menuX + HEADING_LEFT * factor - HEADING_SHIFT);
		headingY = (int) menuY + FIRST_HEADING + SECTION_GAP * section;
	}

	/** The slider's heading, "Build speed" or "Break speed". */
	Component heading() {
		return Component.translatable(speed.heading());
	}

	/** Where the slider's heading starts, in GUI pixels, for the in-game checks: {x, y}. */
	int[] headingAt() {
		return new int[] {headingX, headingY};
	}

	/** Where the slider is drawn from, in GUI pixels, for the in-game checks: {left, top}. */
	double[] drawnAt() {
		return new double[] {left, top};
	}

	@Override
	protected void updateMessage() {
		setMessage(Component.translatable(RepeatSpeed.SPEED_TEXT,
			RepeatSpeed.blocksPerSecond(RepeatSpeed.ticksAt(value))));
	}

	@Override
	public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		Minecraft mc = Minecraft.getInstance();
		graphics.text(mc.font, I18n.get(speed.heading()), headingX, headingY, HEADING_COLOR, true);
		int color = ARGB.white(alpha);
		graphics.pose().pushMatrix();
		graphics.pose().translate((float) left, (float) top);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, isFocused() ? TRACK_FOCUSED : TRACK, 0, 0, getWidth(),
			getHeight(), color);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, isHoveredOrFocused() ? HANDLE_HOVERED : HANDLE,
			(int) (value * (getWidth() - HANDLE_WIDTH)), 0, HANDLE_WIDTH, getHeight(), color);
		graphics.pose().popMatrix();
		graphics.centeredText(mc.font, getMessage(), (int) (left + getWidth() / 2.0), (int) top + (getHeight() - 8) / 2,
			color);
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
