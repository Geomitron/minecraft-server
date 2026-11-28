package dev.buildingplanner.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Checks a saved copy's file against Litematica's own reader. */
class CopyFileTest {
	@TempDir
	Path folder;

	@BeforeAll
	static void setUp() throws Exception {
		LitematicaOutsideTheGame.setUp();
	}

	/** A hut's corner at world positions: planks, a door's two halves and a wall torch, with empty space between. */
	private static Map<BlockPos, BlockState> hut() {
		Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
		BlockPos at = new BlockPos(100, -60, -40);
		for (int x = 0; x < 4; x++) {
			blocks.put(at.offset(x, 0, 0), Blocks.OAK_PLANKS.defaultBlockState());
			blocks.put(at.offset(x, 0, 2), Blocks.OAK_PLANKS.defaultBlockState());
		}
		BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH);
		blocks.put(at.offset(1, 1, 2), door);
		blocks.put(at.offset(1, 2, 2), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		blocks.put(at.offset(3, 2, 1), Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST));
		return blocks;
	}

	@Test
	void aCopyIsWrittenAndReadBackFromItsLowestCorner() throws IOException {
		Path file = CopyFile.path(folder, 1);
		CopyFile.write(file, hut(), "Building Planner copy", "Tester", 1000L);

		Map<BlockPos, BlockState> expected = new HashMap<>();
		hut().forEach((pos, state) -> expected.put(pos.subtract(new BlockPos(100, -60, -40)), state));
		assertEquals(expected, CopyFile.read(file));
		assertEquals(folder.resolve("1.litematic"), file);
		assertFalse(Files.exists(folder.resolve("1.litematic.tmp")), "written through a temporary file and a rename");
	}

	@Test
	void litematicaReadsTheCopyWithStructureVoidInItsEmptySpace() throws IOException {
		CopyFile.write(CopyFile.path(folder, 2), hut(), "Building Planner copy", "Tester", 1000L);

		LitematicaSchematic schematic = LitematicaSchematic.createFromFile(folder, "2");
		assertNotNull(schematic);
		LitematicaBlockStateContainer container = schematic.getSubRegionContainer(CopyFile.REGION);
		assertEquals(new Vec3i(4, 3, 3), container.getSize());
		assertEquals(Blocks.OAK_PLANKS.defaultBlockState(), container.get(0, 0, 0));
		assertEquals(Blocks.OAK_DOOR, container.get(1, 2, 2).getBlock());
		assertEquals(Direction.WEST, container.get(3, 2, 1).getValue(WallTorchBlock.FACING));
		assertEquals(Blocks.STRUCTURE_VOID.defaultBlockState(), container.get(0, 1, 1));
		assertEquals(11, schematic.getMetadata().getTotalBlocks());
	}

	@Test
	void aFileCutShortIsUnreadable() throws IOException {
		Path file = CopyFile.path(folder, 3);
		CopyFile.write(file, hut(), "Building Planner copy", "Tester", 1000L);
		byte[] bytes = Files.readAllBytes(file);
		Files.write(file, Arrays.copyOf(bytes, bytes.length / 2));

		assertThrows(Exception.class, () -> CopyFile.read(file));
	}
}
