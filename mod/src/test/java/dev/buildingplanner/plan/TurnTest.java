package dev.buildingplanner.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TurnTest {
	private static final List<BlockPos> POSITIONS = List.of(new BlockPos(0, 0, 0), new BlockPos(3, 1, 0),
		new BlockPos(0, 2, 5), new BlockPos(2, 0, 7), new BlockPos(-4, 3, 1));
	private static final List<Direction> FACINGS = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
	private static List<BlockState> states;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		states = new ArrayList<>();
		for (Direction facing : FACINGS) {
			for (StairsShape shape : List.of(StairsShape.STRAIGHT, StairsShape.INNER_LEFT, StairsShape.OUTER_RIGHT)) {
				states.add(Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing)
					.setValue(StairBlock.SHAPE, shape));
			}
		}
		for (Direction.Axis axis : Direction.Axis.values()) {
			states.add(Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
		}
	}

	private static List<Turn> allTurns() {
		List<Turn> turns = new ArrayList<>();
		for (Mirror mirror : Mirror.values()) {
			for (Rotation rotation : Rotation.values()) {
				turns.add(new Turn(mirror, rotation));
			}
		}
		return turns;
	}

	/** The flip left to right for a player facing {@code facing}, as positions and states see it. */
	private static Mirror flipFor(Direction facing) {
		return facing.getAxis() == Direction.Axis.Z ? Mirror.FRONT_BACK : Mirror.LEFT_RIGHT;
	}

	private static BlockPos flip(BlockPos pos, Direction facing) {
		return facing.getAxis() == Direction.Axis.Z ? new BlockPos(-pos.getX(), pos.getY(), pos.getZ())
			: new BlockPos(pos.getX(), pos.getY(), -pos.getZ());
	}

	@Test
	void placesPositionsAsLitematicaAndVanillaDo() {
		for (Turn turn : allTurns()) {
			for (BlockPos pos : POSITIONS) {
				assertEquals(StructureTemplate.transform(pos, turn.mirror(), turn.rotation(), BlockPos.ZERO), turn.apply(pos),
					turn + " at " + pos);
			}
		}
	}

	@Test
	void turnsAndMirrorsBlockStates() {
		BlockState north = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
		assertEquals(Direction.EAST, new Turn(Mirror.NONE, Rotation.CLOCKWISE_90).apply(north).getValue(StairBlock.FACING));
		assertEquals(Direction.SOUTH, new Turn(Mirror.LEFT_RIGHT, Rotation.NONE).apply(north).getValue(StairBlock.FACING));
		assertEquals(Direction.NORTH, new Turn(Mirror.FRONT_BACK, Rotation.NONE).apply(north).getValue(StairBlock.FACING));
		// Mirrored first, then turned: south, then a quarter turn clockwise.
		assertEquals(Direction.WEST,
			new Turn(Mirror.LEFT_RIGHT, Rotation.CLOCKWISE_90).apply(north).getValue(StairBlock.FACING));
		BlockState log = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
		assertEquals(Direction.Axis.Z,
			new Turn(Mirror.NONE, Rotation.COUNTERCLOCKWISE_90).apply(log).getValue(RotatedPillarBlock.AXIS));
	}

	@Test
	void turningAddsAQuarterTurnClockwiseAfterTheTurnSoFar() {
		Turn quarter = new Turn(Mirror.NONE, Rotation.CLOCKWISE_90);
		for (Turn turn : allTurns()) {
			Turn turned = turn.turnedClockwise();
			for (BlockPos pos : POSITIONS) {
				assertEquals(quarter.apply(turn.apply(pos)), turned.apply(pos), turn + " at " + pos);
			}
			for (BlockState state : states) {
				assertEquals(quarter.apply(turn.apply(state)), turned.apply(state), turn + " on " + state);
			}
		}
	}

	@Test
	void mirroringFlipsLeftToRightAfterTheTurnSoFar() {
		for (Turn turn : allTurns()) {
			for (Direction facing : FACINGS) {
				Turn flipped = turn.flippedLeftRight(facing);
				for (BlockPos pos : POSITIONS) {
					assertEquals(flip(turn.apply(pos), facing), flipped.apply(pos), turn + " facing " + facing + " at " + pos);
				}
				for (BlockState state : states) {
					assertEquals(turn.apply(state).mirror(flipFor(facing)), flipped.apply(state),
						turn + " facing " + facing + " on " + state);
				}
			}
		}
	}

	@Test
	void mirroringSwapsEastAndWestForAPlayerFacingNorth() {
		Turn flipped = Turn.NONE.flippedLeftRight(Direction.NORTH);
		assertEquals(new BlockPos(-3, 1, 2), flipped.apply(new BlockPos(3, 1, 2)));
		assertEquals(new BlockPos(3, 1, -2), Turn.NONE.flippedLeftRight(Direction.WEST).apply(new BlockPos(3, 1, 2)));
	}

	@Test
	void mirroringTwiceUndoesIt() {
		for (Turn turn : allTurns()) {
			for (Direction facing : FACINGS) {
				Turn twice = turn.flippedLeftRight(facing).flippedLeftRight(facing);
				for (BlockPos pos : POSITIONS) {
					assertEquals(turn.apply(pos), twice.apply(pos), turn + " facing " + facing);
				}
			}
		}
	}
}
