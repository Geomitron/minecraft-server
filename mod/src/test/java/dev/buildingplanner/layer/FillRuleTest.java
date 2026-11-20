package dev.buildingplanner.layer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.WallSide;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The rule for a filled ghost block: each property it compares, and each it ignores. */
class FillRuleTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void theSameStateFillsAndAnotherBlockDoesNot() {
		BlockState stone = Blocks.STONE.defaultBlockState();
		assertTrue(FillRule.fills(stone, stone));
		assertFalse(FillRule.fills(Blocks.COBBLESTONE.defaultBlockState(), stone));
	}

	@Test
	void airFillsNothingAndNothingFillsAir() {
		BlockState air = Blocks.AIR.defaultBlockState();
		assertFalse(FillRule.fills(air, Blocks.STONE.defaultBlockState()));
		assertFalse(FillRule.fills(air, air));
		assertFalse(FillRule.fills(Blocks.STONE.defaultBlockState(), air));
	}

	@Test
	void aStairFacingAnotherWayStays() {
		BlockState north = Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
			Direction.NORTH);
		assertFalse(FillRule.fills(north.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), north));
	}

	@Test
	void anUpsideDownStairStays() {
		BlockState bottom = Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HALF, Half.BOTTOM);
		assertFalse(FillRule.fills(bottom.setValue(BlockStateProperties.HALF, Half.TOP), bottom));
	}

	@Test
	void aStairOfAnotherShapeOrWaterloggedFills() {
		BlockState straight = Blocks.OAK_STAIRS.defaultBlockState();
		assertTrue(FillRule.fills(straight.setValue(BlockStateProperties.STAIRS_SHAPE, StairsShape.OUTER_LEFT), straight));
		assertTrue(FillRule.fills(straight.setValue(BlockStateProperties.WATERLOGGED, true), straight));
	}

	@Test
	void aLogOnAnotherAxisStays() {
		BlockState upright = Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y);
		assertFalse(FillRule.fills(upright.setValue(BlockStateProperties.AXIS, Direction.Axis.X), upright));
		assertTrue(FillRule.fills(upright, upright));
	}

	@Test
	void aFenceWithOtherConnectionsFills() {
		BlockState alone = Blocks.OAK_FENCE.defaultBlockState();
		BlockState joined = alone.setValue(BlockStateProperties.NORTH, true).setValue(BlockStateProperties.EAST, true)
			.setValue(BlockStateProperties.WATERLOGGED, true);
		assertTrue(FillRule.fills(joined, alone));
		assertTrue(FillRule.fills(alone, joined));
	}

	@Test
	void aWallWithOtherConnectionsFills() {
		BlockState post = Blocks.COBBLESTONE_WALL.defaultBlockState();
		BlockState joined = post.setValue(BlockStateProperties.NORTH_WALL, WallSide.TALL)
			.setValue(BlockStateProperties.SOUTH_WALL, WallSide.LOW).setValue(BlockStateProperties.UP, false);
		assertTrue(FillRule.fills(joined, post));
	}

	@Test
	void aWaterloggedSlabFills() {
		BlockState bottom = Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
		assertTrue(FillRule.fills(bottom.setValue(BlockStateProperties.WATERLOGGED, true), bottom));
	}

	@Test
	void aTopSlabDoesNotFillABottomOne() {
		BlockState bottom = Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
		assertFalse(FillRule.fills(bottom.setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP), bottom));
		assertFalse(FillRule.fills(bottom.setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE), bottom));
	}

	@Test
	void aCropOfAnotherAgeFills() {
		BlockState seeded = Blocks.WHEAT.defaultBlockState().setValue(BlockStateProperties.AGE_7, 0);
		assertTrue(FillRule.fills(seeded.setValue(BlockStateProperties.AGE_7, 7), seeded));
	}

	@Test
	void aDoorWithTheOtherHingeStays() {
		BlockState left = Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.LEFT);
		assertFalse(FillRule.fills(left.setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.RIGHT), left));
	}

	@Test
	void anOpenOrPoweredDoorFillsButNotTheOtherHalf() {
		BlockState lower = Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
			DoubleBlockHalf.LOWER);
		assertTrue(FillRule.fills(lower.setValue(BlockStateProperties.OPEN, true)
			.setValue(BlockStateProperties.POWERED, true), lower));
		assertFalse(FillRule.fills(lower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), lower));
	}

	@Test
	void aBedsOtherPartStays() {
		BlockState foot = Blocks.STRAW_BED.defaultBlockState().setValue(BlockStateProperties.BED_PART, BedPart.FOOT);
		assertFalse(FillRule.fills(foot.setValue(BlockStateProperties.BED_PART, BedPart.HEAD), foot));
		assertTrue(FillRule.fills(foot.setValue(BlockStateProperties.OCCUPIED, true), foot));
	}

	@Test
	void aButtonOnAnotherFaceStays() {
		BlockState wall = Blocks.STONE_BUTTON.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL);
		assertFalse(FillRule.fills(wall.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR), wall));
	}

	@Test
	void aSignTurnedAnotherWayStays() {
		BlockState sign = Blocks.OAK_SIGN.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, 3);
		assertFalse(FillRule.fills(sign.setValue(BlockStateProperties.ROTATION_16, 4), sign));
	}

	@Test
	void aLitFurnaceFillsButOneFacingAnotherWayStays() {
		BlockState furnace = Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
			Direction.EAST);
		assertTrue(FillRule.fills(furnace.setValue(BlockStateProperties.LIT, true), furnace));
		assertFalse(FillRule.fills(furnace.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST), furnace));
	}

	@Test
	void aHopperPistonDripstoneAndCrafterFacingAnotherWayStay() {
		BlockState hopper = Blocks.HOPPER.defaultBlockState().setValue(BlockStateProperties.FACING_HOPPER, Direction.DOWN);
		assertFalse(FillRule.fills(hopper.setValue(BlockStateProperties.FACING_HOPPER, Direction.NORTH), hopper));
		BlockState piston = Blocks.PISTON.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP);
		assertFalse(FillRule.fills(piston.setValue(BlockStateProperties.FACING, Direction.DOWN), piston));
		assertTrue(FillRule.fills(piston.setValue(BlockStateProperties.EXTENDED, true), piston));
		BlockState tip = Blocks.POINTED_DRIPSTONE.defaultBlockState().setValue(BlockStateProperties.VERTICAL_DIRECTION,
			Direction.UP);
		assertFalse(FillRule.fills(tip.setValue(BlockStateProperties.VERTICAL_DIRECTION, Direction.DOWN), tip));
		BlockState crafter = Blocks.CRAFTER.defaultBlockState().setValue(BlockStateProperties.ORIENTATION,
			FrontAndTop.NORTH_UP);
		assertFalse(FillRule.fills(crafter.setValue(BlockStateProperties.ORIENTATION, FrontAndTop.SOUTH_UP), crafter));
	}
}
