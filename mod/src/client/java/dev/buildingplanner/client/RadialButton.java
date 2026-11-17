package dev.buildingplanner.client;

import dev.buildingplanner.BuildingPlanner;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import nl.requios.effortlessbuilding.screen.RadialMenu;

/**
 * A button of Building Planner's in Effortless Building's radial menu, drawn as the menu draws its own: a 20 by 20
 * see-through square, lighter under the pointer, with a 16 by 16 icon centred in it, and on hover the button's name,
 * with its key under it when it has one.
 *
 * <p>It acts as the menu's own buttons do (Effortless Building 4.3's {@code RadialMenu}): on a click of any mouse
 * button over it, which also keeps the menu from acting again when it closes ({@code performedActionUsingMouse}), and
 * when the menu closes, by letting go of its key (or by Escape), with the pointer over it and no click having acted
 * since the menu opened ({@link RadialMenuWidgets#menuClosing}). Each time it plays the menu's own quiet click. A button
 * shows only while its condition holds ({@link #refresh}); hidden, it neither draws nor acts.
 */
final class RadialButton extends AbstractWidget {
	static final int SIZE = 20;
	private static final int ICON = 16;
	// Effortless Building's side button colours (RadialMenu's sideButtonColor and highlightColor) as ARGB.
	private static final int COLOR = 0x7F7F7F7F;
	private static final int HOVER_COLOR = 0x9999CCFF;

	private final Supplier<Identifier> icon;
	private final Supplier<String> key;
	private final Runnable action;
	private final BooleanSupplier shown;

	/**
	 * A button at {@code x}, {@code y} (its top left, in GUI pixels) named {@code name}; {@code key} gives its key's name,
	 * or null without one, {@code icon} its icon's texture ({@link #icon}), and {@code shown} whether it shows, all asked
	 * each frame.
	 */
	RadialButton(int x, int y, Component name, Supplier<String> key, Supplier<Identifier> icon, Runnable action,
		BooleanSupplier shown) {
		super(x, y, SIZE, SIZE, name);
		this.icon = icon;
		this.key = key;
		this.action = action;
		this.shown = shown;
		refresh();
	}

	/** Shows or hides the button by its condition; the menu asks before each frame it draws. */
	void refresh() {
		visible = shown.getAsBoolean();
	}

	/**
	 * The icon in {@code assets/buildingplanner/textures/gui/radial/<name>.png}, a 16 by 16 image drawn pixel for pixel
	 * at the GUI's scale, with the nearest-pixel filtering the game gives a texture without its own settings.
	 */
	static Identifier icon(String name) {
		return Identifier.fromNamespaceAndPath(BuildingPlanner.MOD_ID, "textures/gui/radial/" + name + ".png");
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		graphics.fill(getX(), getY(), getX() + SIZE, getY() + SIZE, isHovered() ? HOVER_COLOR : COLOR);
		int offset = (SIZE - ICON) / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, icon.get(), getX() + offset, getY() + offset, 0, 0, ICON, ICON,
			ICON, ICON);
		if (isHovered()) {
			graphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font, hoverText(), mouseX, mouseY);
		}
	}

	/** The lines shown on hover: the button's name, and its key, when it has one. */
	List<Component> hoverText() {
		List<Component> lines = new ArrayList<>();
		lines.add(getMessage().copy().withStyle(ChatFormatting.AQUA));
		String keyName = key.get();
		if (keyName != null) {
			lines.add(Component.literal("[" + keyName + "]").withStyle(ChatFormatting.GRAY));
		}
		return lines;
	}

	/** Any mouse button, as the menu's own buttons take. */
	@Override
	protected boolean isValidClickButton(MouseButtonInfo button) {
		return true;
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		if (Minecraft.getInstance().gui.screen() instanceof RadialMenu menu) {
			menu.performedActionUsingMouse = true;
		}
		act();
	}

	/** The click sound plays in {@link #act}, as the menu plays it with each action. */
	@Override
	public void playDownSound(SoundManager soundManager) {
	}

	void act() {
		RadialMenu.playRadialMenuSound();
		action.run();
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
