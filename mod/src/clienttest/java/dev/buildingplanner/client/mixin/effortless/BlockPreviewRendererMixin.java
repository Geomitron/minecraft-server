package dev.buildingplanner.client.mixin.effortless;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.buildingplanner.client.PreviewProbe;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import nl.requios.effortlessbuilding.buildpipeline.BuildPipelineClient;
import nl.requios.effortlessbuilding.render.BlockPreviewRenderer;
import nl.requios.effortlessbuilding.utilities.BlockEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells the driver which positions Effortless Building's preview draws, for its {@code preview}, and the state of each
 * block it draws, for its {@code previewstate}. In a mixin config of its own that isn't required, since a check may
 * start a client without Effortless Building.
 */
@Mixin(value = BlockPreviewRenderer.class, remap = false)
abstract class BlockPreviewRendererMixin {
	@Inject(method = "renderBoundingBoxFaces", at = @At("HEAD"))
	private static void buildingplanner_clienttest$drawing(PoseStack poseStack, SubmitNodeCollector collector,
		List<BlockPos> positions, double camX, double camY, double camZ, boolean breaking, boolean unbreakable,
		CallbackInfo ci) {
		PreviewProbe.drawing(positions);
		if (!breaking && !unbreakable) {
			// The method draws a block to place red where it's marked missing, as Disable's turned block that can't stay.
			PreviewProbe.drawingRed(positions.stream()
				.filter(BuildPipelineClient.ITEM_USAGE.missingPositions::contains).toList());
		}
	}

	/** The state the preview draws each block in: its start's, after the entry's mirror and turn. */
	@WrapOperation(method = "render", at = @At(value = "INVOKE",
		target = "Lnl/requios/effortlessbuilding/utilities/BlockEntry;applyTransforms(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;"))
	private static BlockState buildingplanner_clienttest$drawingState(BlockEntry entry, BlockState state,
		Operation<BlockState> original) {
		BlockState drawn = original.call(entry, state);
		PreviewProbe.drawingState(entry.blockPos, drawn);
		return drawn;
	}
}
