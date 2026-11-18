package dev.buildingplanner.sync;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.buildingplanner.layer.PackedSection;
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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
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
