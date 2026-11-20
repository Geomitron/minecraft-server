package dev.buildingplanner.mixin.effortless;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.buildingplanner.client.Aim;
import dev.buildingplanner.client.BuildingPlannerClient;
import dev.buildingplanner.client.ClickRule;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.requios.effortlessbuilding.buildpipeline.BuildPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hook E13: on a server, where Building Planner decides what Effortless Building's shapes do, an item starts shapes
 * only if it places a block ({@link ClickRule#placeable}). Effortless Building's own rule also takes buckets
 * and tools, and leaves out doors, beds and tall plants. Hook E25: the modes' later clicks follow the camera's look
 * while Freecam's is on.
 */
@Mixin(value = BuildPipeline.class, remap = false)
abstract class BuildPipelineMixin {
	@ModifyReturnValue(method = "isBuildTriggerItem", at = @At("RETURN"))
	private static boolean buildingplanner$onlyPlaceables(boolean trigger, @Local(argsOnly = true) ItemStack stack) {
		return BuildingPlannerClient.ownsShapes() ? ClickRule.placeable(stack) : trigger;
	}

	/** Hook E25: the look the modes' later clicks follow, the camera's while Freecam's is on ({@link Aim}). */
	@WrapOperation(method = "getPlayerLookVec", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/player/Player;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
	private static Vec3 buildingplanner$lookAtAim(Player player, Operation<Vec3> original) {
		Entity aim = Aim.forEffortless(player);
		return aim == player ? original.call(player) : aim.getLookAngle();
	}
}
