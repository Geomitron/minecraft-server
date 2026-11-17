package dev.buildingplanner.client;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.ClientConfig;
import dev.buildingplanner.input.RepeatSpeed;
import java.io.IOException;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * A speed slider in Effortless Building's radial menu: how fast a held button keeps placing or erasing, from 1 block
 * a second at the left to 20 at the right, in steps of one tick between repeats. Each step it reaches is kept in the
 * player's config at once, and hold to repeat reads it from there.
 */
final class RepeatSlider extends AbstractSliderButton {
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
