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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlanSyncTest {
	private static final String REGION = "House";
	private static BlockState stone;
	private static BlockState dirt;
	private static BlockState glass;
	private static BlockState air;

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
		air = Blocks.AIR.defaultBlockState();
	}

	@BeforeEach
	void setUp() {
		network = new SimulatedNetwork(SyncLimits.DEFAULT);
		planId = addPlan(new Vec3i(8, 4, 8));
		alex = network.client("Alex");
		blair = network.client("Blair");
	}

	private UUID addPlan(Vec3i dimensions) {
		UUID id = UUID.randomUUID();
		network.server.addPlan(id, Plan.create(REGION, "Alex", BlockPos.ZERO, dimensions, 0));
		return id;
	}

	private Plan serverPlan() {
		return network.server.plan(planId).orElseThrow();
	}

	private static List<BlockChange> blocks(BlockState state, BlockPos... positions) {
		return List.of(positions).stream().map(pos -> new BlockChange(REGION, pos, state)).toList();
	}

	private void subscribeBoth() {
		alex.subscribe(planId);
		blair.subscribe(planId);
		network.settle();
	}

	private void assertEveryoneHasTheServersPlan(Client... clients) {
		for (Client client : clients) {
			assertSamePlan(serverPlan(), client.plans.get(planId), client.name);
			assertEquals(network.server.revision(planId), client.revisions.get(planId), client.name + "'s revision");
			assertEquals(0, client.gaps, client.name + "'s revision gaps");
		}
		network.assertRevisionsInOrder();
	}

	@Test
	void subscribeSendsASnapshotAtTheCurrentRevision() {
		alex.subscribe(planId);
		network.settle();
		alex.edit(planId, EditKind.SHAPE, blocks(stone, new BlockPos(1, 0, 1), new BlockPos(2, 0, 1)));
		alex.edit(planId, EditKind.SHAPE, blocks(dirt, new BlockPos(3, 1, 3)));
		network.settle();

		blair.subscribe(planId);
		network.settle();

		assertEquals(2, network.server.revision(planId));
		assertEquals(2, blair.revisions.get(planId));
		assertEquals(dirt, blair.plans.get(planId).firstRegion().get(new BlockPos(3, 1, 3)));
		assertEveryoneHasTheServersPlan(alex, blair);
	}

	@Test
	void eachAppliedEditGoesToEverySubscriberWithItsAuthorAndSequence() {
		subscribeBoth();
		int seq = blair.edit(planId, EditKind.SHAPE, blocks(stone, new BlockPos(0, 0, 0)));
		blair.serverReadsOne();

		for (Client client : List.of(alex, blair)) {
			Payloads.Applied applied = (Payloads.Applied) client.inbox.peek();
			assertEquals(1, applied.rev());
			assertEquals(blair.id, applied.author());
			assertEquals(seq, applied.clientSeq());
		}
	}

	@Test
	void twoClientsInterleavingEditsEndWithIdenticalPlans() {
		for (long seed = 0; seed < 25; seed++) {
			setUp();
			subscribeBoth();
			Random random = new Random(seed);
			List<Client> clients = List.of(alex, blair);
			for (int step = 0; step < 300; step++) {
				Client client = clients.get(random.nextInt(2));
				switch (random.nextInt(4)) {
					case 0 -> randomEdit(client, random);
					case 1 -> client.serverReadsOne();
					default -> client.readsOne();
				}
			}
			network.settle();

			assertTrue(alex.rejections.isEmpty() && blair.rejections.isEmpty(), "seed " + seed + ": no rejections");
			assertTrue(serverPlan().firstRegion().volume() > 8 * 4 * 8, "seed " + seed + ": the plan grew");
			assertEveryoneHasTheServersPlan(alex, blair);
			assertSamePlan(alex.plans.get(planId), blair.plans.get(planId), "seed " + seed + ": Alex and Blair");
		}
	}

	/** A shape that may cross the box (growing it), an erase, or Litematica writes inside the client's own copy. */
	private void randomEdit(Client client, Random random) {
		List<BlockState> states = List.of(stone, dirt, glass);
		BlockState state = states.get(random.nextInt(states.size()));
		List<BlockChange> changes = new ArrayList<>();
		int count = 1 + random.nextInt(6);
		EditKind kind = EditKind.values()[random.nextInt(3)];
		PlanRegion known = client.plans.get(planId).firstRegion();
		for (int i = 0; i < count; i++) {
			BlockPos pos = kind == EditKind.LITEMATICA
				? known.minCorner().offset(random.nextInt(known.dimensions().getX()), random.nextInt(known.dimensions().getY()),
					random.nextInt(known.dimensions().getZ()))
				: new BlockPos(random.nextInt(14) - 3, random.nextInt(8) - 2, random.nextInt(14) - 3);
			changes.add(new BlockChange(REGION, pos, kind == EditKind.ERASE ? air : state));
		}
		client.edit(planId, kind, changes);
	}

	@Test
	void theLastEditInTheServersOrderWinsABlock() {
		subscribeBoth();
		BlockPos pos = new BlockPos(2, 1, 2);
		// Alex's Litematica write shows on Alex's screen at once; Blair's shape reaches the server first.
		alex.edit(planId, EditKind.LITEMATICA, blocks(stone, pos));
		blair.edit(planId, EditKind.SHAPE, blocks(dirt, pos));
		assertEquals(stone, alex.plans.get(planId).firstRegion().get(pos));
		blair.serverReadsOne();
		alex.serverReadsOne();

		alex.readsOne();
		assertEquals(dirt, alex.plans.get(planId).firstRegion().get(pos), "Alex sees the block flip once");
		network.settle();

		assertEquals(stone, serverPlan().firstRegion().get(pos));
		assertEquals(List.of(1, 2), network.sentRevisions.get(planId));
		assertEveryoneHasTheServersPlan(alex, blair);
	}

	@Test
	void growthDuringInterleavedEditsKeepsEveryBlockAtItsPlanPosition() {
		subscribeBoth();
		// Alex grows the plan toward negative x and up; Blair, still seeing the old box, writes inside it and draws a
		// shape out past positive z.
		alex.edit(planId, EditKind.SHAPE, blocks(stone, new BlockPos(-5, 0, 0), new BlockPos(0, 6, 0)));
		blair.edit(planId, EditKind.LITEMATICA, blocks(glass, new BlockPos(7, 3, 7), new BlockPos(0, 0, 0)));
		blair.edit(planId, EditKind.SHAPE, blocks(dirt, new BlockPos(1, 0, 10), new BlockPos(1, 0, 11)));
		alex.serverReadsOne();
		blair.serverReadsOne();
		blair.serverReadsOne();
		alex.edit(planId, EditKind.ERASE, blocks(air, new BlockPos(1, 0, 11), new BlockPos(1, 0, 40)));
		network.settle();

		PlanRegion region = serverPlan().firstRegion();
		assertEquals(new BlockPos(-5, 0, 0), region.minCorner());
		assertEquals(new Vec3i(13, 7, 12), region.dimensions());
		assertEquals(stone, region.get(new BlockPos(-5, 0, 0)));
		assertEquals(glass, region.get(new BlockPos(0, 0, 0)));
		assertEquals(glass, region.get(new BlockPos(7, 3, 7)));
		assertEquals(dirt, region.get(new BlockPos(1, 0, 10)));
		assertEquals(air, region.get(new BlockPos(1, 0, 11)));
		assertEveryoneHasTheServersPlan(alex, blair);
	}

	@Test
	void anEraseOutsideTheBoxDoesNotGrowThePlan() {
		subscribeBoth();
		alex.edit(planId, EditKind.ERASE, blocks(air, new BlockPos(30, 0, 0), new BlockPos(1, 1, 1)));
		network.settle();

		assertEquals(new Vec3i(8, 4, 8), serverPlan().firstRegion().dimensions());
		assertEquals(1, network.server.revision(planId));
		assertEveryoneHasTheServersPlan(alex, blair);
	}

	@Test
	void aClientThatSubscribesMidEditCatchesUp() {
		subscribeBoth();
		Client casey = network.client("Casey");
		Random random = new Random(7);
		for (int i = 0; i < 20; i++) {
			randomEdit(alex, random);
			randomEdit(blair, random);
		}
		// Some edits applied and some still on their way to the server, others applied but unread by their authors.
		for (int i = 0; i < 10; i++) {
			alex.serverReadsOne();
			blair.serverReadsOne();
			alex.readsOne();
		}
		casey.subscribe(planId);
		casey.serverReadsOne();
		casey.readsOne();
		assertTrue(network.server.revision(planId) > 0);
		for (int i = 0; i < 5; i++) {
			randomEdit(casey, random);
			randomEdit(alex, random);
		}
		network.settle();

		assertEveryoneHasTheServersPlan(alex, blair, casey);
	}

	@Test
	void unsubscribedAndDisconnectedPlayersGetNoMoreEdits() {
		subscribeBoth();
		blair.unsubscribe(planId);
		network.settle();
		alex.edit(planId, EditKind.SHAPE, blocks(stone, BlockPos.ZERO));
		network.settle();
		assertTrue(blair.inbox.isEmpty());

		network.server.disconnect(alex.id);
		blair.subscribe(planId);
		network.settle();
		blair.edit(planId, EditKind.SHAPE, blocks(dirt, BlockPos.ZERO));
		network.settle();
		assertEquals(stone, alex.plans.get(planId).firstRegion().get(BlockPos.ZERO), "Alex got nothing after leaving");
	}

	// Rejections.

	private Payloads.Rejected onlyRejection(Client client) {
		assertEquals(1, client.rejections.size(), client.rejections.toString());
		return client.rejections.getFirst();
	}

	@Test
	void anEditToAPlanThatIsGoneIsRejected() {
		subscribeBoth();
		network.server.removePlan(planId);
		int seq = alex.edit(planId, EditKind.SHAPE, blocks(stone, BlockPos.ZERO));
		network.settle();

		Payloads.Rejected rejected = alex.rejections.getFirst();
		assertEquals(seq, rejected.clientSeq());
		assertEquals(PlayerMessage.planGone(), rejected.reason());
		// The fresh snapshot Alex asks for is refused too, so Alex drops the plan.
		assertEquals(Payloads.NO_CLIENT_SEQ, alex.rejections.get(1).clientSeq());
		assertTrue(alex.plans.isEmpty());
	}

	@Test
	void subscribingToAPlanThatIsGoneIsRejected() {
		UUID unknown = UUID.randomUUID();
		alex.subscribe(unknown);
		network.settle();

		Payloads.Rejected rejected = onlyRejection(alex);
		assertEquals(unknown, rejected.plan());
		assertEquals(Payloads.NO_CLIENT_SEQ, rejected.clientSeq());
		assertEquals(PlayerMessage.planGone(), rejected.reason());
	}

	@Test
	void anEditToAPlanThePlayerHasNotLoadedIsRejected() {
		alex.edit(planId, EditKind.SHAPE, blocks(stone, BlockPos.ZERO));
		network.settle();

		assertEquals(PlayerMessage.planNotLoaded(), onlyRejection(alex).reason());
		assertEquals(0, network.server.revision(planId));
	}

	@Test
	void anEditPastTheBlockCapIsRejectedAndTheAuthorDropsItsEarlyCopy() {
		planId = addPlan(new Vec3i(40, 40, 40));
		subscribeBoth();
		List<BlockChange> atCap = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.ZERO, new BlockPos(39, 39, 39))) {
			if (atCap.size() == 50_001) {
				break;
			}
			atCap.add(new BlockChange(REGION, pos.immutable(), stone));
		}
		alex.edit(planId, EditKind.LITEMATICA, atCap);
		assertEquals(stone, alex.plans.get(planId).firstRegion().get(BlockPos.ZERO), "Litematica wrote it early");
		network.settle();

		assertEquals(PlayerMessage.tooManyBlocks(50_001, 50_000), onlyRejection(alex).reason());
		assertEquals(List.of("50001", "50000"), onlyRejection(alex).reason().args());
		assertEquals(0, network.server.revision(planId));
		assertEveryoneHasTheServersPlan(alex, blair);
		assertTrue(alex.plans.get(planId).firstRegion().get(BlockPos.ZERO).isAir(), "the fresh snapshot dropped it");

		blair.edit(planId, EditKind.LITEMATICA, atCap.subList(0, 50_000));
		network.settle();
		assertTrue(blair.rejections.isEmpty(), "50,000 blocks is within the cap");
		assertEquals(1, network.server.revision(planId));
		assertEveryoneHasTheServersPlan(alex, blair);
	}

	@Test
	void growthPastTheSizeCapIsRejected() {
		subscribeBoth();
		// 0..7 grows to -505..7, 513 blocks across.
		alex.edit(planId, EditKind.SHAPE, blocks(stone, new BlockPos(1, 1, 1), new BlockPos(-505, 0, 0)));
		network.settle();

		assertEquals(PlayerMessage.planTooLarge(513, 512), onlyRejection(alex).reason());
		assertEquals(new Vec3i(8, 4, 8), serverPlan().firstRegion().dimensions());
		assertTrue(serverPlan().firstRegion().get(new BlockPos(1, 1, 1)).isAir(), "nothing of the edit applied");
		assertEquals(0, network.server.revision(planId));

		blair.edit(planId, EditKind.SHAPE, blocks(stone, new BlockPos(-504, 0, 0)));
		network.settle();
		assertTrue(blair.rejections.isEmpty(), "512 blocks across is within the cap");
		assertEquals(new Vec3i(512, 4, 8), serverPlan().firstRegion().dimensions());
		assertEveryoneHasTheServersPlan(alex, blair);
	}

	@Test
	void aPlanAlreadyPastTheSizeCapCanStillGrowOnItsOtherSides() {
		planId = addPlan(new Vec3i(600, 1, 1));
		subscribeBoth();
		alex.edit(planId, EditKind.SHAPE, blocks(stone, new BlockPos(0, 5, 0)));
		alex.edit(planId, EditKind.SHAPE, blocks(stone, new BlockPos(600, 0, 0)));
		network.settle();

		assertEquals(PlayerMessage.planTooLarge(601, 512), onlyRejection(alex).reason());
		assertEquals(new Vec3i(600, 6, 1), serverPlan().firstRegion().dimensions());
		assertEveryoneHasTheServersPlan(alex, blair);
	}

	@Test
	void editsThatDoNotFitThePlanAreRejected() {
		subscribeBoth();
		alex.edit(planId, EditKind.LITEMATICA, blocks(stone, new BlockPos(1, 1, 1), new BlockPos(8, 0, 0)));
		alex.edit(planId, EditKind.SHAPE, List.of(new BlockChange("Garden", BlockPos.ZERO, stone)));
		network.settle();

		assertEquals(List.of(PlayerMessage.editDoesNotFit(), PlayerMessage.editDoesNotFit()),
			alex.rejections.stream().map(Payloads.Rejected::reason).toList());
		assertEquals(0, network.server.revision(planId));
		assertEveryoneHasTheServersPlan(alex, blair);
	}
}
