package dev.buildingplanner.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.sync.ListData;
import dev.buildingplanner.sync.ListSort;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ListFilesTest {
	private static final UUID ALEX = UUID.fromString("527e7f9d-ae90-3a44-aa88-30a47a9cdf34");

	@TempDir
	Path world;
	private Path root;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@BeforeEach
	void setUp() {
		root = world.resolve("building-planner").resolve("material-lists");
	}

	static ListData list(int planks) {
		return new ListData(Optional.empty(), OptionalLong.empty(), Identifier.withDefaultNamespace("overworld"),
			new BlockPos(20, -60, -11), new BlockPos(29, -59, -9), 3, ListSort.COUNT_MISSING, true, true,
			List.of(new ListData.Row(Items.OAK_PLANKS, planks, 4, 1), new ListData.Row(Items.STONE, 20, 0, 0)));
	}

	private static PlayerLists lists() {
		PlayerLists lists = new PlayerLists();
		lists.save(list(10).named("Planks", 1_000));
		lists.save(list(12).named("Wall", 2_000));
		lists.setLastCounted(Optional.of(list(14).counted(3_000)));
		lists.setCurrent(Optional.of(list(10).named("Planks", 1_000)));
		lists.setPanel(true);
		return lists;
	}

	private static void assertSame(PlayerLists expected, PlayerLists actual) {
		assertEquals(expected.saved(), actual.saved());
		assertEquals(expected.lastCounted(), actual.lastCounted());
		assertEquals(expected.current(), actual.current());
		assertEquals(expected.panel(), actual.panel());
	}

	@Test
	void aPlayersListsRoundTripThroughTheirFile() {
		try (ListFiles files = new ListFiles(root)) {
			files.write(ALEX, lists());
		}
		assertTrue(Files.exists(root.resolve(ALEX + ".dat")));

		assertSame(lists(), new ListFiles(root).read(ALEX));
	}

	@Test
	void aCurrentListWithNoNameOrTimeAndNoLastCountedRoundTrip() {
		PlayerLists lists = new PlayerLists();
		lists.setCurrent(Optional.of(list(10)));
		try (ListFiles files = new ListFiles(root)) {
			files.write(ALEX, lists);
		}

		assertSame(lists, new ListFiles(root).read(ALEX));
	}

	@Test
	void theFileHoldsTheLayoutTheDesignGives() throws IOException {
		try (ListFiles files = new ListFiles(root)) {
			files.write(ALEX, lists());
		}
		CompoundTag root = NbtIo.readCompressed(this.root.resolve(ALEX + ".dat"), net.minecraft.nbt.NbtAccounter.unlimitedHeap());

		assertEquals(1, root.getIntOr("version", 0));
		assertTrue(root.getInt("data_version").isPresent());
		assertEquals(2, root.getListOrEmpty("saved").size());
		CompoundTag lastCounted = root.getCompoundOrEmpty("last_counted");
		assertEquals(3_000, lastCounted.getLongOr("time", 0));
		assertFalse(lastCounted.contains("name"));
		assertEquals("minecraft:overworld", lastCounted.getStringOr("dimension", ""));
		assertEquals(20, lastCounted.getCompoundOrEmpty("min").getIntOr("x", 0));
		assertEquals(-9, lastCounted.getCompoundOrEmpty("max").getIntOr("z", 0));
		assertEquals(3, lastCounted.getIntOr("multiplier", 0));
		assertEquals("COUNT_MISSING", lastCounted.getStringOr("sort", ""));
		assertEquals(1, lastCounted.getByteOr("reverse", (byte) 0));
		assertEquals(1, lastCounted.getByteOr("hide_available", (byte) 0));
		CompoundTag row = lastCounted.getListOrEmpty("rows").getCompoundOrEmpty(0);
		assertEquals("minecraft:oak_planks", row.getStringOr("item", ""));
		assertEquals(14, row.getIntOr("total", 0));
		assertEquals(4, row.getIntOr("missing", 0));
		assertEquals(1, row.getIntOr("wrong", 0));
		assertEquals("Planks", root.getCompoundOrEmpty("current").getStringOr("name", ""));
		assertEquals(1, root.getByteOr("panel", (byte) 0));
	}

	@Test
	void anItemTheGameDoesntKnowDropsFromItsList() throws IOException {
		CompoundTag contents = ListFiles.toTag(lists());
		ListTag rows = contents.getCompoundOrEmpty("last_counted").getListOrEmpty("rows");
		rows.getCompoundOrEmpty(0).putString("item", "someothermod:brass_planks");
		Files.createDirectories(root);
		NbtIo.writeCompressed(contents, root.resolve(ALEX + ".dat"));

		PlayerLists read = new ListFiles(root).read(ALEX);

		assertEquals(List.of(new ListData.Row(Items.STONE, 20, 0, 0)), read.lastCounted().orElseThrow().rows());
		assertEquals(lists().saved(), read.saved());
		assertTrue(Files.exists(root.resolve(ALEX + ".dat")), "the file stays");
	}

	@Test
	void aPlayerWithNoFileHasNoLists() {
		PlayerLists read = new ListFiles(root).read(ALEX);

		assertSame(new PlayerLists(), read);
		assertFalse(Files.exists(root));
	}

	@Test
	void aFileCutShortIsRenamedAndThePlayerStartsWithNoLists() throws IOException {
		try (ListFiles files = new ListFiles(root)) {
			files.write(ALEX, lists());
		}
		Path file = root.resolve(ALEX + ".dat");
		byte[] whole = Files.readAllBytes(file);
		byte[] cut = Arrays.copyOf(whole, whole.length / 2);
		Files.write(file, cut);

		assertSame(new PlayerLists(), new ListFiles(root).read(ALEX));
		assertFalse(Files.exists(file));
		assertArrayEquals(cut, Files.readAllBytes(root.resolve(ALEX + ".dat.unreadable")));
	}

	@Test
	void aSecondUnreadableFileReplacesTheOlderOne() throws IOException {
		Files.createDirectories(root);
		Path file = root.resolve(ALEX + ".dat");
		Files.writeString(file, "not compressed NBT", StandardCharsets.UTF_8);
		new ListFiles(root).read(ALEX);
		Files.writeString(file, "still not compressed NBT", StandardCharsets.UTF_8);

		assertSame(new PlayerLists(), new ListFiles(root).read(ALEX));
		assertEquals("still not compressed NBT", Files.readString(root.resolve(ALEX + ".dat.unreadable")));
	}

	@Test
	void anotherVersionOrAMissingEntryMakesAFileUnreadable() throws IOException {
		List<Consumer<CompoundTag>> damages = List.of(
			contents -> contents.putInt("version", 2),
			contents -> contents.remove("data_version"),
			contents -> contents.remove("saved"),
			contents -> contents.remove("panel"),
			contents -> contents.getCompoundOrEmpty("last_counted").remove("time"),
			contents -> contents.getCompoundOrEmpty("last_counted").remove("rows"),
			contents -> contents.getCompoundOrEmpty("current").remove("min"),
			contents -> contents.getListOrEmpty("saved").getCompoundOrEmpty(0).remove("name"),
			contents -> contents.getCompoundOrEmpty("last_counted").getListOrEmpty("rows").getCompoundOrEmpty(0)
				.remove("wrong"));
		Files.createDirectories(root);
		Path file = root.resolve(ALEX + ".dat");
		for (Consumer<CompoundTag> damage : damages) {
			CompoundTag contents = ListFiles.toTag(lists());
			damage.accept(contents);
			NbtIo.writeCompressed(contents, file);

			assertSame(new PlayerLists(), new ListFiles(root).read(ALEX));
			assertFalse(Files.exists(file));
			assertTrue(Files.exists(root.resolve(ALEX + ".dat.unreadable")));
		}
	}
}
