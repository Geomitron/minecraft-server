package dev.buildingplanner.mixin.effortless;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.buildingplanner.client.ClickRule;
import nl.requios.effortlessbuilding.buildmode.BuildModeEnum;
import nl.requios.effortlessbuilding.buildmode.BuildModes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Hook E15: Effortless Building's mode reads as Disable during a pick that leaves its reach out (see {@link MinecraftMixin}). */
@Mixin(value = BuildModes.class, remap = false)
abstract class BuildModesMixin {
	@ModifyReturnValue(method = "getBuildMode", at = @At("RETURN"))
	private BuildModeEnum buildingplanner$disabledForPick(BuildModeEnum mode) {
		return ClickRule.modeForPick(mode);
	}
}
