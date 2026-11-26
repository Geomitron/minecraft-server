package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.buildingplanner.input.CopyPlacement.Spot;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import org.junit.jupiter.api.Test;

class CopyGridTest {
	/**
	 * A copy 2 blocks east to west, 2 high and 3 north to south, from 0 64 0 to 1 65 2; its middle is 0 64 1. The block
	 * each test gives is the one Effortless Building's Line, Wall or Floor would find from that middle.
	 */
	private static final Spot FIRST = new Spot(new BlockPos(0, 64, 0), new Vec3i(2, 2, 3));

	private static List<BlockPos> mins(List<Spot> set) {
		return set.stream().map(Spot::min).toList();
	}

	private static List<BlockPos> grid(CopyMode mode, int x, int y, int z) {
		return mins(CopyGrid.of(mode, FIRST, new BlockPos(x, y, z), CopyGrid.MAX_COPIES));
	}

	@Test
	void theModesCycleOffLineWallFloor() {
		assertEquals(CopyMode.LINE, CopyMode.OFF.next());
		assertEquals(CopyMode.WALL, CopyMode.LINE.next());
		assertEquals(CopyMode.FLOOR, CopyMode.WALL.next());
		assertEquals(CopyMode.OFF, CopyMode.FLOOR.next());
	}

	@Test
	void offIsTheFirstCopyAlone() {
		assertEquals(List.of(FIRST), CopyGrid.of(CopyMode.OFF, FIRST, new BlockPos(40, 64, 1), CopyGrid.MAX_COPIES));
	}

	@Test
	void aLineGoesUpOrDownTheCopysHeightApart() {
		// Up to the block at 71, in the fourth copy (70..71): 4 copies.
		assertEquals(List.of(new BlockPos(0, 64, 0), new BlockPos(0, 66, 0), new BlockPos(0, 68, 0),
			new BlockPos(0, 70, 0)), grid(CopyMode.LINE, 0, 71, 1));
		assertEquals(List.of(new BlockPos(0, 64, 0), new BlockPos(0, 62, 0), new BlockPos(0, 60, 0)),
			grid(CopyMode.LINE, 0, 60, 1));
	}

	@Test
	void aLineGoesEachCompassDirectionTheCopysLengthApart() {
		// East and west the copy is 2 long, north and south 3.
		assertEquals(List.of(new BlockPos(0, 64, 0), new BlockPos(2, 64, 0), new BlockPos(4, 64, 0)),
			grid(CopyMode.LINE, 5, 64, 1));
		assertEquals(List.of(new BlockPos(0, 64, 0), new BlockPos(-2, 64, 0), new BlockPos(-4, 64, 0)),
			grid(CopyMode.LINE, -3, 64, 1));
		assertEquals(List.of(new BlockPos(0, 64, 0), new BlockPos(0, 64, 3), new BlockPos(0, 64, 6)),
			grid(CopyMode.LINE, 0, 64, 7));
		assertEquals(List.of(new BlockPos(0, 64, 0), new BlockPos(0, 64, -3)), grid(CopyMode.LINE, 0, 64, -1));
	}

	@Test
	void aBlockWithinTheFirstCopyIsTheFirstCopyAlone() {
		assertEquals(List.of(FIRST.min()), grid(CopyMode.LINE, 1, 65, 2));
		assertEquals(List.of(FIRST.min()), grid(CopyMode.FLOOR, 0, 64, 1));
	}

	@Test
	void aWallIsAGridInItsUprightPlaneRowByRow() {
		// East and up: 3 copies along, 2 up, each row east to west from the first.
		assertEquals(List.of(new BlockPos(0, 64, 0), new BlockPos(2, 64, 0), new BlockPos(4, 64, 0),
			new BlockPos(0, 66, 0), new BlockPos(2, 66, 0), new BlockPos(4, 66, 0)), grid(CopyMode.WALL, 5, 66, 1));
		// North and down: 3 copies along, 2 down.
		assertEquals(List.of(new BlockPos(0, 64, 0), new BlockPos(0, 64, -3), new BlockPos(0, 64, -6),
			new BlockPos(0, 62, 0), new BlockPos(0, 62, -3), new BlockPos(0, 62, -6)), grid(CopyMode.WALL, 0, 63, -4));
	}

	@Test
	void aFloorIsAGridOnTheFirstCopysLevelRowByRow() {
		// 4 by 4: each row east from the first, the rows south from it.
		List<BlockPos> floor = grid(CopyMode.FLOOR, 7, 64, 10);
		assertEquals(16, floor.size());
		assertEquals(List.of(new BlockPos(0, 64, 0), new BlockPos(2, 64, 0), new BlockPos(4, 64, 0),
			new BlockPos(6, 64, 0), new BlockPos(0, 64, 3)), floor.subList(0, 5));
		assertEquals(new BlockPos(6, 64, 9), floor.getLast());
		// West and north.
		assertEquals(List.of(new BlockPos(0, 64, 0), new BlockPos(-2, 64, 0), new BlockPos(0, 64, -3),
			new BlockPos(-2, 64, -3)), grid(CopyMode.FLOOR, -1, 64, -2));
	}

	@Test
	void eachDirectionStopsAtTenCopies() {
		List<BlockPos> line = grid(CopyMode.LINE, 0, 500, 1);
		assertEquals(10, line.size());
		assertEquals(new BlockPos(0, 82, 0), line.getLast());
		List<BlockPos> floor = grid(CopyMode.FLOOR, 500, 64, -500);
		assertEquals(100, floor.size());
		assertEquals(new BlockPos(18, 64, -27), floor.getLast());
		assertEquals(100, grid(CopyMode.WALL, -500, -500, 1).size());
	}

	@Test
	void theCopiesTouchWithoutOverlappingAtTheTurnedSize() {
		// Turned a quarter, the copy is 3 east to west and 2 north to south.
		Spot turned = new Spot(new BlockPos(0, 64, 0), new Vec3i(3, 2, 2));
		assertEquals(List.of(new BlockPos(0, 64, 0), new BlockPos(3, 64, 0), new BlockPos(6, 64, 0)),
			mins(CopyGrid.of(CopyMode.LINE, turned, new BlockPos(7, 64, 0), CopyGrid.MAX_COPIES)));
		List<Spot> set = CopyGrid.of(CopyMode.LINE, FIRST, new BlockPos(30, 64, 1), CopyGrid.MAX_COPIES);
		for (int i = 1; i < set.size(); i++) {
			assertEquals(set.get(i - 1).max().getX() + 1, set.get(i).min().getX());
			assertEquals(FIRST.size(), set.get(i).size());
		}
	}

	@Test
	void theBoxAroundTheSet() {
		List<Spot> set = CopyGrid.of(CopyMode.WALL, FIRST, new BlockPos(-3, 66, 1), CopyGrid.MAX_COPIES);
		assertEquals(Spot.between(new BlockPos(-4, 64, 0), new BlockPos(1, 67, 2)), CopyGrid.box(set));
	}
}
