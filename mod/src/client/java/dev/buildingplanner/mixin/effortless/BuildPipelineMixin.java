package dev.buildingplanner.mixin.effortless;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.buildingplanner.client.BuildingPlannerClient;
import nl.requios.effortlessbuilding.buildpipeline.BuildPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Hook E6: while the mix is on, any held item starts shapes, as Effortless Building's Randomizer does. */
@Mixin(value = BuildPipeline.class, remap = false)
abstract class BuildPipelineMixin {
	@ModifyExpressionValue(method = "isBuildTriggerItem", at = @At(value = "CONSTANT",
		args = "classValue=nl/requios/effortlessbuilding/item/RandomizerToolItem"))
	private static boolean buildingplanner$mixIsRandomizer(boolean isRandomizer) {
		return isRandomizer || BuildingPlannerClient.mixOn();
	}
}
