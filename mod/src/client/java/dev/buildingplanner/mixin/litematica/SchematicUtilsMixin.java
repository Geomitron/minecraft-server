package dev.buildingplanner.mixin.litematica;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.buildingplanner.client.BuildingPlannerClient;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.util.SchematicUtils;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hook L1: the four writes Litematica's ten Edit Schematic actions make. A write to a plan's region is recorded for
 * the server before it happens.
 */
@Mixin(value = SchematicUtils.class, remap = false)
abstract class SchematicUtilsMixin {
	@WrapOperation(
		// Two of the names are overloaded, and a bare name selects only the first method by that name.
		method = {
			"setTargetedSchematicBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
			"setSchematicBlockStates",
			"setAllStatesToAirExcept(Lfi/dy/masa/litematica/schematic/placement/SchematicPlacementManager;Lfi/dy/masa/litematica/schematic/placement/SchematicPlacementManager$PlacementPart;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;)Z",
			"replaceAllIdenticalBlocks"},
		at = @At(value = "INVOKE",
			target = "Lfi/dy/masa/litematica/schematic/container/LitematicaBlockStateContainer;set(IIILnet/minecraft/world/level/block/state/BlockState;)V"),
		require = 4)
	private static void buildingplanner$capture(LitematicaBlockStateContainer container, int x, int y, int z,
		BlockState state, Operation<Void> original) {
		BuildingPlannerClient.captureWrite(container, x, y, z, state);
		original.call(container, x, y, z, state);
	}
}
