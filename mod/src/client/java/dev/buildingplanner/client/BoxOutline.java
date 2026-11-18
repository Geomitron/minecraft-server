package dev.buildingplanner.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import java.util.function.Supplier;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Vector4f;

/**
 * Draws the box for Copy or the material list each frame through MaLiLib's renderer, as Litematica draws its area
 * selections: an outline and see-through sides, white, or red while the box breaks a limit.
 */
final class BoxOutline implements IRenderer {
	private static final Color4f WHITE = new Color4f(1f, 1f, 1f, 1f);
	private static final Color4f WHITE_SIDES = new Color4f(1f, 1f, 1f, 0.15f);
	private static final Color4f RED = new Color4f(1f, 0.2f, 0.2f, 1f);
	private static final Color4f RED_SIDES = new Color4f(1f, 0.2f, 0.2f, 0.2f);

	private final Supplier<BoxDraw.Shown> box;

	private BoxOutline(Supplier<BoxDraw.Shown> box) {
		this.box = box;
	}

	/** An outline of the box {@code box} gives, or none while it gives null, drawn after the world each frame. */
	static BoxOutline registered(Supplier<BoxDraw.Shown> box) {
		BoxOutline outline = new BoxOutline(box);
		RenderEventHandler.getInstance().registerWorldLastRenderer(outline);
		return outline;
	}

	@Override
	public void onRenderWorldLast(RenderTarget fb, CameraRenderState cameraState, Frustum culling,
		RenderBuffers buffers, GpuBufferSlice terrainFog, Vector4f fogColor, ProfilerFiller profiler) {
		BoxDraw.Shown shown = box.get();
		if (shown == null || shown.min() == null) {
			return;
		}
		boolean red = shown.limit() != null;
		Color4f line = red ? RED : WHITE;
		profiler.push("buildingplanner_box");
		RenderUtils.renderAreaOutline(shown.min(), shown.max(), 2f, line, line, line);
		RenderUtils.renderAreaSides(shown.min(), shown.max(), red ? RED_SIDES : WHITE_SIDES);
		profiler.pop();
	}
}
