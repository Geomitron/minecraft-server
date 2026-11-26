package dev.buildingplanner.sync;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.buildingplanner.layer.PackedSection;
import io.netty.buffer.Unpooled;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
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
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PayloadsTest {
	private static final Identifier NETHER = Identifier.withDefaultNamespace("the_nether");
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
		List<ChunkPos> columns = List.of(new ChunkPos(-1, 2), new ChunkPos(1_875_000, -1_875_000));
		assertEquals(new Payloads.Watch(NETHER, columns), roundTrip(Payloads.Watch.CODEC, new Payloads.Watch(NETHER, columns)));
		assertEquals(new Payloads.Unwatch(NETHER, columns),
			roundTrip(Payloads.Unwatch.CODEC, new Payloads.Unwatch(NETHER, columns)));
		Payloads.Edit edit = new Payloads.Edit(NETHER, 42, EditKind.ERASE, List.of(
			new LayerChange(new BlockPos(-3, -64, 1_000_000), Blocks.AIR.defaultBlockState()),
			new LayerChange(new BlockPos(1, 319, 3), stairs)));
		assertEquals(edit, roundTrip(Payloads.Edit.CODEC, edit));
	}

	/** An edit of the most blocks the wire carries, each with the highest block state id, fits what the payload is registered for. */
	@Test
	void anEditOfTheMostBlocksFitsItsRegisteredSize() {
		BlockState highest = Block.BLOCK_STATE_REGISTRY.byId(Block.BLOCK_STATE_REGISTRY.size() - 1);
		BlockPos far = new BlockPos(-30_000_000, -64, 30_000_000);
		Payloads.Edit edit = new Payloads.Edit(Identifier.fromNamespaceAndPath("some_namespace", "some/long/dimension_path"),
			Integer.MAX_VALUE, EditKind.SHAPE,
			java.util.Collections.nCopies(SyncLimits.WIRE_MAX_EDIT_BLOCKS, new LayerChange(far, highest)));
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

		Payloads.Edit.CODEC.encode(buf, edit);

		assertTrue(buf.readableBytes() <= Payloads.MAX_EDIT_BYTES,
			buf.readableBytes() + " bytes, room for " + Payloads.MAX_EDIT_BYTES);
		assertEquals(edit, Payloads.Edit.CODEC.decode(buf));
	}

	@Test
	void theDefaultCapIsTheMostAnEditCanCarry() {
		assertEquals(200_000, SyncLimits.DEFAULT.maxEditBlocks());
		assertEquals(SyncLimits.WIRE_MAX_EDIT_BLOCKS, SyncLimits.DEFAULT.maxEditBlocks());
	}

	@Test
	void serverPayloadsRoundTrip() {
		Payloads.Hello hello = new Payloads.Hello(Payloads.PROTOCOL_VERSION, 1234);
		assertEquals(hello, roundTrip(Payloads.Hello.CODEC, hello));
		Payloads.Applied applied = new Payloads.Applied(NETHER, UUID.randomUUID(), Payloads.NO_CLIENT_SEQ,
			List.of(new LayerChange(new BlockPos(-4, 0, 0), stairs)));
		assertEquals(applied, roundTrip(Payloads.Applied.CODEC, applied));
		Payloads.Rejected rejected = new Payloads.Rejected(3, PlayerMessage.tooManyBlocks(600, 512));
		assertEquals(rejected, roundTrip(Payloads.Rejected.CODEC, rejected));
		Payloads.Notice notice = new Payloads.Notice(PlayerMessage.nothingToUndo());
		assertEquals(notice, roundTrip(Payloads.Notice.CODEC, notice));
	}

	@Test
	void columnsRoundTripWithTheirSections() {
		long[] blocks = new long[128];
		blocks[0] = 0b01;
		Payloads.Columns columns = new Payloads.Columns(NETHER, List.of(
			new Payloads.Column(new ChunkPos(3, -4), List.of(new PackedSection(-4, List.of(Blocks.AIR.defaultBlockState(),
				stairs), blocks))),
			new Payloads.Column(new ChunkPos(4, -4), List.of())));

		Payloads.Columns read = roundTrip(Payloads.Columns.CODEC, columns);

		assertEquals(NETHER, read.dimension());
		assertEquals(List.of(new ChunkPos(3, -4), new ChunkPos(4, -4)), read.columns().stream().map(Payloads.Column::pos).toList());
		PackedSection section = read.columns().getFirst().sections().getFirst();
		assertEquals(-4, section.y());
		assertEquals(List.of(Blocks.AIR.defaultBlockState(), stairs), section.palette());
		assertArrayEquals(blocks, section.blocks());
		assertEquals(List.of(), read.columns().get(1).sections());
	}

	/** A list with no name or time, made after the bootstrap, which items need. */
	private static ListData planks() {
		return new ListData(Optional.empty(), OptionalLong.empty(), NETHER, new BlockPos(-3, -64, 1),
			new BlockPos(10, 2, 20), 4, ListSort.COUNT_MISSING, true, false,
			List.of(new ListData.Row(Items.OAK_PLANKS, 10, 4, 1), new ListData.Row(Items.STONE, 20, 0, 0)));
	}

	@Test
	void listPayloadsFromTheGameRoundTrip() {
		ListData named = planks().named("Planks", 1_763_594_460_000L);
		assertEquals(new Payloads.SaveList("Planks", planks()), roundTrip(Payloads.SaveList.CODEC,
			new Payloads.SaveList("Planks", planks())));
		assertEquals(new Payloads.DeleteList("Planks"), roundTrip(Payloads.DeleteList.CODEC, new Payloads.DeleteList("Planks")));
		assertEquals(new Payloads.OpenList(""), roundTrip(Payloads.OpenList.CODEC, new Payloads.OpenList("")));
		Payloads.CurrentList current = new Payloads.CurrentList(Optional.of(named), true, false);
		assertEquals(current, roundTrip(Payloads.CurrentList.CODEC, current));
		Payloads.CurrentList none = new Payloads.CurrentList(Optional.empty(), false, true);
		assertEquals(none, roundTrip(Payloads.CurrentList.CODEC, none));
	}

	@Test
	void listPayloadsFromTheServerRoundTrip() {
		Payloads.Lists lists = new Payloads.Lists(Optional.of(Payloads.ListSummary.of(planks().counted(5))),
			List.of(Payloads.ListSummary.of(planks().named("Planks", 7)), new Payloads.ListSummary("Wall", 6, new Vec3i(12, 5, 8))));
		assertEquals(lists, roundTrip(Payloads.Lists.CODEC, lists));
		assertEquals(new Payloads.ListSummary("", 5, new Vec3i(14, 67, 20)), lists.lastCounted().orElseThrow());
		Payloads.Lists empty = new Payloads.Lists(Optional.empty(), List.of());
		assertEquals(empty, roundTrip(Payloads.Lists.CODEC, empty));
		Payloads.OpenedList opened = new Payloads.OpenedList(planks().named("Planks", 7));
		assertEquals(opened, roundTrip(Payloads.OpenedList.CODEC, opened));
		Payloads.Current current = new Payloads.Current(Optional.of(planks().counted(5)), true);
		assertEquals(current, roundTrip(Payloads.Current.CODEC, current));
		Payloads.Current none = new Payloads.Current(Optional.empty(), false);
		assertEquals(none, roundTrip(Payloads.Current.CODEC, none));
	}

	/** A list of exactly 64 KiB as sent is taken; one byte more is skipped, and the message says so. */
	@Test
	void aListAt64KibIsTakenAndOneOverItRefused() {
		ListData atLimit = listOfBytes(ListLimits.MAX_LIST_BYTES);
		ListData over = listOfBytes(ListLimits.MAX_LIST_BYTES + 1);
		assertEquals(ListLimits.MAX_LIST_BYTES, Payloads.listBytes(atLimit));
		assertEquals(ListLimits.MAX_LIST_BYTES + 1, Payloads.listBytes(over));

		Payloads.SaveList saved = roundTrip(Payloads.SaveList.CODEC, new Payloads.SaveList("Big", atLimit));
		assertEquals(atLimit, saved.list());
		assertFalse(saved.tooLarge());
		Payloads.SaveList refused = roundTrip(Payloads.SaveList.CODEC, new Payloads.SaveList("Big", over));
		assertEquals("Big", refused.name());
		assertNull(refused.list());
		assertTrue(refused.tooLarge());

		Payloads.CurrentList current = roundTrip(Payloads.CurrentList.CODEC,
			new Payloads.CurrentList(Optional.of(atLimit), true, true));
		assertEquals(Optional.of(atLimit), current.list());
		assertFalse(current.tooLarge());
		Payloads.CurrentList currentOver = roundTrip(Payloads.CurrentList.CODEC,
			new Payloads.CurrentList(Optional.of(over), true, true));
		assertEquals(Optional.empty(), currentOver.list());
		assertTrue(currentOver.tooLarge());
		assertTrue(currentOver.panel(), "the fields after the skipped list still read");
	}

	/**
	 * A list of exactly that many bytes as sent: rows of stone, as many as fit, then a total past 127, which takes a
	 * byte more, in as many rows as bytes are left. The codec doesn't check the rows; the server does.
	 */
	private static ListData listOfBytes(int bytes) {
		ListData.Row zero = new ListData.Row(Items.STONE, 0, 0, 0);
		ListData.Row wide = new ListData.Row(Items.STONE, 128, 0, 0);
		int empty = Payloads.listBytes(withRows(List.of()));
		int row = Payloads.listBytes(withRows(List.of(zero))) - empty;
		int count = (bytes - empty) / row;
		// The row count's own length grows past one byte.
		while (empty - 1 + varIntBytes(count) + count * row > bytes) {
			count--;
		}
		int left = bytes - (empty - 1 + varIntBytes(count) + count * row);
		List<ListData.Row> rows = new ArrayList<>(java.util.Collections.nCopies(count, zero));
		for (int i = 0; i < left; i++) {
			rows.set(i, wide);
		}
		return withRows(rows);
	}

	private static ListData withRows(List<ListData.Row> rows) {
		return new ListData(Optional.of("Big"), OptionalLong.of(1), NETHER, BlockPos.ZERO, BlockPos.ZERO, 1,
			ListSort.COUNT_TOTAL, false, false, rows);
	}

	private static int varIntBytes(int value) {
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		buf.writeVarInt(value);
		return buf.readableBytes();
	}

	@Test
	void oneBlockLeftAsItIsHasItsOwnText() {
		assertEquals(new PlayerMessage("buildingplanner.undo.left_as_it_is", List.of()), PlayerMessage.blocksLeftAsTheyAre(1));
		assertEquals(new PlayerMessage("buildingplanner.undo.left_as_they_are", List.of("2")),
			PlayerMessage.blocksLeftAsTheyAre(2));
	}

	@Test
	void theDeleteBoxsCountIsWrittenWithDigitGrouping() {
		assertEquals(new PlayerMessage("buildingplanner.delete.done", List.of("999")), PlayerMessage.erased(999));
		assertEquals(new PlayerMessage("buildingplanner.delete.done", List.of("16,001")), PlayerMessage.erased(16001));
		assertEquals(new PlayerMessage("buildingplanner.delete.done", List.of("200,000")), PlayerMessage.erased(200000));
		assertEquals(new PlayerMessage("buildingplanner.delete.done_one", List.of()), PlayerMessage.erased(1));
	}

	/**
	 * Every message the add-on can send has its text in en_us.json, with one %s per argument, and every other text there
	 * is a key binding's ({@code key.}), a screen's ({@code buildingplanner.ui.}) or a screen's (i) icon's
	 * ({@code buildingplanner.info.}).
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
		others.removeIf(key -> key.startsWith("key.") || key.startsWith("buildingplanner.ui.")
			|| key.startsWith("buildingplanner.info."));
		assertEquals(Set.of(), others, "en_us.json holds only messages the add-on sends, key bindings and screens");
	}
}
