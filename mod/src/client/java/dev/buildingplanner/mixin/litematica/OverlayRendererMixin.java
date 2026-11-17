package dev.buildingplanner.mixin.litematica;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.buildingplanner.client.BuildingPlannerClient;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hook L6: Litematica draws no box or origin marker for a tile. Its own switch for those boxes would hide the copy's
 * box too.
 */
@Mixin(value = OverlayRenderer.class, remap = false)
abstract class OverlayRendererMixin {
	@WrapOperation(method = "updatePlacementCache", at = @At(value = "INVOKE",
		target = "Lfi/dy/masa/litematica/schematic/placement/SchematicPlacementManager;getAllSchematicsPlacements()Ljava/util/List;"))
	private List<SchematicPlacement> buildingplanner$withoutTiles(SchematicPlacementManager manager,
		Operation<List<SchematicPlacement>> original) {
		return BuildingPlannerClient.withoutTiles(original.call(manager));
	}
}
