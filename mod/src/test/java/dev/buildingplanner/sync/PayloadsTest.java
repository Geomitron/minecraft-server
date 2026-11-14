package dev.buildingplanner.sync;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.buildingplanner.plan.Plan;
import dev.buildingplanner.plan.RegionSnapshot;
import io.netty.buffer.Unpooled;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

class PayloadsTest {
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
		assertEquals(new Payloads.Subscribe(plan), roundTrip(Payloads.Subscribe.CODEC, new Payloads.Subscribe(plan)));
		assertEquals(new Payloads.Unsubscribe(plan), roundTrip(Payloads.Unsubscribe.CODEC, new Payloads.Unsubscribe(plan)));
		Payloads.Edit edit = new Payloads.Edit(plan, 42, EditKind.ERASE, List.of(
			new BlockChange("Main", new BlockPos(-3, 70, 1_000_000), Blocks.AIR.defaultBlockState()),
			new BlockChange("Tower", new BlockPos(1, 2, 3), stairs),
			new BlockChange("Main", BlockPos.ZERO, Blocks.STONE.defaultBlockState())));
		Payloads.Edit read = roundTrip(Payloads.Edit.CODEC, edit);
		assertEquals(edit.plan(), read.plan());
		assertEquals(42, read.clientSeq());
		assertEquals(EditKind.ERASE, read.kind());
		// Changes travel grouped by region, keeping each region's own order.
		assertEquals(List.of(edit.changes().get(0), edit.changes().get(2), edit.changes().get(1)), read.changes());
	}

	@Test
	void serverPayloadsRoundTrip() {
		UUID plan = UUID.randomUUID();
		Payloads.Hello hello = new Payloads.Hello(Payloads.PROTOCOL_VERSION, new SyncLimits(1234, 99));
		assertEquals(hello, roundTrip(Payloads.Hello.CODEC, hello));

		Payloads.Applied applied = new Payloads.Applied(plan, 7, UUID.randomUUID(), Payloads.NO_CLIENT_SEQ,
			List.of(new RegionBox("Main", new BlockPos(-4, 0, 0), new Vec3i(9, 3, 2))),
			List.of(new BlockChange("Main", new BlockPos(-4, 0, 0), stairs)));
		assertEquals(applied, roundTrip(Payloads.Applied.CODEC, applied));

		Payloads.Rejected rejected = new Payloads.Rejected(plan, 3, PlayerMessage.planTooLarge(600, 512));
		assertEquals(rejected, roundTrip(Payloads.Rejected.CODEC, rejected));
		Payloads.Notice notice = new Payloads.Notice(PlayerMessage.planGone());
		assertEquals(notice, roundTrip(Payloads.Notice.CODEC, notice));
	}

	@Test
	void aSnapshotRoundTripsToTheSamePlan() {
		Plan plan = Plan.create("Hall", "Alex", new BlockPos(-2, 0, 5), new Vec3i(5, 3, 4), 0);
		plan.firstRegion().set(new BlockPos(-2, 0, 5), stairs);
		plan.firstRegion().set(new BlockPos(2, 2, 8), Blocks.GLASS.defaultBlockState());
		Payloads.Snapshot snapshot = new Payloads.Snapshot(UUID.randomUUID(), 12, plan.snapshot());

		Payloads.Snapshot read = roundTrip(Payloads.Snapshot.CODEC, snapshot);

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

		Payloads.Snapshot.CODEC.encode(buf, new Payloads.Snapshot(UUID.randomUUID(), 1, plan.snapshot()));

		int bytes = buf.readableBytes();
		assertTrue(bytes > 1024 * 1024 && bytes < 1024 * 1024 + 16 * 1024, "the snapshot took " + bytes + " bytes");
	}

	@Test
	void oneBlockLeftAsItIsHasItsOwnText() {
		assertEquals(new PlayerMessage("buildingplanner.undo.left_as_it_is", List.of()), PlayerMessage.blocksLeftAsTheyAre(1));
		assertEquals(new PlayerMessage("buildingplanner.undo.left_as_they_are", List.of("2")),
			PlayerMessage.blocksLeftAsTheyAre(2));
	}

	/**
	 * Every message the add-on can send has its text in en_us.json, with one %s per argument, and every other text there
	 * is a key binding's ({@code key.}) or a screen's ({@code buildingplanner.ui.}).
	 */
	@Test
	void everyPlayerMessageHasItsText() throws Exception {
		JsonObject lang;
		try (var reader = new InputStreamReader(
			PayloadsTest.class.getResourceAsStream("/assets/buildingplanner/lang/en_us.json"), StandardCharsets.UTF_8)) {
			lang = JsonParser.parseReader(reader).getAsJsonObject();
		}
		Set<String> used = new HashSet<>();
		for (Method factory : PlayerMessage.class.getDeclaredMethods()) {
			if (!Modifier.isStatic(factory.getModifiers()) || factory.getReturnType() != PlayerMessage.class) {
				continue;
			}
			// Counts of 1 and 2, since a message can have its own text for one.
			for (int count : new int[] {1, 2}) {
				Object[] args = Arrays.stream(factory.getParameterTypes()).map(type -> type == String.class ? (Object) "Alex" : (Object) count).toArray();
				PlayerMessage message = (PlayerMessage) factory.invoke(null, args);
				assertTrue(lang.has(message.key()), "en_us.json has " + message.key());
				Matcher placeholders = Pattern.compile("%s").matcher(lang.get(message.key()).getAsString());
				assertEquals(message.args().size(), placeholders.results().count(), message.key() + "'s placeholders");
				used.add(message.key());
			}
		}
		Set<String> others = new HashSet<>(lang.keySet());
		others.removeAll(used);
		others.removeIf(key -> key.startsWith("key.") || key.startsWith("buildingplanner.ui."));
		assertEquals(Set.of(), others, "en_us.json holds only messages the add-on sends, key bindings and screens");
	}
}
