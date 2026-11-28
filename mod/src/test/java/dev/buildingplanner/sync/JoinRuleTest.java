package dev.buildingplanner.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.buildingplanner.sync.JoinRule.Session;
import org.junit.jupiter.api.Test;

/** Which session the game starts at joining a server and at its {@code hello}. */
class JoinRuleTest {
	private static final int ALL = 10;

	@Test
	void aServerThatTakesNoneOfThePayloadsStartsALocalSession() {
		assertEquals(Session.LOCAL, JoinRule.atJoin(false, 0));
	}

	@Test
	void aServerThatTakesSomeOrAllOfThemWaitsForItsHello() {
		assertEquals(Session.NONE, JoinRule.atJoin(false, 1));
		assertEquals(Session.NONE, JoinRule.atJoin(false, ALL - 1));
		assertEquals(Session.NONE, JoinRule.atJoin(false, ALL));
	}

	@Test
	void theGamesOwnWorldWaitsForItsHello() {
		assertEquals(Session.NONE, JoinRule.atJoin(true, 0));
		assertEquals(Session.NONE, JoinRule.atJoin(true, ALL));
	}

	@Test
	void aHelloOfThisVersionStartsTheServersSessionWhateverRuns() {
		// During a local session too: the hello replaces it.
		assertEquals(Session.SERVER, JoinRule.atHello(Payloads.PROTOCOL_VERSION));
	}

	@Test
	void aHelloOfAnotherVersionLeavesBuildingPlannerOff() {
		assertEquals(Session.NONE, JoinRule.atHello(Payloads.PROTOCOL_VERSION - 1));
		assertEquals(Session.NONE, JoinRule.atHello(Payloads.PROTOCOL_VERSION + 1));
	}
}
