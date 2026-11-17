package dev.buildingplanner.client;

import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.layer.LayerColumn;
import dev.buildingplanner.layer.LayerSection;
import dev.buildingplanner.sync.LayerReplica;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/**
 * The game's own copy of the layer compared with a list of expected ghost blocks, for the import's check
 * ({@code clienttest/import.sh}). The game must stand where it has loaded the columns of every expected block.
 */
final class LayerProbe {
	private LayerProbe() {
	}

	/** The expected blocks in {@code file}, one {@code x y z state} a line. */
	private static Map<BlockPos, String> expected(Path file) throws IOException {
		Map<BlockPos, String> blocks = new LinkedHashMap<>();
		for (String line : Files.readAllLines(file)) {
			String[] a = line.split(" ", 4);
			if (a.length == 4) {
				blocks.put(new BlockPos(Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2])), a[3]);
			}
		}
		return blocks;
	}

	private static ChunkPos column(BlockPos pos) {
		return new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
	}

	/**
	 * How the game's layer compares with the expected blocks: "N of N blocks match, N in the layer, N of N columns
	 * held". It matches when every expected block is there with its state, the columns hold no other ghost block, and
	 * the game holds every column.
	 */
	static String compare(Path file) throws Exception {
		Map<BlockPos, String> expected = expected(file);
		LayerReplica replica = Driver.session().replica();
		Layer layer = replica.layer();
		Set<ChunkPos> columns = new LinkedHashSet<>();
		long matched = expected.entrySet().stream().filter(entry -> {
			columns.add(column(entry.getKey()));
			return BlockStateParser.serialize(layer.get(entry.getKey())).equals(entry.getValue());
		}).count();
		long inLayer = 0;
		for (ChunkPos pos : columns) {
			LayerColumn column = layer.column(pos.x(), pos.z());
			if (column != null) {
				for (LayerSection section : column.sections().values()) {
					inLayer += section.blocks();
				}
			}
		}
		return matched + " of " + expected.size() + " blocks match, " + inLayer + " in the layer, "
			+ columns.stream().filter(pos -> replica.holds(pos.x(), pos.z())).count() + " of " + columns.size()
			+ " columns held";
	}

	static boolean matches(Path file) throws Exception {
		int n = expected(file).size();
		String result = compare(file);
		return result.startsWith(n + " of " + n + " blocks match, " + n + " in the layer")
			&& result.matches(".* (\\d+) of \\1 columns held");
	}
}
