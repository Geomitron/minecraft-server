package dev.buildingplanner.mixin.effortless;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.buildingplanner.client.EffortlessShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import nl.requios.effortlessbuilding.buildmode.ThreeClicksBuildMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hook E37, in the modes of three clicks (Cube, Diagonal Line, Diagonal Wall, Slope Floor, Cylinder, Sphere): while
 * the second or third position's step is locked, the preview, the click that sets the second position and the click
 * that finishes the shape take its locked position in place of the one the aim finds (see "Locking and the first
 * click").
 */
@Mixin(value = ThreeClicksBuildMode.class, remap = false)
abstract class ThreeClicksBuildModeMixin {
	@WrapOperation(method = {"onClick", "findCoordinates"}, at = @At(value = "INVOKE",
		target = "Lnl/requios/effortlessbuilding/buildmode/ThreeClicksBuildMode;findSecondPos(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Z)Lnet/minecraft/core/BlockPos;"),
		require = 2)
	private BlockPos buildingplanner$lockedSecond(ThreeClicksBuildMode mode, Player player, BlockPos first,
		boolean skipRaytrace, Operation<BlockPos> original) {
		return EffortlessShapes.stepPosition(1, original.call(mode, player, first, skipRaytrace));
	}

	@WrapOperation(method = "findCoordinates", at = @At(value = "INVOKE",
		target = "Lnl/requios/effortlessbuilding/buildmode/ThreeClicksBuildMode;findThirdPos(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Z)Lnet/minecraft/core/BlockPos;"))
	private BlockPos buildingplanner$lockedThird(ThreeClicksBuildMode mode, Player player, BlockPos first,
		BlockPos second, boolean skipRaytrace, Operation<BlockPos> original) {
		return EffortlessShapes.stepPosition(2, original.call(mode, player, first, second, skipRaytrace));
	}
}
