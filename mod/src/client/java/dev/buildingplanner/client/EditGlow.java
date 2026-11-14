package dev.buildingplanner.client;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Vector4f;

/**
 * Makes the ghost blocks someone else just changed glow for a few seconds, fading out, with the editor's name above
 * them. Draws through MaLiLib's world renderer, as Litematica draws its own overlays.
 */
final class EditGlow implements IRenderer {
	private static final long GLOW_MILLIS = 3000;
	/** Above this many blocks, an edit glows as one box around them instead of block by block. */
	private static final int MAX_OUTLINED_BLOCKS = 512;
	private static final float RED = 0.35f;
	private static final float GREEN = 0.9f;
	private static final float BLUE = 1.0f;

	private record Glow(List<BlockPos> blocks, BlockPos min, BlockPos max, String editor, long start) {
	}

	private final List<Glow> glows = new ArrayList<>();

	/** Starts a glow on {@code blocks} (world positions), changed by {@code author}. */
	void add(List<BlockPos> blocks, UUID author) {
		BlockPos min = blocks.getFirst();
		BlockPos max = blocks.getFirst();
		for (BlockPos pos : blocks) {
			min = BlockPos.min(min, pos);
			max = BlockPos.max(max, pos);
		}
		glows.add(new Glow(blocks.size() <= MAX_OUTLINED_BLOCKS ? List.copyOf(blocks) : List.of(), min, max,
			editorName(author), System.currentTimeMillis()));
	}

	void clear() {
		glows.clear();
	}

	private static String editorName(UUID author) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		PlayerInfo info = connection == null ? null : connection.getPlayerInfo(author);
		return info == null ? null : info.getProfile().name();
	}

	@Override
	public void onRenderWorldLast(RenderTarget fb, CameraRenderState cameraState, Frustum culling,
		RenderBuffers buffers, GpuBufferSlice terrainFog, Vector4f fogColor, ProfilerFiller profiler) {
		long now = System.currentTimeMillis();
		glows.removeIf(glow -> now - glow.start() >= GLOW_MILLIS);
		if (glows.isEmpty()) {
			return;
		}
		profiler.push("buildingplanner_edit_glow");
		try (RenderContext ctx = new RenderContext(() -> "buildingplanner:edit_glow/outlines",
			MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_NO_DEPTH_NO_CULL, 0)) {
			BufferBuilder buffer = ctx.getBuilder();
			for (Glow glow : glows) {
				Color4f color = new Color4f(RED, GREEN, BLUE, fade(glow, now));
				for (BlockPos pos : glow.blocks()) {
					RenderUtils.drawBlockBoundingBoxOutlinesBatchedLinesSimple(pos, color, 0.002, 2.0f, buffer);
				}
			}
			draw(ctx, buffer, true);
			buffer = ctx.start(() -> "buildingplanner:edit_glow/sides",
				MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_NO_DEPTH_NO_CULL, 0);
			for (Glow glow : glows) {
				Color4f color = new Color4f(RED, GREEN, BLUE, 0.35f * fade(glow, now));
				if (glow.blocks().isEmpty()) {
					RenderUtils.renderAreaSidesBatched(glow.min(), glow.max(), color, 0.002, buffer);
				}
				for (BlockPos pos : glow.blocks()) {
					RenderUtils.renderAreaSidesBatched(pos, pos, color, 0.002, buffer);
				}
			}
			draw(ctx, buffer, false);
		} catch (Exception e) {
			// A frame without the glow is better than a crash; MaLiLib's own renderers do the same.
		}
		float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		for (Glow glow : glows) {
			if (glow.editor() != null) {
				RenderUtils.drawTextPlate(List.of(glow.editor()), (glow.min().getX() + glow.max().getX() + 1) / 2.0,
					glow.max().getY() + 1.6, (glow.min().getZ() + glow.max().getZ() + 1) / 2.0, 0.025f, partialTick);
			}
		}
		profiler.pop();
	}

	private static void draw(RenderContext ctx, BufferBuilder buffer, boolean lines) {
		MeshData mesh = buffer.build();
		if (mesh != null) {
			ctx.draw(mesh, false, lines);
			mesh.close();
		}
		ctx.reset();
	}

	private static float fade(Glow glow, long now) {
		return 1.0f - (float) (now - glow.start()) / GLOW_MILLIS;
	}
}
