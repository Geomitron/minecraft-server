package dev.buildingplanner.mixin.litematica;

import dev.buildingplanner.client.BuildingPlannerClient;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hook L4: a tile, or the copy, is never selected; selecting one leaves the selection as it was. Covers {@code addSchematicPlacement},
 * which selects a new placement when none is selected, {@code removeSchematicPlacement}, which then selects the first
 * placement, selection by aim, and the placement list. The selection stays as it was rather than turning to none,
 * since selecting none also clears Litematica's material list. The copy is known as the copy before it's added.
 */
@Mixin(value = SchematicPlacementManager.class, remap = false)
abstract class SchematicPlacementManagerMixin {
	@Inject(method = "setSelectedSchematicPlacement", at = @At("HEAD"), cancellable = true)
	private void buildingplanner$neverATile(SchematicPlacement placement, CallbackInfo ci) {
		if (placement != null && BuildingPlannerClient.isOurs(placement)) {
			ci.cancel();
		}
	}
}
