package dev.buildingplanner.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import dev.buildingplanner.input.CopyColour;
import dev.buildingplanner.input.CopyPlacement.Spot;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Vector4f;

/**
 * Draws the box of the copy at the aim, and of each copy in its row, each frame through MaLiLib's renderer, as the box
 * for Copy is drawn, in Satisfactory's hologram colours ({@link CopyColour}): blue, yellow or red. Hook L6 leaves these
 * placements out of Litematica's own boxes.
 */
final class CopyOutline implements IRenderer {
	/** #00D4FF. */
	private static final Color4f BLUE = new Color4f(0f, 212 / 255f, 1f, 1f);
	/** #FFE100. */
	private static final Color4f YELLOW = new Color4f(1f, 225 / 255f, 0f, 1f);
	/** #FF7070. */
	private static final Color4f RED = new Color4f(1f, 112 / 255f, 112 / 255f, 1f);

	/** The copy's colour and the boxes of the copies, the copy's own first. */
	record Shown(CopyColour.Shown colour, List<Spot> boxes) {
	}

	private final Supplier<Shown> copy;

	private CopyOutline(Supplier<Shown> copy) {
		this.copy = copy;
	}

	/** An outline of the copies {@code copy} gives, or none while it gives null, drawn after the world each frame. */
	static CopyOutline registered(Supplier<Shown> copy) {
		CopyOutline outline = new CopyOutline(copy);
		RenderEventHandler.getInstance().registerWorldLastRenderer(outline);
		return outline;
	}

	@Override
	public void onRenderWorldLast(RenderTarget fb, CameraRenderState cameraState, Frustum culling,
		RenderBuffers buffers, GpuBufferSlice terrainFog, Vector4f fogColor, ProfilerFiller profiler) {
		Shown shown = copy.get();
		if (shown == null) {
			return;
		}
		Color4f line = switch (shown.colour().colour()) {
			case BLUE -> BLUE;
			case YELLOW -> YELLOW;
			case RED -> RED;
		};
		profiler.push("buildingplanner_copy");
		for (Spot box : shown.boxes()) {
			RenderUtils.renderAreaOutline(box.min(), box.max(), 2f, line, line, line);
		}
		profiler.pop();
	}
}
