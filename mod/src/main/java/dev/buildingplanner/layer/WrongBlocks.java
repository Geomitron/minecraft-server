package dev.buildingplanner.layer;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The ghost blocks drawn through walls ("Wrong blocks through walls"): each ghost block within {@value #RANGE} blocks
 * of the camera's block on every axis whose real block is another block (red) or the same block in another state
 * (orange), by the rule Litematica colours its overlay with. A real block of air or a fluid leaves the ghost block
 * missing, not wrong, so it isn't drawn. Only the layer's sections that hold ghost blocks are read.
 */
public final class WrongBlocks {
	/** How far from the camera's block, on each axis, a ghost block is drawn. */
	public static final int RANGE = 32;

	/** A wrong block's colour through walls. */
	public enum Colour {
		RED, ORANGE
	}

	/** The wrong blocks found, each by {@link BlockPos#asLong}. */
	public record Found(LongSet red, LongSet orange) {
		public static final Found NONE = new Found(LongSets.EMPTY_SET, LongSets.EMPTY_SET);

		public boolean isEmpty() {
			return red.isEmpty() && orange.isEmpty();
		}
	}

	private WrongBlocks() {
	}

	/** The colour a ghost block shows through walls over {@code real}, or null when it isn't wrong. */
	public static Colour colour(BlockState ghost, BlockState real) {
		if (ghost.isAir() || ghost == real || real.isAir() || real.liquid()) {
			return null;
		}
		return ghost.getBlock() == real.getBlock() ? Colour.ORANGE : Colour.RED;
	}

	/** The wrong blocks of {@code layer} within {@link #RANGE} of {@code centre}, with {@code real} giving real blocks. */
	public static Found scan(Layer layer, Function<BlockPos, BlockState> real, BlockPos centre) {
		int minX = centre.getX() - RANGE;
		int maxX = centre.getX() + RANGE;
		int minY = centre.getY() - RANGE;
		int maxY = centre.getY() + RANGE;
		int minZ = centre.getZ() - RANGE;
		int maxZ = centre.getZ() + RANGE;
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
								if (colour != null) {
									(colour == Colour.RED ? red : orange).add(pos.asLong());
								}
							}
						}
					}
				}
			}
		}
		return red.isEmpty() && orange.isEmpty() ? Found.NONE : new Found(red, orange);
	}
}
