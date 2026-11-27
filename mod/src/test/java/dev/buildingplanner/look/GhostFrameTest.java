package dev.buildingplanner.look;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class GhostFrameTest {
	/** A ghost block, a real block, and the mirror's copy of each across x 0. */
	private static final BlockPos GHOST = new BlockPos(3, -60, 7);
	private static final BlockPos GHOST_COPY = new BlockPos(-4, -60, 7);
	private static final BlockPos REAL = new BlockPos(5, -61, 7);
	private static final BlockPos REAL_COPY = new BlockPos(-6, -61, 7);
	private static final Predicate<BlockPos> GHOSTS = Set.of(GHOST, GHOST_COPY)::contains;

	@Test
	void theBoxDrawsOnAFramedGhostBlock() {
		assertEquals(List.of(GHOST), GhostFrame.on(List.of(GHOST), GHOSTS));
	}

	@Test
	void aFramedRealBlockKeepsEffortlessBuildingsLookAlone() {
		assertEquals(List.of(), GhostFrame.on(List.of(REAL), GHOSTS));
		assertEquals(List.of(), GhostFrame.on(List.of(REAL, REAL_COPY), GHOSTS));
	}

	@Test
	void amongTheCopiesOnlyTheGhostBlocksInOrder() {
		assertEquals(List.of(GHOST, GHOST_COPY), GhostFrame.on(List.of(GHOST, GHOST_COPY), GHOSTS));
		assertEquals(List.of(GHOST), GhostFrame.on(List.of(GHOST, REAL_COPY), GHOSTS));
		assertEquals(List.of(GHOST_COPY), GhostFrame.on(List.of(REAL, GHOST_COPY), GHOSTS));
	}

	@Test
	void noFrameNoBox() {
		assertEquals(List.of(), GhostFrame.on(List.of(), GHOSTS));
	}

	@Test
	void theBoxIsEffortlessBuildingsEraseRedWithStrongSidesAndAThickOpaqueOutline() {
		assertEquals(0xFF0000, GhostFrame.RGB);
		assertEquals(0.5f, GhostFrame.SIDES_ALPHA);
		assertEquals(1f, GhostFrame.LINE_ALPHA);
		assertEquals(4f, GhostFrame.LINE_WIDTH);
	}

	@Test
	void theSidesStandInFrontOfEffortlessBuildingsOwn() {
		assertEquals(0.01f, GhostFrame.SIDES_OUT);
	}
}
