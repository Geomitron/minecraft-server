package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.sync.JoinRule.Session;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The turn hook M6 sends before a right click in Disable, and when it sends one. */
class DisableTurnTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	private static BlockTurn.Held stairs(String facing, Direction horizontal) {
		return new BlockTurn.Held(Items.OAK_STAIRS, new BlockTurn.Turn(Map.of("facing", facing, "half", "bottom"),
			horizontal));
	}

	@Test
	void activeInDisableWithTheServersSessionAndABlockThatTurnsOrFlips() {
		assertTrue(DisableTurn.active(Session.SERVER, true, false, true));
	}

	@Test
	void notActiveInAnotherModeWithoutASessionWithAMouseModeOrForABlockThatNeitherTurnsNorFlips() {
		assertFalse(DisableTurn.active(Session.SERVER, false, false, true));
		assertFalse(DisableTurn.active(Session.NONE, true, false, true));
		assertFalse(DisableTurn.active(Session.SERVER, true, true, true));
		assertFalse(DisableTurn.active(Session.SERVER, true, false, false));
	}

	@Test
	void notActiveDuringALocalSessionOnAServerWithoutBuildingPlanner() {
		assertFalse(DisableTurn.active(Session.LOCAL, true, false, true));
	}

	@Test
	void theClickTakesTheTurnWhileLeftCtrlIsHeld() {
		BlockTurn.Held turn = stairs("east", Direction.EAST);
		assertSame(turn, DisableTurn.clickTakes(true, true, turn));
	}

	@Test
	void withoutLeftCtrlOrOutsideDisableOrWithNoTurnTheClickTakesNone() {
		BlockTurn.Held turn = stairs("east", Direction.EAST);
		assertSame(BlockTurn.Held.NONE, DisableTurn.clickTakes(true, false, turn));
		assertSame(BlockTurn.Held.NONE, DisableTurn.clickTakes(false, true, turn));
		assertSame(BlockTurn.Held.NONE, DisableTurn.clickTakes(true, true, BlockTurn.Held.NONE));
	}

	@Test
	void aTurnIsSentOnlyWhenItDiffersFromTheLastOneSent() {
		BlockTurn.Held east = stairs("east", Direction.EAST);
		assertTrue(DisableTurn.sends(BlockTurn.Held.NONE, east));
		assertFalse(DisableTurn.sends(east, stairs("east", Direction.EAST)));
		// The horizontal facing a flip goes back to isn't sent, so it alone sends nothing.
		assertFalse(DisableTurn.sends(east, stairs("east", Direction.WEST)));
		assertTrue(DisableTurn.sends(east, stairs("south", Direction.SOUTH)));
		assertTrue(DisableTurn.sends(east, new BlockTurn.Held(Items.SPRUCE_STAIRS, east.turn())));
		assertTrue(DisableTurn.sends(east, BlockTurn.Held.NONE));
		assertFalse(DisableTurn.sends(BlockTurn.Held.NONE, BlockTurn.Held.NONE));
	}
}
