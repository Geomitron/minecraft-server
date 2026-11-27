package dev.buildingplanner.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.look.GhostFrame;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.List;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Vector4f;

/**
 * Hook E38's red frame for the left click, drawn more strongly on ghost blocks ({@link GhostFrame}): the framed ghost
 * blocks hook E38 hands over each frame, drawn after the world and after the ghost blocks through walls, as one shape
 * ({@link OutlineMesh}) in Effortless Building's erase red, its sides in front of Effortless Building's own and only
 * where the ghost block shows, and its outline over everything, as Delete's marks are. The shape is built again only
 * when the positions change.
 */
final class GhostFrameOutline implements IRenderer {
	private static final OutlineMesh.Look LOOK = new OutlineMesh.Look("ghost_frame",
		(GhostFrame.RGB >> 16 & 0xFF) / 255f, (GhostFrame.RGB >> 8 & 0xFF) / 255f, (GhostFrame.RGB & 0xFF) / 255f,
		GhostFrame.SIDES_ALPHA, GhostFrame.LINE_ALPHA, GhostFrame.LINE_WIDTH, GhostFrame.SIDES_OUT,
		() -> MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_LEQUAL_DEPTH,
		() -> MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_NO_DEPTH_NO_CULL);

	/** The framed ghost blocks hook E38 handed over for this frame, or none. */
	private List<BlockPos> pending = List.of();
	/** The positions the shape was built for. */
	private List<BlockPos> built = List.of();
	/** The positions the last frame drew. */
	private List<BlockPos> drawn = List.of();
	private final Long2ObjectOpenHashMap<OutlineMesh.Section> sections = new Long2ObjectOpenHashMap<>();
	private boolean warned;

	/** The frame on the ghost blocks hook E38 hands over each frame, drawn after the renderers registered before it. */
	static GhostFrameOutline registered() {
		GhostFrameOutline outline = new GhostFrameOutline();
		RenderEventHandler.getInstance().registerWorldLastRenderer(outline);
		return outline;
	}

	/** Hook E38: the framed ghost blocks for this frame, in order. */
	void frame(List<BlockPos> ghosts) {
		pending = ghosts;
	}

	/** The positions the last frame drew the frame on, or none. */
	List<BlockPos> drawn() {
		return drawn;
	}

	@Override
	public void onRenderWorldLast(RenderTarget fb, CameraRenderState cameraState, Frustum culling,
		RenderBuffers buffers, GpuBufferSlice terrainFog, Vector4f fogColor, ProfilerFiller profiler) {
		List<BlockPos> ghosts = pending;
		pending = List.of();
		drawn = List.of();
		if (ghosts.isEmpty()) {
			close();
			return;
		}
		profiler.push("buildingplanner_ghost_frame");
		try {
			if (!ghosts.equals(built)) {
				close();
				LongOpenHashSet shape = new LongOpenHashSet();
				ghosts.forEach(pos -> shape.add(pos.asLong()));
				OutlineMesh.split(shape, sections);
				for (Long2ObjectMap.Entry<OutlineMesh.Section> entry : sections.long2ObjectEntrySet()) {
					OutlineMesh.build(entry.getLongKey(), entry.getValue(), shape, LOOK);
				}
				built = ghosts;
			}
			OutlineMesh.draw(sections);
			drawn = ghosts;
		} catch (RuntimeException e) {
			close();
			if (!warned) {
				warned = true;
				BuildingPlanner.LOGGER.warn("Couldn't draw the red frame on ghost blocks", e);
			}
		}
		profiler.pop();
	}

	/** Lets go of the shape's buffers. */
	private void close() {
		sections.values().forEach(OutlineMesh.Section::close);
		sections.clear();
		built = List.of();
	}
}
