package dev.buildingplanner.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.look.BoxLook;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

/**
 * Draws the box for Copy, Delete or the material list each frame through MaLiLib's renderer, as Litematica draws its
 * area selections: an outline and see-through sides in its purpose's colour, or red while the box breaks a limit
 * ({@link BoxLook}); and, before its first corner is set, the top face of the block aimed at shaded black.
 */
final class BoxOutline implements IRenderer {
	private static final Map<BoxLook.Colour, Color4f> LINES = new EnumMap<>(BoxLook.Colour.class);
	private static final Map<BoxLook.Colour, Color4f> SIDES = new EnumMap<>(BoxLook.Colour.class);
	private static final Color4f SHADE = new Color4f(0f, 0f, 0f, BoxLook.SHADE_ALPHA);

	static {
		for (BoxLook.Colour colour : BoxLook.Colour.values()) {
			float red = (colour.rgb() >> 16 & 0xFF) / 255f;
			float green = (colour.rgb() >> 8 & 0xFF) / 255f;
			float blue = (colour.rgb() & 0xFF) / 255f;
			LINES.put(colour, new Color4f(red, green, blue, 1f));
			SIDES.put(colour, new Color4f(red, green, blue, colour.sidesAlpha()));
		}
	}

	private final Supplier<BoxDraw.Shown> box;
	private boolean warned;

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
		BoxLook.Colour colour = shown.colour();
		Color4f line = LINES.get(colour);
		profiler.push("buildingplanner_box");
		if (shown.shade() != null) {
			shade(shown.shade());
		}
		RenderUtils.renderAreaOutline(shown.min(), shown.max(), 2f, line, line, line);
		RenderUtils.renderAreaSides(shown.min(), shown.max(), SIDES.get(colour));
		profiler.pop();
	}

	/** The shaded square, drawn as MaLiLib draws an area's sides, from the camera. */
	private void shade(BoxLook.Square square) {
		Vec3 camera = RenderUtils.camPos();
		RenderContext context = new RenderContext(() -> "buildingplanner:box_shade",
			MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_LEQUAL_DEPTH, 0);
		try {
			BufferBuilder builder = context.getBuilder();
			RenderUtils.drawBoxTopBatchedQuads((float) (square.minX() - camera.x), (float) (square.minZ() - camera.z),
				(float) (square.maxX() - camera.x), (float) (square.y() - camera.y), (float) (square.maxZ() - camera.z),
				SHADE, builder);
			MeshData mesh = builder.build();
			if (mesh != null) {
				context.upload(mesh, false);
				mesh.close();
				context.drawPost();
			}
			context.close();
		} catch (Exception e) {
			if (!warned) {
				warned = true;
				BuildingPlanner.LOGGER.warn("Couldn't draw the shaded face under the box's first corner", e);
			}
		}
	}
}
