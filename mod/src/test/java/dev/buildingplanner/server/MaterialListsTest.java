package dev.buildingplanner.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.sync.ListData;
import dev.buildingplanner.sync.Payloads;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MaterialListsTest {
	private static final UUID ALEX = UUID.fromString("527e7f9d-ae90-3a44-aa88-30a47a9cdf34");
	private static final UUID SAM = UUID.fromString("0e1f5d7c-2b3a-3c4d-8e9f-a0b1c2d3e4f5");
	private static final Identifier OVERWORLD = Identifier.withDefaultNamespace("overworld");

	/** The files: each player's lists as last written, and how many writes and reads there were. */
	private final Map<UUID, PlayerLists> files = new HashMap<>();
	private final List<UUID> writes = new ArrayList<>();
	private final List<UUID> reads = new ArrayList<>();
	private final List<CustomPacketPayload> sent = new ArrayList<>();
	private MaterialLists lists;
	private long now;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@BeforeEach
	void setUp() {
		now = 1_000_000;
		lists = new MaterialLists(new MaterialLists.Store() {
			@Override
			public PlayerLists read(UUID player) {
				reads.add(player);
				return files.getOrDefault(player, new PlayerLists());
			}

			@Override
			public void write(UUID player, PlayerLists lists) {
				writes.add(player);
				files.put(player, lists);
			}
		}, (player, payload) -> {
			if (player.equals(ALEX)) {
				sent.add(payload);
			}
		}, OVERWORLD::equals, now);
	}

	private static ListData list(int planks) {
		return ListFilesTest.list(planks);
	}

	private void save(String name, ListData list) {
		lists.save(ALEX, "Alex", new Payloads.SaveList(name, list), now);
	}

	private void current(ListData list, boolean counted) {
		lists.current(ALEX, "Alex", new Payloads.CurrentList(Optional.of(list), counted, false), now);
	}

	private PlayerLists alex() {
		return lists.lists(ALEX);
	}

	private CustomPacketPayload lastSent() {
		return sent.getLast();
	}

	@Test
	void joinReadsThePlayersFileOnceAndSendsTheIndexThenTheCurrentList() {
		PlayerLists kept = new PlayerLists();
		kept.save(list(10).named("Planks", 5));
		kept.setLastCounted(Optional.of(list(12).counted(6)));
		kept.setCurrent(Optional.of(list(10).named("Planks", 5)));
		kept.setPanel(true);
		files.put(ALEX, kept);

		List<CustomPacketPayload> first = lists.join(ALEX);
		List<CustomPacketPayload> second = lists.join(ALEX);

		assertEquals(List.of(ALEX), reads);
		Payloads.Lists index = new Payloads.Lists(Optional.of(Payloads.ListSummary.of(list(12).counted(6))),
			List.of(Payloads.ListSummary.of(list(10).named("Planks", 5))));
		assertEquals(List.of(index, new Payloads.Current(Optional.of(list(10).named("Planks", 5)), true)), first);
		assertEquals(first, second);
	}

	@Test
	void aPlayerWithNoListsJoinsWithAnEmptyIndexAndNoCurrentList() {
		assertEquals(List.of(new Payloads.Lists(Optional.empty(), List.of()), new Payloads.Current(Optional.empty(), false)),
			lists.join(ALEX));
	}

	@Test
	void aNameSavedTwiceReplacesTheFirst() {
		save("Planks", list(10));
		now += 1_000;
		save("Wall", list(11));
		now += 1_000;
		save("Planks", list(12));

		assertEquals(List.of(list(12).named("Planks", now), list(11).named("Wall", now - 1_000)), alex().saved());
		assertEquals(new Payloads.Lists(Optional.empty(), List.of(Payloads.ListSummary.of(list(12).named("Planks", now)),
			Payloads.ListSummary.of(list(11).named("Wall", now - 1_000)))), lastSent());
	}

	@Test
	void aDeleteRemovesTheListAndSendsTheIndex() {
		save("Planks", list(10));
		save("Wall", list(11));

		lists.delete(ALEX, "Alex", new Payloads.DeleteList("Planks"), now);

		assertEquals(List.of(list(11).named("Wall", now)), alex().saved());
		assertEquals(new Payloads.Lists(Optional.empty(), List.of(Payloads.ListSummary.of(list(11).named("Wall", now)))),
			lastSent());
	}

	@Test
	void openSendsAKeptListOrLastCountedOrTheIndexForANameNoLongerKept() {
		save("Planks", list(10));
		current(list(12), true);

		lists.open(ALEX, "Alex", new Payloads.OpenList("Planks"), now);
		assertEquals(new Payloads.OpenedList(list(10).named("Planks", now)), lastSent());

		lists.open(ALEX, "Alex", new Payloads.OpenList(""), now);
		assertEquals(new Payloads.OpenedList(list(12).counted(now)), lastSent());

		lists.delete(ALEX, "Alex", new Payloads.DeleteList("Planks"), now);
		sent.clear();
		lists.open(ALEX, "Alex", new Payloads.OpenList("Planks"), now);
		assertEquals(List.of(new Payloads.Lists(Optional.of(Payloads.ListSummary.of(list(12).counted(now))), List.of())),
			sent);
	}

	@Test
	void openingLastCountedWhenThereIsNoneSendsTheIndex() {
		lists.open(ALEX, "Alex", new Payloads.OpenList(""), now);

		assertEquals(List.of(new Payloads.Lists(Optional.empty(), List.of())), sent);
	}

	@Test
	void aCountedCurrentListReplacesLastCountedAndAnUncountedOneLeavesIt() {
		current(list(10), true);
		assertEquals(Optional.of(list(10).counted(now)), alex().lastCounted());
		assertEquals(Optional.of(list(10)), alex().current());

		now += 5_000;
		current(list(11).named("Planks", 7), false);
		assertEquals(Optional.of(list(10).counted(now - 5_000)), alex().lastCounted());
		assertEquals(Optional.of(list(11).named("Planks", 7)), alex().current());

		current(list(12), true);
		assertEquals(Optional.of(list(12).counted(now)), alex().lastCounted());
		assertTrue(sent.isEmpty(), "current_list has no answer");
	}

	@Test
	void theCurrentListAndThePanelCanBeNone() {
		current(list(10), true);
		lists.current(ALEX, "Alex", new Payloads.CurrentList(Optional.empty(), false, true), now);

		assertEquals(Optional.empty(), alex().current());
		assertTrue(alex().panel());
		assertEquals(Optional.of(list(10).counted(now)), alex().lastCounted());
	}

	@Test
	void aPlayersFileIsWrittenAtMostEvery30SecondsWhileTheirListsChanged() {
		long start = now;
		save("Planks", list(10));
		lists.tick(start + 29_999);
		assertEquals(List.of(), writes);

		lists.tick(start + 30_000);
		assertEquals(List.of(ALEX), writes);
		assertEquals(List.of(list(10).named("Planks", start)), files.get(ALEX).saved());

		lists.tick(start + 60_000);
		assertEquals(List.of(ALEX), writes, "nothing changed since");

		now = start + 61_000;
		current(list(11), false);
		lists.tick(start + 89_999);
		assertEquals(List.of(ALEX), writes);
		lists.tick(start + 90_000);
		assertEquals(List.of(ALEX, ALEX), writes);
	}

	@Test
	void theSameCurrentListAgainChangesNothing() {
		current(list(10), false);
		lists.save();
		writes.clear();

		current(list(10), false);
		lists.save();

		assertEquals(List.of(), writes);
	}

	@Test
	void savingWhenTheServerStopsWritesOnlyThePlayersWhoseListsChanged() {
		lists.join(SAM);
		save("Planks", list(10));

		lists.save();

		assertEquals(List.of(ALEX), writes);
	}

	/** Each message that breaks a limit changes nothing and gets no answer. */
	private void assertDropped(Runnable message) {
		save("Kept", list(10));
		sent.clear();
		List<ListData> saved = alex().saved();
		Optional<ListData> lastCounted = alex().lastCounted();
		Optional<ListData> current = alex().current();

		message.run();

		assertEquals(saved, alex().saved());
		assertEquals(lastCounted, alex().lastCounted());
		assertEquals(current, alex().current());
		assertEquals(List.of(), sent);
	}

	@Test
	void aNameIsOneTo64CharactersWithNoControlCharacter() {
		assertDropped(() -> save("", list(10)));
		assertDropped(() -> save("x".repeat(65), list(10)));
		assertDropped(() -> save("Line\nbreak", list(10)));
		assertDropped(() -> save("Bell\u0007", list(10)));
		assertDropped(() -> save("Line separator", list(10)));
		assertDropped(() -> lists.delete(ALEX, "Alex", new Payloads.DeleteList("x".repeat(65)), now));
		assertDropped(() -> lists.open(ALEX, "Alex", new Payloads.OpenList("Tab\t"), now));
		assertDropped(() -> current(list(10).named("x".repeat(65), 1), false));

		save("x".repeat(64), list(10));
		assertEquals(Optional.of(list(10).named("x".repeat(64), now)), alex().find("x".repeat(64)));
	}

	@Test
	void aListHasAtMostOneRowForEachItem() {
		ListData twice = withRows(List.of(new ListData.Row(Items.STONE, 5, 0, 0), new ListData.Row(Items.STONE, 3, 0, 0)));
		assertDropped(() -> save("Twice", twice));
		assertDropped(() -> current(twice, true));
	}

	@Test
	void missingIsAtMostTotalAndWrongAtMostMissing() {
		assertDropped(() -> save("Over", withRows(List.of(new ListData.Row(Items.STONE, 5, 6, 0)))));
		assertDropped(() -> save("Wrong", withRows(List.of(new ListData.Row(Items.STONE, 5, 2, 3)))));
		assertDropped(() -> save("Negative", withRows(List.of(new ListData.Row(Items.STONE, 5, 2, -1)))));
		assertDropped(() -> current(withRows(List.of(new ListData.Row(Items.STONE, 5, 6, 0))), true));

		save("Fine", withRows(List.of(new ListData.Row(Items.STONE, 5, 5, 5))));
		assertTrue(alex().find("Fine").isPresent());
	}

	@Test
	void aBoxIsAtMost256BlocksASide() {
		assertDropped(() -> save("Long", box(new BlockPos(0, 0, 0), new BlockPos(256, 0, 0))));
		assertDropped(() -> save("Tall", box(new BlockPos(0, -64, 0), new BlockPos(0, 192, 0))));
		assertDropped(() -> save("Inside out", box(new BlockPos(1, 0, 0), new BlockPos(0, 0, 0))));
		assertDropped(() -> current(box(new BlockPos(0, 0, -256), new BlockPos(0, 0, 0)), true));

		save("Widest", box(new BlockPos(0, 0, 0), new BlockPos(255, 255, 255)));
		assertTrue(alex().find("Widest").isPresent());
	}

	@Test
	void aBoxIsInADimensionTheServerHas() {
		ListData elsewhere = new ListData(Optional.empty(), list(10).time(), Identifier.fromNamespaceAndPath("somemod", "moon"),
			list(10).min(), list(10).max(), 1, list(10).sort(), false, false, list(10).rows());
		assertDropped(() -> save("Moon", elsewhere));
		assertDropped(() -> current(elsewhere, true));
	}

	@Test
	void theMultiplierIsAtLeastOne() {
		ListData none = new ListData(Optional.empty(), list(10).time(), OVERWORLD, list(10).min(), list(10).max(), 0,
			list(10).sort(), false, false, list(10).rows());
		assertDropped(() -> save("Zero", none));
		assertDropped(() -> current(none, false));
	}

	@Test
	void aListOverTheSizeLimitIsDropped() {
		assertDropped(() -> lists.save(ALEX, "Alex", new Payloads.SaveList("Big", null, true), now));
		assertDropped(() -> lists.current(ALEX, "Alex", new Payloads.CurrentList(Optional.empty(), true, true, true), now));
	}

	@Test
	void the21stListMessageIn10SecondsIsDropped() {
		// 20 messages of all four kinds in 10 seconds less a millisecond.
		for (int i = 0; i < 20; i++) {
			long at = now + i * 500;
			switch (i % 4) {
				case 0 -> lists.save(ALEX, "Alex", new Payloads.SaveList("List " + i, list(10)), at);
				case 1 -> lists.delete(ALEX, "Alex", new Payloads.DeleteList("List " + (i - 1)), at);
				case 2 -> lists.open(ALEX, "Alex", new Payloads.OpenList(""), at);
				default -> lists.current(ALEX, "Alex", new Payloads.CurrentList(Optional.of(list(i)), true, false), at);
			}
		}
		assertEquals(List.of(), alex().saved());
		long first = now;
		now = first + 9_999;
		sent.clear();

		save("Dropped", list(10));
		assertEquals(List.of(), alex().saved());
		assertEquals(List.of(), sent);
		lists.current(SAM, "Sam", new Payloads.CurrentList(Optional.of(list(10)), true, false), now);
		assertEquals(Optional.of(list(10).counted(now)), lists.lists(SAM).lastCounted(), "each player has their own count");

		// Ten seconds after the first, it no longer counts.
		now = first + 10_000;
		save("Taken", list(10));
		assertTrue(alex().find("Taken").isPresent());
	}

	private static ListData withRows(List<ListData.Row> rows) {
		return new ListData(Optional.empty(), list(10).time(), OVERWORLD, list(10).min(), list(10).max(), 1,
			list(10).sort(), false, false, rows);
	}

	private static ListData box(BlockPos min, BlockPos max) {
		return new ListData(Optional.empty(), list(10).time(), OVERWORLD, min, max, 1, list(10).sort(), false, false,
			list(10).rows());
	}
}
