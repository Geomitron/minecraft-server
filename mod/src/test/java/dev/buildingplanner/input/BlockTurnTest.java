package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Turning and flipping the block in hand: quarter turns, the flips' order, the turn on other blocks, and dropping it. */
class BlockTurnTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	/** An oak stair facing north, bottom half; made after the bootstrap, which must come before {@code Blocks} loads. */
	private static BlockState stair() {
		return Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
			.setValue(BlockStateProperties.HALF, Half.BOTTOM);
	}

	/** The state after a quarter turn of {@code placed}, as Minecraft would place it, clockwise or not. */
	private static BlockState turned(BlockState placed, boolean clockwise) {
		return BlockTurn.apply(placed, BlockTurn.turned(BlockTurn.start(placed, Direction.SOUTH), placed, clockwise));
	}

	/** The states {@code placed} takes over {@code flips} flips in a row, each read with {@code read}. */
	private static <T> List<T> flips(BlockState placed, Direction toward, int flips, Function<BlockState, T> read) {
		List<T> seen = new ArrayList<>();
		BlockTurn.Turn turn = BlockTurn.start(placed, toward);
		for (int i = 0; i < flips; i++) {
			turn = BlockTurn.flipped(turn, placed);
			seen.add(read.apply(BlockTurn.apply(placed, turn)));
		}
		return seen;
	}

	@Test
	void aStairTurnsAQuarterEachWay() {
		assertEquals(Direction.EAST, turned(stair(), true).getValue(BlockStateProperties.HORIZONTAL_FACING));
		assertEquals(Direction.WEST, turned(stair(), false).getValue(BlockStateProperties.HORIZONTAL_FACING));
	}

	@Test
	void aLogTurnsBetweenEastWestAndNorthSouth() {
		BlockState log = Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X);
		assertEquals(Direction.Axis.Z, turned(log, true).getValue(BlockStateProperties.AXIS));
		assertEquals(Direction.Axis.Z, turned(log, false).getValue(BlockStateProperties.AXIS));
	}

	@Test
	void aChestTurnsAQuarterEachWay() {
		BlockState chest = Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
			Direction.SOUTH);
		assertEquals(Direction.WEST, turned(chest, true).getValue(BlockStateProperties.HORIZONTAL_FACING));
		assertEquals(Direction.EAST, turned(chest, false).getValue(BlockStateProperties.HORIZONTAL_FACING));
	}

	@Test
	void aSignTurnsAQuarterOfItsSixteenRotations() {
		BlockState sign = Blocks.OAK_SIGN.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, 0);
		assertEquals(4, turned(sign, true).getValue(BlockStateProperties.ROTATION_16));
		assertEquals(12, turned(sign, false).getValue(BlockStateProperties.ROTATION_16));
	}

	@Test
	void twoTurnsAddUp() {
		BlockTurn.Turn turn = BlockTurn.start(stair(), Direction.SOUTH);
		turn = BlockTurn.turned(BlockTurn.turned(turn, stair(), true), stair(), true);
		assertEquals(Direction.SOUTH, BlockTurn.apply(stair(), turn).getValue(BlockStateProperties.HORIZONTAL_FACING));
	}

	@Test
	void aLogFlipsUpAndBack() {
		BlockState log = Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Z);
		assertEquals(List.of(Direction.Axis.Y, Direction.Axis.Z, Direction.Axis.Y),
			flips(log, Direction.SOUTH, 3, state -> state.getValue(BlockStateProperties.AXIS)));
	}

	@Test
	void aStairFlipsToItsTopHalfAndBack() {
		assertEquals(List.of(Half.TOP, Half.BOTTOM),
			flips(stair(), Direction.SOUTH, 2, state -> state.getValue(BlockStateProperties.HALF)));
	}

	@Test
	void aSlabFlipsBetweenBottomAndTopAndNeverToDouble() {
		BlockState slab = Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
		assertEquals(List.of(SlabType.TOP, SlabType.BOTTOM, SlabType.TOP),
			flips(slab, Direction.SOUTH, 3, state -> state.getValue(BlockStateProperties.SLAB_TYPE)));
		BlockState doubled = slab.setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE);
		assertEquals(List.of(SlabType.BOTTOM, SlabType.TOP),
			flips(doubled, Direction.SOUTH, 2, state -> state.getValue(BlockStateProperties.SLAB_TYPE)));
	}

	@Test
	void aPistonFlipsUpDownAndBackToItsTurn() {
		BlockState piston = Blocks.PISTON.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH);
		assertEquals(List.of(Direction.UP, Direction.DOWN, Direction.NORTH),
			flips(piston, Direction.SOUTH, 3, state -> state.getValue(BlockStateProperties.FACING)));
	}

	@Test
	void aPistonPlacedFacingUpFlipsToTheWayItWouldFaceOnItsSide() {
		BlockState piston = Blocks.PISTON.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP);
		assertEquals(List.of(Direction.DOWN, Direction.SOUTH, Direction.UP),
			flips(piston, Direction.SOUTH, 3, state -> state.getValue(BlockStateProperties.FACING)));
	}

	@Test
	void aTurnWhileFlippedUpTurnsTheWayItGoesBackTo() {
		BlockState piston = Blocks.PISTON.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH);
		BlockTurn.Turn turn = BlockTurn.flipped(BlockTurn.start(piston, Direction.SOUTH), piston);
		turn = BlockTurn.turned(turn, piston, true);
		assertEquals(Direction.UP, BlockTurn.apply(piston, turn).getValue(BlockStateProperties.FACING));
		turn = BlockTurn.flipped(BlockTurn.flipped(turn, piston), piston);
		assertEquals(Direction.EAST, BlockTurn.apply(piston, turn).getValue(BlockStateProperties.FACING));
	}

	@Test
	void aButtonFlipsFromTheFloorToAWallTheCeilingAndBack() {
		BlockState button = Blocks.OAK_BUTTON.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,
			AttachFace.FLOOR);
		assertEquals(List.of(AttachFace.WALL, AttachFace.CEILING, AttachFace.FLOOR),
			flips(button, Direction.SOUTH, 3, state -> state.getValue(BlockStateProperties.ATTACH_FACE)));
	}

	@Test
	void aLanternFlipsBetweenHangingAndStanding() {
		BlockState lantern = Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, false);
		assertEquals(List.of(true, false),
			flips(lantern, Direction.SOUTH, 2, state -> state.getValue(BlockStateProperties.HANGING)));
	}

	@Test
	void theTurnIsKeptAsValuesAndSetOnAnotherBlockOfTheMixThatHasThem() {
		BlockTurn.Turn turn = BlockTurn.flipped(BlockTurn.turned(BlockTurn.start(stair(), Direction.SOUTH), stair(), true),
			stair());
		assertEquals("east", turn.values().get("facing"));
		assertEquals("top", turn.values().get("half"));
		BlockState spruce = BlockTurn.apply(Blocks.SPRUCE_STAIRS.defaultBlockState(), turn);
		assertEquals(Direction.EAST, spruce.getValue(BlockStateProperties.HORIZONTAL_FACING));
		assertEquals(Half.TOP, spruce.getValue(BlockStateProperties.HALF));
		assertEquals(Direction.EAST,
			BlockTurn.apply(Blocks.PISTON.defaultBlockState(), turn).getValue(BlockStateProperties.FACING));
		BlockState stone = Blocks.STONE.defaultBlockState();
		assertSame(stone, BlockTurn.apply(stone, turn));
	}

	@Test
	void theTurnLeavesOtherPropertiesOfTheSameNameAndTheRest() {
		BlockTurn.Turn turn = BlockTurn.flipped(BlockTurn.start(stair(), Direction.SOUTH), stair());
		BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
			DoubleBlockHalf.LOWER);
		assertEquals(DoubleBlockHalf.LOWER,
			BlockTurn.apply(door, turn).getValue(BlockStateProperties.DOUBLE_BLOCK_HALF));
		BlockState wet = stair().setValue(BlockStateProperties.WATERLOGGED, true);
		assertTrue(BlockTurn.apply(wet, turn).getValue(BlockStateProperties.WATERLOGGED));
	}

	@Test
	void aBlockThatNeitherTurnsNorFlipsKeepsItsTurn() {
		assertFalse(BlockTurn.turns(Blocks.STONE));
		assertFalse(BlockTurn.flips(Blocks.STONE));
		BlockState stone = Blocks.STONE.defaultBlockState();
		BlockTurn.Turn turn = BlockTurn.start(stone, Direction.SOUTH);
		assertEquals(turn, BlockTurn.flipped(turn, stone));
		assertEquals(stone, BlockTurn.apply(stone, BlockTurn.turned(turn, stone, true)));
	}

	@Test
	void whichBlocksTurnAndWhichFlip() {
		assertTrue(BlockTurn.turns(Blocks.OAK_STAIRS));
		assertTrue(BlockTurn.flips(Blocks.OAK_STAIRS));
		assertTrue(BlockTurn.turns(Blocks.OAK_LOG));
		assertTrue(BlockTurn.flips(Blocks.OAK_LOG));
		assertTrue(BlockTurn.turns(Blocks.CHEST));
		assertFalse(BlockTurn.flips(Blocks.CHEST));
		assertFalse(BlockTurn.turns(Blocks.OAK_SLAB));
		assertTrue(BlockTurn.flips(Blocks.OAK_SLAB));
	}

	@Test
	void theTurnDropsWhenTheItemInHandChanges() {
		BlockTurn.Held held = new BlockTurn.Held(Items.OAK_STAIRS, BlockTurn.start(stair(), Direction.SOUTH));
		assertSame(held, held.follow(Items.OAK_STAIRS));
		assertSame(BlockTurn.Held.NONE, held.follow(Items.STONE));
		assertNull(BlockTurn.Held.NONE.follow(Items.OAK_STAIRS).turn());
	}
}
