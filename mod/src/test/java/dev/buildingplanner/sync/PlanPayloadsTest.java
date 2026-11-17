package dev.buildingplanner.sync;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.plan.Plan;
import dev.buildingplanner.plan.RegionSnapshot;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PlanPayloadsTest {
	private static BlockState stairs;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		stairs = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
	}

	private static <T> T roundTrip(StreamCodec<FriendlyByteBuf, T> codec, T value) {
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		codec.encode(buf, value);
		T read = codec.decode(buf);
		assertEquals(0, buf.readableBytes(), "the codec read every byte it wrote");
		return read;
	}

	@Test
	void clientPayloadsRoundTrip() {
		UUID plan = UUID.randomUUID();
		assertEquals(new PlanPayloads.Subscribe(plan), roundTrip(PlanPayloads.Subscribe.CODEC, new PlanPayloads.Subscribe(plan)));
		assertEquals(new PlanPayloads.Unsubscribe(plan), roundTrip(PlanPayloads.Unsubscribe.CODEC, new PlanPayloads.Unsubscribe(plan)));
		PlanPayloads.Edit edit = new PlanPayloads.Edit(plan, 42, EditKind.ERASE, List.of(
			new BlockChange("Main", new BlockPos(-3, 70, 1_000_000), Blocks.AIR.defaultBlockState()),
			new BlockChange("Tower", new BlockPos(1, 2, 3), stairs),
			new BlockChange("Main", BlockPos.ZERO, Blocks.STONE.defaultBlockState())));
		PlanPayloads.Edit read = roundTrip(PlanPayloads.Edit.CODEC, edit);
		assertEquals(edit.plan(), read.plan());
		assertEquals(42, read.clientSeq());
		assertEquals(EditKind.ERASE, read.kind());
		// Changes travel grouped by region, keeping each region's own order.
		assertEquals(List.of(edit.changes().get(0), edit.changes().get(2), edit.changes().get(1)), read.changes());
	}

	@Test
	void serverPayloadsRoundTrip() {
		UUID plan = UUID.randomUUID();
		PlanPayloads.Hello hello = new PlanPayloads.Hello(PlanPayloads.PROTOCOL_VERSION, new SyncLimits(1234));
		assertEquals(hello, roundTrip(PlanPayloads.Hello.CODEC, hello));

		PlanPayloads.Applied applied = new PlanPayloads.Applied(plan, 7, UUID.randomUUID(), PlanPayloads.NO_CLIENT_SEQ,
			List.of(new RegionBox("Main", new BlockPos(-4, 0, 0), new Vec3i(9, 3, 2))),
			List.of(new BlockChange("Main", new BlockPos(-4, 0, 0), stairs)));
		assertEquals(applied, roundTrip(PlanPayloads.Applied.CODEC, applied));

		PlanPayloads.Rejected rejected = new PlanPayloads.Rejected(plan, 3, PlayerMessage.tooManyBlocks(600, 512));
		assertEquals(rejected, roundTrip(PlanPayloads.Rejected.CODEC, rejected));
	}

	@Test
	void aSnapshotRoundTripsToTheSamePlan() {
		Plan plan = Plan.create("Hall", "Alex", new BlockPos(-2, 0, 5), new Vec3i(5, 3, 4), 0);
		plan.firstRegion().set(new BlockPos(-2, 0, 5), stairs);
		plan.firstRegion().set(new BlockPos(2, 2, 8), Blocks.GLASS.defaultBlockState());
		PlanPayloads.Snapshot snapshot = new PlanPayloads.Snapshot(UUID.randomUUID(), 12, plan.snapshot());

		PlanPayloads.Snapshot read = roundTrip(PlanPayloads.Snapshot.CODEC, snapshot);

		assertEquals(snapshot.plan(), read.plan());
		assertEquals(12, read.rev());
		RegionSnapshot region = read.regions().getFirst();
		assertEquals("Hall", region.name());
		// Only the two sections holding blocks travel.
		assertEquals(2, region.sections().size());
		for (int i = 0; i < 2; i++) {
			assertArrayEquals(snapshot.regions().getFirst().sections().get(i).packed(), region.sections().get(i).packed());
		}
		Plan copy = Plan.fromSnapshot(read.regions());
		assertEquals(new BlockPos(-2, 0, 5), copy.firstRegion().minCorner());
		assertEquals(new Vec3i(5, 3, 4), copy.firstRegion().dimensions());
		assertEquals(stairs, copy.firstRegion().get(new BlockPos(-2, 0, 5)));
		assertEquals(Blocks.GLASS.defaultBlockState(), copy.firstRegion().get(new BlockPos(2, 2, 8)));
	}

	/**
	 * A plan at the size cap on every side with one full floor: its snapshot carries the floor's sections, about 1 MiB,
	 * where every position of the box took at least 32 MiB.
	 */
	@Test
	void aSnapshotOfALargeMostlyEmptyPlanCarriesOnlyItsBlocks() {
		Plan plan = Plan.create("Hall", "Alex", BlockPos.ZERO, new Vec3i(512, 512, 512), 0);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = 0; x < 512; x++) {
			for (int z = 0; z < 512; z++) {
				plan.firstRegion().set(pos.set(x, 0, z), stairs);
			}
		}
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

		PlanPayloads.Snapshot.CODEC.encode(buf, new PlanPayloads.Snapshot(UUID.randomUUID(), 1, plan.snapshot()));

		int bytes = buf.readableBytes();
		assertTrue(bytes > 1024 * 1024 && bytes < 1024 * 1024 + 16 * 1024, "the snapshot took " + bytes + " bytes");
	}
}
