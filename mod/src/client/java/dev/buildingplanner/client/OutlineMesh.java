package dev.buildingplanner.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.buildingplanner.BuildingPlanner;
import fi.dy.masa.malilib.render.RenderContext;
import fi.dy.masa.malilib.render.RenderUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import java.util.function.Supplier;
import org.joml.Matrix4fStack;

/**
 * One see-through shape over a set of blocks, drawn through MaLiLib's renderer: sides in a colour, and an outline in
 * the same colour. Only the faces that don't touch another block of the shape are drawn, and only the edges where the
 * shape turns, so a wall of blocks shows as one shape. The shape is built in sections of 16 blocks a side, as the game
 * builds its terrain, and each frame draws each section where it is from the camera.
 */
final class OutlineMesh {
	/** How far a shape's sides stand out from the blocks, as MaLiLib's box sides do, so they don't flicker. */
	static final float EXPAND = 0.002f;
	private static final Direction[] DIRECTIONS = Direction.values();

	/**
	 * A shape's colour (each part 0 to 1), its sides' and lines' alpha, its lines' width in pixels, how far its sides
	 * stand out from the blocks ({@link #EXPAND} but where they must stand in front of other sides), and the pipelines
	 * that draw them, read when the shape is built: MaLiLib sets its pipelines after Building Planner's renderers are
	 * made.
	 */
	record Look(String name, float red, float green, float blue, float sidesAlpha, float linesAlpha, float lineWidth,
		float expand, Supplier<RenderPipeline> sides, Supplier<RenderPipeline> lines) {
	}

	/** A section's blocks, and its shape's sides and lines, counted from the section's lowest corner. */
	static final class Section {
		final LongOpenHashSet blocks = new LongOpenHashSet();
		private RenderContext sides;
		private RenderContext lines;

		void close() {
			closeQuietly(sides);
			closeQuietly(lines);
			sides = null;
			lines = null;
		}
	}

	private OutlineMesh() {
	}

	/** The sections of {@code shape}, by {@link SectionPos#asLong}, each holding its blocks, not yet built. */
	static void split(LongSet shape, Long2ObjectMap<Section> sections) {
		for (LongIterator it = shape.iterator(); it.hasNext();) {
			long key = it.nextLong();
			sections.computeIfAbsent(SectionPos.blockToSection(key), k -> new Section()).blocks.add(key);
		}
	}

	/** Draws every section's sides, then every section's lines. */
	static void draw(Long2ObjectMap<Section> sections) {
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
	 * Builds a section's part of {@code shape}: the sides of its blocks that touch no other block of the shape, and the
	 * edges where the shape turns.
	 */
	static void build(long sectionKey, Section section, LongSet shape, Look look) {
		section.close();
		int originX = SectionPos.sectionToBlockCoord(SectionPos.x(sectionKey));
		int originY = SectionPos.sectionToBlockCoord(SectionPos.y(sectionKey));
		int originZ = SectionPos.sectionToBlockCoord(SectionPos.z(sectionKey));
		RenderContext sideContext = new RenderContext(() -> "buildingplanner:" + look.name() + "_sides",
			look.sides().get(), 0);
		RenderContext lineContext = new RenderContext(() -> "buildingplanner:" + look.name() + "_lines",
			look.lines().get(), 0);
		BufferBuilder sideBuilder = sideContext.getBuilder();
		BufferBuilder lineBuilder = lineContext.getBuilder();
		for (LongIterator it = section.blocks.iterator(); it.hasNext();) {
			long key = it.nextLong();
			int x = BlockPos.getX(key) - originX;
			int y = BlockPos.getY(key) - originY;
			int z = BlockPos.getZ(key) - originZ;
			for (Direction side : DIRECTIONS) {
				if (shape.contains(BlockPos.offset(key, side))) {
					continue;
				}
				face(sideBuilder, x, y, z, side, look);
				for (Direction along : DIRECTIONS) {
					if (along.getAxis() != side.getAxis() && drawsEdge(shape, key, side, along)) {
						edge(lineBuilder, x, y, z, side, along, look);
					}
				}
			}
		}
		section.sides = upload(sideContext, sideBuilder);
		section.lines = upload(lineContext, lineBuilder);
	}

	/**
	 * Whether the face of the block at {@code key} on its {@code side}, which touches no block of the shape, draws its
	 * edge toward {@code along}: not where the face goes on flat into the next block's, and once for each edge two faces
	 * share, from the face whose side comes first in {@link Direction}'s order.
	 */
	private static boolean drawsEdge(LongSet shape, long key, Direction side, Direction along) {
		long next = BlockPos.offset(key, along);
		if (!shape.contains(next)) {
			// The shape turns away: the face toward along shares the edge.
			return side.ordinal() < along.ordinal();
		}
		if (!shape.contains(BlockPos.offset(next, side))) {
			// The next block's face goes on flat.
			return false;
		}
		// The shape turns in: the face of the block beyond, toward the opposite of along, shares the edge.
		return side.ordinal() < along.getOpposite().ordinal();
	}

	/** The block's face on {@code side}, its corners counter-clockwise seen from outside. */
	private static void face(BufferBuilder builder, int x, int y, int z, Direction side, Look look) {
		float lo = -look.expand();
		float hi = 1 + look.expand();
		float[][] corners = switch (side) {
			case UP -> new float[][] {{lo, hi, hi}, {hi, hi, hi}, {hi, hi, lo}, {lo, hi, lo}};
			case DOWN -> new float[][] {{lo, lo, lo}, {hi, lo, lo}, {hi, lo, hi}, {lo, lo, hi}};
			case NORTH -> new float[][] {{lo, lo, lo}, {lo, hi, lo}, {hi, hi, lo}, {hi, lo, lo}};
			case SOUTH -> new float[][] {{lo, lo, hi}, {hi, lo, hi}, {hi, hi, hi}, {lo, hi, hi}};
			case WEST -> new float[][] {{lo, lo, lo}, {lo, lo, hi}, {lo, hi, hi}, {lo, hi, lo}};
			case EAST -> new float[][] {{hi, lo, lo}, {hi, hi, lo}, {hi, hi, hi}, {hi, lo, hi}};
		};
		for (float[] corner : corners) {
			builder.addVertex(x + corner[0], y + corner[1], z + corner[2])
				.setColor(look.red(), look.green(), look.blue(), look.sidesAlpha());
		}
	}

	/** The block's edge between its {@code side} face and its {@code along} face. */
	private static void edge(BufferBuilder builder, int x, int y, int z, Direction side, Direction along, Look look) {
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
		builder.addVertex(x + from[0], y + from[1], z + from[2])
			.setColor(look.red(), look.green(), look.blue(), look.linesAlpha()).setLineWidth(look.lineWidth());
		builder.addVertex(x + to[0], y + to[1], z + to[2])
			.setColor(look.red(), look.green(), look.blue(), look.linesAlpha()).setLineWidth(look.lineWidth());
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

	private static void closeQuietly(RenderContext context) {
		if (context == null) {
			return;
		}
		try {
			context.close();
		} catch (Exception e) {
			BuildingPlanner.LOGGER.warn("Couldn't let go of an outline's buffers", e);
		}
	}
}
