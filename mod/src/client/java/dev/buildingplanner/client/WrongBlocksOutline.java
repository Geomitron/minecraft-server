package dev.buildingplanner.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.layer.WrongBlocks;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Vector4f;

/**
 * Draws the wrong blocks through walls ("Wrong blocks through walls"): each ghost block {@link WrongBlocks} finds near
 * the camera, in Litematica's red (#FF3333) or orange (#FF9010), its outline at 0.8 alpha and its sides at 0.1, with
 * the depth test off, so they show through any block in front of them. Each colour is one shape ({@link OutlineMesh}).
 * The blocks are found again every {@value #RESCAN_TICKS} ticks, and the shapes are built again only when they change.
 */
final class WrongBlocksOutline implements IRenderer {
	static final int RESCAN_TICKS = 10;
	private static final OutlineMesh.Look RED = look("wrong_block", 0xFF3333);
	private static final OutlineMesh.Look ORANGE = look("wrong_state", 0xFF9010);

	private final Supplier<Layer> layer;
	/** The wrong blocks the shapes were built for, or none. */
	private WrongBlocks.Found built = WrongBlocks.Found.NONE;
	/** Whether the last frame drew the shapes. */
	private boolean drawn;
	private ClientLevel scannedLevel;
	private long scannedAt;
	private final Long2ObjectOpenHashMap<OutlineMesh.Section> red = new Long2ObjectOpenHashMap<>();
	private final Long2ObjectOpenHashMap<OutlineMesh.Section> orange = new Long2ObjectOpenHashMap<>();
	private boolean warned;

	private WrongBlocksOutline(Supplier<Layer> layer) {
		this.layer = layer;
	}

	/** The wrong blocks of the layer {@code layer} gives, or none while it gives null, drawn after the world. */
	static WrongBlocksOutline registered(Supplier<Layer> layer) {
		WrongBlocksOutline outline = new WrongBlocksOutline(layer);
		RenderEventHandler.getInstance().registerWorldLastRenderer(outline);
		return outline;
	}

	private static OutlineMesh.Look look(String name, int rgb) {
		return new OutlineMesh.Look(name, (rgb >> 16 & 0xFF) / 255f, (rgb >> 8 & 0xFF) / 255f, (rgb & 0xFF) / 255f,
			0.1f, 0.8f, () -> MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_NO_DEPTH,
			() -> MaLiLibPipelines.DEBUG_LINES_TRANSLUCENT_NO_DEPTH);
	}

	/** The wrong blocks the last frame drew, or none. */
	WrongBlocks.Found drawn() {
		return drawn ? built : WrongBlocks.Found.NONE;
	}

	@Override
	public void onRenderWorldLast(RenderTarget fb, CameraRenderState cameraState, Frustum culling,
		RenderBuffers buffers, GpuBufferSlice terrainFog, Vector4f fogColor, ProfilerFiller profiler) {
		drawn = false;
		Layer shown = layer.get();
		ClientLevel level = Minecraft.getInstance().level;
		if (shown == null || level == null) {
			scannedLevel = null;
			rebuild(WrongBlocks.Found.NONE);
			return;
		}
		profiler.push("buildingplanner_wrong_blocks");
		try {
			long now = level.getGameTime();
			if (level != scannedLevel || now - scannedAt >= RESCAN_TICKS || now < scannedAt) {
				scannedLevel = level;
				scannedAt = now;
				rebuild(WrongBlocks.scan(shown, level::getBlockState, BlockPos.containing(RenderUtils.camPos())));
			}
			OutlineMesh.draw(red);
			OutlineMesh.draw(orange);
			drawn = !built.isEmpty();
		} catch (RuntimeException e) {
			if (!warned) {
				warned = true;
				BuildingPlanner.LOGGER.warn("Couldn't draw the wrong blocks", e);
			}
		}
		profiler.pop();
	}

	/**
	 * Builds both shapes again for {@code found}, unless they were built for the same blocks; until both are built, it
	 * counts as built for none, so the next scan tries again.
	 */
	private void rebuild(WrongBlocks.Found found) {
		if (found.equals(built)) {
			return;
		}
		built = WrongBlocks.Found.NONE;
		build(red, found.red(), RED);
		build(orange, found.orange(), ORANGE);
		built = found;
	}

	private static void build(Long2ObjectOpenHashMap<OutlineMesh.Section> sections, LongSet shape,
		OutlineMesh.Look look) {
		sections.values().forEach(OutlineMesh.Section::close);
		sections.clear();
		OutlineMesh.split(shape, sections);
		for (Long2ObjectMap.Entry<OutlineMesh.Section> entry : sections.long2ObjectEntrySet()) {
			OutlineMesh.build(entry.getLongKey(), entry.getValue(), shape, look);
		}
	}
}
