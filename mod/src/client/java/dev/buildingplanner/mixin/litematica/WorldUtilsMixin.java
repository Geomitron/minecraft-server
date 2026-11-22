package dev.buildingplanner.mixin.litematica;

import dev.buildingplanner.client.BuildingPlannerClient;
import fi.dy.masa.litematica.util.WorldUtils;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hook L11: while a mouse mode is on, Litematica picks no block from its placements, so its pick block hotkey (the
 * middle click unless the player changed it) leaves the click to the game's pick block key, which mirrors the copy or
 * does nothing (hook M4). The copy sits under the crosshair, so the click would otherwise pick one of its blocks. The
 * same holds while the middle click flips the block in hand (Minecraft's sprint key held), so the click on a ghost
 * block flips the block rather than picking the ghost block.
 */
@Mixin(value = WorldUtils.class, remap = false)
abstract class WorldUtilsMixin {
	@Inject(method = "doSchematicWorldPickBlock", at = @At("HEAD"), cancellable = true)
	private static void buildingplanner$noPickInMouseMode(boolean closest, Minecraft mc,
		CallbackInfoReturnable<Boolean> cir) {
		if (BuildingPlannerClient.mouseModeOn() || BuildingPlannerClient.flipTakesMiddle()) {
			cir.setReturnValue(false);
		}
	}
}
