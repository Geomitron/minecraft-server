package dev.buildingplanner.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.look.StrokeIcon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import nl.requios.effortlessbuilding.screen.RadialMenu;

/**
 * A button of Building Planner's in Effortless Building's radial menu, drawn as the menu draws its own: a 20 by 20
 * see-through square, lighter under the pointer, with a 16 by 16 icon in it, and on hover the button's name, with its
 * key under it when it has one. A click plays the menu's own quiet click.
 */
final class RadialButton extends AbstractWidget {
	static final int SIZE = 20;
	private static final int ICON = 16;
	// Effortless Building's side button colours (RadialMenu's sideButtonColor and highlightColor) as ARGB.
	private static final int COLOR = 0x7F7F7F7F;
	private static final int HOVER_COLOR = 0x9999CCFF;

	/** A button's 16 by 16 icon. */
	interface Icon {
		void draw(GuiGraphicsExtractor graphics, int x, int y);
	}

	private final Supplier<Icon> icon;
	private final Supplier<String> key;
	private final Runnable action;

	/**
	 * A button at {@code x}, {@code y} (its top left, in GUI pixels) named {@code name}; {@code key} gives its key's name,
	 * or null without one, and {@code icon} its icon, both asked each frame.
	 */
	RadialButton(int x, int y, Component name, Supplier<String> key, Supplier<Icon> icon, Runnable action) {
		super(x, y, SIZE, SIZE, name);
		this.icon = icon;
		this.key = key;
		this.action = action;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		graphics.fill(getX(), getY(), getX() + SIZE, getY() + SIZE, isHovered() ? HOVER_COLOR : COLOR);
		icon.get().draw(graphics, getX() + (SIZE - ICON) / 2, getY() + (SIZE - ICON) / 2);
		if (isHovered()) {
			List<Component> lines = new ArrayList<>();
			lines.add(getMessage().copy().withStyle(ChatFormatting.AQUA));
			String keyName = key.get();
			if (keyName != null) {
				lines.add(Component.literal("[" + keyName + "]").withStyle(ChatFormatting.GRAY));
			}
			graphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font, lines, mouseX, mouseY);
		}
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		action.run();
	}

	@Override
	public void playDownSound(SoundManager soundManager) {
		RadialMenu.playRadialMenuSound();
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}

	/** A vanilla item drawn as the icon, as the mix panel draws its blocks. */
	static Icon item(ItemStack stack) {
		return (graphics, x, y) -> graphics.item(stack, x, y);
	}

	/**
	 * One of {@link LucideIcons}, drawn white, as a texture made for the GUI scale it's drawn at, so each of the icon's
	 * pixels is one of the screen's.
	 */
	static Icon lucide(String name, List<String> elements) {
		Map<Integer, Identifier> textures = new HashMap<>();
		return (graphics, x, y) -> {
			int scale = Math.max(1, (int) Minecraft.getInstance().getWindow().getGuiScale());
			Identifier id = textures.computeIfAbsent(scale, s -> texture(name, elements, s * ICON));
			int size = scale * ICON;
			graphics.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0, 0, ICON, ICON, size, size, size, size);
		};
	}

	private static Identifier texture(String name, List<String> elements, int size) {
		int[] coverage = StrokeIcon.rasterize(elements, size);
		NativeImage image = new NativeImage(size, size, false);
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				image.setPixel(x, y, coverage[y * size + x] << 24 | 0xFFFFFF);
			}
		}
		Identifier id = Identifier.fromNamespaceAndPath(BuildingPlanner.MOD_ID, "radial_icon/" + name + "_" + size);
		Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, image));
		return id;
	}
}
