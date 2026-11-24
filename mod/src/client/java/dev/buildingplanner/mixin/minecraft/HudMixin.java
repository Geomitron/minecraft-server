package dev.buildingplanner.mixin.minecraft;

import dev.buildingplanner.client.BuildingPlannerClient;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Hook M8: the game draws the held item's name, after the player switches to it, 59 above the screen's bottom in
 * survival mode and 45 in creative ({@code extractSelectedItemName}), where Building Planner's lines above the hotbar
 * show; while one shows, the name draws 2 above the top of its highest row instead.
 */
@Mixin(Hud.class)
abstract class HudMixin {
	@ModifyArg(method = "extractSelectedItemName", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;textWithBackdrop(Lnet/minecraft/client/gui/Font;"
			+ "Lnet/minecraft/network/chat/Component;IIII)V"), index = 3)
	private int buildingplanner$itemNameAboveLines(int y) {
		return BuildingPlannerClient.itemNameTop(y);
	}
}
