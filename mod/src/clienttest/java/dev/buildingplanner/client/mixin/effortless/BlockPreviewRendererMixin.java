package dev.buildingplanner.client.mixin.effortless;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.buildingplanner.client.PreviewProbe;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import nl.requios.effortlessbuilding.render.BlockPreviewRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells the driver which positions Effortless Building's preview draws, for its {@code preview}. In a mixin config of
 * its own that isn't required, since a check may start a client without Effortless Building.
 */
@Mixin(value = BlockPreviewRenderer.class, remap = false)
abstract class BlockPreviewRendererMixin {
	@Inject(method = "renderBoundingBoxFaces", at = @At("HEAD"))
	private static void buildingplanner_clienttest$drawing(PoseStack poseStack, SubmitNodeCollector collector,
		List<BlockPos> positions, double camX, double camY, double camZ, boolean breaking, boolean unbreakable,
		CallbackInfo ci) {
		PreviewProbe.drawing(positions);
	}
}
