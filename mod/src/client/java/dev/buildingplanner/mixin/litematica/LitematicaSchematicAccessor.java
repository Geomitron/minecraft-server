package dev.buildingplanner.mixin.litematica;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import java.util.Map;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Hook L3: the maps a region's blocks, position and size live in, so a plan's region can change in place. */
@Mixin(value = LitematicaSchematic.class, remap = false)
public interface LitematicaSchematicAccessor {
	@Accessor("blockContainers")
	Map<String, LitematicaBlockStateContainer> buildingplanner$blockContainers();

	@Accessor("subRegionPositions")
	Map<String, BlockPos> buildingplanner$subRegionPositions();

	@Accessor("subRegionSizes")
	Map<String, BlockPos> buildingplanner$subRegionSizes();
}
