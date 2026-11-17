package dev.buildingplanner.client;

import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.layer.LayerColumn;
import dev.buildingplanner.layer.LayerSection;
import dev.buildingplanner.sync.Payloads;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/**
 * The server's layer as this game receives it in {@code columns}, for the import's check ({@code clienttest/import.sh}):
 * the driver asks for the columns of a list of expected ghost blocks and compares. It takes {@code columns} only when
 * Building Planner's client has no receiver for them, and keeps what arrives on the render thread.
 */
final class LayerProbe {
	private static final Layer LAYER = new Layer();
	private static final Set<ChunkPos> RECEIVED = new LinkedHashSet<>();

	private LayerProbe() {
	}

	/** Takes {@code columns}, once every mod has started; until then Building Planner's client may still add its own. */
	static void register() {
		if (!ClientPlayNetworking.getGlobalReceivers().contains(Payloads.Columns.TYPE.id())) {
			ClientPlayNetworking.registerGlobalReceiver(Payloads.Columns.TYPE, (payload, context) -> {
				for (Payloads.Column column : payload.columns()) {
					LAYER.replaceColumn(column.pos().x(), column.pos().z(), column.sections());
					RECEIVED.add(column.pos());
				}
			});
		}
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

	/** Asks the server for the columns of the expected blocks in the player's dimension; returns how many. */
	static int watch(Minecraft mc, Path file) throws IOException {
		Set<ChunkPos> columns = new LinkedHashSet<>();
		expected(file).keySet().forEach(pos -> columns.add(column(pos)));
		ClientPlayNetworking.send(new Payloads.Watch(mc.level.dimension().identifier(), List.copyOf(columns)));
		return columns.size();
	}

	/**
	 * How the received layer compares with the expected blocks: "N of N blocks match, N in the layer". It matches when
	 * every expected block is there with its state and the received columns hold no other ghost block.
	 */
	static String compare(Path file) throws IOException {
		Map<BlockPos, String> expected = expected(file);
		Set<ChunkPos> columns = new LinkedHashSet<>();
		long matched = expected.entrySet().stream().filter(entry -> {
			columns.add(column(entry.getKey()));
			return BlockStateParser.serialize(LAYER.get(entry.getKey())).equals(entry.getValue());
		}).count();
		long inLayer = 0;
		for (ChunkPos pos : columns) {
			LayerColumn column = LAYER.column(pos.x(), pos.z());
			if (column != null) {
				for (LayerSection section : column.sections().values()) {
					inLayer += section.blocks();
				}
			}
		}
		return matched + " of " + expected.size() + " blocks match, " + inLayer + " in the layer, "
			+ columns.stream().filter(RECEIVED::contains).count() + " of " + columns.size() + " columns received";
	}

	static boolean matches(Path file) throws IOException {
		Map<BlockPos, String> expected = expected(file);
		String result = compare(file);
		int n = expected.size();
		return result.startsWith(n + " of " + n + " blocks match, " + n + " in the layer");
	}
}
