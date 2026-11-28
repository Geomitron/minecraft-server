package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.input.CopyPlacement.Spot;
import dev.buildingplanner.plan.Turn;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.Test;

class CopyPlacementTest {
	private static final BlockPos AIMED = new BlockPos(10, 64, -20);
	/** Odd along x, even along y and z. */
	private static final Vec3i ODD_EVEN = new Vec3i(3, 2, 4);
	/** Even along x, odd along y and z. */
	private static final Vec3i EVEN_ODD = new Vec3i(4, 5, 1);

	private static Spot spot(int x1, int y1, int z1, int x2, int y2, int z2) {
		return Spot.between(new BlockPos(x1, y1, z1), new BlockPos(x2, y2, z2));
	}

	@Test
	void restsOnATopFaceWithTheMiddleOfItsBottomOnTheBlockAbove() {
		// The block above is 10 65 -20: x 3 wide has its middle 1 in, z 4 deep the lower of its two middles, 1 in.
		assertEquals(spot(9, 65, -21, 11, 66, -18), CopyPlacement.againstFace(AIMED, Direction.UP, ODD_EVEN));
		// x 4 wide: the lower middle, 1 in; z 1 deep: 0 in.
		assertEquals(spot(9, 65, -20, 12, 69, -20), CopyPlacement.againstFace(AIMED, Direction.UP, EVEN_ODD));
	}

	@Test
	void hangsUnderABottomFace() {
		assertEquals(spot(9, 62, -21, 11, 63, -18), CopyPlacement.againstFace(AIMED, Direction.DOWN, ODD_EVEN));
		assertEquals(spot(9, 59, -20, 12, 63, -20), CopyPlacement.againstFace(AIMED, Direction.DOWN, EVEN_ODD));
	}

	@Test
	void sitsBesideEachSideFaceWithTheMiddleOfTheTouchingSideOnTheBlockBeside() {
		// North of the block, 10 64 -21: the copy ends there, its middle across x and up y on it.
		assertEquals(spot(9, 64, -24, 11, 65, -21), CopyPlacement.againstFace(AIMED, Direction.NORTH, ODD_EVEN));
		assertEquals(spot(9, 62, -21, 12, 66, -21), CopyPlacement.againstFace(AIMED, Direction.NORTH, EVEN_ODD));
		// South, from 10 64 -19.
		assertEquals(spot(9, 64, -19, 11, 65, -16), CopyPlacement.againstFace(AIMED, Direction.SOUTH, ODD_EVEN));
		assertEquals(spot(9, 62, -19, 12, 66, -19), CopyPlacement.againstFace(AIMED, Direction.SOUTH, EVEN_ODD));
		// East, from 11 64 -20.
		assertEquals(spot(11, 64, -21, 13, 65, -18), CopyPlacement.againstFace(AIMED, Direction.EAST, ODD_EVEN));
		assertEquals(spot(11, 62, -20, 14, 66, -20), CopyPlacement.againstFace(AIMED, Direction.EAST, EVEN_ODD));
		// West, ending at 9 64 -20.
		assertEquals(spot(7, 64, -21, 9, 65, -18), CopyPlacement.againstFace(AIMED, Direction.WEST, ODD_EVEN));
		assertEquals(spot(6, 62, -20, 9, 66, -20), CopyPlacement.againstFace(AIMED, Direction.WEST, EVEN_ODD));
	}

	@Test
	void showsInFrontOfThePlayerOnTheirLevelWhateverWayTheyFace() {
		BlockPos feet = new BlockPos(10, 64, -20);
		// Facing south, the near side is at z -18, two blocks on; x 3 wide has its middle on the feet's x.
		assertEquals(spot(9, 64, -18, 11, 65, -15), CopyPlacement.inFront(feet, Direction.SOUTH, ODD_EVEN));
		// Facing north, the near side is at z -22, and the copy reaches away from the player, to z -25.
		assertEquals(spot(9, 64, -25, 11, 65, -22), CopyPlacement.inFront(feet, Direction.NORTH, ODD_EVEN));
		// Facing east, from x 12; z 4 deep has the lower of its two middles on the feet's z.
		assertEquals(spot(12, 64, -21, 14, 65, -18), CopyPlacement.inFront(feet, Direction.EAST, ODD_EVEN));
		// Facing west, ending at x 8.
		assertEquals(spot(6, 64, -21, 8, 65, -18), CopyPlacement.inFront(feet, Direction.WEST, ODD_EVEN));
		// x 4 wide: the lower middle, 1 in; z 1 deep.
		assertEquals(spot(9, 64, -18, 12, 68, -18), CopyPlacement.inFront(feet, Direction.SOUTH, EVEN_ODD));
		assertEquals(spot(9, 64, -22, 12, 68, -22), CopyPlacement.inFront(feet, Direction.NORTH, EVEN_ODD));
		assertEquals(spot(12, 64, -20, 15, 68, -20), CopyPlacement.inFront(feet, Direction.EAST, EVEN_ODD));
		assertEquals(spot(5, 64, -20, 8, 68, -20), CopyPlacement.inFront(feet, Direction.WEST, EVEN_ODD));
	}

	@Test
	void inFrontNeverCoversThePlayerOrTheBlockBeforeThem() {
		BlockPos feet = new BlockPos(10, 64, -20);
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			for (Vec3i size : new Vec3i[] {new Vec3i(1, 1, 1), ODD_EVEN, EVEN_ODD, new Vec3i(64, 20, 64)}) {
				Spot spot = CopyPlacement.inFront(feet, facing, size);
				String name = facing + " " + size;
				assertEquals(64, spot.min().getY(), name);
				for (BlockPos clear : new BlockPos[] {feet, feet.above(), feet.relative(facing),
					feet.relative(facing).above()}) {
					assertTrue(!spot.contains(clear), name + " covers " + clear);
				}
				assertTrue(spot.contains(feet.relative(facing, 2)), name);
			}
		}
	}

	@Test
	void neverOverlapsTheAimedBlockAndTouchesItsFace() {
		for (Direction face : Direction.values()) {
			for (Vec3i size : new Vec3i[] {ODD_EVEN, EVEN_ODD, new Vec3i(1, 1, 1), new Vec3i(6, 3, 7)}) {
				Spot spot = CopyPlacement.againstFace(AIMED, face, size);
				assertTrue(!spot.contains(AIMED), face + " " + size);
				assertTrue(spot.contains(AIMED.relative(face)), face + " " + size);
			}
		}
	}

	@Test
	void aOneBlockCopySitsOnTheBlockBesideTheFace() {
		for (Direction face : Direction.values()) {
			assertEquals(new Spot(AIMED.relative(face), new Vec3i(1, 1, 1)),
				CopyPlacement.againstFace(AIMED, face, new Vec3i(1, 1, 1)), face.toString());
		}
	}

	@Test
	void eachTurnAndMirrorFillsTheTurnedBoxFromItsOrigin() {
		BlockPos min = new BlockPos(-7, 70, 12);
		for (Mirror mirror : Mirror.values()) {
			for (Rotation rotation : Rotation.values()) {
				Turn turn = new Turn(mirror, rotation);
				for (Vec3i size : new Vec3i[] {ODD_EVEN, EVEN_ODD}) {
					Vec3i turned = CopyPlacement.turnedSize(size, turn);
					BlockPos origin = CopyPlacement.origin(min, size, turn);
					Set<BlockPos> placed = new HashSet<>();
					for (BlockPos pos : BlockPos.betweenClosed(BlockPos.ZERO,
						new BlockPos(size.getX() - 1, size.getY() - 1, size.getZ() - 1))) {
						placed.add(origin.offset(turn.apply(pos)));
					}
					Set<BlockPos> box = new HashSet<>();
					for (BlockPos pos : BlockPos.betweenClosed(min, new Spot(min, turned).max())) {
						box.add(pos.immutable());
					}
					assertEquals(box, placed, turn + " " + size);
				}
			}
		}
	}

	@Test
	void aQuarterTurnSwapsTheSidesAlongXAndZ() {
		assertEquals(new Vec3i(4, 2, 3), CopyPlacement.turnedSize(ODD_EVEN, new Turn(Mirror.NONE, Rotation.CLOCKWISE_90)));
		assertEquals(new Vec3i(4, 2, 3),
			CopyPlacement.turnedSize(ODD_EVEN, new Turn(Mirror.LEFT_RIGHT, Rotation.COUNTERCLOCKWISE_90)));
		assertEquals(ODD_EVEN, CopyPlacement.turnedSize(ODD_EVEN, new Turn(Mirror.FRONT_BACK, Rotation.CLOCKWISE_180)));
		assertEquals(ODD_EVEN, CopyPlacement.turnedSize(ODD_EVEN, Turn.NONE));
	}

	@Test
	void turningAndMirroringKeepTheMiddleBlock() {
		Spot before = spot(0, 0, 0, 2, 1, 3);
		assertEquals(new BlockPos(1, 0, 1), before.middle());
		// A quarter turn: 4 along x, 3 along z, around the same middle.
		Spot turned = CopyPlacement.aroundMiddle(before, new Vec3i(4, 2, 3));
		assertEquals(spot(0, 0, 0, 3, 1, 2), turned);
		assertEquals(before.middle(), turned.middle());
		// Turned back, it's where it was.
		assertEquals(before, CopyPlacement.aroundMiddle(turned, ODD_EVEN));
		// A mirror or a half turn keeps the sizes, so the box stays.
		assertEquals(before, CopyPlacement.aroundMiddle(before, ODD_EVEN));
		// Odd sides both ways turn about their exact middle.
		Spot odd = spot(5, 0, 5, 9, 0, 7);
		assertEquals(spot(6, 0, 4, 8, 0, 8), CopyPlacement.aroundMiddle(odd, new Vec3i(3, 1, 5)));
	}

	@Test
	void aTurnOnAFaceSpinsTheCopyInPlace() {
		// On a top face, the middle of the copy's bottom stays on the block above the aimed one, whichever way it's turned.
		for (Rotation rotation : Rotation.values()) {
			Vec3i size = CopyPlacement.turnedSize(ODD_EVEN, new Turn(Mirror.NONE, rotation));
			Spot spot = CopyPlacement.againstFace(AIMED, Direction.UP, size);
			assertEquals(new BlockPos(10, 65, -20), spot.middle().atY(65), rotation.toString());
		}
	}
}
