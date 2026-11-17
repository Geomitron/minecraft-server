package dev.buildingplanner.mixin.effortless;

import dev.buildingplanner.client.RadialMenuWidgets;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nl.requios.effortlessbuilding.screen.RadialMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks E11 and E12: Building Planner's widgets in Effortless Building's radial menu. Minecraft clears a screen's
 * widgets and calls {@code init} each time it opens, so E11 adds them there; as children of the screen they get the
 * clicks and drags the menu passes on to {@code Screen}. The menu draws itself without {@code Screen}'s own drawing, so
 * E12 draws them, after the menu's ring and buttons and before its texts, so a hovered mode's name, which can fall on
 * the sliders, shows over them. The menu has no widgets of its own.
 */
@Mixin(value = RadialMenu.class, remap = false)
abstract class RadialMenuMixin extends Screen {
	private RadialMenuMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void buildingplanner$addWidgets(CallbackInfo ci) {
		RadialMenuWidgets.create(width, height).forEach(this::addRenderableWidget);
	}

	@Inject(method = "extractRenderState", at = @At(value = "INVOKE",
		target = "Lnl/requios/effortlessbuilding/screen/RadialMenu;drawTexts"))
	private void buildingplanner$drawWidgets(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks,
		CallbackInfo ci) {
		for (GuiEventListener child : children()) {
			if (child instanceof Renderable widget) {
				widget.extractRenderState(graphics, mouseX, mouseY, partialTicks);
			}
		}
	}
}
