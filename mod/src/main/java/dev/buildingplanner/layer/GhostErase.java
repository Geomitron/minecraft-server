package dev.buildingplanner.layer;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Erasing ghost blocks, which leaves real blocks as they are: an erase edit names air only where the layer holds a ghost
 * block, with the other half of each two-block ghost block ({@link OtherHalf}), as breaking either half of a real door
 * breaks both. The Delete box erases every ghost block in it, and a shape drawn with Effortless Building's break click
 * (hook E2) those among its positions.
 */
public final class GhostErase {
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();
	/** No real blocks, for counting the ghost blocks alone. */
	private static final BoxCount.Sections NO_REAL = (x, y, z) -> null;

	private GhostErase() {
	}

	/**
	 * Hook E2's filter: air for each of {@code positions} that holds a ghost block in {@code layer}, in their order, and
	 * for the other half of each such two-block ghost block.
	 */
	public static Map<BlockPos, BlockState> of(Layer layer, Iterable<BlockPos> positions) {
		Map<BlockPos, BlockState> erase = new LinkedHashMap<>();
		for (BlockPos pos : positions) {
			BlockState ghost = layer.get(pos);
			if (!ghost.isAir()) {
				add(layer, pos.immutable(), ghost, erase);
			}
		}
		return erase;
	}

	/**
	 * The Delete box's erase edit: air for each ghost block in {@code layer} in the box from {@code min} to {@code max}
	 * (both corners inside it), and for the other half of each such two-block ghost block, inside the box or not.
	 */
	public static Map<BlockPos, BlockState> inBox(Layer layer, BlockPos min, BlockPos max) {
		Map<BlockPos, BlockState> erase = new LinkedHashMap<>();
		layer.forEachIn(min, max, (pos, ghost) -> add(layer, pos, ghost, erase));
		return erase;
	}

	private static void add(Layer layer, BlockPos pos, BlockState ghost, Map<BlockPos, BlockState> erase) {
		erase.put(pos, AIR);
		BlockPos other = OtherHalf.of(pos, ghost);
		if (other != null && OtherHalf.matches(ghost, layer.get(other))) {
			erase.put(other, AIR);
		}
	}

	/** How many of each ghost block the box from {@code min} to {@code max} holds, for the Delete box's list. */
	public static Object2IntOpenHashMap<BlockState> counts(Layer layer, BlockPos min, BlockPos max) {
		return BoxCount.of(min, max, layer, NO_REAL, true);
	}

	/**
	 * Whether the box from {@code min} to {@code max} holds more than {@code cap} ghost blocks, which the Delete box can't
	 * erase in one edit. A section the box takes in whole counts from its number of ghost blocks; the count stops once
	 * it passes the cap.
	 */
	public static boolean overCap(Layer layer, BlockPos min, BlockPos max, int cap) {
		long count = 0;
		for (int sx = min.getX() >> LayerSection.SHIFT; sx <= max.getX() >> LayerSection.SHIFT; sx++) {
			int x0 = Math.max(min.getX(), sx << LayerSection.SHIFT);
			int x1 = Math.min(max.getX(), (sx << LayerSection.SHIFT) + LayerSection.SIDE - 1);
			for (int sz = min.getZ() >> LayerSection.SHIFT; sz <= max.getZ() >> LayerSection.SHIFT; sz++) {
				LayerColumn column = layer.column(sx, sz);
				if (column == null) {
					continue;
				}
				int z0 = Math.max(min.getZ(), sz << LayerSection.SHIFT);
				int z1 = Math.min(max.getZ(), (sz << LayerSection.SHIFT) + LayerSection.SIDE - 1);
				for (Int2ObjectMap.Entry<LayerSection> entry : column.sections().int2ObjectEntrySet()) {
					int bottom = entry.getIntKey() << LayerSection.SHIFT;
					int y0 = Math.max(min.getY(), bottom);
					int y1 = Math.min(max.getY(), bottom + LayerSection.SIDE - 1);
					if (y0 > y1) {
						continue;
					}
					LayerSection section = entry.getValue();
					if (x1 - x0 == LayerSection.SIDE - 1 && y1 - y0 == LayerSection.SIDE - 1
						&& z1 - z0 == LayerSection.SIDE - 1) {
						count += section.blocks();
					} else {
						for (int y = y0; y <= y1; y++) {
							for (int z = z0; z <= z1; z++) {
								for (int x = x0; x <= x1; x++) {
									if (!section.get(LayerSection.index(x, y, z)).isAir()) {
										count++;
									}
								}
							}
						}
					}
					if (count > cap) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
