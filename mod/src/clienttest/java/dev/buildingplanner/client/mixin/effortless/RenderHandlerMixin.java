package dev.buildingplanner.client.mixin.effortless;

import dev.buildingplanner.client.StackProbe;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import nl.requios.effortlessbuilding.render.RenderHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells the driver which item icons Effortless Building draws beside the crosshair during a shape, for its
 * {@code stacks}: each stack with its count, and whether it's drawn as missing, in red.
 */
@Mixin(value = RenderHandler.class, remap = false)
abstract class RenderHandlerMixin {
	@Inject(method = "drawStacks", at = @At("HEAD"))
	private static void buildingplanner_clienttest$beforeStacks(GuiGraphicsExtractor gui, CallbackInfo ci) {
		StackProbe.beforeFrame();
	}

	@Inject(method = "drawItemStack(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/item/ItemStack;IIZ)V",
		at = @At("HEAD"))
	private static void buildingplanner_clienttest$drawing(GuiGraphicsExtractor gui, ItemStack stack, int x, int y,
		boolean missing, CallbackInfo ci) {
		StackProbe.drawing(stack, missing);
	}
}
