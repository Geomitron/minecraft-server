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
 * see-through square, lighter under the pointer and blue while selected, with a 16 by 16 icon centred in it, and on
 * hover the button's name, with its key under it when it has one. Like the menu's own buttons, it sits at a fixed
 * place from the menu's middle, nearer it and smaller while the menu opens ({@link #place}).
 *
 * <p>It acts as the menu's own buttons do (Effortless Building 4.3's {@code RadialMenu}): on a click of any mouse
 * button over it, which also keeps the menu from acting again when it closes ({@code performedActionUsingMouse}), and
 * when the menu closes, by letting go of its key (or by Escape), with the pointer over it and no click having acted
 * since the menu opened ({@link RadialMenuWidgets#menuClosing}). Each time it plays the menu's own quiet click.
 */
final class RadialButton extends AbstractWidget implements RadialMenuWidgets.Placed {
	static final int SIZE = 20;
	private static final int ICON = 16;
	// Effortless Building's side button colours (RadialMenu's sideButtonColor, highlightColor, selectedColor and
	// highlightSelectedColor) as ARGB.
	private static final int COLOR = 0x7F7F7F7F;
	private static final int HOVER_COLOR = 0x9999CCFF;
	private static final int SELECTED_COLOR = 0x7F007FFF;
	private static final int HOVER_SELECTED_COLOR = 0xB233B2FF;

	private final int offsetX;
	private final int offsetY;
	private final Supplier<Identifier> icon;
	private final Supplier<String> key;
	private final BooleanSupplier selected;
	private final Runnable action;
	/** Where the button's middle is now, in GUI pixels, and its size as a share of its full size. */
	private double middleX;
	private double middleY;
	private double factor = 1;

	/**
	 * A button with its middle {@code offsetX}, {@code offsetY} from the menu's middle (in GUI pixels) named
	 * {@code name}; {@code key} gives its key's name, or null without one, {@code icon} its icon's texture
	 * ({@link #icon}), and {@code selected} whether it shows selected, each asked each frame.
	 */
	RadialButton(int offsetX, int offsetY, Component name, Supplier<String> key, Supplier<Identifier> icon,
		BooleanSupplier selected, Runnable action) {
		super(0, 0, SIZE, SIZE, name);
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.icon = icon;
		this.key = key;
		this.selected = selected;
		this.action = action;
	}

	/**
	 * The icon in {@code assets/buildingplanner/textures/gui/radial/<name>.png}, a 16 by 16 image drawn pixel for pixel
	 * at the GUI's scale, with the nearest-pixel filtering the game gives a texture without its own settings.
	 */
	static Identifier icon(String name) {
		return Identifier.fromNamespaceAndPath(BuildingPlanner.MOD_ID, "textures/gui/radial/" + name + ".png");
	}

	/**
	 * Puts the button's middle at its place from the menu's middle times {@code factor}, and makes it {@code factor} of
	 * its size, as the menu places and sizes its own buttons' backgrounds. The widget's box, which takes the pointer, is
	 * the drawn square cut to whole GUI pixels.
	 */
	@Override
	public void place(double menuX, double menuY, double factor) {
		this.factor = factor;
		middleX = menuX + offsetX * factor;
		middleY = menuY + offsetY * factor;
		double half = SIZE / 2.0 * factor;
		int left = (int) Math.floor(middleX - half);
		int top = (int) Math.floor(middleY - half);
		setX(left);
		setY(top);
		setWidth((int) Math.floor(middleX + half) - left);
		setHeight((int) Math.floor(middleY + half) - top);
	}

	/** The button's middle now, in GUI pixels, and its size as a share of its full size. */
	double middleX() {
		return middleX;
	}

	double middleY() {
		return middleY;
	}

	double factor() {
		return factor;
	}

	/** Whether the button shows selected: its mouse mode is on. */
	boolean selected() {
		return selected.getAsBoolean();
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		boolean hovered = isHovered();
		int color = selected() ? hovered ? HOVER_SELECTED_COLOR : SELECTED_COLOR : hovered ? HOVER_COLOR : COLOR;
		double half = SIZE / 2.0 * factor;
		graphics.pose().pushMatrix();
		graphics.pose().translate((float) (middleX - half), (float) (middleY - half));
		graphics.pose().scale((float) factor);
		graphics.fill(0, 0, SIZE, SIZE, color);
		graphics.pose().popMatrix();
		// As the menu draws its own icons (drawIcons): at full size, cut to whole GUI pixels from the middle.
		graphics.blit(RenderPipelines.GUI_TEXTURED, icon.get(), (int) (middleX - ICON / 2.0),
			(int) (middleY - ICON / 2.0), 0, 0, ICON, ICON, ICON, ICON);
		if (hovered) {
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
