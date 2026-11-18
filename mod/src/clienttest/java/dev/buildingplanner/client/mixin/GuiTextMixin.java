package dev.buildingplanner.client.mixin;

import dev.buildingplanner.client.ScreenProbe;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tells the driver each string drawn as plain text, with where, so a check can read what a screen shows. */
@Mixin(GuiGraphicsExtractor.class)
abstract class GuiTextMixin {
	@Inject(method = "text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V", at = @At("HEAD"))
	private void buildingplanner_clienttest$record(Font font, String text, int x, int y, int color, boolean shadow,
		CallbackInfo ci) {
		ScreenProbe.drawn(font, text, x, y);
	}
}
