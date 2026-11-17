package dev.buildingplanner.mixin.effortless;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.buildingplanner.client.EffortlessShapes;
import nl.requios.effortlessbuilding.render.BlockPreviewRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Hook E6: while the mix applies, the preview draws each position's block from the mix. */
@Mixin(value = BlockPreviewRenderer.class, remap = false)
abstract class BlockPreviewRendererMixin {
	@ModifyExpressionValue(method = "render", at = @At(value = "CONSTANT",
		args = "classValue=nl/requios/effortlessbuilding/item/RandomizerToolItem"))
	private static boolean buildingplanner$mixIsRandomizer(boolean isRandomizer) {
		return isRandomizer || EffortlessShapes.mixApplies();
	}
}
