package dev.buildingplanner.mixin.litematica;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.buildingplanner.client.BuildingPlannerClient;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.util.SchematicUtils;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hook L1: Litematica's Edit Schematic actions never write into a tile, so they never change the layer, even on a
 * computer whose Litematica tool mode is still "Edit Schematic". These four methods hold the only writes the ten
 * actions make. Writes to other placements, such as the copy, stay Litematica's own.
 */
@Mixin(value = SchematicUtils.class, remap = false)
abstract class SchematicUtilsMixin {
	@WrapWithCondition(method = {
		"setTargetedSchematicBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
		"setSchematicBlockStates",
		"setAllStatesToAirExcept(Lfi/dy/masa/litematica/schematic/placement/SchematicPlacementManager;Lfi/dy/masa/litematica/schematic/placement/SchematicPlacementManager$PlacementPart;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;)Z",
		"replaceAllIdenticalBlocks"},
		at = @At(value = "INVOKE",
			target = "Lfi/dy/masa/litematica/schematic/container/LitematicaBlockStateContainer;set(IIILnet/minecraft/world/level/block/state/BlockState;)V"),
		require = 4)
	private static boolean buildingplanner$notIntoTiles(LitematicaBlockStateContainer container, int x, int y, int z,
		BlockState state) {
		return !BuildingPlannerClient.isTileContainer(container);
	}
}
