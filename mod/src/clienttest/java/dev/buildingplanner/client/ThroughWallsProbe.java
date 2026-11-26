package dev.buildingplanner.client;

import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.layer.ThroughWalls;
import fi.dy.masa.malilib.render.RenderUtils;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** The driver's reads of the ghost blocks drawn through walls, and the cost of finding them and building their shapes. */
final class ThroughWallsProbe {
	/** How many 1-block edits {@link #scanCost} times building the shapes again after. */
	private static final int EDITS = 20;

	private ThroughWallsProbe() {
	}

	/**
	 * The ghost blocks the last frame drew through walls, as "lightblue x,y,z ... red x,y,z ... orange x,y,z ...", each
	 * colour's sorted, or none.
	 */
	static String drawn() {
		ThroughWalls.Found found = BuildingPlannerClient.throughWallsDrawn();
		return found.isEmpty() ? "none" : "lightblue " + list(found.lightBlue()) + " red " + list(found.red())
			+ " orange " + list(found.orange());
	}

	private static String list(LongSet positions) {
		if (positions.isEmpty()) {
			return "none";
		}
		return Arrays.stream(positions.toLongArray()).sorted().mapToObj(BlockPos::of)
			.map(pos -> pos.getX() + "," + pos.getY() + "," + pos.getZ()).collect(Collectors.joining(" "));
	}

	/**
	 * The time {@link ThroughWalls#scan} takes, run {@code times} times on the client thread over a layer of its own
	 * with a ghost stone brick at every position within its range of the camera's block, against this game's real
	 * blocks; then the time building the shapes of what it found takes whole, and again after each of {@value #EDITS}
	 * 1-block edits spread over the range (a ghost block erased, then drawn again), rescanned first: "N scans of B ghost
	 * blocks, L light blue, R red and O orange: median X ms, slowest Y ms; shapes built whole in W ms; after a 1-block
	 * edit, M ms mean, K ms longest".
	 */
	static String scanCost(int times) {
		Minecraft mc = Minecraft.getInstance();
		BlockPos centre = BlockPos.containing(RenderUtils.camPos());
		Layer layer = new Layer();
		BlockState ghost = Blocks.STONE_BRICKS.defaultBlockState();
		int range = ThroughWalls.RANGE;
		BlockPos min = centre.offset(-range, -range, -range);
		for (BlockPos pos : BlockPos.betweenClosed(min, centre.offset(range, range, range))) {
			layer.set(pos, ghost);
		}
		long[] nanos = new long[times];
		ThroughWalls.Found found = ThroughWalls.Found.NONE;
		for (int i = 0; i < times; i++) {
			long start = System.nanoTime();
			found = ThroughWalls.scan(layer, mc.level::getBlockState, centre);
			nanos[i] = System.nanoTime() - start;
		}
		Arrays.sort(nanos);
		int blocks = (2 * range + 1) * (2 * range + 1) * (2 * range + 1);
		String scans = String.format(Locale.ROOT, "%d scans of %d ghost blocks, %d light blue, %d red and %d orange:"
			+ " median %.1f ms, slowest %.1f ms", times, blocks, found.lightBlue().size(), found.red().size(),
			found.orange().size(), nanos[times / 2] / 1e6, nanos[times - 1] / 1e6);

		ThroughWallsOutline outline = new ThroughWallsOutline(() -> null);
		try {
			long began = System.nanoTime();
			outline.rebuild(found);
			long whole = System.nanoTime() - began;
			long total = 0;
			long longest = 0;
			int edits = 0;
			for (int i = 0; i < EDITS; i++) {
				// Spread over the range, off the middle so the edits fall in different sections.
				BlockPos pos = centre.offset(-range + 1 + i * 3, -range + 2 + i * 3, -range + 3 + i * 3);
				for (BlockState state : new BlockState[] {Blocks.AIR.defaultBlockState(), ghost}) {
					layer.set(pos, state);
					ThroughWalls.Found again = ThroughWalls.scan(layer, mc.level::getBlockState, centre);
					long start = System.nanoTime();
					outline.rebuild(again);
					long took = System.nanoTime() - start;
					total += took;
					longest = Math.max(longest, took);
					edits++;
				}
			}
			return scans + String.format(Locale.ROOT, "; shapes built whole in %.1f ms; after a 1-block edit, %.2f ms"
				+ " mean, %.2f ms longest", whole / 1e6, total / 1e6 / edits, longest / 1e6);
		} finally {
			outline.close();
		}
	}
}
