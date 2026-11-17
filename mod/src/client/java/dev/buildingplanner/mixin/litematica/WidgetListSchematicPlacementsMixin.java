package dev.buildingplanner.mixin.litematica;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.buildingplanner.client.BuildingPlannerClient;
import fi.dy.masa.litematica.gui.widgets.WidgetListSchematicPlacements;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import java.util.Collection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Hook L5: Litematica's placement list leaves out the tiles and the copy. */
@Mixin(value = WidgetListSchematicPlacements.class, remap = false)
abstract class WidgetListSchematicPlacementsMixin {
	@ModifyReturnValue(method = "getAllEntries", at = @At("RETURN"))
	private Collection<SchematicPlacement> buildingplanner$withoutTiles(Collection<SchematicPlacement> placements) {
		return BuildingPlannerClient.withoutOurs(placements);
	}
}
