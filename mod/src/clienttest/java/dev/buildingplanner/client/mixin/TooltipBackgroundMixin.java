package dev.buildingplanner.client.mixin;

import dev.buildingplanner.client.ScreenProbe;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tells the driver where each tooltip's background is drawn, for outlining a tooltip in a screenshot. */
@Mixin(TooltipRenderUtil.class)
abstract class TooltipBackgroundMixin {
	@Inject(method = "extractTooltipBackground", at = @At("HEAD"))
	private static void buildingplanner_clienttest$record(GuiGraphicsExtractor graphics, int x, int y, int width,
		int height, Identifier style, CallbackInfo ci) {
		ScreenProbe.tooltipBackground(x, y, width, height);
	}
}
