package dev.buildingplanner.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.layer.LayerColumn;
import dev.buildingplanner.layer.LayerFiles;
import dev.buildingplanner.layer.LayerSection;
import dev.buildingplanner.plan.LitematicFormat;
import dev.buildingplanner.plan.Plan;
import dev.buildingplanner.plan.PlanRegion;
import dev.buildingplanner.plan.Turn;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The one-time import of every plan Syncmatica shared into the layer, hidden ones included, from Syncmatica's list of
 * placements and each plan's file; it needs no Syncmatica classes. Each block goes where Litematica drew it
 * ({@code LitematicaSchematic.placeBlocksToWorld}), and where plans overlap the later plan's blocks win. The files
 * read, and the order, are in {@code docs/building-planner.md}, "The import of today's plans".
 */
public final class PlanImport {
	/** The import writes to a folder beside the layer's with this suffix, then renames it. */
	private static final String STAGING_SUFFIX = "-import";

	private PlanImport() {
	}

	/** A sub-region a player moved or turned: its position from the plan's origin, before the plan's turn, and its own turn. */
	record RegionMove(BlockPos position, Turn turn) {
	}

	/** One entry of Syncmatica's list of placements. */
	record Placement(UUID id, UUID hash, Identifier dimension, BlockPos origin, Turn turn, Map<String, RegionMove> moves) {
	}

	/** The layer built from the shared plans, how many of them it took in, and its ghost blocks. */
	record Result(Map<Identifier, Layer> layers, int plans, int shared, long blocks) {
	}

	/**
	 * Builds the layer from the plans shared in {@code world} (its {@code syncmatica/placements.json}) and writes it to
	 * {@code layerRoot}, which must not exist yet: the files go to a staging folder beside it, which then takes its name,
	 * so a crash part way leaves no layer and the next start imports again. {@code serverDir} holds Syncmatica's stored
	 * files, {@code syncmatics/}. Fails when the list of placements or the layer's files can't be read or written.
	 */
	public static void run(Path world, Path serverDir, Path layerRoot) throws IOException {
		Path staging = layerRoot.resolveSibling(layerRoot.getFileName() + STAGING_SUFFIX);
		deleteTree(staging);
		Result result = read(world, serverDir);
		try (LayerFiles files = new LayerFiles(staging)) {
			result.layers().forEach(files::save);
		}
		Files.createDirectories(staging);
		Files.move(staging, layerRoot, StandardCopyOption.ATOMIC_MOVE);
		BuildingPlanner.LOGGER.info("Building Planner moved {} of {} shared plans into the layer: {} ghost blocks",
			result.plans(), result.shared(), result.blocks());
	}

	/** The layer built from the plans shared in {@code world}, without writing it. */
	static Result read(Path world, Path serverDir) throws IOException {
		List<Placement> placements = readPlacements(world.resolve("syncmatica").resolve("placements.json"));
		record Shared(Placement placement, Plan plan) {
		}
		List<Shared> plans = new ArrayList<>();
		for (Placement placement : placements) {
			Path own = world.resolve("building-planner").resolve("plans").resolve(placement.id() + ".litematic");
			Path file = Files.exists(own) ? own
				: serverDir.resolve("syncmatics").resolve(placement.hash() + ".litematic");
			try {
				plans.add(new Shared(placement, LitematicFormat.readFile(file)));
			} catch (IOException | RuntimeException e) {
				BuildingPlanner.LOGGER.error("Building Planner couldn't read plan {} from {}, so its blocks aren't in the "
					+ "layer", placement.id(), file, e);
			}
		}
		plans.sort(Comparator.comparingLong((Shared shared) -> shared.plan().timeCreated())
			.thenComparing(shared -> shared.placement().id().toString()));

		Map<Identifier, Layer> layers = new HashMap<>();
		for (Shared shared : plans) {
			Layer layer = layers.computeIfAbsent(shared.placement().dimension(), dimension -> new Layer());
			place(shared.plan(), shared.placement(), layer::set);
		}
		long blocks = 0;
		for (Layer layer : layers.values()) {
			for (LayerColumn column : layer.columns().values()) {
				for (LayerSection section : column.sections().values()) {
					blocks += section.blocks();
				}
			}
		}
		return new Result(layers, plans.size(), placements.size(), blocks);
	}

	interface BlockSink {
		void set(BlockPos pos, BlockState state);
	}

	/**
	 * Sends each block of the plan, other than air and structure void, to its world position and state as Litematica
	 * places it: from the region's position, mirrored and turned by the plan's placement and then by the region's own,
	 * plus the region's position turned by the plan's, plus the origin.
	 */
	static void place(Plan plan, Placement placement, BlockSink sink) {
		Turn main = placement.turn();
		for (PlanRegion region : plan.regions()) {
			RegionMove move = placement.moves().getOrDefault(region.name(), new RegionMove(region.position(), Turn.NONE));
			Turn sub = move.turn();
			BlockPos offset = main.apply(move.position()).offset(placement.origin());
			// Litematica swaps the region's mirror for its state when the plan's turn is a quarter.
			Mirror stateMirror = sub.mirror();
			if (stateMirror != Mirror.NONE
				&& (main.rotation() == Rotation.CLOCKWISE_90 || main.rotation() == Rotation.COUNTERCLOCKWISE_90)) {
				stateMirror = stateMirror == Mirror.FRONT_BACK ? Mirror.LEFT_RIGHT : Mirror.FRONT_BACK;
			}
			Mirror regionMirror = stateMirror;
			Rotation rotation = main.rotation().getRotated(sub.rotation());
			region.forEachSetBlock((pos, state) -> {
				if (state.isAir() || state.is(Blocks.STRUCTURE_VOID)) {
					return;
				}
				BlockPos world = sub.apply(main.apply(pos.subtract(region.position()))).offset(offset);
				sink.set(world, state.mirror(main.mirror()).mirror(regionMirror).rotate(rotation));
			});
		}
	}

	/**
	 * Syncmatica's list of placements ({@code ServerPlacement.toJson}); none when the file doesn't exist. An entry that
	 * lacks a field or has a bad value is logged and left out, as Syncmatica leaves it out.
	 */
	static List<Placement> readPlacements(Path file) throws IOException {
		if (!Files.exists(file)) {
			return List.of();
		}
		JsonElement root;
		try (Reader reader = Files.newBufferedReader(file)) {
			root = JsonParser.parseReader(reader);
		} catch (RuntimeException e) {
			throw new IOException("Couldn't parse " + file, e);
		}
		if (!root.isJsonObject() || !root.getAsJsonObject().has("placements")) {
			throw new IOException(file + " has no list of placements");
		}
		List<Placement> placements = new ArrayList<>();
		for (JsonElement entry : root.getAsJsonObject().getAsJsonArray("placements")) {
			try {
				placements.add(placement(entry.getAsJsonObject()));
			} catch (RuntimeException e) {
				BuildingPlanner.LOGGER.error("Building Planner left out a shared plan it couldn't read in {}: {}", file,
					entry, e);
			}
		}
		return placements;
	}

	private static Placement placement(JsonObject entry) {
		JsonObject origin = entry.getAsJsonObject("origin");
		Identifier dimension = Identifier.parse(origin.get("dimension").getAsString());
		Map<String, RegionMove> moves = new HashMap<>();
		if (entry.has("subregionData")) {
			for (JsonElement element : entry.getAsJsonArray("subregionData")) {
				JsonObject move = element.getAsJsonObject();
				moves.put(move.get("name").getAsString(), new RegionMove(position(move.getAsJsonArray("position")),
					turn(move)));
			}
		}
		return new Placement(UUID.fromString(entry.get("id").getAsString()), UUID.fromString(entry.get("hash").getAsString()),
			dimension, position(origin.getAsJsonArray("position")), turn(entry), moves);
	}

	private static Turn turn(JsonObject entry) {
		return new Turn(Mirror.valueOf(entry.get("mirror").getAsString()),
			Rotation.valueOf(entry.get("rotation").getAsString()));
	}

	private static BlockPos position(JsonArray array) {
		if (array.size() != 3) {
			throw new IllegalArgumentException("A position needs three coordinates: " + array);
		}
		return new BlockPos(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt());
	}

	private static void deleteTree(Path root) throws IOException {
		if (!Files.exists(root)) {
			return;
		}
		List<Path> paths;
		try (Stream<Path> walk = Files.walk(root)) {
			paths = walk.sorted(Comparator.reverseOrder()).toList();
		}
		for (Path path : paths) {
			Files.delete(path);
		}
	}
}
