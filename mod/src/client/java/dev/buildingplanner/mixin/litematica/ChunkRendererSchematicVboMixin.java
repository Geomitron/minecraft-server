package dev.buildingplanner.mixin.litematica;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.buildingplanner.client.BuildingPlannerClient;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo;
import fi.dy.masa.litematica.util.OverlayType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hook L7: with a session, no real block is marked as an extra block. Litematica marks a real block as extra wherever
 * a placement's box holds air there, and a tile's box covers whole sections of a chunk column, so it would mark every
 * real block in them; the layer holds only blocks to place. Missing and wrong blocks keep their colours. Runs on
 * Litematica's rebuild threads too.
 */
@Mixin(value = ChunkRendererSchematicVbo.class, remap = false)
abstract class ChunkRendererSchematicVboMixin {
	@ModifyReturnValue(method = "getOverlayType", at = @At("RETURN"))
	private OverlayType buildingplanner$noExtraBlocks(OverlayType type) {
		return type == OverlayType.EXTRA && BuildingPlannerClient.active() ? OverlayType.NONE : type;
	}
}
