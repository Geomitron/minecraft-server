package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SingleRuleTest {
	@Test
	void activeWithASessionSingleModeNoMouseModeAndABlockInHand() {
		assertTrue(SingleRule.active(true, true, false, true));
	}

	@Test
	void notActiveWithoutASession() {
		assertFalse(SingleRule.active(false, true, false, true));
	}

	@Test
	void notActiveInAnotherMode() {
		assertFalse(SingleRule.active(true, false, false, true));
	}

	@Test
	void notActiveWhileAMouseModeIsOn() {
		assertFalse(SingleRule.active(true, true, true, true));
	}

	@Test
	void notActiveWithAnItemThatPlacesNoBlock() {
		assertFalse(SingleRule.active(true, true, false, false));
	}

	@Test
	void turningActiveInEveryModeButDisable() {
		assertTrue(SingleRule.turning(true, false, false, true, false));
		assertFalse(SingleRule.turning(true, true, false, true, false));
	}

	@Test
	void turningNotActiveWithoutASessionAMouseModeOrABlockInHand() {
		assertFalse(SingleRule.turning(false, false, false, true, false));
		assertFalse(SingleRule.turning(true, false, true, true, false));
		assertFalse(SingleRule.turning(true, false, false, false, false));
	}

	@Test
	void turningNotActiveDuringAShapeDrawnWithTheBreakClick() {
		assertFalse(SingleRule.turning(true, false, false, true, true));
	}

	@Test
	void followingTheAimInSingleBothClicksAreEffortlessBuildings() {
		SingleRule.State state = new SingleRule.State(CopyMode.SINGLE, false, false);
		assertFalse(SingleRule.takesPlace(state));
		assertFalse(SingleRule.takesBreak(state));
	}

	@Test
	void onlyTheBlockFollowingTheAimInSingleIsDrawnAtTheAim() {
		assertTrue(SingleRule.drawnAtAim(new SingleRule.State(CopyMode.SINGLE, false, false)));
		assertFalse(SingleRule.drawnAtAim(new SingleRule.State(CopyMode.SINGLE, true, false)));
		assertFalse(SingleRule.drawnAtAim(new SingleRule.State(CopyMode.ZOOP, false, false)));
		assertFalse(SingleRule.drawnAtAim(new SingleRule.State(CopyMode.VERTICAL, false, true)));
	}

	@Test
	void lockedBothClicksAreBuildingPlanners() {
		SingleRule.State state = new SingleRule.State(CopyMode.SINGLE, true, false);
		assertTrue(SingleRule.takesPlace(state));
		assertTrue(SingleRule.takesBreak(state));
	}

	@Test
	void inZoopOrVerticalBeforeTheFirstClickOnlyThePlaceClickIsBuildingPlanners() {
		for (CopyMode mode : new CopyMode[] {CopyMode.ZOOP, CopyMode.VERTICAL}) {
			SingleRule.State state = new SingleRule.State(mode, false, false);
			assertTrue(SingleRule.takesPlace(state));
			assertFalse(SingleRule.takesBreak(state));
		}
	}

	@Test
	void lockedInZoopBothClicksAreBuildingPlanners() {
		SingleRule.State state = new SingleRule.State(CopyMode.ZOOP, true, false);
		assertTrue(SingleRule.takesPlace(state));
		assertTrue(SingleRule.takesBreak(state));
	}

	@Test
	void withARowFixedBothClicksAreBuildingPlanners() {
		SingleRule.State state = new SingleRule.State(CopyMode.VERTICAL, false, true);
		assertTrue(SingleRule.takesPlace(state));
		assertTrue(SingleRule.takesBreak(state));
	}
}
