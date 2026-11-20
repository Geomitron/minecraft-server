package dev.buildingplanner.client;

import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.render.schematic.ChunkMeshDataSchematic;
import fi.dy.masa.litematica.render.schematic.ChunkRenderDataSchematic;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.level.ChunkPos;

/**
 * What Litematica draws of chunk columns, read from its renderer's fields: the schematic world can hold a ghost block
 * that the renderer's last build of its column left out, or a column the renderer leaves out of what it draws, which
 * only the renderer's own state shows.
 */
final class RenderProbe {
	private RenderProbe() {
	}

	/**
	 * For each column from (x1, z1) to (x2, z2), x first, then z: the indices its last build drew in all block layers,
	 * {@code -} when Litematica doesn't draw the column this frame, and {@code new} before its first build; a {@code *}
	 * marks one waiting for a rebuild.
	 */
	static String columns(int x1, int z1, int x2, int z2) throws ReflectiveOperationException {
		Object worldRenderer = LitematicaRenderer.getInstance().getWorldRenderer();
		Map<?, ?> renderers = (Map<?, ?>) field(field(worldRenderer, "chunkRendererDispatcher"), "chunkRenderers");
		List<?> drawn = (List<?>) field(worldRenderer, "renderInfos");
		List<String> counts = new ArrayList<>();
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				Object renderer = renderers.get(ChunkPos.pack(x, z));
				counts.add(renderer == null || !drawn.contains(renderer) ? "-" : count(renderer));
			}
		}
		return String.join(" ", counts);
	}

	private static String count(Object renderer) throws ReflectiveOperationException {
		ChunkRenderDataSchematic data = (ChunkRenderDataSchematic) field(renderer, "chunkRenderData");
		ChunkMeshDataSchematic mesh = data == null ? null : data.getMeshDataCache();
		String pending = (boolean) field(renderer, "needsUpdate") ? "*" : "";
		if (mesh == null || mesh == ChunkMeshDataSchematic.UNCOMPILED) {
			return "new" + pending;
		}
		int indices = 0;
		for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
			ChunkMeshDataSchematic.DrawState state = mesh.getDrawState(layer);
			if (state != null) {
				indices += state.indexCount();
			}
		}
		return indices + pending;
	}

	/** Throws away every build and builds each column again, as Litematica's own re-render does. */
	static void rerender() throws ReflectiveOperationException {
		Object renderer = LitematicaRenderer.getInstance().getWorldRenderer();
		renderer.getClass().getMethod("loadRenderers", net.minecraft.util.profiling.ProfilerFiller.class)
			.invoke(renderer, InactiveProfiler.INSTANCE);
	}

	private static Object field(Object owner, String name) throws ReflectiveOperationException {
		for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
			try {
				Field field = type.getDeclaredField(name);
				field.setAccessible(true);
				return field.get(owner);
			} catch (NoSuchFieldException e) {
				// Declared higher up.
			}
		}
		throw new NoSuchFieldException(name);
	}
}
