package dev.buildingplanner.mixin.effortless;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.buildingplanner.client.BuildingPlannerClient;
import nl.requios.effortlessbuilding.buildpipeline.ConstraintSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hook E7: shapes drawn into plans skip the checks on real blocks: survival's (placed by the player, hardness,
 * tools) and protected block entities. The world border, build height and the block cap still apply.
 */
@Mixin(value = ConstraintSystem.class, remap = false)
abstract class ConstraintSystemMixin {
	@ModifyExpressionValue(method = "processBlocks", at = @At(value = "FIELD",
		target = "Lnet/minecraft/world/entity/player/Abilities;instabuild:Z"))
	private boolean buildingplanner$skipSurvivalChecks(boolean creative) {
		return creative || BuildingPlannerClient.active();
	}

	@ModifyExpressionValue(method = "processBlocks", at = @At(value = "INVOKE",
		target = "Lnl/requios/effortlessbuilding/buildpipeline/ConstraintSystem;getProtectTileEntities()Z"))
	private boolean buildingplanner$skipProtectedBlockEntities(boolean protect) {
		return protect && !BuildingPlannerClient.active();
	}
}
