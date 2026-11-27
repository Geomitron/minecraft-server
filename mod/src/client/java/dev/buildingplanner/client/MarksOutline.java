package dev.buildingplanner.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.layer.DeleteMarks;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.function.Supplier;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Vector4f;

/**
 * Draws Delete's marks each frame as one shape ({@link OutlineMesh}), as the copy's box is drawn: a red outline and
 * see-through red sides, in the red of the copy's box (#FF7070, sides at 0.2 alpha). A section is built again only
 * when a mark in or beside it changes ({@link DeleteMarks#takeChanged}), so a sweep over thousands of marks builds only
 * the few sections it touches.
 */
final class MarksOutline implements IRenderer {
	private static final OutlineMesh.Look LOOK = new OutlineMesh.Look("marks", 1f, 112 / 255f, 112 / 255f, 0.2f, 1f,
		2f, OutlineMesh.EXPAND, () -> MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_LEQUAL_DEPTH,
		() -> MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_NO_DEPTH_NO_CULL);

	private final Supplier<DeleteMarks> marks;
	/** The marks the sections were built for. */
	private DeleteMarks builtFor;
	/** The sections that hold a mark, by {@link SectionPos#asLong}. */
	private final Long2ObjectOpenHashMap<OutlineMesh.Section> sections = new Long2ObjectOpenHashMap<>();
	private boolean warned;

	MarksOutline(Supplier<DeleteMarks> marks) {
		this.marks = marks;
	}

	/** An outline of the marks {@code marks} gives, or none while it gives null, drawn after the world each frame. */
	static MarksOutline registered(Supplier<DeleteMarks> marks) {
		MarksOutline outline = new MarksOutline(marks);
		RenderEventHandler.getInstance().registerWorldLastRenderer(outline);
		return outline;
	}

	@Override
	public void onRenderWorldLast(RenderTarget fb, CameraRenderState cameraState, Frustum culling,
		RenderBuffers buffers, GpuBufferSlice terrainFog, Vector4f fogColor, ProfilerFiller profiler) {
		DeleteMarks shown = marks.get();
		if (shown == null || shown.size() == 0) {
			if (builtFor != null) {
				close();
			}
			return;
		}
		profiler.push("buildingplanner_marks");
		try {
			update(shown);
			OutlineMesh.draw(sections);
		} catch (RuntimeException e) {
			if (!warned) {
				warned = true;
				BuildingPlanner.LOGGER.warn("Couldn't draw the marked ghost blocks", e);
			}
		}
		profiler.pop();
	}

	/**
	 * Brings the sections up to the marks: all of them for marks it hasn't built, or when every mark counts as changed;
	 * else each section that holds a changed position or a block beside one, whose faces and edges it may change.
	 */
	void update(DeleteMarks shown) {
		LongSet changed = shown.takeChanged();
		if (shown != builtFor || changed == null) {
			close();
			builtFor = shown;
			OutlineMesh.split(shown.positions(), sections);
			for (Long2ObjectMap.Entry<OutlineMesh.Section> entry : sections.long2ObjectEntrySet()) {
				OutlineMesh.build(entry.getLongKey(), entry.getValue(), shown.positions(), LOOK);
			}
			return;
		}
		if (changed.isEmpty()) {
			return;
		}
		LongSet marked = shown.positions();
		LongOpenHashSet touched = new LongOpenHashSet();
		for (LongIterator it = changed.iterator(); it.hasNext();) {
			long key = it.nextLong();
			long sectionKey = SectionPos.blockToSection(key);
			if (marked.contains(key)) {
				sections.computeIfAbsent(sectionKey, k -> new OutlineMesh.Section()).blocks.add(key);
			} else if (sections.containsKey(sectionKey)) {
				sections.get(sectionKey).blocks.remove(key);
			}
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						touched.add(SectionPos.blockToSection(BlockPos.offset(key, dx, dy, dz)));
					}
				}
			}
		}
		for (LongIterator it = touched.iterator(); it.hasNext();) {
			long sectionKey = it.nextLong();
			OutlineMesh.Section section = sections.get(sectionKey);
			if (section == null) {
				continue;
			}
			if (section.blocks.isEmpty()) {
				section.close();
				sections.remove(sectionKey);
			} else {
				OutlineMesh.build(sectionKey, section, marked, LOOK);
			}
		}
	}

	/** Lets go of every section's buffers. */
	void close() {
		sections.values().forEach(OutlineMesh.Section::close);
		sections.clear();
		builtFor = null;
	}
}
