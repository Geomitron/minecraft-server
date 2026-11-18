package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.buildingplanner.input.BoxCorners.Limit;
import java.util.OptionalInt;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class BoxCornersTest {
	private static final Vec3 EYE = new Vec3(0.5, 10.5, 0.5);

	/** The direction from the eyes to a point, as the player looks there. */
	private static Vec3 lookAt(double x, double y, double z) {
		return new Vec3(x, y, z).subtract(EYE).normalize();
	}

	@Test
	void theFirstCornerIsTheSpaceAboveABlocksTopAndTheBlockItselfOnAnyOtherFace() {
		BlockPos ground = new BlockPos(4, 0, 7);
		assertEquals(new BlockPos(4, 1, 7), BoxCorners.first(ground, Direction.UP));
		for (Direction face : new Direction[] {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST,
			Direction.WEST}) {
			assertEquals(ground, BoxCorners.first(ground, face), face.toString());
		}
	}

	@Test
	void theSecondCornerIsWhereTheLineOfSightCrossesTheFirstCornersLevel() {
		BlockPos first = new BlockPos(2, 1, 2);
		// Aiming at the ground's top at 12.3, 1, -5.7, the floor of the first corner's level.
		assertEquals(new BlockPos(12, 1, -6), BoxCorners.second(EYE, lookAt(12.3, 1, -5.7), first));
		// The line of sight crosses that level beyond the point aimed at, still on the level.
		assertEquals(new BlockPos(20, 1, 0), BoxCorners.second(EYE, lookAt(10.5, 5.75, 0.5), first));
	}

	@Test
	void theSecondCornerNeedsTheLevelCrossedInFrontWithinReach() {
		BlockPos first = new BlockPos(2, 1, 2);
		assertNull(BoxCorners.second(EYE, new Vec3(1, 0, 0), first), "looking level");
		assertNull(BoxCorners.second(EYE, lookAt(10, 20, 0), first), "looking up, so the level is behind");
		// 1.5 blocks down over 255.5 along x: just within the reach of 256 from the eyes, then a block past it.
		assertEquals(new BlockPos(256, 9, 0), BoxCorners.second(EYE, lookAt(256, 9, 0.5), new BlockPos(0, 9, 0)));
		assertNull(BoxCorners.second(EYE, lookAt(257, 9, 0.5), new BlockPos(0, 9, 0)));
	}

	@Test
	void theHeightIsWhereTheLineOfSightPassesNearestTheVerticalLineThroughTheSecondCorner() {
		BlockPos second = new BlockPos(10, 1, 0);
		assertEquals(OptionalInt.of(25), BoxCorners.height(EYE, lookAt(10.5, 25.5, 0.5), second));
		assertEquals(OptionalInt.of(-4), BoxCorners.height(EYE, lookAt(10.5, -3.5, 0.5), second));
		Vec3 sideways = lookAt(10.5, 30.5, 10.5);
		// The line of sight passes the vertical line's middle at x 10.5 nearest at 5.5, 20.5, 5.5 along its way.
		assertEquals(OptionalInt.of(20), BoxCorners.height(EYE, sideways, second));
	}

	@Test
	void theHeightNeedsALineOfSightThatPassesInFrontWithinReach() {
		BlockPos second = new BlockPos(10, 1, 0);
		assertEquals(OptionalInt.empty(), BoxCorners.height(EYE, new Vec3(0, -1, 0), second), "straight down");
		assertEquals(OptionalInt.empty(), BoxCorners.height(EYE, lookAt(-10, 5, 0.5), second), "looking away");
		assertEquals(OptionalInt.empty(), BoxCorners.height(EYE, lookAt(10.5, 300, 0.5), second), "too high to reach");
	}

	@Test
	void cornersStayWithinTheBuildLimits() {
		assertEquals(new BlockPos(3, 319, 4), BoxCorners.withinHeight(new BlockPos(3, 400, 4), -64, 319));
		assertEquals(new BlockPos(3, -64, 4), BoxCorners.withinHeight(new BlockPos(3, -80, 4), -64, 319));
		assertEquals(new BlockPos(3, 70, 4), BoxCorners.withinHeight(new BlockPos(3, 70, 4), -64, 319));
	}

	@Test
	void theSizeCountsBlocksAlongEachSide() {
		assertArrayEquals(new int[] {12, 5, 8}, BoxCorners.size(new BlockPos(0, 64, 0), new BlockPos(-11, 60, 7)));
	}

	@Test
	void aBoxMayBe256BlocksASideAndNoMore() {
		BlockPos corner = new BlockPos(-100, 0, 0);
		assertNull(BoxCorners.limit(corner, new BlockPos(155, 255, -255), (x, z) -> true));
		assertEquals(Limit.SIDE, BoxCorners.limit(corner, new BlockPos(156, 0, 0), (x, z) -> true));
		assertEquals(Limit.SIDE, BoxCorners.limit(corner, new BlockPos(-100, 256, 0), (x, z) -> true));
		assertEquals(Limit.SIDE, BoxCorners.limit(corner, new BlockPos(-100, 0, -256), (x, z) -> true));
		// The side's limit comes first, wherever the chunks are loaded.
		assertEquals(Limit.SIDE, BoxCorners.limit(corner, new BlockPos(156, 0, 0), (x, z) -> false));
	}

	@Test
	void everyChunkTheBoxTouchesMustBeLoaded() {
		// From chunk -1 to chunk 1 along x and 0 along z; chunk 1, 0 isn't loaded.
		Set<Long> loaded = Set.of(key(-1, 0), key(0, 0));
		BoxCorners.LoadedChunks chunks = (x, z) -> loaded.contains(key(x, z));
		assertNull(BoxCorners.limit(new BlockPos(-16, 5, 0), new BlockPos(15, 9, 15), chunks));
		assertEquals(Limit.NOT_LOADED, BoxCorners.limit(new BlockPos(-16, 5, 0), new BlockPos(16, 9, 15), chunks));
		assertEquals(Limit.NOT_LOADED, BoxCorners.limit(new BlockPos(0, 5, 0), new BlockPos(0, 9, 16), chunks));
	}

	private static long key(int x, int z) {
		return ((long) x << 32) | (z & 0xFFFFFFFFL);
	}
}
