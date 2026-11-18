package dev.buildingplanner.mixin.effortless;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.buildingplanner.client.BuildingPlannerClient;
import dev.buildingplanner.client.EffortlessShapes;
import nl.requios.effortlessbuilding.render.BlockPreviewRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hook E6: while the mix applies, the preview draws each position's block from the mix. Hook E22: no preview while a
 * mouse mode is on.
 */
@Mixin(value = BlockPreviewRenderer.class, remap = false)
abstract class BlockPreviewRendererMixin {
	@ModifyExpressionValue(method = "render", at = @At(value = "CONSTANT",
		args = "classValue=nl/requios/effortlessbuilding/item/RandomizerToolItem"))
	private static boolean buildingplanner$mixIsRandomizer(boolean isRandomizer) {
		return isRandomizer || BuildingPlannerClient.active() && EffortlessShapes.mixApplies();
	}

	/** Hook E22: the method draws the current mode's first block at the aim each frame, which the copy stands in for. */
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private static void buildingplanner$noPreviewInMouseMode(CallbackInfo ci) {
		if (BuildingPlannerClient.mouseModeOn()) {
			ci.cancel();
		}
	}
}
