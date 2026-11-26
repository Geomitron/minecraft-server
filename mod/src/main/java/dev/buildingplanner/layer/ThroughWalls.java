package dev.buildingplanner.layer;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The ghost blocks drawn through walls ("Ghost blocks through walls"): each ghost block within {@value #RANGE} blocks
 * of the camera's block on every axis, in the colour Litematica's overlay gives it: light blue where its real block is
 * air or a fluid (missing), red where it is another block, and orange where it is the same block in another state. A
 * ghost block its real block matches draws nothing. Only the layer's sections that hold ghost blocks are read.
 */
public final class ThroughWalls {
	/** How far from the camera's block, on each axis, a ghost block is drawn. */
	public static final int RANGE = 32;

	/** A ghost block's colour through walls. */
	public enum Colour {
		LIGHT_BLUE, RED, ORANGE
	}

	/** The ghost blocks found, each colour's by {@link BlockPos#asLong}. */
	public record Found(LongSet lightBlue, LongSet red, LongSet orange) {
		public static final Found NONE = new Found(LongSets.EMPTY_SET, LongSets.EMPTY_SET, LongSets.EMPTY_SET);

		public boolean isEmpty() {
			return lightBlue.isEmpty() && red.isEmpty() && orange.isEmpty();
		}

		/** The positions of one colour. */
		public LongSet of(Colour colour) {
			return switch (colour) {
				case LIGHT_BLUE -> lightBlue;
				case RED -> red;
				case ORANGE -> orange;
			};
		}
	}

	private ThroughWalls() {
	}

	/** The colour a ghost block shows through walls over {@code real}, or null when it draws nothing. */
	public static Colour colour(BlockState ghost, BlockState real) {
		if (ghost.isAir() || ghost == real) {
			return null;
		}
		if (real.isAir() || real.liquid()) {
			return Colour.LIGHT_BLUE;
		}
		return ghost.getBlock() == real.getBlock() ? Colour.ORANGE : Colour.RED;
	}

	/** The ghost blocks of {@code layer} within {@link #RANGE} of {@code centre}, with {@code real} giving real blocks. */
	public static Found scan(Layer layer, Function<BlockPos, BlockState> real, BlockPos centre) {
		int minX = centre.getX() - RANGE;
		int maxX = centre.getX() + RANGE;
		int minY = centre.getY() - RANGE;
		int maxY = centre.getY() + RANGE;
		int minZ = centre.getZ() - RANGE;
		int maxZ = centre.getZ() + RANGE;
		LongOpenHashSet lightBlue = new LongOpenHashSet();
		LongOpenHashSet red = new LongOpenHashSet();
		LongOpenHashSet orange = new LongOpenHashSet();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int chunkX = minX >> LayerSection.SHIFT; chunkX <= maxX >> LayerSection.SHIFT; chunkX++) {
			for (int chunkZ = minZ >> LayerSection.SHIFT; chunkZ <= maxZ >> LayerSection.SHIFT; chunkZ++) {
				LayerColumn column = layer.column(chunkX, chunkZ);
				if (column == null) {
					continue;
				}
				int x0 = Math.max(minX, chunkX << LayerSection.SHIFT);
				int x1 = Math.min(maxX, (chunkX << LayerSection.SHIFT) + LayerSection.SIDE - 1);
				int z0 = Math.max(minZ, chunkZ << LayerSection.SHIFT);
				int z1 = Math.min(maxZ, (chunkZ << LayerSection.SHIFT) + LayerSection.SIDE - 1);
				for (Int2ObjectMap.Entry<LayerSection> entry : column.sections()
					.subMap(minY >> LayerSection.SHIFT, (maxY >> LayerSection.SHIFT) + 1).int2ObjectEntrySet()) {
					LayerSection section = entry.getValue();
					int bottom = entry.getIntKey() << LayerSection.SHIFT;
					int y0 = Math.max(minY, bottom);
					int y1 = Math.min(maxY, bottom + LayerSection.SIDE - 1);
					for (int y = y0; y <= y1; y++) {
						for (int z = z0; z <= z1; z++) {
							for (int x = x0; x <= x1; x++) {
								BlockState ghost = section.get(LayerSection.index(x, y, z));
								if (ghost.isAir()) {
									continue;
								}
								Colour colour = colour(ghost, real.apply(pos.set(x, y, z)));
								if (colour == null) {
									continue;
								}
								(switch (colour) {
									case LIGHT_BLUE -> lightBlue;
									case RED -> red;
									case ORANGE -> orange;
								}).add(pos.asLong());
							}
						}
					}
				}
			}
		}
		return lightBlue.isEmpty() && red.isEmpty() && orange.isEmpty() ? Found.NONE
			: new Found(lightBlue, red, orange);
	}

	/** The positions in one of {@code before} and {@code after} but not the other: those that joined or left a shape. */
	public static LongSet changed(LongSet before, LongSet after) {
		LongOpenHashSet changed = new LongOpenHashSet();
		for (LongIterator it = before.iterator(); it.hasNext();) {
			long key = it.nextLong();
			if (!after.contains(key)) {
				changed.add(key);
			}
		}
		for (LongIterator it = after.iterator(); it.hasNext();) {
			long key = it.nextLong();
			if (!before.contains(key)) {
				changed.add(key);
			}
		}
		return changed;
	}

	/**
	 * The 16-block sections, by {@link SectionPos#asLong}, whose part of a shape a change at {@code changed} may change:
	 * each position's own section, and, for a position on a section's side, edge or corner, the sections beside it,
	 * whose blocks' faces and edges against it may change.
	 */
	public static LongSet sectionsTouched(LongSet changed) {
		LongOpenHashSet touched = new LongOpenHashSet();
		for (LongIterator it = changed.iterator(); it.hasNext();) {
			long key = it.nextLong();
			int x = BlockPos.getX(key);
			int y = BlockPos.getY(key);
			int z = BlockPos.getZ(key);
			int sectionX = SectionPos.blockToSectionCoord(x);
			int sectionY = SectionPos.blockToSectionCoord(y);
			int sectionZ = SectionPos.blockToSectionCoord(z);
			int fromX = (x & 15) == 0 ? -1 : 0;
			int toX = (x & 15) == 15 ? 1 : 0;
			int fromY = (y & 15) == 0 ? -1 : 0;
			int toY = (y & 15) == 15 ? 1 : 0;
			int fromZ = (z & 15) == 0 ? -1 : 0;
			int toZ = (z & 15) == 15 ? 1 : 0;
			for (int dx = fromX; dx <= toX; dx++) {
				for (int dy = fromY; dy <= toY; dy++) {
					for (int dz = fromZ; dz <= toZ; dz++) {
						touched.add(SectionPos.asLong(sectionX + dx, sectionY + dy, sectionZ + dz));
					}
				}
			}
		}
		return touched;
	}
}
