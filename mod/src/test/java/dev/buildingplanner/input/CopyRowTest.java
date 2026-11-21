package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.buildingplanner.input.CopyPlacement.Spot;
import dev.buildingplanner.plan.Turn;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class CopyRowTest {
	/** A copy 3 blocks east to west, 2 high and 5 north to south, from 10 64 20 to 12 65 24; its middle is 11 64 22. */
	private static final Spot FIRST = new Spot(new BlockPos(10, 64, 20), new Vec3i(3, 2, 5));

	private static List<BlockPos> mins(List<Spot> row) {
		return row.stream().map(Spot::min).toList();
	}

	@Test
	void theModesCycleSingleZoopVertical() {
		assertEquals(CopyMode.ZOOP, CopyMode.SINGLE.next());
		assertEquals(CopyMode.VERTICAL, CopyMode.ZOOP.next());
		assertEquals(CopyMode.SINGLE, CopyMode.VERTICAL.next());
	}

	@Test
	void singleIsTheFirstCopyAlone() {
		assertEquals(List.of(FIRST), CopyRow.row(CopyMode.SINGLE, FIRST, new BlockPos(40, 64, 22)));
	}

	@Test
	void zoopGoesEachCompassDirectionTheCopysLengthApart() {
		// East and west the copy is 3 long, north and south 5.
		assertEquals(List.of(new BlockPos(10, 64, 20), new BlockPos(13, 64, 20), new BlockPos(16, 64, 20)),
			mins(CopyRow.row(CopyMode.ZOOP, FIRST, new BlockPos(17, 64, 22))));
		assertEquals(List.of(new BlockPos(10, 64, 20), new BlockPos(7, 64, 20)),
			mins(CopyRow.row(CopyMode.ZOOP, FIRST, new BlockPos(9, 64, 22))));
		assertEquals(List.of(new BlockPos(10, 64, 20), new BlockPos(10, 64, 25), new BlockPos(10, 64, 30)),
			mins(CopyRow.row(CopyMode.ZOOP, FIRST, new BlockPos(11, 64, 30))));
		assertEquals(List.of(new BlockPos(10, 64, 20), new BlockPos(10, 64, 15)),
			mins(CopyRow.row(CopyMode.ZOOP, FIRST, new BlockPos(11, 64, 19))));
	}

	@Test
	void theCopiesTouchWithoutOverlapping() {
		List<Spot> row = CopyRow.row(CopyMode.ZOOP, FIRST, new BlockPos(30, 64, 22));
		for (int i = 1; i < row.size(); i++) {
			assertEquals(row.get(i - 1).max().getX() + 1, row.get(i).min().getX());
			assertEquals(FIRST.size(), row.get(i).size());
		}
	}

	@Test
	void zoopTakesTheDirectionTheAimIsFarthestAlong() {
		// 6 east and 4 south of the middle: east.
		assertEquals(new BlockPos(16, 64, 20), CopyRow.row(CopyMode.ZOOP, FIRST, new BlockPos(17, 70, 26)).getLast().min());
		// 4 east and 6 north: north.
		assertEquals(new BlockPos(10, 64, 15), CopyRow.row(CopyMode.ZOOP, FIRST, new BlockPos(15, 64, 16)).getLast().min());
		// A tie goes east or west.
		assertEquals(new BlockPos(13, 64, 20), CopyRow.row(CopyMode.ZOOP, FIRST, new BlockPos(14, 64, 25)).getLast().min());
	}

	@Test
	void theRowEndsWithTheCopyThatHoldsTheAim() {
		// The first copy holds x 10 to 12, the second 13 to 15.
		assertEquals(1, CopyRow.row(CopyMode.ZOOP, FIRST, new BlockPos(12, 64, 22)).size());
		assertEquals(2, CopyRow.row(CopyMode.ZOOP, FIRST, new BlockPos(13, 64, 22)).size());
		assertEquals(2, CopyRow.row(CopyMode.ZOOP, FIRST, new BlockPos(15, 64, 22)).size());
		assertEquals(3, CopyRow.row(CopyMode.ZOOP, FIRST, new BlockPos(16, 64, 22)).size());
		// Aimed at the first copy's middle, the row is the first copy alone.
		assertEquals(List.of(FIRST), CopyRow.row(CopyMode.ZOOP, FIRST, FIRST.middle()));
	}

	@Test
	void theRowStopsAtTenCopies() {
		List<Spot> row = CopyRow.row(CopyMode.ZOOP, FIRST, new BlockPos(500, 64, 22));
		assertEquals(CopyRow.MAX_COPIES, row.size());
		assertEquals(new BlockPos(37, 64, 20), row.getLast().min());
		assertEquals(10, CopyRow.row(CopyMode.VERTICAL, FIRST, new BlockPos(11, -500, 22)).size());
	}

	@Test
	void aTurnedCopyIsItsTurnedLengthApart() {
		// The same copy given a quarter turn is 5 east to west.
		Spot turned = new Spot(new BlockPos(9, 64, 21),
			CopyPlacement.turnedSize(FIRST.size(), new Turn(Mirror.NONE, Rotation.CLOCKWISE_90)));
		assertEquals(new Vec3i(5, 2, 3), turned.size());
		assertEquals(List.of(new BlockPos(9, 64, 21), new BlockPos(14, 64, 21), new BlockPos(19, 64, 21)),
			mins(CopyRow.row(CopyMode.ZOOP, turned, new BlockPos(20, 64, 22))));
	}

	@Test
	void verticalGoesUpOrDownTheCopysHeightApart() {
		assertEquals(List.of(new BlockPos(10, 64, 20), new BlockPos(10, 66, 20), new BlockPos(10, 68, 20)),
			mins(CopyRow.row(CopyMode.VERTICAL, FIRST, new BlockPos(11, 69, 22))));
		assertEquals(List.of(new BlockPos(10, 64, 20), new BlockPos(10, 62, 20)),
			mins(CopyRow.row(CopyMode.VERTICAL, FIRST, new BlockPos(11, 62, 22))));
		// Far to the side, Vertical still goes only up or down.
		assertEquals(List.of(new BlockPos(10, 64, 20), new BlockPos(10, 66, 20)),
			mins(CopyRow.row(CopyMode.VERTICAL, FIRST, new BlockPos(40, 66, 50))));
		assertEquals(1, CopyRow.row(CopyMode.VERTICAL, FIRST, new BlockPos(11, 65, 22)).size());
	}

	@Test
	void verticalFollowsTheViewUpAndDown() {
		// From 6 blocks south of the copy's middle, eyes at 65.6, looking north and up at 45 degrees: the view passes
		// over the middle's line 6 blocks higher.
		Vec3 eye = new Vec3(11.5, 65.6, 28.5);
		Vec3 up = new Vec3(0, 1, -1).normalize();
		assertEquals(new BlockPos(11, 71, 22), CopyRow.verticalAim(FIRST, eye, up, 64));
		Vec3 down = new Vec3(0, -1, -1).normalize();
		assertEquals(new BlockPos(11, 59, 22), CopyRow.verticalAim(FIRST, eye, down, 64));
		// Looking away, the nearest point is the eye.
		assertEquals(new BlockPos(11, 65, 22), CopyRow.verticalAim(FIRST, eye, new Vec3(0, 0.6, 0.8), 64));
		// Straight up, as far as the reach.
		assertEquals(new BlockPos(11, 129, 22), CopyRow.verticalAim(FIRST, eye, new Vec3(0, 1, 0), 64));
	}

	@Test
	void theRowsBoxHoldsEveryCopy() {
		List<Spot> row = CopyRow.row(CopyMode.ZOOP, FIRST, new BlockPos(9, 64, 22));
		assertEquals(Spot.between(new BlockPos(7, 64, 20), new BlockPos(12, 65, 24)), CopyRow.box(row));
	}
}
