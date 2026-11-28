package dev.buildingplanner.mixin.litematica;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.buildingplanner.client.BuildingPlannerClient;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo;
import fi.dy.masa.litematica.util.OverlayType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hook L7: with a session, a real block is marked as an extra block only inside the box of a placement that isn't a
 * tile or the copy ({@link BuildingPlannerClient#overlayType}). Litematica marks a real block as extra wherever a
 * placement's box holds air there, and a tile's box covers whole sections of a chunk column. {@code getOverlayType}
 * has no position, so the hook wraps its three calls, where the block's position is known: the block's own mark, and
 * its neighbours' for the sides and edges drawn between marks. Runs on Litematica's rebuild threads too.
 */
@Mixin(value = ChunkRendererSchematicVbo.class, remap = false)
abstract class ChunkRendererSchematicVboMixin {
	private static final String GET_OVERLAY_TYPE = "Lfi/dy/masa/litematica/render/schematic/ChunkRendererSchematicVbo;"
		+ "getOverlayType(Lnet/minecraft/world/level/block/state/BlockState;"
		+ "Lnet/minecraft/world/level/block/state/BlockState;)Lfi/dy/masa/litematica/util/OverlayType;";

	@WrapOperation(method = "renderBlocksAndOverlay", at = @At(value = "INVOKE", target = GET_OVERLAY_TYPE))
	private OverlayType buildingplanner$blockMark(ChunkRendererSchematicVbo renderer, BlockState schematic,
		BlockState client, Operation<OverlayType> original, @Local(argsOnly = true) BlockPos pos) {
		return BuildingPlannerClient.overlayType(original.call(renderer, schematic, client), pos);
	}

	/** The neighbours' marks: each call reads the neighbour's states at the method's second mutable position. */
	@WrapOperation(method = "renderOverlay", at = @At(value = "INVOKE", target = GET_OVERLAY_TYPE), require = 2)
	private OverlayType buildingplanner$neighbourMark(ChunkRendererSchematicVbo renderer, BlockState schematic,
		BlockState client, Operation<OverlayType> original, @Local(ordinal = 1) BlockPos.MutableBlockPos pos) {
		return BuildingPlannerClient.overlayType(original.call(renderer, schematic, client), pos);
	}
}
