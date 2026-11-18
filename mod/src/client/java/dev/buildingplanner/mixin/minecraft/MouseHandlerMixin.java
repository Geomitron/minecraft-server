package dev.buildingplanner.mixin.minecraft;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import dev.buildingplanner.client.BuildingPlannerClient;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hook M5: while the copy is at the aim, the scroll wheel turns it instead of changing the hotbar slot. The method
 * reaches the slot change only with no screen open and the player not a spectator; {@code wheel}, the method's only int
 * local there, is the notches scrolled, above 0 for up.
 */
@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
	@WrapWithCondition(method = "onScroll", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/player/Inventory;setSelectedSlot(I)V"))
	private boolean buildingplanner$turnCopy(Inventory inventory, int slot, @Local int wheel) {
		return BuildingPlannerClient.scroll(wheel);
	}
}
