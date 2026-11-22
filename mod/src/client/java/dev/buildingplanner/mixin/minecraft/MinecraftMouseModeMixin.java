package dev.buildingplanner.mixin.minecraft;

import dev.buildingplanner.client.BuildingPlannerClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks M1, M3 and M4: while a mouse mode is on, the game's attack and pick block keys (the left and middle clicks
 * unless the player moved them) work the mode instead; M2 ({@link MinecraftUseItemMixin}) takes the use key. {@code
 * handleKeybinds} calls each method with no screen open, so the mode's controls act only then. Freecam cancels these
 * three methods at their head while its camera is on and its "Allow interaction" off. This mixin's priority, 900, below
 * the default 1000 that Freecam's has, puts its hooks ahead of Freecam's at the head (a mixin of lower priority is
 * applied first, and its hooks at a head come first), so the box and the copy work from Freecam's camera.
 */
@Mixin(value = Minecraft.class, priority = 900)
abstract class MinecraftMouseModeMixin {
	/**
	 * Hook M1: the left click sets the box's next corner, pastes the copy or starts Delete by aim's hold to erase, and
	 * attacks and breaks nothing.
	 */
	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void buildingplanner$leftClick(CallbackInfoReturnable<Boolean> cir) {
		if (BuildingPlannerClient.leftClick()) {
			cir.setReturnValue(false);
		}
	}

	/** Hook M3: holding the left click breaks nothing, nor after the click that finished a box. */
	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void buildingplanner$holdLeftClick(boolean down, CallbackInfo ci) {
		if (BuildingPlannerClient.holdingAttackTaken()) {
			ci.cancel();
		}
	}

	/**
	 * Hook M4: the middle click mirrors the copy, does nothing while a box is drawn, or with the sprint key held flips the
	 * block in hand, and then picks no block.
	 */
	@Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
	private void buildingplanner$middleClick(CallbackInfo ci) {
		if (BuildingPlannerClient.middleClick()) {
			ci.cancel();
		}
	}
}
