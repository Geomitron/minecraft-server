package dev.buildingplanner.mixin.effortless;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.buildingplanner.client.BuildingPlannerClient;
import dev.buildingplanner.client.ClickRule;
import net.minecraft.world.item.ItemStack;
import nl.requios.effortlessbuilding.buildpipeline.BuildPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hook E13: on a server, where Building Planner decides what Effortless Building's shapes do, an item starts shapes
 * only if it places a block ({@link ClickRule#placeable}). Effortless Building's own rule also takes buckets
 * and tools, and leaves out doors, beds and tall plants. While a box is awaited for a copy, any item, or none, starts
 * the cube.
 */
@Mixin(value = BuildPipeline.class, remap = false)
abstract class BuildPipelineMixin {
	@ModifyReturnValue(method = "isBuildTriggerItem", at = @At("RETURN"))
	private static boolean buildingplanner$onlyPlaceables(boolean trigger, @Local(argsOnly = true) ItemStack stack) {
		return BuildingPlannerClient.ownsShapes() ? ClickRule.boxAwaited() || ClickRule.placeable(stack) : trigger;
	}
}
