package dev.buildingplanner.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.layer.DeleteMarks;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderContext;
import fi.dy.masa.malilib.render.RenderUtils;
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
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;

/**
 * Draws Delete's marks each frame through MaLiLib's renderer, as the copy's box is drawn: a red outline and
 * see-through red sides, in the red of the copy's box (#FF7070, sides at 0.2 alpha). Only the faces that don't touch
 * another marked block are drawn, and only the edges where the marks' shape turns, so a marked wall shows as one red
 * shape. The shape is built in sections of 16 blocks a side, as the game builds its terrain, and a section is built
 * again only when a mark in or beside it changes ({@link DeleteMarks#takeChanged}), so a sweep over thousands of marks
 * builds only the few sections it touches. Each frame draws each section where it is from the camera.
 */
final class MarksOutline implements IRenderer {
	private static final float RED = 1f;
	private static final float GREEN = 112 / 255f;
	private static final float BLUE = 112 / 255f;
	private static final float SIDES_ALPHA = 0.2f;
	private static final float LINE_WIDTH = 2f;
	/** How far the sides stand out from the blocks, as MaLiLib's box sides do, so they don't flicker with them. */
	private static final float EXPAND = 0.002f;
	private static final Direction[] DIRECTIONS = Direction.values();

	/** A section's marks, and its shape's sides and lines, counted from the section's lowest corner. */
	private static final class Section {
		final LongOpenHashSet marks = new LongOpenHashSet();
		RenderContext sides;
		RenderContext lines;

		void close() {
			closeQuietly(sides);
			closeQuietly(lines);
			sides = null;
			lines = null;
		}
	}

	private final Supplier<DeleteMarks> marks;
	/** The marks the sections were built for. */
	private DeleteMarks builtFor;
	/** The sections that hold a mark, by {@link SectionPos#asLong}. */
	private final Long2ObjectOpenHashMap<Section> sections = new Long2ObjectOpenHashMap<>();
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
			Vec3 camera = RenderUtils.camPos();
			float[] offset = new float[3];
			for (int pass = 0; pass < 2; pass++) {
				for (Long2ObjectMap.Entry<Section> entry : sections.long2ObjectEntrySet()) {
					long key = entry.getLongKey();
					offset[0] = (float) (SectionPos.sectionToBlockCoord(SectionPos.x(key)) - camera.x);
					offset[1] = (float) (SectionPos.sectionToBlockCoord(SectionPos.y(key)) - camera.y);
					offset[2] = (float) (SectionPos.sectionToBlockCoord(SectionPos.z(key)) - camera.z);
					draw(pass == 0 ? entry.getValue().sides : entry.getValue().lines, offset, pass == 1);
				}
			}
		} catch (RuntimeException e) {
			if (!warned) {
				warned = true;
				BuildingPlanner.LOGGER.warn("Couldn't draw the marked ghost blocks", e);
			}
		}
		profiler.pop();
	}

	/**
	 * Draws a section's sides, or its {@code lines}, moved by {@code offset} through the model-view matrix: the shaders
	 * these pipelines use take no model offset of their own.
	 */
	private static void draw(RenderContext context, float[] offset, boolean lines) {
		if (context == null) {
			return;
		}
		Matrix4fStack modelView = RenderSystem.getModelViewStack();
		modelView.pushMatrix();
		modelView.translate(offset[0], offset[1], offset[2]);
		try {
			context.drawPost(null, false, lines);
		} finally {
			modelView.popMatrix();
		}
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
			for (LongIterator it = shown.positions().iterator(); it.hasNext();) {
				long key = it.nextLong();
				sections.computeIfAbsent(SectionPos.blockToSection(key), k -> new Section()).marks.add(key);
			}
			for (Long2ObjectMap.Entry<Section> entry : sections.long2ObjectEntrySet()) {
				build(entry.getLongKey(), entry.getValue());
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
				sections.computeIfAbsent(sectionKey, k -> new Section()).marks.add(key);
			} else if (sections.containsKey(sectionKey)) {
				sections.get(sectionKey).marks.remove(key);
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
			Section section = sections.get(sectionKey);
			if (section == null) {
				continue;
			}
			if (section.marks.isEmpty()) {
				section.close();
				sections.remove(sectionKey);
			} else {
				build(sectionKey, section);
			}
		}
	}

	/** Builds a section's shape: the sides of its marks that touch no other mark, and the edges where the shape turns. */
	private void build(long sectionKey, Section section) {
		section.close();
		LongSet marked = builtFor.positions();
		int originX = SectionPos.sectionToBlockCoord(SectionPos.x(sectionKey));
		int originY = SectionPos.sectionToBlockCoord(SectionPos.y(sectionKey));
		int originZ = SectionPos.sectionToBlockCoord(SectionPos.z(sectionKey));
		RenderContext sideContext = new RenderContext(() -> "buildingplanner:marks_sides",
			MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_LEQUAL_DEPTH, 0);
		RenderContext lineContext = new RenderContext(() -> "buildingplanner:marks_lines",
			MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_NO_DEPTH_NO_CULL, 0);
		BufferBuilder sideBuilder = sideContext.getBuilder();
		BufferBuilder lineBuilder = lineContext.getBuilder();
		for (LongIterator it = section.marks.iterator(); it.hasNext();) {
			long key = it.nextLong();
			int x = BlockPos.getX(key) - originX;
			int y = BlockPos.getY(key) - originY;
			int z = BlockPos.getZ(key) - originZ;
			for (Direction side : DIRECTIONS) {
				if (marked.contains(BlockPos.offset(key, side))) {
					continue;
				}
				face(sideBuilder, x, y, z, side);
				for (Direction along : DIRECTIONS) {
					if (along.getAxis() != side.getAxis() && drawsEdge(marked, key, side, along)) {
						edge(lineBuilder, x, y, z, side, along);
					}
				}
			}
		}
		section.sides = upload(sideContext, sideBuilder);
		section.lines = upload(lineContext, lineBuilder);
	}

	/**
	 * Whether the face of the mark at {@code key} on its {@code side}, which touches no mark, draws its edge toward
	 * {@code along}: not where the face goes on flat into the next mark's, and once for each edge two faces share, from
	 * the face whose side comes first in {@link Direction}'s order.
	 */
	private static boolean drawsEdge(LongSet marked, long key, Direction side, Direction along) {
		long next = BlockPos.offset(key, along);
		if (!marked.contains(next)) {
			// The shape turns away: the face toward along shares the edge.
			return side.ordinal() < along.ordinal();
		}
		if (!marked.contains(BlockPos.offset(next, side))) {
			// The next mark's face goes on flat.
			return false;
		}
		// The shape turns in: the face of the mark beyond, toward the opposite of along, shares the edge.
		return side.ordinal() < along.getOpposite().ordinal();
	}

	/** The block's face on {@code side}, its corners counter-clockwise seen from outside. */
	private static void face(BufferBuilder builder, int x, int y, int z, Direction side) {
		float lo = -EXPAND;
		float hi = 1 + EXPAND;
		float[][] corners = switch (side) {
			case UP -> new float[][] {{lo, hi, hi}, {hi, hi, hi}, {hi, hi, lo}, {lo, hi, lo}};
			case DOWN -> new float[][] {{lo, lo, lo}, {hi, lo, lo}, {hi, lo, hi}, {lo, lo, hi}};
			case NORTH -> new float[][] {{lo, lo, lo}, {lo, hi, lo}, {hi, hi, lo}, {hi, lo, lo}};
			case SOUTH -> new float[][] {{lo, lo, hi}, {hi, lo, hi}, {hi, hi, hi}, {lo, hi, hi}};
			case WEST -> new float[][] {{lo, lo, lo}, {lo, lo, hi}, {lo, hi, hi}, {lo, hi, lo}};
			case EAST -> new float[][] {{hi, lo, lo}, {hi, hi, lo}, {hi, hi, hi}, {hi, lo, hi}};
		};
		for (float[] corner : corners) {
			builder.addVertex(x + corner[0], y + corner[1], z + corner[2]).setColor(RED, GREEN, BLUE, SIDES_ALPHA);
		}
	}

	/** The block's edge between its {@code side} face and its {@code along} face. */
	private static void edge(BufferBuilder builder, int x, int y, int z, Direction side, Direction along) {
		float[] from = new float[3];
		float[] to = new float[3];
		for (Direction.Axis axis : Direction.Axis.values()) {
			int i = axis.ordinal();
			if (axis == side.getAxis() || axis == along.getAxis()) {
				Direction on = axis == side.getAxis() ? side : along;
				float at = on.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0;
				from[i] = at;
				to[i] = at;
			} else {
				from[i] = 0;
				to[i] = 1;
			}
		}
		builder.addVertex(x + from[0], y + from[1], z + from[2]).setColor(RED, GREEN, BLUE, 1f).setLineWidth(LINE_WIDTH);
		builder.addVertex(x + to[0], y + to[1], z + to[2]).setColor(RED, GREEN, BLUE, 1f).setLineWidth(LINE_WIDTH);
	}

	private static RenderContext upload(RenderContext context, BufferBuilder builder) {
		MeshData mesh = builder.build();
		if (mesh == null) {
			closeQuietly(context);
			return null;
		}
		context.upload(mesh, false);
		mesh.close();
		return context;
	}

	/** Lets go of every section's buffers. */
	void close() {
		sections.values().forEach(Section::close);
		sections.clear();
		builtFor = null;
	}

	private static void closeQuietly(RenderContext context) {
		if (context == null) {
			return;
		}
		try {
			context.close();
		} catch (Exception e) {
			BuildingPlanner.LOGGER.warn("Couldn't let go of the marks' buffers", e);
		}
	}
}
