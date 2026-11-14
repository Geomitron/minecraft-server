package dev.buildingplanner.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Checks the reader and writer against files Litematica's own code writes and reads. */
class LitematicFormatTest {
	@TempDir
	Path dir;

	@BeforeAll
	static void setUp() throws Exception {
		LitematicaOutsideTheGame.setUp();
	}

	/** A few block states with properties, then every block with a default state, for palettes past 16 entries. */
	private static List<BlockState> states(int count) {
		List<BlockState> states = new ArrayList<>(List.of(
			Blocks.COBBLESTONE.defaultBlockState(),
			Blocks.MOSSY_COBBLESTONE.defaultBlockState(),
			Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.TOP)));
		for (var block : BuiltInRegistries.BLOCK) {
			if (states.size() >= count) {
				break;
			}
			BlockState state = block.defaultBlockState();
			if (!state.isAir() && !states.contains(state)) {
				states.add(state);
			}
		}
		return states;
	}

	/** Fills each region's Litematica container with seeded random blocks (about a third air) and saves the file. */
	private Path writeWithLitematica(AreaSelection area, int stateCount, String fileName) {
		LitematicaSchematic schematic = LitematicaSchematic.createEmptySchematic(area, "Tester");
		assertNotNull(schematic);
		List<BlockState> states = states(stateCount);
		Random random = new Random(fileName.hashCode());
		for (String region : schematic.getAreaSizes().keySet()) {
			LitematicaBlockStateContainer container = schematic.getSubRegionContainer(region);
			Vec3i size = container.getSize();
			for (int y = 0; y < size.getY(); y++) {
				for (int z = 0; z < size.getZ(); z++) {
					for (int x = 0; x < size.getX(); x++) {
						if (random.nextInt(3) > 0) {
							container.set(x, y, z, states.get(random.nextInt(states.size())));
						}
					}
				}
			}
		}
		assertTrue(schematic.writeToFile(dir, fileName, true));
		return dir.resolve(fileName + LitematicaSchematic.FILE_EXTENSION);
	}

	private static AreaSelection area(Box... boxes) {
		AreaSelection area = new AreaSelection();
		area.setName("Test plan");
		for (Box box : boxes) {
			area.addSubRegionBox(box, false);
		}
		area.setExplicitOrigin(BlockPos.ZERO);
		return area;
	}

	private static CompoundTag readTag(Path file) throws IOException {
		return NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
	}

	/** Every block of every region, as Litematica's reader sees the file, matches the plan. */
	private static void assertSameBlocks(LitematicaSchematic schematic, Plan plan) {
		assertEquals(schematic.getAreaSizes().keySet().size(), plan.regions().size());
		for (String name : schematic.getAreaSizes().keySet()) {
			PlanRegion region = plan.region(name).orElseThrow();
			assertEquals(schematic.getSubRegionPosition(name), region.position(), "position of " + name);
			assertEquals(new Vec3i(schematic.getAreaSize(name).getX(), schematic.getAreaSize(name).getY(),
				schematic.getAreaSize(name).getZ()), region.size(), "size of " + name);
			LitematicaBlockStateContainer container = schematic.getSubRegionContainer(name);
			assertEquals(region.dimensions(), container.getSize(), "dimensions of " + name);
			BlockPos min = region.minCorner();
			Vec3i size = container.getSize();
			for (int y = 0; y < size.getY(); y++) {
				for (int z = 0; z < size.getZ(); z++) {
					for (int x = 0; x < size.getX(); x++) {
						assertEquals(container.get(x, y, z), region.get(min.offset(x, y, z)), name + " at " + x + "," + y + "," + z);
					}
				}
			}
		}
	}

	@Test
	void readsAndRewritesASmallLitematicaFile() throws IOException {
		Path file = writeWithLitematica(area(new Box(new BlockPos(0, 0, 0), new BlockPos(4, 2, 3), "Main")), 3, "small");
		CompoundTag written = readTag(file);
		Plan plan = LitematicFormat.read(written);

		assertSameBlocks(LitematicaSchematic.createFromFile(dir, "small"), plan);
		assertEquals(written, LitematicFormat.write(plan));
	}

	@Test
	void readsAndRewritesSeveralRegionsWithNegativeSizesAndLargePalettes() throws IOException {
		AreaSelection area = area(
			new Box(new BlockPos(0, 0, 0), new BlockPos(9, 4, 7), "Positive"),
			new Box(new BlockPos(3, 6, 2), new BlockPos(-5, 2, -6), "Negative"),
			new Box(new BlockPos(-20, -3, 15), new BlockPos(-17, -9, 11), "Mixed"));
		Path file = writeWithLitematica(area, 40, "regions");
		CompoundTag written = readTag(file);
		Plan plan = LitematicFormat.read(written);

		LitematicaSchematic schematic = LitematicaSchematic.createFromFile(dir, "regions");
		assertSameBlocks(schematic, plan);
		assertTrue(plan.region("Negative").orElseThrow().size().getX() < 0, "kept the negative size");
		assertTrue(plan.region("Positive").orElseThrow().palette().size() > 16, "palette needs more than 4 bits");
		assertEquals(written, LitematicFormat.write(plan));
	}

	@Test
	void litematicaReadsTheAddOnsFile() throws IOException {
		Path source = writeWithLitematica(area(
			new Box(new BlockPos(0, 0, 0), new BlockPos(6, 3, 6), "A"),
			new Box(new BlockPos(2, 1, 1), new BlockPos(-3, -2, -4), "B")), 25, "source");
		Plan plan = LitematicFormat.readFile(source);
		plan.region("A").orElseThrow().set(new BlockPos(1, 1, 1), Blocks.GLASS.defaultBlockState());

		LitematicFormat.writeFile(plan, dir.resolve("addon.litematic"));

		assertSameBlocks(LitematicaSchematic.createFromFile(dir, "addon"), plan);
		assertFalse(dir.resolve("addon.litematic.tmp").toFile().exists());
	}

	@Test
	void litematicaReadsANewPlanGrownTowardNegativeCoordinates() throws IOException {
		Plan plan = Plan.create("New plan", "Tester", BlockPos.ZERO, new Vec3i(3, 2, 3), 1000L);
		PlanRegion region = plan.firstRegion();
		BlockState stone = Blocks.STONE.defaultBlockState();
		BlockState glass = Blocks.GLASS.defaultBlockState();
		region.set(new BlockPos(0, 0, 0), stone);
		region.set(new BlockPos(2, 1, 2), stone);

		assertTrue(region.growToInclude(new BlockPos(-4, -1, -2), new BlockPos(-4, -1, -2)));
		region.set(new BlockPos(-4, -1, -2), glass);
		plan.refreshMetadata(2000L);
		LitematicFormat.writeFile(plan, dir.resolve("grown.litematic"));

		LitematicaSchematic schematic = LitematicaSchematic.createFromFile(dir, "grown");
		assertSameBlocks(schematic, plan);
		assertEquals(new BlockPos(-4, -1, -2), schematic.getSubRegionPosition("New plan"));
		assertEquals(new Vec3i(7, 3, 5), schematic.getSubRegionContainer("New plan").getSize());
		// Container coordinates count from the new minimum corner.
		assertEquals(stone, schematic.getSubRegionContainer("New plan").get(4, 1, 2));
		assertEquals(stone, schematic.getSubRegionContainer("New plan").get(6, 2, 4));
		assertEquals(glass, schematic.getSubRegionContainer("New plan").get(0, 0, 0));
		assertEquals(3, schematic.getMetadata().getTotalBlocks());
		assertEquals(7 * 3 * 5, schematic.getMetadata().getTotalVolume());
		assertEquals(new Vec3i(7, 3, 5), schematic.getMetadata().getEnclosingSize());
	}

	@Test
	void updatesBlockNamesFromOlderMinecraftVersions() {
		Plan plan = Plan.create("Old plan", "Tester", BlockPos.ZERO, new Vec3i(1, 1, 2), 1000L);
		plan.firstRegion().set(new BlockPos(0, 0, 1), Blocks.STONE.defaultBlockState());
		CompoundTag root = LitematicFormat.write(plan);
		// Minecraft 1.20.1 called short grass "minecraft:grass".
		root.putInt("MinecraftDataVersion", 3465);
		CompoundTag region = root.getCompoundOrEmpty("Regions").getCompoundOrEmpty("Old plan");
		region.getListOrEmpty("BlockStatePalette").getCompoundOrEmpty(1).putString("Name", "minecraft:grass");

		Plan read = LitematicFormat.read(root);

		assertEquals(Blocks.SHORT_GRASS.defaultBlockState(), read.firstRegion().get(new BlockPos(0, 0, 1)));
		assertTrue(read.firstRegion().get(BlockPos.ZERO).isAir());
	}
}
