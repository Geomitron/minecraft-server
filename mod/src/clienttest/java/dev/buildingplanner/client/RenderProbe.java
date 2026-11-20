package dev.buildingplanner.client;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.render.schematic.ChunkMeshDataSchematic;
import fi.dy.masa.litematica.render.schematic.ChunkRenderDataSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;

/**
 * What Litematica draws of chunk columns, read from its renderer's fields: the schematic world can hold a ghost block
 * that the renderer's last build of its column left out, or a column the renderer leaves out of what it draws, which
 * only the renderer's own state shows. Also takes a block out of what Litematica holds alone, for the self-check.
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

	/**
	 * Takes the block at the position out of the container of the tile whose box holds it, as structure void, and has
	 * Litematica rebuild the column: Litematica then holds less than the game's copy of the layer, which the self-check
	 * finds at the column's next change. Returns {@code ok}, or {@code no tile}.
	 */
	static String unhold(BlockPos pos) {
		for (SchematicPlacement p : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements()) {
			if (!BuildingPlannerClient.isTile(p)) {
				continue;
			}
			BlockPos origin = p.getOrigin();
			var container = p.getSchematic().getSubRegionContainer(Tiles.NAME);
			var size = container.getSize();
			int x = pos.getX() - origin.getX();
			int y = pos.getY() - origin.getY();
			int z = pos.getZ() - origin.getZ();
			if (x >= 0 && y >= 0 && z >= 0 && x < size.getX() && y < size.getY() && z < size.getZ()) {
				container.set(x, y, z, Blocks.STRUCTURE_VOID.defaultBlockState());
				DataManager.getSchematicPlacementManager().markChunkForRebuild(pos.getX() >> 4, pos.getZ() >> 4);
				return "ok";
			}
		}
		return "no tile";
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
