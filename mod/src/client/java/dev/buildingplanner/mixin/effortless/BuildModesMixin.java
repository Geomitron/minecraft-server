package dev.buildingplanner.mixin.effortless;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.buildingplanner.client.BuildingPlannerClient;
import dev.buildingplanner.client.ClickRule;
import nl.requios.effortlessbuilding.buildmode.BuildModeEnum;
import nl.requios.effortlessbuilding.buildmode.BuildModes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hook E15: Effortless Building's mode reads as Disable during a pick that leaves its reach out (see
 * {@link MinecraftMixin}). Hook E21: choosing a mode ends Building Planner's mouse mode.
 */
@Mixin(value = BuildModes.class, remap = false)
abstract class BuildModesMixin {
	@ModifyReturnValue(method = "getBuildMode", at = @At("RETURN"))
	private BuildModeEnum buildingplanner$disabledForPick(BuildModeEnum mode) {
		return ClickRule.modeForPick(mode);
	}

	/**
	 * Hook E21: choosing a mode (the radial menu's {@code performAction} is the only caller outside {@code BuildModes})
	 * puts the copy away, even when the player chooses the mode they had.
	 */
	@Inject(method = "setBuildMode", at = @At("HEAD"))
	private void buildingplanner$endMouseMode(BuildModeEnum mode, CallbackInfo ci) {
		BuildingPlannerClient.buildModeChosen();
	}
}
