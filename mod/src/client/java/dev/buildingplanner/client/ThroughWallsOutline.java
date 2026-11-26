package dev.buildingplanner.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.layer.ThroughWalls;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Vector4f;

/**
 * Draws ghost blocks through walls ("Ghost blocks through walls"): each ghost block {@link ThroughWalls} finds near the
 * camera, in Litematica's light blue (#33B3E6), red (#FF3333) or orange (#FF9010), its outline at 0.8 alpha and its
 * sides at 0.1, with the depth test off, so they show through any block in front of them. Each colour is one shape
 * ({@link OutlineMesh}). The blocks are found again every {@value #RESCAN_TICKS} ticks, and only the 16-block sections
 * whose positions or colours changed since are built again.
 */
final class ThroughWallsOutline implements IRenderer {
	static final int RESCAN_TICKS = 10;
	private static final Map<ThroughWalls.Colour, OutlineMesh.Look> LOOKS = new EnumMap<>(Map.of(
		ThroughWalls.Colour.LIGHT_BLUE, look("missing", 0x33B3E6),
		ThroughWalls.Colour.RED, look("wrong_block", 0xFF3333),
		ThroughWalls.Colour.ORANGE, look("wrong_state", 0xFF9010)));

	private final Supplier<Layer> layer;
	/** The ghost blocks the shapes were built for, or none. */
	private ThroughWalls.Found built = ThroughWalls.Found.NONE;
	/** Whether the last frame drew the shapes. */
	private boolean drawn;
	private ClientLevel scannedLevel;
	private long scannedAt;
	/** The game time of the scan that last changed the shapes. */
	private long changedAt;
	/** Each colour's sections that hold a block of it, by {@link SectionPos#asLong}. */
	private final Map<ThroughWalls.Colour, Long2ObjectOpenHashMap<OutlineMesh.Section>> shapes =
		new EnumMap<>(ThroughWalls.Colour.class);
	private boolean warned;

	ThroughWallsOutline(Supplier<Layer> layer) {
		this.layer = layer;
		for (ThroughWalls.Colour colour : ThroughWalls.Colour.values()) {
			shapes.put(colour, new Long2ObjectOpenHashMap<>());
		}
	}

	/** The ghost blocks of the layer {@code layer} gives, or none while it gives null, drawn after the world. */
	static ThroughWallsOutline registered(Supplier<Layer> layer) {
		ThroughWallsOutline outline = new ThroughWallsOutline(layer);
		RenderEventHandler.getInstance().registerWorldLastRenderer(outline);
		return outline;
	}

	private static OutlineMesh.Look look(String name, int rgb) {
		return new OutlineMesh.Look(name, (rgb >> 16 & 0xFF) / 255f, (rgb >> 8 & 0xFF) / 255f, (rgb & 0xFF) / 255f,
			0.1f, 0.8f, () -> MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_NO_DEPTH,
			() -> MaLiLibPipelines.DEBUG_LINES_TRANSLUCENT_NO_DEPTH);
	}

	/** The ghost blocks the last frame drew, or none. */
	ThroughWalls.Found drawn() {
		return drawn ? built : ThroughWalls.Found.NONE;
	}

	/** The game time of the scan that last changed the shapes. */
	long changedAt() {
		return changedAt;
	}

	@Override
	public void onRenderWorldLast(RenderTarget fb, CameraRenderState cameraState, Frustum culling,
		RenderBuffers buffers, GpuBufferSlice terrainFog, Vector4f fogColor, ProfilerFiller profiler) {
		drawn = false;
		Layer shown = layer.get();
		ClientLevel level = Minecraft.getInstance().level;
		if (shown == null || level == null) {
			scannedLevel = null;
			close();
			return;
		}
		profiler.push("buildingplanner_through_walls");
		try {
			long now = level.getGameTime();
			if (level != scannedLevel || now - scannedAt >= RESCAN_TICKS || now < scannedAt) {
				scannedLevel = level;
				scannedAt = now;
				if (rebuild(ThroughWalls.scan(shown, level::getBlockState,
					BlockPos.containing(RenderUtils.camPos())))) {
					changedAt = now;
				}
			}
			for (Long2ObjectOpenHashMap<OutlineMesh.Section> sections : shapes.values()) {
				OutlineMesh.draw(sections);
			}
			drawn = !built.isEmpty();
		} catch (RuntimeException e) {
			if (!warned) {
				warned = true;
				BuildingPlanner.LOGGER.warn("Couldn't draw ghost blocks through walls", e);
			}
		}
		profiler.pop();
	}

	/**
	 * Brings the shapes up to {@code found}, building again only each colour's sections that a position joining or
	 * leaving it touches; returns whether anything changed. If building fails, every shape is let go of, so the next
	 * scan builds them whole.
	 */
	boolean rebuild(ThroughWalls.Found found) {
		boolean changed = false;
		ThroughWalls.Found before = built;
		built = ThroughWalls.Found.NONE;
		try {
			for (ThroughWalls.Colour colour : ThroughWalls.Colour.values()) {
				changed |= update(shapes.get(colour), before.of(colour), found.of(colour), LOOKS.get(colour));
			}
		} catch (RuntimeException e) {
			close();
			throw e;
		}
		built = found;
		return changed;
	}

	private static boolean update(Long2ObjectOpenHashMap<OutlineMesh.Section> sections, LongSet before, LongSet after,
		OutlineMesh.Look look) {
		LongSet changed = ThroughWalls.changed(before, after);
		if (changed.isEmpty()) {
			return false;
		}
		for (LongIterator it = changed.iterator(); it.hasNext();) {
			long key = it.nextLong();
			long sectionKey = SectionPos.blockToSection(key);
			if (after.contains(key)) {
				sections.computeIfAbsent(sectionKey, k -> new OutlineMesh.Section()).blocks.add(key);
			} else if (sections.containsKey(sectionKey)) {
				sections.get(sectionKey).blocks.remove(key);
			}
		}
		for (LongIterator it = ThroughWalls.sectionsTouched(changed).iterator(); it.hasNext();) {
			long sectionKey = it.nextLong();
			OutlineMesh.Section section = sections.get(sectionKey);
			if (section == null) {
				continue;
			}
			if (section.blocks.isEmpty()) {
				section.close();
				sections.remove(sectionKey);
			} else {
				OutlineMesh.build(sectionKey, section, after, look);
			}
		}
		return true;
	}

	/** Lets go of every shape's buffers; the next scan builds them whole. */
	void close() {
		for (Long2ObjectOpenHashMap<OutlineMesh.Section> sections : shapes.values()) {
			sections.values().forEach(OutlineMesh.Section::close);
			sections.clear();
		}
		built = ThroughWalls.Found.NONE;
	}
}
