package dev.buildingplanner.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.endte.syncmatica.data.ServerPlacement;
import ch.endte.syncmatica.extended_core.PlayerIdentifier;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.layer.LayerColumn;
import dev.buildingplanner.layer.LayerFiles;
import dev.buildingplanner.layer.LayerSection;
import dev.buildingplanner.plan.LitematicFormat;
import dev.buildingplanner.plan.LitematicaOutsideTheGame;
import dev.buildingplanner.plan.Plan;
import dev.buildingplanner.plan.PlanRegion;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.PositionUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The import against files Building Planner, Litematica and Syncmatica write themselves: a {@code placements.json}
 * from Syncmatica's own {@code ServerPlacement.toJson}, plans in Building Planner's own files and in Syncmatica's
 * stored files (written by Litematica), turned, mirrored and overlapping, one with a moved and turned sub-region.
 * Each block is compared with where Litematica's own code places it ({@code LitematicaSchematic.placeBlocksToWorld}).
 */
class PlanImportTest {
	// Before the block states below, which need the game's registries.
	static {
		try {
			LitematicaOutsideTheGame.setUp();
		} catch (Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static final Identifier OVERWORLD = Identifier.withDefaultNamespace("overworld");
	private static final Identifier NETHER = Identifier.withDefaultNamespace("the_nether");
	private static final BlockState BRICKS = Blocks.STONE_BRICKS.defaultBlockState();
	private static final BlockState COBBLE = Blocks.COBBLESTONE.defaultBlockState();
	private static final BlockState GLASS = Blocks.GLASS.defaultBlockState();
	private static final BlockState GOLD = Blocks.GOLD_BLOCK.defaultBlockState();
	private static final BlockState STAIRS = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
	private static final BlockState LOG = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
	private static final BlockState VOID = Blocks.STRUCTURE_VOID.defaultBlockState();

	@TempDir
	Path dir;

	/** A shared plan in the fixture: its placement, and the file its blocks come from. */
	private record Shared(String name, long timeCreated, ServerPlacement placement, Path file) {
	}

	/** Writes the fixture's world ({@code root/world}) and server folder ({@code root/server}); returns its plans. */
	private static List<Shared> writeFixture(Path root) throws IOException {
		Path world = root.resolve("world");
		Path plans = Files.createDirectories(world.resolve("building-planner").resolve("plans"));
		Path stored = Files.createDirectories(root.resolve("server").resolve("syncmatics"));
		List<Shared> shared = new ArrayList<>();

		// Hall: Building Planner's own file, turned a quarter.
		Plan hall = Plan.create("Hall", "Tester", BlockPos.ZERO, new Vec3i(5, 3, 4), 1000);
		PlanRegion region = hall.firstRegion();
		region.set(new BlockPos(0, 0, 0), BRICKS);
		region.set(new BlockPos(1, 0, 0), BRICKS);
		region.set(new BlockPos(3, 0, 0), GLASS);
		region.set(new BlockPos(2, 0, 1), BRICKS);
		region.set(new BlockPos(4, 0, 3), STAIRS);
		region.set(new BlockPos(1, 1, 1), LOG);
		region.set(new BlockPos(2, 2, 2), VOID);
		UUID hallId = UUID.fromString("00000000-0000-4000-8000-000000000001");
		LitematicFormat.writeFile(hall, plans.resolve(hallId + ".litematic"));
		shared.add(new Shared("Hall", 1000, placement(hallId, "Hall", UUID.randomUUID())
			.move(OVERWORLD.toString(), new BlockPos(4, -60, 6), Rotation.CLOCKWISE_90, Mirror.NONE),
			plans.resolve(hallId + ".litematic")));

		// Tower: over Hall, mirrored and turned half; a checkerboard, so its air leaves some of Hall's blocks and its
		// blocks replace others. Its stale stored file (gold) must not be read, since the plan has its own.
		Plan tower = Plan.create("Tower", "Tester", new BlockPos(-6, 0, -6), new Vec3i(13, 2, 13), 2000);
		for (int x = -6; x <= 6; x++) {
			for (int z = -6; z <= 6; z++) {
				if ((x + z & 1) == 0) {
					tower.firstRegion().set(new BlockPos(x, 0, z), COBBLE);
				}
			}
		}
		tower.firstRegion().set(new BlockPos(1, 1, 2), STAIRS);
		UUID towerId = UUID.fromString("00000000-0000-4000-8000-000000000002");
		UUID towerHash = UUID.fromString("00000000-0000-4000-8000-0000000000a2");
		LitematicFormat.writeFile(tower, plans.resolve(towerId + ".litematic"));
		writeWithLitematica(stored, towerHash.toString(), 0, GOLD,
			new Box(new BlockPos(0, 0, 0), new BlockPos(2, 1, 2), "Tower"));
		shared.add(new Shared("Tower", 2000, placement(towerId, "Tower", towerHash)
			.move(OVERWORLD.toString(), new BlockPos(4, -60, 6), Rotation.CLOCKWISE_180, Mirror.LEFT_RIGHT),
			plans.resolve(towerId + ".litematic")));

		// Yard: only Syncmatica's stored file, written by Litematica, with a region of negative size that a player moved,
		// turned and mirrored under the plan's quarter turn and mirror.
		UUID yardId = UUID.fromString("00000000-0000-4000-8000-000000000003");
		UUID yardHash = UUID.fromString("00000000-0000-4000-8000-0000000000a3");
		writeWithLitematica(stored, yardHash.toString(), 3000, null,
			new Box(new BlockPos(0, 0, 0), new BlockPos(5, 1, 4), "Ground"),
			new Box(new BlockPos(3, 4, 2), new BlockPos(-1, 2, -2), "Shed"));
		ServerPlacement yard = placement(yardId, "Yard", yardHash)
			.move(OVERWORLD.toString(), new BlockPos(-20, -60, -10), Rotation.COUNTERCLOCKWISE_90, Mirror.FRONT_BACK);
		yard.getSubRegionData().modify("Shed", new BlockPos(2, 3, 8), Rotation.CLOCKWISE_90, Mirror.LEFT_RIGHT);
		shared.add(new Shared("Yard", 3000, yard, stored.resolve(yardHash + ".litematic")));

		// Bridge: the Nether, the earliest plan.
		Plan bridge = Plan.create("Bridge", "Tester", BlockPos.ZERO, new Vec3i(3, 1, 1), 500);
		bridge.firstRegion().set(new BlockPos(0, 0, 0), BRICKS);
		bridge.firstRegion().set(new BlockPos(2, 0, 0), STAIRS);
		UUID bridgeId = UUID.fromString("00000000-0000-4000-8000-000000000004");
		LitematicFormat.writeFile(bridge, plans.resolve(bridgeId + ".litematic"));
		shared.add(new Shared("Bridge", 500, placement(bridgeId, "Bridge", UUID.randomUUID())
			.move(NETHER.toString(), new BlockPos(100, 70, 100), Rotation.NONE, Mirror.NONE),
			plans.resolve(bridgeId + ".litematic")));

		// A plan with no file anywhere, and one whose own file is damaged: both left out.
		ServerPlacement lost = placement(UUID.fromString("00000000-0000-4000-8000-000000000005"), "Lost", UUID.randomUUID())
			.move(OVERWORLD.toString(), BlockPos.ZERO, Rotation.NONE, Mirror.NONE);
		UUID damagedId = UUID.fromString("00000000-0000-4000-8000-000000000006");
		Files.writeString(plans.resolve(damagedId + ".litematic"), "not a plan");
		ServerPlacement damaged = placement(damagedId, "Damaged", UUID.randomUUID())
			.move(OVERWORLD.toString(), BlockPos.ZERO, Rotation.NONE, Mirror.NONE);

		// Syncmatica's list, as its SyncmaticManager saves it, in no particular order.
		JsonArray list = new JsonArray();
		for (ServerPlacement placement : List.of(yard, lost, shared.get(1).placement(), damaged, shared.get(0).placement(),
			shared.get(3).placement())) {
			list.add(placement.toJson());
		}
		JsonObject json = new JsonObject();
		json.add("placements", list);
		Files.createDirectories(world.resolve("syncmatica"));
		Files.writeString(world.resolve("syncmatica").resolve("placements.json"),
			new GsonBuilder().setPrettyPrinting().create().toJson(json));
		return shared;
	}

	private static ServerPlacement placement(UUID id, String name, UUID hash) {
		return new ServerPlacement(id, name + ".litematic", name, hash, PlayerIdentifier.MISSING_PLAYER);
	}

	/**
	 * Writes {@code <name>.litematic} with Litematica's own writer, created at {@code timeCreated} (now when 0): each
	 * region filled with {@code fill}, or with seeded random blocks (some air and structure void) when null.
	 */
	private static void writeWithLitematica(Path folder, String name, long timeCreated, BlockState fill, Box... boxes) {
		AreaSelection area = new AreaSelection();
		area.setName(name);
		for (Box box : boxes) {
			area.addSubRegionBox(box, false);
		}
		area.setExplicitOrigin(BlockPos.ZERO);
		LitematicaSchematic schematic = LitematicaSchematic.createEmptySchematic(area, "Tester");
		assertNotNull(schematic);
		if (timeCreated != 0) {
			schematic.getMetadata().setTimeCreated(timeCreated);
		}
		List<BlockState> states = List.of(BRICKS, GLASS, STAIRS, LOG, VOID, Blocks.OAK_PLANKS.defaultBlockState(),
			Blocks.AIR.defaultBlockState());
		Random random = new Random(name.hashCode());
		for (String region : schematic.getAreaSizes().keySet()) {
			LitematicaBlockStateContainer container = schematic.getSubRegionContainer(region);
			Vec3i size = container.getSize();
			for (int y = 0; y < size.getY(); y++) {
				for (int z = 0; z < size.getZ(); z++) {
					for (int x = 0; x < size.getX(); x++) {
						container.set(x, y, z, fill != null ? fill : states.get(random.nextInt(states.size())));
					}
				}
			}
		}
		assertTrue(schematic.writeToFile(folder, name, true));
	}

	/**
	 * Where Litematica's own code places each plan's blocks, plans in the order of their creation, each block written by
	 * the latest plan to hold one there: the arithmetic of {@code LitematicaSchematic.placeBlocksToWorld}, with
	 * Litematica's reader and its {@code PositionUtils}. Also counts the positions more than one plan writes.
	 */
	private static Map<Identifier, Map<BlockPos, BlockState>> litematicaPlaces(List<Shared> plans, int[] overlaps) {
		Map<Identifier, Map<BlockPos, BlockState>> worlds = new HashMap<>();
		List<Shared> ordered = new ArrayList<>(plans);
		ordered.sort((a, b) -> Long.compare(a.timeCreated(), b.timeCreated()));
		for (Shared shared : ordered) {
			ServerPlacement placement = shared.placement();
			Map<BlockPos, BlockState> blocks = worlds.computeIfAbsent(Identifier.parse(placement.getDimension()),
				dimension -> new LinkedHashMap<>());
			String fileName = shared.file().getFileName().toString().replace(LitematicaSchematic.FILE_EXTENSION, "");
			LitematicaSchematic schematic = LitematicaSchematic.createFromFile(shared.file().getParent(), fileName);
			assertNotNull(schematic, shared.name());
			BlockPos origin = placement.getPosition();
			Rotation mainRotation = placement.getRotation();
			Mirror mainMirror = placement.getMirror();
			for (String name : schematic.getAreaSizes().keySet()) {
				var move = placement.getSubRegionData().isModified()
					? placement.getSubRegionData().getModificationData().get(name) : null;
				BlockPos regionPos = move != null ? move.position : schematic.getSubRegionPosition(name);
				Rotation subRotation = move != null ? move.rotation : Rotation.NONE;
				Mirror subMirror = move != null ? move.mirror : Mirror.NONE;
				BlockPos regionSize = schematic.getAreaSizes().get(name);
				LitematicaBlockStateContainer container = schematic.getSubRegionContainer(name);

				BlockPos posEndRel = PositionUtils.getRelativeEndPositionFromAreaSize(regionSize).offset(regionPos);
				BlockPos posMinRel = PositionUtils.getMinCorner(regionPos, posEndRel);
				BlockPos regionPosTransformed = PositionUtils.getTransformedBlockPos(regionPos, mainMirror, mainRotation);
				Rotation rotationCombined = mainRotation.getRotated(subRotation);
				Mirror stateSubMirror = subMirror;
				if (subMirror != Mirror.NONE
					&& (mainRotation == Rotation.CLOCKWISE_90 || mainRotation == Rotation.COUNTERCLOCKWISE_90)) {
					stateSubMirror = subMirror == Mirror.FRONT_BACK ? Mirror.LEFT_RIGHT : Mirror.FRONT_BACK;
				}
				Vec3i size = container.getSize();
				for (int y = 0; y < size.getY(); y++) {
					for (int z = 0; z < size.getZ(); z++) {
						for (int x = 0; x < size.getX(); x++) {
							BlockState state = container.get(x, y, z);
							if (state.isAir() || state.getBlock() == Blocks.STRUCTURE_VOID) {
								continue;
							}
							BlockPos pos = new BlockPos(posMinRel.getX() + x - regionPos.getX(),
								posMinRel.getY() + y - regionPos.getY(), posMinRel.getZ() + z - regionPos.getZ());
							pos = PositionUtils.getTransformedBlockPos(pos, mainMirror, mainRotation);
							pos = PositionUtils.getTransformedBlockPos(pos, subMirror, subRotation);
							pos = pos.offset(regionPosTransformed).offset(origin);
							state = state.mirror(mainMirror).mirror(stateSubMirror).rotate(rotationCombined);
							if (blocks.put(pos, state) != null) {
								overlaps[0]++;
							}
						}
					}
				}
			}
		}
		return worlds;
	}

	private static long blockCount(Layer layer) {
		long blocks = 0;
		for (LayerColumn column : layer.columns().values()) {
			for (LayerSection section : column.sections().values()) {
				blocks += section.blocks();
			}
		}
		return blocks;
	}

	private static void assertSameBlocks(Map<Identifier, Map<BlockPos, BlockState>> expected, Map<Identifier, Layer> layers) {
		assertEquals(expected.keySet(), layers.keySet());
		expected.forEach((dimension, blocks) -> {
			Layer layer = layers.get(dimension);
			blocks.forEach((pos, state) -> assertEquals(state, layer.get(pos), dimension + " " + pos));
			assertEquals(blocks.size(), blockCount(layer), "ghost blocks in " + dimension);
		});
	}

	@Test
	void eachBlockGoesWhereLitematicaDrewItAndTheLaterPlanWins() throws IOException {
		List<Shared> plans = writeFixture(dir);
		int[] overlaps = {0};
		Map<Identifier, Map<BlockPos, BlockState>> expected = litematicaPlaces(plans, overlaps);

		PlanImport.Result result = PlanImport.read(dir.resolve("world"), dir.resolve("server"));

		assertSameBlocks(expected, result.layers());
		assertEquals(4, result.plans());
		assertEquals(6, result.shared());
		assertEquals(expected.values().stream().mapToLong(Map::size).sum(), result.blocks());
		assertTrue(overlaps[0] > 0, "the fixture's plans overlap");
		Map<BlockPos, BlockState> overworld = expected.get(OVERWORLD);
		assertTrue(overworld.containsValue(BRICKS) && overworld.containsValue(COBBLE),
			"Tower's air left some of Hall's blocks, and its blocks replaced others");
		assertFalse(overworld.containsValue(GOLD), "Tower came from its own file, not Syncmatica's");
		assertTrue(overworld.values().stream().anyMatch(state -> state.is(Blocks.OAK_STAIRS)
			&& state.getValue(StairBlock.FACING) != Direction.EAST), "stairs turned with their plan");
	}

	@Test
	void theImportWritesTheLayersFilesOnce() throws IOException {
		List<Shared> plans = writeFixture(dir);
		Map<Identifier, Map<BlockPos, BlockState>> expected = litematicaPlaces(plans, new int[1]);
		Path layerRoot = dir.resolve("world").resolve("building-planner").resolve("layer");
		// A crash part way through an earlier import left its staging folder.
		Files.createDirectories(layerRoot.resolveSibling("layer-import").resolve("minecraft").resolve("overworld"));
		Files.writeString(layerRoot.resolveSibling("layer-import").resolve("minecraft").resolve("overworld").resolve("9.9.dat"),
			"left over");

		PlanImport.run(dir.resolve("world"), dir.resolve("server"), layerRoot);

		assertFalse(Files.exists(layerRoot.resolveSibling("layer-import")));
		try (LayerFiles files = new LayerFiles(layerRoot)) {
			assertSameBlocks(expected, files.readAll());
		}
		assertFalse(Files.exists(layerRoot.resolve("minecraft").resolve("overworld").resolve("9.9.dat")));
		assertTrue(Files.exists(dir.resolve("world").resolve("syncmatica").resolve("placements.json")),
			"Syncmatica's files stay where they are");
	}

	@Test
	void withNoSharedPlansTheLayerStartsEmpty() throws IOException {
		Path layerRoot = dir.resolve("world").resolve("building-planner").resolve("layer");

		PlanImport.run(dir.resolve("world"), dir.resolve("server"), layerRoot);

		assertTrue(Files.isDirectory(layerRoot), "so the import doesn't run again");
		try (LayerFiles files = new LayerFiles(layerRoot)) {
			assertTrue(files.readAll().isEmpty());
		}
	}

	@Test
	void aPlacementItCantReadIsLeftOutAndAListItCantReadStopsTheImport() throws IOException {
		Path list = Files.createDirectories(dir.resolve("syncmatica")).resolve("placements.json");
		Files.writeString(list, """
			{"placements": [
			  {"id": "00000000-0000-4000-8000-000000000001", "hash": "00000000-0000-4000-8000-0000000000a1",
			   "origin": {"position": [1, 2, 3], "dimension": "minecraft:overworld"}, "rotation": "CLOCKWISE_90",
			   "mirror": "NONE", "subregionData": [{"name": "Main", "position": [4, 5, 6], "rotation": "NONE",
			   "mirror": "FRONT_BACK"}]},
			  {"id": "00000000-0000-4000-8000-000000000002", "origin": {"position": [1, 2], "dimension": "minecraft:overworld"}},
			  {"id": "00000000-0000-4000-8000-000000000003", "hash": "00000000-0000-4000-8000-0000000000a3",
			   "origin": {"position": [0, 0, 0], "dimension": "minecraft:overworld"}, "rotation": "SIDEWAYS",
			   "mirror": "NONE"}
			]}""");

		List<PlanImport.Placement> placements = PlanImport.readPlacements(list);

		assertEquals(1, placements.size());
		PlanImport.Placement placement = placements.getFirst();
		assertEquals(new BlockPos(1, 2, 3), placement.origin());
		assertEquals(Rotation.CLOCKWISE_90, placement.turn().rotation());
		assertEquals(new BlockPos(4, 5, 6), placement.moves().get("Main").position());
		assertEquals(Mirror.FRONT_BACK, placement.moves().get("Main").turn().mirror());

		Files.writeString(list, "{\"placements\": [");
		assertThrows(IOException.class, () -> PlanImport.readPlacements(list));
	}

	/**
	 * Writes the fixture to {@code build/import-fixture/} for the in-game check {@code clienttest/import.sh}: its world
	 * and server folders, and {@code expected.txt}, each overworld ghost block as {@code x y z state}.
	 */
	@Test
	void writesTheFixtureForTheDevServer() throws IOException {
		Path root = Path.of("build", "import-fixture");
		if (Files.exists(root)) {
			try (var walk = Files.walk(root)) {
				for (Path path : walk.sorted(java.util.Comparator.reverseOrder()).toList()) {
					Files.delete(path);
				}
			}
		}
		List<Shared> plans = writeFixture(root);
		StringBuilder lines = new StringBuilder();
		litematicaPlaces(plans, new int[1]).get(OVERWORLD).forEach((pos, state) -> lines.append(pos.getX()).append(' ')
			.append(pos.getY()).append(' ').append(pos.getZ()).append(' ').append(BlockStateParser.serialize(state))
			.append('\n'));
		Files.writeString(root.resolve("expected.txt"), lines);
	}
}
