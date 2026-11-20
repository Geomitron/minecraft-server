package dev.buildingplanner.mixin.effortless;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.buildingplanner.client.Aim;
import dev.buildingplanner.client.BuildingPlannerClient;
import dev.buildingplanner.client.EffortlessShapes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import nl.requios.effortlessbuilding.render.BlockPreviewRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hook E6: while the mix applies, the preview draws each position's block from the mix. Hook E22: no preview while a
 * mouse mode is on. Hook E24: the preview's first block aims, and faces, from Freecam's camera while it's on.
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

	private static final String PLACEMENT_STATE = "getPlacementState(Lnet/minecraft/world/item/BlockItem;"
		+ "Lnet/minecraft/client/Minecraft;Lnet/minecraft/world/item/ItemStack;)"
		+ "Lnet/minecraft/world/level/block/state/BlockState;";

	/** Hook E24: the preview's first block goes where the camera aims while Freecam's is on ({@link Aim}). */
	@WrapOperation(method = PLACEMENT_STATE, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/player/Player;getEyePosition()Lnet/minecraft/world/phys/Vec3;"))
	private static Vec3 buildingplanner$eyeAtAim(Player player, Operation<Vec3> original) {
		Entity aim = Aim.forEffortless(player);
		return aim == player ? original.call(player) : aim.getEyePosition();
	}

	/** Hook E24, for the look. */
	@WrapOperation(method = PLACEMENT_STATE, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/player/Player;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
	private static Vec3 buildingplanner$lookAtAim(Player player, Operation<Vec3> original) {
		Entity aim = Aim.forEffortless(player);
		return aim == player ? original.call(player) : aim.getLookAngle();
	}

	/** Hook E24, for the block's facing, which then matches the one hook E1 places. */
	@ModifyArg(method = PLACEMENT_STATE, at = @At(value = "INVOKE",
		target = "Lnl/requios/effortlessbuilding/render/BlockPreviewRenderer$OpenBlockPlaceContext;<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/BlockHitResult;)V"),
		index = 1)
	private static Player buildingplanner$placedByAim(Player player) {
		return Aim.placerForEffortless(player);
	}
}
