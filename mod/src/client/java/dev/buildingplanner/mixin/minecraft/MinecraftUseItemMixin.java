package dev.buildingplanner.mixin.minecraft;

import dev.buildingplanner.client.BuildingPlannerClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hook M2: while a mouse mode is on, the game's use key (the right click unless the player moved it) works the mode:
 * it pastes the copy at the aim, and otherwise leaves the mode.
 * Freecam doesn't cancel {@code startUseItem}, so this hook keeps the default priority, unlike M1, M3 and M4
 * ({@link MinecraftMouseModeMixin}).
 */
@Mixin(Minecraft.class)
abstract class MinecraftUseItemMixin {
	/** Hook M2: the right click pastes the copy or ends the box and uses nothing, nor does the use button held after it. */
	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void buildingplanner$rightClick(CallbackInfo ci) {
		if (BuildingPlannerClient.rightClick()) {
			ci.cancel();
		}
	}
}
