package dev.buildingplanner.server;

import static dev.buildingplanner.server.SimulatedNetwork.assertSamePlan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.plan.Plan;
import dev.buildingplanner.plan.PlanRegion;
import dev.buildingplanner.server.SimulatedNetwork.Client;
import dev.buildingplanner.sync.BlockChange;
import dev.buildingplanner.sync.EditKind;
import dev.buildingplanner.sync.Payloads;
import dev.buildingplanner.sync.PlayerMessage;
import dev.buildingplanner.sync.SyncLimits;
import java.util.List;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class UndoTest {
	private static final String REGION = "House";
	private static final BlockPos A = new BlockPos(1, 0, 1);
	private static final BlockPos B = new BlockPos(2, 0, 1);
	private static final BlockPos C = new BlockPos(3, 0, 1);
	private static BlockState stone;
	private static BlockState dirt;
	private static BlockState glass;

	private SimulatedNetwork network;
	private UUID planId;
	private Client alex;
	private Client blair;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		stone = Blocks.STONE.defaultBlockState();
		dirt = Blocks.DIRT.defaultBlockState();
		glass = Blocks.GLASS.defaultBlockState();
	}

	private void start(boolean redo) {
		network = new SimulatedNetwork(SyncLimits.DEFAULT, redo);
		planId = addPlan();
		alex = network.client("Alex");
		blair = network.client("Blair");
		alex.subscribe(planId);
		blair.subscribe(planId);
		network.settle();
	}

	private UUID addPlan() {
		UUID id = UUID.randomUUID();
		network.server.addPlan(id, Plan.create(REGION, "Alex", BlockPos.ZERO, new Vec3i(8, 4, 8), 0));
		return id;
	}

	private PlanRegion region() {
		return network.server.plan(planId).orElseThrow().firstRegion();
	}

	private void edit(Client client, BlockState state, BlockPos... positions) {
		edit(client, planId, state, positions);
	}

	private void edit(Client client, UUID plan, BlockState state, BlockPos... positions) {
		client.edit(plan, EditKind.SHAPE, List.of(positions).stream().map(pos -> new BlockChange(REGION, pos, state)).toList());
		network.settle();
	}

	private void undo(Client client) {
		client.undo();
		network.settle();
	}

	private void assertEveryoneHasTheServersPlan() {
		for (Client client : List.of(alex, blair)) {
			assertSamePlan(network.server.plan(planId).orElseThrow(), client.plans.get(planId), client.name);
			assertEquals(0, client.gaps, client.name + "'s revision gaps");
		}
		network.assertRevisionsInOrder();
	}

	@Test
	void undoTakesBackThePlayersNewestStepForEveryone() {
		start(false);
		edit(alex, stone, A, B, C);
		blair.inbox.clear();
		alex.undo();
		alex.serverReadsOne();

		Payloads.Applied applied = (Payloads.Applied) blair.inbox.peek();
		assertEquals(2, applied.rev());
		assertEquals(alex.id, applied.author());
		assertEquals(Payloads.NO_CLIENT_SEQ, applied.clientSeq());
		network.settle();
		for (BlockPos pos : List.of(A, B, C)) {
			assertTrue(region().get(pos).isAir(), pos + " is back to air");
		}
		assertTrue(alex.notices.isEmpty(), alex.notices.toString());
		assertEveryoneHasTheServersPlan();
	}

	@Test
	void eachPlayerUndoesOnlyTheirOwnStepsNewestFirst() {
		start(false);
		edit(alex, stone, A);
		edit(blair, dirt, B);
		edit(alex, glass, A, C);

		undo(alex);
		assertEquals(stone, region().get(A));
		assertTrue(region().get(C).isAir());
		assertEquals(dirt, region().get(B), "Blair's step stays");

		undo(alex);
		assertTrue(region().get(A).isAir());
		assertEquals(dirt, region().get(B));

		undo(alex);
		assertEquals(List.of(PlayerMessage.nothingToUndo()), alex.notices);
		assertEquals(dirt, region().get(B));
		assertEveryoneHasTheServersPlan();
	}

	@Test
	void undoTakesTheWholeEditBackToItsStateBeforeTheEdit() {
		start(false);
		edit(alex, dirt, A);
		// One edit writes A twice: undo goes back to the state before the edit, not between its writes.
		alex.edit(planId, EditKind.SHAPE, List.of(new BlockChange(REGION, A, stone), new BlockChange(REGION, A, glass)));
		network.settle();

		undo(alex);
		assertEquals(dirt, region().get(A));
		assertEveryoneHasTheServersPlan();
	}

	@Test
	void undoLeavesBlocksSomeoneElseChangedSince() {
		start(false);
		edit(alex, stone, A, B, C);
		edit(blair, dirt, B);

		undo(alex);

		assertTrue(region().get(A).isAir());
		assertEquals(dirt, region().get(B), "Blair's block stays as Blair left it");
		assertTrue(region().get(C).isAir());
		assertEquals(List.of(PlayerMessage.blocksLeftAsTheyAre(1)), alex.notices);
		assertEquals(List.of("1"), alex.notices.getFirst().args());
		assertEveryoneHasTheServersPlan();
	}

	@Test
	void anUndoWhoseBlocksAllChangedSinceChangesNothing() {
		start(false);
		edit(alex, stone, A, B);
		edit(blair, dirt, A, B);
		int revision = network.server.revision(planId);

		undo(alex);

		assertEquals(revision, network.server.revision(planId), "no applied edit for an empty undo");
		assertEquals(dirt, region().get(A));
		assertEquals(List.of(PlayerMessage.blocksLeftAsTheyAre(2)), alex.notices);
		assertEveryoneHasTheServersPlan();
	}

	@Test
	void undoAfterGrowthKeepsTheGrownBoxAndPlanPositions() {
		start(false);
		edit(alex, stone, A);
		// Blair grows the plan toward negative coordinates after Alex's step; Alex grows it upward.
		edit(blair, dirt, new BlockPos(-6, 0, -2));
		edit(alex, glass, new BlockPos(0, 9, 0), B);
		Vec3i grown = region().dimensions();
		assertEquals(new Vec3i(14, 10, 10), grown);

		undo(alex);
		assertTrue(region().get(new BlockPos(0, 9, 0)).isAir());
		assertTrue(region().get(B).isAir());
		assertEquals(grown, region().dimensions(), "undo doesn't shrink the plan");

		undo(alex);
		assertTrue(region().get(A).isAir(), "the step from before the growth lands at its plan position");
		assertEquals(dirt, region().get(new BlockPos(-6, 0, -2)));
		assertEveryoneHasTheServersPlan();
	}

	@Test
	void aDeletedPlansStepsGoWithIt() {
		start(false);
		UUID otherPlan = addPlan();
		alex.subscribe(otherPlan);
		network.settle();
		edit(alex, otherPlan, stone, A);
		edit(alex, dirt, A);
		edit(alex, glass, B);

		network.server.removePlan(planId);
		undo(alex);

		Plan other = network.server.plan(otherPlan).orElseThrow();
		assertTrue(other.firstRegion().get(A).isAir(), "the undo went to the other plan's step");
		assertTrue(alex.notices.isEmpty(), alex.notices.toString());
		undo(alex);
		assertEquals(List.of(PlayerMessage.nothingToUndo()), alex.notices);
	}

	@Test
	void undoAfterThePlansDeletedHasNothingToUndo() {
		start(false);
		edit(alex, stone, A);
		network.server.removePlan(planId);
		alex.inbox.clear();

		undo(alex);

		assertEquals(List.of(PlayerMessage.nothingToUndo()), alex.notices);
		assertTrue(blair.inbox.isEmpty());
	}

	@Test
	void eachPlayerKeepsTheNewest50Steps() {
		start(false);
		for (int i = 0; i < 51; i++) {
			edit(alex, i % 2 == 0 ? stone : dirt, new BlockPos(i % 8, 0, i / 8));
		}
		for (int i = 0; i < 51; i++) {
			undo(alex);
		}

		assertEquals(stone, region().get(BlockPos.ZERO), "the oldest step fell off the list");
		for (int i = 1; i < 51; i++) {
			assertTrue(region().get(new BlockPos(i % 8, 0, i / 8)).isAir(), "step " + i + " undone");
		}
		assertEquals(List.of(PlayerMessage.nothingToUndo()), alex.notices);
	}

	@Test
	void stepsOutlastTheConnection() {
		start(false);
		edit(alex, stone, A);
		network.server.disconnect(alex.id);

		alex.undo();
		alex.serverReadsOne();

		assertTrue(region().get(A).isAir());
	}

	@Test
	void withRedoOffThereIsNothingToRedo() {
		start(false);
		edit(alex, stone, A);
		undo(alex);

		alex.redo();
		network.settle();

		assertTrue(region().get(A).isAir());
		assertEquals(List.of(PlayerMessage.nothingToRedo()), alex.notices);
	}

	@Test
	void withRedoOnAnUndoneStepCanBeRedoneUntilTheNextEdit() {
		start(true);
		edit(alex, stone, A, B);
		edit(alex, dirt, C);
		undo(alex);
		undo(alex);
		edit(blair, glass, B);

		alex.redo();
		network.settle();
		assertEquals(stone, region().get(A));
		assertEquals(glass, region().get(B), "Blair's block stays on redo too");
		assertTrue(region().get(C).isAir(), "the newer undone step waits");
		assertEquals(List.of(PlayerMessage.blocksLeftAsTheyAre(1)), alex.notices);

		edit(alex, glass, new BlockPos(5, 0, 5));
		alex.redo();
		network.settle();
		assertTrue(region().get(C).isAir(), "a new edit clears the redo list");
		assertEquals(PlayerMessage.nothingToRedo(), alex.notices.getLast());

		undo(alex);
		undo(alex);
		assertTrue(region().get(A).isAir(), "a redone step can be undone again");
		assertEveryoneHasTheServersPlan();
	}
}
