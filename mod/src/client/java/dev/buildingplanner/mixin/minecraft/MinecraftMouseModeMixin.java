package dev.buildingplanner.mixin.minecraft;

import dev.buildingplanner.client.BuildingPlannerClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks M1 to M4: while a mouse mode is on, the game's attack, use and pick block keys (the left, right and middle
 * clicks unless the player moved them) work the mode instead. {@code handleKeybinds} calls each method with no screen
 * open, so the mode's controls act only then.
 */
@Mixin(Minecraft.class)
abstract class MinecraftMouseModeMixin {
	/** Hook M1: the left click sets the box's next corner or pastes the copy, and attacks and breaks nothing. */
	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void buildingplanner$leftClick(CallbackInfoReturnable<Boolean> cir) {
		if (BuildingPlannerClient.leftClick()) {
			cir.setReturnValue(false);
		}
	}

	/** Hook M2: the right click ends the box or puts the copy away and uses nothing, nor does the use button held after it. */
	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void buildingplanner$rightClick(CallbackInfo ci) {
		if (BuildingPlannerClient.rightClick()) {
			ci.cancel();
		}
	}

	/** Hook M3: holding the left click breaks nothing, nor after the click that finished a box. */
	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void buildingplanner$holdLeftClick(boolean down, CallbackInfo ci) {
		if (BuildingPlannerClient.holdingAttackTaken()) {
			ci.cancel();
		}
	}

	/** Hook M4: the middle click mirrors the copy, or does nothing while a box is drawn, and picks no block. */
	@Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
	private void buildingplanner$middleClick(CallbackInfo ci) {
		if (BuildingPlannerClient.middleClick()) {
			ci.cancel();
		}
	}
}
