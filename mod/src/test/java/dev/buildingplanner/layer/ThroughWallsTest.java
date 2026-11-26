package dev.buildingplanner.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Which ghost blocks show through walls, in which colour, how far from the camera, and which sections of their shapes
 * change between two scans.
 */
class ThroughWallsTest {
	private static final BlockPos CAMERA = new BlockPos(100, 0, 100);
	private static final BlockState BRICKS = Blocks.STONE_BRICKS.defaultBlockState();
	private static final BlockState COBBLESTONE = Blocks.COBBLESTONE.defaultBlockState();

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	private final Layer layer = new Layer();
	private final Map<BlockPos, BlockState> real = new HashMap<>();

	private void put(BlockPos pos, BlockState ghost, BlockState realBlock) {
		layer.set(pos, ghost);
		real.put(pos, realBlock);
	}

	private ThroughWalls.Found scan() {
		return ThroughWalls.scan(layer, pos -> real.getOrDefault(pos, Blocks.AIR.defaultBlockState()), CAMERA);
	}

	private static LongSet positions(BlockPos... positions) {
		LongOpenHashSet set = new LongOpenHashSet();
		for (BlockPos pos : positions) {
			set.add(pos.asLong());
		}
		return set;
	}

	private static LongSet sections(int... xyz) {
		LongOpenHashSet set = new LongOpenHashSet();
		for (int i = 0; i < xyz.length; i += 3) {
			set.add(SectionPos.asLong(xyz[i], xyz[i + 1], xyz[i + 2]));
		}
		return set;
	}

	@Test
	void airWaterAndLavaAreLightBlueAnotherBlockRedAndTheSameBlockFacingAnotherWayOrange() {
		BlockState stairs = Blocks.OAK_STAIRS.defaultBlockState();
		BlockPos air = CAMERA.offset(1, 0, 0);
		BlockPos water = CAMERA.offset(2, 0, 0);
		BlockPos lava = CAMERA.offset(3, 0, 0);
		BlockPos cobblestone = CAMERA.offset(4, 0, 0);
		BlockPos stairFacingNorth = CAMERA.offset(5, 0, 0);
		put(air, BRICKS, Blocks.AIR.defaultBlockState());
		put(water, BRICKS, Blocks.WATER.defaultBlockState());
		put(lava, BRICKS, Blocks.LAVA.defaultBlockState());
		put(cobblestone, BRICKS, COBBLESTONE);
		put(stairFacingNorth, stairs.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST),
			stairs.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
		// The matching block leaves its ghost block out, and a real block with no ghost block is nothing to draw.
		put(CAMERA.offset(6, 0, 0), BRICKS, BRICKS);
		real.put(CAMERA.offset(7, 0, 0), COBBLESTONE);

		ThroughWalls.Found found = scan();

		assertEquals(positions(air, water, lava), found.lightBlue());
		assertEquals(positions(cobblestone), found.red());
		assertEquals(positions(stairFacingNorth), found.orange());
	}

	@Test
	void eachColourIsLitematicasForItsRealBlock() {
		assertEquals(ThroughWalls.Colour.LIGHT_BLUE, ThroughWalls.colour(BRICKS, Blocks.AIR.defaultBlockState()));
		assertEquals(ThroughWalls.Colour.LIGHT_BLUE, ThroughWalls.colour(BRICKS, Blocks.CAVE_AIR.defaultBlockState()));
		assertEquals(ThroughWalls.Colour.LIGHT_BLUE, ThroughWalls.colour(BRICKS, Blocks.WATER.defaultBlockState()));
		assertEquals(ThroughWalls.Colour.LIGHT_BLUE, ThroughWalls.colour(BRICKS, Blocks.LAVA.defaultBlockState()));
		assertEquals(ThroughWalls.Colour.RED, ThroughWalls.colour(BRICKS, COBBLESTONE));
		assertEquals(null, ThroughWalls.colour(BRICKS, BRICKS));
		assertEquals(null, ThroughWalls.colour(Blocks.AIR.defaultBlockState(), COBBLESTONE));
	}

	@Test
	void onlyBlocksWithin32OfTheCameraOnEveryAxisShow() {
		BlockPos ten = CAMERA.offset(0, 0, -10);
		BlockPos edge = CAMERA.offset(32, -32, 32);
		BlockPos tenOverAir = CAMERA.offset(0, 0, 10);
		BlockPos edgeOverAir = CAMERA.offset(-32, 32, -32);
		for (BlockPos pos : new BlockPos[] {ten, edge, CAMERA.offset(-33, 0, 0), CAMERA.offset(0, 33, 0),
			CAMERA.offset(0, 0, 33)}) {
			put(pos, BRICKS, COBBLESTONE);
		}
		for (BlockPos pos : new BlockPos[] {tenOverAir, edgeOverAir, CAMERA.offset(33, 0, 0),
			CAMERA.offset(0, -33, 0), CAMERA.offset(0, 0, -33)}) {
			layer.set(pos, BRICKS);
		}

		ThroughWalls.Found found = scan();

		assertEquals(positions(ten, edge), found.red());
		assertEquals(positions(tenOverAir, edgeOverAir), found.lightBlue());
	}

	@Test
	void noGhostBlockThatDrawsFindsNone() {
		put(CAMERA, BRICKS, BRICKS);

		assertEquals(ThroughWalls.Found.NONE, scan());
	}

	@Test
	void theChangedPositionsAreThoseThatJoinedOrLeft() {
		BlockPos stays = CAMERA.offset(1, 0, 0);
		BlockPos leaves = CAMERA.offset(2, 0, 0);
		BlockPos joins = CAMERA.offset(3, 0, 0);

		assertEquals(positions(leaves, joins), ThroughWalls.changed(positions(stays, leaves), positions(stays, joins)));
		assertTrue(ThroughWalls.changed(positions(stays), positions(stays)).isEmpty());
	}

	@Test
	void aPositionInsideASectionTouchesItsSectionAlone() {
		assertEquals(sections(6, 0, 6), ThroughWalls.sectionsTouched(positions(new BlockPos(100, 5, 100))));
	}

	@Test
	void aPositionOnASectionsSideEdgeOrCornerTouchesTheSectionsBesideIt() {
		// x 96 is the lowest of section 6, so section 5 beside it shares the face.
		assertEquals(sections(6, 0, 6, 5, 0, 6), ThroughWalls.sectionsTouched(positions(new BlockPos(96, 5, 100))));
		// x 111 and y 15 are the highest of their sections: an edge, touching 4 sections.
		assertEquals(sections(6, 0, 6, 7, 0, 6, 6, 1, 6, 7, 1, 6),
			ThroughWalls.sectionsTouched(positions(new BlockPos(111, 15, 100))));
		// A corner touches 8.
		assertEquals(8, ThroughWalls.sectionsTouched(positions(new BlockPos(96, 0, 96))).size());
		// Below y 0, the section is -1, and y -16 is its lowest.
		assertEquals(sections(6, -1, 6, 6, -2, 6), ThroughWalls.sectionsTouched(positions(new BlockPos(100, -16, 100))));
	}

	@Test
	void betweenTwoScansOnlyTheSectionsOfTheChangedBlocksChange() {
		// A light blue wall across two sections, a red block in a third, and a block built into the wall between the
		// scans: the built block leaves light blue and joins red, so each shape changes in its section alone.
		for (int x = 100; x <= 120; x++) {
			for (int y = 1; y <= 4; y++) {
				layer.set(new BlockPos(x, y, 100), BRICKS);
			}
		}
		BlockPos wrong = new BlockPos(100, 1, 130);
		put(wrong, BRICKS, COBBLESTONE);
		ThroughWalls.Found before = scan();
		BlockPos built = new BlockPos(118, 2, 100);
		real.put(built, COBBLESTONE);

		ThroughWalls.Found after = scan();

		LongSet lightBlue = ThroughWalls.changed(before.lightBlue(), after.lightBlue());
		LongSet red = ThroughWalls.changed(before.red(), after.red());
		assertEquals(positions(built), lightBlue);
		assertEquals(positions(built), red);
		assertTrue(ThroughWalls.changed(before.orange(), after.orange()).isEmpty());
		assertEquals(sections(7, 0, 6), ThroughWalls.sectionsTouched(lightBlue));
		assertEquals(sections(7, 0, 6), ThroughWalls.sectionsTouched(red));
		assertEquals(positions(wrong, built), after.red());

		// Erasing a ghost block on a section's side changes the sections on both sides of it.
		layer.set(new BlockPos(112, 3, 100), Blocks.AIR.defaultBlockState());
		ThroughWalls.Found erased = scan();
		assertEquals(sections(6, 0, 6, 7, 0, 6),
			ThroughWalls.sectionsTouched(ThroughWalls.changed(after.lightBlue(), erased.lightBlue())));
	}
}
