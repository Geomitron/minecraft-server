package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.buildingplanner.input.CopyPlacement.Spot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import org.junit.jupiter.api.Test;

class PasteBoxesTest {
	/** A paste from 0 0 0 to 4 2 6. */
	private static final Spot PASTE = spot(0, 0, 0, 4, 2, 6);
	private static final Vec3i SIZE = new Vec3i(2, 2, 3);

	private static Spot spot(int x1, int y1, int z1, int x2, int y2, int z2) {
		return Spot.between(new BlockPos(x1, y1, z1), new BlockPos(x2, y2, z2));
	}

	private static PasteBoxes with(Spot... boxes) {
		PasteBoxes pastes = new PasteBoxes();
		for (Spot box : boxes) {
			pastes.add(box);
		}
		return pastes;
	}

	@Test
	void withNoPasteAimedAtTheCopyRestsAgainstTheFace() {
		BlockPos aimed = new BlockPos(20, 0, 20);
		assertEquals(CopyPlacement.againstFace(aimed, Direction.UP, SIZE),
			new PasteBoxes().place(aimed, Direction.UP, SIZE, false));
		// Aimed beside a paste, not in its box.
		assertEquals(CopyPlacement.againstFace(new BlockPos(5, 0, 0), Direction.UP, SIZE),
			with(PASTE).place(new BlockPos(5, 0, 0), Direction.UP, SIZE, false));
	}

	@Test
	void sitsFlushAgainstEachFaceOfAPastesBoxAlignedAtTheCornerNearestTheAim() {
		PasteBoxes pastes = with(PASTE);
		// Aimed near the paste's low x and low z corner: the copy's low edges meet the paste's.
		BlockPos low = new BlockPos(1, 1, 1);
		assertEquals(spot(0, 3, 0, 1, 4, 2), pastes.place(low, Direction.UP, SIZE, false));
		assertEquals(spot(0, -2, 0, 1, -1, 2), pastes.place(low, Direction.DOWN, SIZE, false));
		assertEquals(spot(-2, 0, 0, -1, 1, 2), pastes.place(low, Direction.WEST, SIZE, false));
		assertEquals(spot(0, 0, -3, 1, 1, -1), pastes.place(low, Direction.NORTH, SIZE, false));
		// Aimed near the high corner: the copy's high edges meet the paste's.
		BlockPos high = new BlockPos(4, 2, 5);
		assertEquals(spot(5, 1, 4, 6, 2, 6), pastes.place(high, Direction.EAST, SIZE, false));
		assertEquals(spot(3, 1, 7, 4, 2, 9), pastes.place(high, Direction.SOUTH, SIZE, false));
		assertEquals(spot(3, 3, 4, 4, 4, 6), pastes.place(high, Direction.UP, SIZE, false));
	}

	@Test
	void theFaceOfThePastesBoxCountsEvenWhenTheAimedBlockIsInsideIt() {
		// The top of a block in the middle of the paste: the copy goes on top of the paste's whole box.
		assertEquals(spot(3, 3, 4, 4, 4, 6), with(PASTE).place(new BlockPos(3, 0, 4), Direction.UP, SIZE, false));
	}

	@Test
	void theMostRecentPasteHoldingTheAimedBlockWins() {
		Spot older = spot(0, 0, 0, 9, 9, 9);
		Spot newer = spot(2, 0, 2, 3, 0, 3);
		assertEquals(spot(2, 1, 2, 3, 2, 4), with(older, newer).place(new BlockPos(2, 0, 2), Direction.UP, SIZE, false));
		assertEquals(spot(0, 10, 0, 1, 11, 2), with(newer, older).place(new BlockPos(2, 0, 2), Direction.UP, SIZE, false));
	}

	@Test
	void keepsTheSixteenMostRecentPastes() {
		PasteBoxes pastes = with(PASTE);
		for (int i = 1; i < PasteBoxes.KEPT; i++) {
			pastes.add(spot(100 * i, 0, 0, 100 * i, 0, 0));
		}
		assertEquals(spot(0, 3, 0, 1, 4, 2), pastes.place(new BlockPos(1, 1, 1), Direction.UP, SIZE, false));
		pastes.add(spot(-100, 0, 0, -100, 0, 0));
		// The 17th paste pushes the first out: the aim at it now rests against the face alone.
		assertEquals(CopyPlacement.againstFace(new BlockPos(1, 1, 1), Direction.UP, SIZE),
			pastes.place(new BlockPos(1, 1, 1), Direction.UP, SIZE, false));
		pastes.clear();
		assertNull(pastes.latest());
	}

	@Test
	void withTheSprintKeyTheCopyStaysOnTheLatestPastesRow() {
		// The paste is 2 by 2 by 3 at 0 0 0; the aim is far along x and a little off along z, on higher ground.
		PasteBoxes pastes = with(spot(0, 0, 0, 1, 1, 2));
		Spot free = pastes.place(new BlockPos(20, 5, 3), Direction.UP, SIZE, false);
		assertEquals(spot(20, 6, 2, 21, 7, 4), free);
		// In line: on the paste's level and its row, moving only along x.
		assertEquals(spot(20, 0, 0, 21, 1, 2), pastes.place(new BlockPos(20, 5, 3), Direction.UP, SIZE, true));
		assertEquals(spot(-30, 0, 0, -29, 1, 2), pastes.place(new BlockPos(-30, 5, -2), Direction.UP, SIZE, true));
	}

	@Test
	void withTheSprintKeyTheCopyStaysOnTheLatestPastesColumnWhenTheAimIsNearer() {
		PasteBoxes pastes = with(spot(0, 0, 0, 1, 1, 2));
		assertEquals(spot(0, 0, 20, 1, 1, 22), pastes.place(new BlockPos(2, -4, 21), Direction.UP, SIZE, true));
		assertEquals(spot(0, 0, -15, 1, 1, -13), pastes.place(new BlockPos(-1, 8, -14), Direction.UP, SIZE, true));
	}

	@Test
	void inLineAlignsTheEdgeOnTheSideTheCopyIsOn() {
		// A copy narrower than the paste lines up with the paste's edge nearer its own middle.
		Spot paste = spot(0, 0, 0, 0, 0, 6);
		assertEquals(spot(10, 0, 0, 10, 0, 1), PasteBoxes.inLine(paste, spot(10, 3, 1, 10, 3, 2)));
		assertEquals(spot(10, 0, 5, 10, 0, 6), PasteBoxes.inLine(paste, spot(10, 3, 4, 10, 3, 5)));
	}

	@Test
	void inLineFollowsOnlyTheLatestPaste() {
		PasteBoxes pastes = with(spot(0, 0, 0, 1, 1, 2), spot(50, 10, 50, 51, 11, 52));
		assertEquals(spot(70, 10, 50, 71, 11, 52), pastes.place(new BlockPos(70, 0, 51), Direction.UP, SIZE, true));
	}

	@Test
	void theSprintKeyWithNoPasteChangesNothing() {
		assertEquals(CopyPlacement.againstFace(new BlockPos(3, 4, 5), Direction.EAST, SIZE),
			new PasteBoxes().place(new BlockPos(3, 4, 5), Direction.EAST, SIZE, true));
	}

	@Test
	void flushAndInLineTogetherPutTheCopyEndToEndWithTheLatestPaste() {
		PasteBoxes pastes = with(spot(0, 0, 0, 1, 1, 2));
		assertEquals(spot(2, 0, 0, 3, 1, 2), pastes.place(new BlockPos(1, 1, 2), Direction.EAST, SIZE, true));
	}
}
