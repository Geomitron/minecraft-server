package dev.buildingplanner.client.mixin;

import dev.buildingplanner.client.ScreenProbe;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells the driver each string drawn as plain text, and each component drawn with a shadow, with where, and each
 * tooltip of components set for the next frame (as the radial menu and its buttons set their hover texts), so a check
 * can read what a screen and the HUD show.
 */
@Mixin(GuiGraphicsExtractor.class)
abstract class GuiTextMixin {
	@Inject(method = "text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V", at = @At("HEAD"))
	private void buildingplanner_clienttest$record(Font font, String text, int x, int y, int color, boolean shadow,
		CallbackInfo ci) {
		ScreenProbe.drawn(font, text, x, y);
	}

	@Inject(method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
		at = @At("HEAD"))
	private void buildingplanner_clienttest$recordComponent(Font font, Component text, int x, int y, int color,
		boolean shadow, CallbackInfo ci) {
		ScreenProbe.drawn(font, text, x, y);
	}

	@Inject(method = "setComponentTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V",
		at = @At("HEAD"))
	private void buildingplanner_clienttest$recordTooltip(Font font, List<Component> lines, int x, int y,
		CallbackInfo ci) {
		ScreenProbe.tooltip(lines);
	}
}
