package dev.buildingplanner.mixin.effortless;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Set;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import nl.requios.effortlessbuilding.render.BlockPreviewRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Hook E38: Effortless Building's private drawing of a shape's blocks, with which the red frame for the left click
 * draws in its own look for a shape drawn with the break click (see "Locking and the first click": "The first click's
 * blocks"). {@code computeBorderEdges} gives, and {@code renderEdgeQuads} takes, a set of a private record, which these
 * name as a raw {@code Set}.
 */
@Mixin(value = BlockPreviewRenderer.class, remap = false)
public interface BlockPreviewRendererInvoker {
	/** The sides of the blocks at {@code positions}, with the checkered texture: red with {@code breaking}. */
	@Invoker("renderBoundingBoxFaces")
	static void buildingplanner$faces(PoseStack poseStack, SubmitNodeCollector collector, List<BlockPos> positions,
		double camX, double camY, double camZ, boolean breaking, boolean unbreakable) {
		throw new AssertionError();
	}

	/** The edges around the blocks at {@code positions}, where they don't touch each other. */
	@Invoker("computeBorderEdges")
	static Set buildingplanner$borderEdges(List<BlockPos> positions) {
		throw new AssertionError();
	}

	/** Draws those edges as lines of {@code halfWidth} each side, in that colour. */
	@Invoker("renderEdgeQuads")
	static void buildingplanner$edges(PoseStack poseStack, SubmitNodeCollector collector, Set edges, double camX,
		double camY, double camZ, float halfWidth, int r, int g, int b, int a) {
		throw new AssertionError();
	}
}
