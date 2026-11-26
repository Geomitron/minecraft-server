package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.buildingplanner.input.EscSteps.State;
import dev.buildingplanner.input.EscSteps.Step;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EscStepsTest {
	/**
	 * During a copy's Line, with Line chosen in Effortless Building too: Esc drops the set, then puts the copy away, then
	 * sets the build mode to Disable, then pauses.
	 */
	@Test
	void duringACopysSetEscDropsTheSetThenPutsTheCopyAwayThenDisablesTheBuildModeThenPauses() {
		List<Step> steps = new ArrayList<>();
		State state = new State(MouseMode.COPY_AT_AIM, false, false, true, false, true);
		steps.add(EscSteps.next(state));
		state = new State(MouseMode.COPY_AT_AIM, false, false, false, false, true);
		steps.add(EscSteps.next(state));
		state = new State(null, false, false, false, false, true);
		steps.add(EscSteps.next(state));
		state = new State(null, false, false, false, false, false);
		steps.add(EscSteps.next(state));
		assertEquals(List.of(Step.DROP_SET, Step.PUT_COPY_AWAY, Step.DISABLE_BUILD_MODE, Step.PAUSE), steps);
	}

	/** In Delete, Esc ends a box being drawn, then clears the marks, then ends Delete, before anything else. */
	@Test
	void inDeleteEscEndsItsBoxThenClearsTheMarksThenEndsDelete() {
		assertEquals(Step.END_DELETE_BOX, EscSteps.next(new State(MouseMode.DELETE, true, true, false, false, true)));
		assertEquals(Step.CLEAR_MARKS, EscSteps.next(new State(MouseMode.DELETE, false, true, false, false, true)));
		assertEquals(Step.END_DELETE, EscSteps.next(new State(MouseMode.DELETE, false, false, false, false, true)));
	}

	/** A box for Copy or the material list ends at the first Esc, whatever build mode is chosen. */
	@Test
	void aBoxEndsFirst() {
		assertEquals(Step.END_BOX, EscSteps.next(new State(MouseMode.BOX, false, false, false, false, true)));
		assertEquals(Step.END_BOX, EscSteps.next(new State(MouseMode.BOX, false, false, false, false, false)));
	}

	/** With no mouse mode, a shape under way is cancelled before the build mode becomes Disable. */
	@Test
	void aShapeUnderWayIsCancelledBeforeTheBuildModeIsDisabled() {
		assertEquals(Step.CANCEL_SHAPE, EscSteps.next(new State(null, false, false, false, true, true)));
		assertEquals(Step.DISABLE_BUILD_MODE, EscSteps.next(new State(null, false, false, false, false, true)));
	}

	/** In Disable with no mode on, Esc pauses at once. */
	@Test
	void inDisableWithNoModeEscPauses() {
		assertEquals(Step.PAUSE, EscSteps.next(new State(null, false, false, false, false, false)));
	}
}
