package dev.buildingplanner.mixin.effortless;

import dev.buildingplanner.BuildingPlanner;
import nl.requios.effortlessbuilding.EffortlessBuildingClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Marks in the log that the Effortless Building hooks applied; the shape hooks (E1 to E9) join this package. */
@Mixin(value = EffortlessBuildingClient.class, remap = false)
abstract class EffortlessBuildingClientMixin {
	@Inject(method = "onInitializeClient", at = @At("TAIL"))
	private void buildingplanner$hooksActive(CallbackInfo ci) {
		BuildingPlanner.LOGGER.info("Effortless Building hooks active");
	}
}
