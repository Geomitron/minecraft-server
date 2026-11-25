package dev.buildingplanner.client;

import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.layer.WrongBlocks;
import fi.dy.masa.malilib.render.RenderUtils;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Arrays;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** The driver's reads of the wrong blocks drawn through walls, and the cost of finding them. */
final class WrongBlocksProbe {
	private WrongBlocksProbe() {
	}

	/** The wrong blocks the last frame drew, as "red x,y,z ... orange x,y,z ...", each sorted, or none. */
	static String drawn() {
		WrongBlocks.Found found = BuildingPlannerClient.wrongBlocksDrawn();
		return found.isEmpty() ? "none" : "red " + list(found.red()) + " orange " + list(found.orange());
	}

	private static String list(LongSet positions) {
		if (positions.isEmpty()) {
			return "none";
		}
		return Arrays.stream(positions.toLongArray()).sorted().mapToObj(BlockPos::of)
			.map(pos -> pos.getX() + "," + pos.getY() + "," + pos.getZ()).collect(Collectors.joining(" "));
	}

	/**
	 * The time {@link WrongBlocks#scan} takes, run {@code times} times on the client thread over a layer of its own
	 * with a ghost stone brick at every position within its range of the camera's block, against this game's real
	 * blocks: "N scans of B ghost blocks, R red and O orange: median X ms, slowest Y ms".
	 */
	static String scanCost(int times) {
		Minecraft mc = Minecraft.getInstance();
		BlockPos centre = BlockPos.containing(RenderUtils.camPos());
		Layer layer = new Layer();
		BlockState ghost = Blocks.STONE_BRICKS.defaultBlockState();
		int range = WrongBlocks.RANGE;
		BlockPos min = centre.offset(-range, -range, -range);
		for (BlockPos pos : BlockPos.betweenClosed(min, centre.offset(range, range, range))) {
			layer.set(pos, ghost);
		}
		long[] nanos = new long[times];
		WrongBlocks.Found found = WrongBlocks.Found.NONE;
		for (int i = 0; i < times; i++) {
			long start = System.nanoTime();
			found = WrongBlocks.scan(layer, mc.level::getBlockState, centre);
			nanos[i] = System.nanoTime() - start;
		}
		Arrays.sort(nanos);
		int blocks = (2 * range + 1) * (2 * range + 1) * (2 * range + 1);
		return times + " scans of " + blocks + " ghost blocks, " + found.red().size() + " red and "
			+ found.orange().size() + " orange: median " + millis(nanos[times / 2]) + " ms, slowest "
			+ millis(nanos[times - 1]) + " ms";
	}

	private static String millis(long nanos) {
		return String.format("%.1f", nanos / 1e6);
	}
}
