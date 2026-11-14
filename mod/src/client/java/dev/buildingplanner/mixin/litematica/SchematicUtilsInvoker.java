package dev.buildingplanner.mixin.litematica;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.SchematicUtils;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Hook L2: a world position in a region's container coordinates, without clamping to the region's box, so a shape
 * that crosses the box's edge keeps its blocks outside it.
 */
@Mixin(value = SchematicUtils.class, remap = false)
public interface SchematicUtilsInvoker {
	@Invoker("getReverserTransformedWorldPosition")
	static BlockPos buildingplanner$toContainer(BlockPos worldPos, LitematicaSchematic schematic, String regionName,
		SchematicPlacement schematicPlacement, SubRegionPlacement regionPlacement) {
		throw new AssertionError();
	}
}
