package dev.buildingplanner.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Which ghost blocks show through walls, in which colour, and how far from the camera. */
class WrongBlocksTest {
	private static final BlockPos CAMERA = new BlockPos(100, 0, 100);

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

	private WrongBlocks.Found scan() {
		return WrongBlocks.scan(layer, pos -> real.getOrDefault(pos, Blocks.AIR.defaultBlockState()), CAMERA);
	}

	private static LongSet positions(BlockPos... positions) {
		LongOpenHashSet set = new LongOpenHashSet();
		for (BlockPos pos : positions) {
			set.add(pos.asLong());
		}
		return set;
	}

	@Test
	void anotherBlockIsRedAndTheSameBlockFacingAnotherWayIsOrange() {
		BlockState stairs = Blocks.OAK_STAIRS.defaultBlockState();
		BlockPos cobblestone = CAMERA.offset(1, 0, 0);
		BlockPos stairFacingNorth = CAMERA.offset(2, 0, 0);
		put(cobblestone, Blocks.STONE_BRICKS.defaultBlockState(), Blocks.COBBLESTONE.defaultBlockState());
		put(stairFacingNorth, stairs.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST),
			stairs.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
		// The matching block, air, water and lava leave their ghost blocks out, and a real block with no ghost block
		// is nothing to draw.
		put(CAMERA.offset(3, 0, 0), Blocks.STONE_BRICKS.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState());
		put(CAMERA.offset(4, 0, 0), Blocks.STONE_BRICKS.defaultBlockState(), Blocks.AIR.defaultBlockState());
		put(CAMERA.offset(5, 0, 0), Blocks.STONE_BRICKS.defaultBlockState(), Blocks.WATER.defaultBlockState());
		put(CAMERA.offset(6, 0, 0), Blocks.STONE_BRICKS.defaultBlockState(), Blocks.LAVA.defaultBlockState());
		real.put(CAMERA.offset(7, 0, 0), Blocks.COBBLESTONE.defaultBlockState());

		WrongBlocks.Found found = scan();

		assertEquals(positions(cobblestone), found.red());
		assertEquals(positions(stairFacingNorth), found.orange());
	}

	@Test
	void onlyBlocksWithin32OfTheCameraOnEveryAxisShow() {
		BlockState ghost = Blocks.STONE_BRICKS.defaultBlockState();
		BlockState cobblestone = Blocks.COBBLESTONE.defaultBlockState();
		BlockPos ten = CAMERA.offset(0, 0, -10);
		BlockPos edge = CAMERA.offset(32, -32, 32);
		for (BlockPos pos : new BlockPos[] {ten, edge, CAMERA.offset(-33, 0, 0), CAMERA.offset(0, 33, 0),
			CAMERA.offset(0, 0, 33)}) {
			put(pos, ghost, cobblestone);
		}

		assertEquals(positions(ten, edge), scan().red());
	}

	@Test
	void noWrongBlockFindsNone() {
		put(CAMERA, Blocks.STONE_BRICKS.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState());

		assertEquals(WrongBlocks.Found.NONE, scan());
	}
}
