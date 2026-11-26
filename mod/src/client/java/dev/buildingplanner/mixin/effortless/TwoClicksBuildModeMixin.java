package dev.buildingplanner.mixin.effortless;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.buildingplanner.client.EffortlessShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import nl.requios.effortlessbuilding.buildmode.TwoClicksBuildMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hook E37, in the modes of two clicks (Line, Wall, Floor, Circle): while the second position's step is locked, the
 * preview and the click that finishes the shape take its locked position in place of the one the aim finds (see
 * "Locking and the first click").
 */
@Mixin(value = TwoClicksBuildMode.class, remap = false)
abstract class TwoClicksBuildModeMixin {
	@WrapOperation(method = "findCoordinates", at = @At(value = "INVOKE",
		target = "Lnl/requios/effortlessbuilding/buildmode/TwoClicksBuildMode;findSecondPos(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Z)Lnet/minecraft/core/BlockPos;"))
	private BlockPos buildingplanner$lockedSecond(TwoClicksBuildMode mode, Player player, BlockPos first,
		boolean skipRaytrace, Operation<BlockPos> original) {
		return EffortlessShapes.stepPosition(1, original.call(mode, player, first, skipRaytrace));
	}
}
