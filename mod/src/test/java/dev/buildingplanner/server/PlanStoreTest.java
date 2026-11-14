package dev.buildingplanner.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.plan.LitematicFormat;
import dev.buildingplanner.plan.Plan;
import dev.buildingplanner.sync.BlockChange;
import dev.buildingplanner.sync.EditKind;
import dev.buildingplanner.sync.SyncLimits;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
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
import org.junit.jupiter.api.io.TempDir;

class PlanStoreTest {
	private static final long START = 1_000_000;
	private static BlockState stone;
	private static BlockState glass;

	@TempDir
	Path world;
	private Path root;
	private PlanSync sync;
	private UUID planId;
	private final UUID player = UUID.randomUUID();

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		stone = Blocks.STONE.defaultBlockState();
		glass = Blocks.GLASS.defaultBlockState();
	}

	@BeforeEach
	void setUp() {
		root = world.resolve("building-planner");
		sync = new PlanSync(SyncLimits.DEFAULT, (to, payload) -> {
		});
		planId = UUID.randomUUID();
		sync.addPlan(planId, plan(stone));
		sync.subscribe(player, planId);
	}

	private static Plan plan(BlockState corner) {
		Plan plan = Plan.create("Shed", "Alex", BlockPos.ZERO, new Vec3i(3, 2, 3), START);
		plan.firstRegion().set(BlockPos.ZERO, corner);
		return plan;
	}

	private void edit(BlockPos pos, BlockState state) {
		sync.edit(player, planId, 1, EditKind.SHAPE, List.of(new BlockChange("Shed", pos, state)));
	}

	private Path sharedFile(Plan plan) throws IOException {
		Path file = world.resolve("syncmatics").resolve("shared.litematic");
		Files.createDirectories(file.getParent());
		LitematicFormat.writeFile(plan, file);
		return file;
	}

	@Test
	void loadsTheAddOnsFileOrElseSyncmaticas() throws IOException {
		PlanStore store = new PlanStore(root, START);
		Path shared = sharedFile(plan(glass));

		assertEquals(glass, store.load(planId, shared).orElseThrow().firstRegion().get(BlockPos.ZERO));

		Files.createDirectories(store.planFile(planId).getParent());
		LitematicFormat.writeFile(plan(stone), store.planFile(planId));
		assertEquals(stone, store.load(planId, shared).orElseThrow().firstRegion().get(BlockPos.ZERO));
		store.close();
	}

	@Test
	void loadsNothingFromAnUnreadableFileAndLeavesIt() throws IOException {
		PlanStore store = new PlanStore(root, START);
		Files.createDirectories(store.planFile(planId).getParent());
		Files.writeString(store.planFile(planId), "not a plan");

		assertTrue(store.load(planId, sharedFile(plan(glass))).isEmpty());
		assertTrue(store.load(UUID.randomUUID(), null).isEmpty());
		assertEquals("not a plan", Files.readString(store.planFile(planId)));
		store.close();
	}

	@Test
	void writesAChangedPlanAtMostEveryThirtySeconds() throws IOException {
		PlanStore store = new PlanStore(root, START);
		Path file = store.planFile(planId);
		edit(new BlockPos(1, 0, 0), glass);

		store.tick(sync, START + PlanStore.SAVE_INTERVAL_MILLIS - 1);
		assertFalse(Files.exists(file), "not before 30 seconds");
		store.tick(sync, START + PlanStore.SAVE_INTERVAL_MILLIS);
		store.close();
		Plan saved = LitematicFormat.readFile(file);
		assertEquals(glass, saved.firstRegion().get(new BlockPos(1, 0, 0)));
		assertEquals(2, saved.firstRegion().nonAirBlocks());
		assertFalse(Files.exists(file.resolveSibling(file.getFileName() + ".tmp")));

		PlanStore next = new PlanStore(root, START);
		Files.delete(file);
		next.tick(sync, START + 2 * PlanStore.SAVE_INTERVAL_MILLIS);
		assertFalse(Files.exists(file), "an unchanged plan isn't written again");
		edit(new BlockPos(2, 0, 0), glass);
		next.tick(sync, START + 2 * PlanStore.SAVE_INTERVAL_MILLIS + 1);
		next.close();
		assertFalse(Files.exists(file), "a change waits for the next 30 seconds");
	}

	@Test
	void savesEveryChangedPlanAtStop() throws IOException {
		PlanStore store = new PlanStore(root, START);
		UUID untouched = UUID.randomUUID();
		sync.addPlan(untouched, plan(stone));
		edit(new BlockPos(1, 0, 0), glass);

		store.saveChanged(sync, START + 1);
		store.close();

		assertEquals(glass, LitematicFormat.readFile(store.planFile(planId)).firstRegion().get(new BlockPos(1, 0, 0)));
		assertFalse(Files.exists(store.planFile(untouched)));
	}

	@Test
	void keepsADeletedPlanAsItWasWhenDeleted() throws IOException {
		PlanStore store = new PlanStore(root, START);
		edit(new BlockPos(1, 0, 0), glass);
		store.saveChanged(sync, START + 1);
		edit(new BlockPos(2, 0, 0), glass);

		Optional<Plan> plan = sync.plan(planId);
		sync.removePlan(planId);
		store.archive(planId, plan, START + 2);
		store.close();

		assertFalse(Files.exists(store.planFile(planId)));
		Plan archived = LitematicFormat.readFile(store.deletedFile(planId));
		assertEquals(glass, archived.firstRegion().get(new BlockPos(2, 0, 0)));
		assertEquals(3, archived.firstRegion().nonAirBlocks());
	}

	@Test
	void movesTheFileOfADeletedPlanThatWasNotSyncing() throws IOException {
		PlanStore store = new PlanStore(root, START);
		Files.createDirectories(store.planFile(planId).getParent());
		LitematicFormat.writeFile(plan(glass), store.planFile(planId));

		store.archive(planId, Optional.empty(), START + 1);
		store.archive(UUID.randomUUID(), Optional.empty(), START + 1);
		store.close();

		assertFalse(Files.exists(store.planFile(planId)));
		assertEquals(glass, LitematicFormat.readFile(store.deletedFile(planId)).firstRegion().get(BlockPos.ZERO));
	}

	@Test
	void onlyThePlansSharerMayDeleteItUnlessOperatorsMay() {
		UUID owner = UUID.randomUUID();
		UUID other = UUID.randomUUID();

		assertTrue(SyncmaticaPlans.allowsDelete(owner, owner, false, false));
		assertFalse(SyncmaticaPlans.allowsDelete(owner, other, false, false));
		assertFalse(SyncmaticaPlans.allowsDelete(owner, other, true, false));
		assertTrue(SyncmaticaPlans.allowsDelete(owner, other, true, true));
		assertFalse(SyncmaticaPlans.allowsDelete(owner, other, false, true));
	}
}
