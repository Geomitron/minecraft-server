package dev.buildingplanner.layer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.plan.LitematicFormat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LayerFilesTest {
	private static final Identifier OVERWORLD = Identifier.withDefaultNamespace("overworld");
	private static final Identifier NETHER = Identifier.withDefaultNamespace("the_nether");
	private static BlockState stone;
	private static BlockState stairs;

	@TempDir
	Path world;
	private Path root;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		stone = Blocks.STONE.defaultBlockState();
		stairs = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
	}

	@BeforeEach
	void setUp() {
		root = world.resolve("building-planner").resolve("layer");
	}

	@Test
	void aLayerRoundTripsThroughItsFiles() throws IOException {
		Map<BlockPos, BlockState> overworld = new HashMap<>();
		overworld.put(new BlockPos(0, -64, 0), stone);
		overworld.put(new BlockPos(511, 319, 511), stairs);
		overworld.put(new BlockPos(-1, 70, -1), stairs);
		overworld.put(new BlockPos(-512, 2031, 513), stone);
		overworld.put(new BlockPos(-513, -2032, 0), Blocks.GLASS.defaultBlockState());
		overworld.put(new BlockPos(29_999_999, 64, -29_999_999), stone);
		Map<BlockPos, BlockState> nether = Map.of(new BlockPos(8, 40, 8), Blocks.NETHERRACK.defaultBlockState());

		try (LayerFiles files = new LayerFiles(root)) {
			files.save(OVERWORLD, layerOf(overworld));
			files.save(NETHER, layerOf(nether));
		}

		assertTrue(Files.exists(root.resolve("minecraft/overworld/0.0.dat")));
		assertTrue(Files.exists(root.resolve("minecraft/overworld/-1.-1.dat")));
		assertTrue(Files.exists(root.resolve("minecraft/overworld/-1.1.dat")));
		assertTrue(Files.exists(root.resolve("minecraft/overworld/-2.0.dat")));
		assertTrue(Files.exists(root.resolve("minecraft/overworld/58593.-58594.dat")));
		assertTrue(Files.exists(root.resolve("minecraft/the_nether/0.0.dat")));
		Map<Identifier, Layer> read = new LayerFiles(root).readAll();
		assertEquals(Set.of(OVERWORLD, NETHER), read.keySet());
		assertEquals(overworld, blocksOf(read.get(OVERWORLD)));
		assertEquals(nether, blocksOf(read.get(NETHER)));
	}

	@Test
	void writesTheLayoutTheSpecDescribes() throws IOException {
		Layer layer = layerOf(Map.of(new BlockPos(-1, -1, 33), stairs));
		try (LayerFiles files = new LayerFiles(root)) {
			files.save(OVERWORLD, layer);
		}
		CompoundTag contents = net.minecraft.nbt.NbtIo.readCompressed(root.resolve("minecraft/overworld/-1.0.dat"),
			net.minecraft.nbt.NbtAccounter.unlimitedHeap());
		assertEquals(LayerFiles.VERSION, contents.getIntOr("version", -1));
		CompoundTag column = contents.getListOrEmpty("columns").getCompoundOrEmpty(0);
		assertEquals(-1, column.getIntOr("x", 0));
		assertEquals(2, column.getIntOr("z", 0));
		CompoundTag section = column.getListOrEmpty("sections").getCompoundOrEmpty(0);
		assertEquals(-1, section.getIntOr("y", 0));
		ListTag palette = section.getListOrEmpty("palette");
		assertEquals("minecraft:air", palette.getCompoundOrEmpty(0).getStringOr("Name", ""));
		assertEquals("minecraft:oak_stairs", palette.getCompoundOrEmpty(1).getStringOr("Name", ""));
		assertEquals("west", palette.getCompoundOrEmpty(1).getCompoundOrEmpty("Properties").getStringOr("facing", ""));
		assertEquals(LayerSection.VOLUME * 2 / 64, section.getLongArray("blocks").orElseThrow().length);
	}

	@Test
	void deletesTheFileOfAnAreaLeftEmpty() throws IOException {
		Layer layer = layerOf(Map.of(new BlockPos(1, 1, 1), stone, new BlockPos(2, 1, 1), stone,
			new BlockPos(600, 1, 1), stone));
		Path emptied = root.resolve("minecraft/overworld/0.0.dat");
		Path kept = root.resolve("minecraft/overworld/1.0.dat");
		try (LayerFiles files = new LayerFiles(root)) {
			files.save(OVERWORLD, layer);
			layer.set(new BlockPos(1, 1, 1), Blocks.AIR.defaultBlockState());
			files.save(OVERWORLD, layer);
		}
		assertTrue(Files.exists(emptied), "an area with a ghost block left keeps its file");

		try (LayerFiles files = new LayerFiles(root)) {
			layer.set(new BlockPos(2, 1, 1), Blocks.AIR.defaultBlockState());
			files.save(OVERWORLD, layer);
		}
		assertFalse(Files.exists(emptied));
		assertTrue(Files.exists(kept));
		assertEquals(Map.of(new BlockPos(600, 1, 1), stone), blocksOf(new LayerFiles(root).readAll().get(OVERWORLD)));
	}

	@Test
	void savesOnlyTheAreasThatChanged() throws IOException {
		Layer layer = layerOf(Map.of(new BlockPos(1, 1, 1), stone, new BlockPos(600, 1, 1), stone));
		try (LayerFiles files = new LayerFiles(root)) {
			files.save(OVERWORLD, layer);
		}
		Path untouched = root.resolve("minecraft/overworld/1.0.dat");
		Files.write(untouched, new byte[] {1, 2, 3});
		try (LayerFiles files = new LayerFiles(root)) {
			layer.set(new BlockPos(1, 2, 1), stone);
			files.save(OVERWORLD, layer);
		}
		assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(untouched));
	}

	@Test
	void aPartialFileIsLeftOutAndNeverOverwritten() throws IOException {
		Layer layer = layerOf(Map.of(new BlockPos(1, 1, 1), stone, new BlockPos(600, 1, 1), stairs));
		try (LayerFiles files = new LayerFiles(root)) {
			files.save(OVERWORLD, layer);
		}
		Path partial = root.resolve("minecraft/overworld/0.0.dat");
		byte[] whole = Files.readAllBytes(partial);
		byte[] half = Arrays.copyOf(whole, whole.length / 2);
		Files.write(partial, half);

		try (LayerFiles files = new LayerFiles(root)) {
			Layer read = files.readAll().get(OVERWORLD);
			assertEquals(Map.of(new BlockPos(600, 1, 1), stairs), blocksOf(read));
			read.set(new BlockPos(1, 1, 1), stone);
			read.set(new BlockPos(600, 2, 1), stone);
			files.save(OVERWORLD, read);
		}
		assertArrayEquals(half, Files.readAllBytes(partial), "an edit never overwrites an unreadable file");
		assertEquals(Map.of(new BlockPos(600, 1, 1), stairs, new BlockPos(600, 2, 1), stone),
			blocksOf(new LayerFiles(root).readAll().get(OVERWORLD)));
	}

	@Test
	void aDamagedFileAddsNothing() throws IOException {
		Layer layer = layerOf(Map.of(new BlockPos(1, 1, 1), stone, new BlockPos(40, 1, 1), stone));
		CompoundTag contents = LayerFiles.writeArea(layer, 0, 0).orElseThrow();
		// The second column claims a chunk outside the file's area.
		contents.getListOrEmpty("columns").getCompoundOrEmpty(1).putInt("x", 32);
		Path file = root.resolve("minecraft/overworld/0.0.dat");
		Files.createDirectories(file.getParent());
		LitematicFormat.writeFile(contents, file);

		Map<Identifier, Layer> read = new LayerFiles(root).readAll();
		assertTrue(read.isEmpty(), "no ghost block from a damaged file");
		assertTrue(Files.exists(file));
	}

	@Test
	void skipsFilesThatArentAreas() throws IOException {
		Layer layer = layerOf(Map.of(new BlockPos(1, 1, 1), stone));
		try (LayerFiles files = new LayerFiles(root)) {
			files.save(OVERWORLD, layer);
		}
		Path dir = root.resolve("minecraft/overworld");
		Files.copy(dir.resolve("0.0.dat"), dir.resolve("notes.dat"));
		Files.copy(dir.resolve("0.0.dat"), dir.resolve("00.0.dat"));
		Files.write(dir.resolve("0.0.dat.tmp"), new byte[] {1});
		Files.copy(dir.resolve("0.0.dat"), root.resolve("stray.dat"));

		Map<Identifier, Layer> read = new LayerFiles(root).readAll();
		assertEquals(Map.of(new BlockPos(1, 1, 1), stone), blocksOf(read.get(OVERWORLD)));
		assertEquals(Set.of(OVERWORLD), read.keySet());
	}

	@Test
	void noFolderReadsAsAnEmptyLayer() throws IOException {
		assertTrue(new LayerFiles(root).readAll().isEmpty());
	}

	private static Layer layerOf(Map<BlockPos, BlockState> blocks) {
		Layer layer = new Layer();
		blocks.forEach(layer::set);
		return layer;
	}

	/** Every ghost block in a layer, by world position. */
	private static Map<BlockPos, BlockState> blocksOf(Layer layer) {
		Map<BlockPos, BlockState> blocks = new HashMap<>();
		layer.columns().forEach((key, column) -> column.sections().forEach((y, section) -> {
			for (int index = 0; index < LayerSection.VOLUME; index++) {
				BlockState state = section.get(index);
				if (!state.isAir()) {
					blocks.put(new BlockPos(Layer.keyX(key) * 16 + (index & 15), y * 16 + (index >> 8),
						Layer.keyZ(key) * 16 + (index >> 4 & 15)), state);
				}
			}
		}));
		return blocks;
	}
}
