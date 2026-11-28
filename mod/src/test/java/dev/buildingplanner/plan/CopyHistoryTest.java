package dev.buildingplanner.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.Vec3i;
import org.junit.jupiter.api.Test;

class CopyHistoryTest {
	private static final Vec3i SIZE = new Vec3i(12, 5, 8);
	/** The pattern of the language file's {@code buildingplanner.ui.lists.date_time}, which names a new copy. */
	private static final DateTimeFormatter NAMES = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US)
		.withZone(ZoneOffset.UTC);

	private static String name(long time) {
		return NAMES.format(Instant.ofEpochMilli(time));
	}

	/** {@code count} copies, each a minute after the one before, the last the newest. */
	private static CopyHistory copies(CopyHistory history, int count) {
		for (int i = 0; i < count; i++) {
			long time = 60_000L * (history.next() + 1000);
			history = history.add(name(time), time, SIZE, 120).history();
		}
		return history;
	}

	private static List<Integer> files(CopyHistory history) {
		return history.copies().stream().map(CopyHistory.Saved::file).toList();
	}

	@Test
	void aNewCopyGoesAtTheTopNamedByItsTimeAndNotKept() {
		CopyHistory history = copies(CopyHistory.EMPTY, 2);
		long time = 1_763_576_460_000L;
		CopyHistory.Change change = history.add(name(time), time, SIZE, 120);

		CopyHistory.Saved newest = change.history().newest().orElseThrow();
		assertEquals(new CopyHistory.Saved(3, "Nov 19, 6:21 PM", time, false, SIZE, 120), newest);
		assertEquals(List.of(3, 2, 1), files(change.history()));
		assertEquals(4, change.history().next());
		assertEquals(List.of(), change.dropped());
	}

	@Test
	void theEleventhCopyDropsTheOldest() {
		CopyHistory ten = copies(CopyHistory.EMPTY, 10);
		assertEquals(List.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1), files(ten));

		CopyHistory.Change eleventh = ten.add("Eleventh", 1, SIZE, 1);
		assertEquals(List.of(11, 10, 9, 8, 7, 6, 5, 4, 3, 2), files(eleventh.history()));
		assertEquals(List.of(1), eleventh.dropped());
	}

	@Test
	void renamedCopiesDoNotCountAmongTheTen() {
		CopyHistory history = copies(CopyHistory.EMPTY, 10).rename(1, "Hut").rename(5, "Tower");
		CopyHistory.Saved hut = history.find(1).orElseThrow();
		assertEquals("Hut", hut.name());
		assertTrue(hut.kept());
		assertEquals(List.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1), files(history), "a rename keeps the copy's place");

		CopyHistory.Change change = history.add("New", 1, SIZE, 1);
		assertEquals(List.of(), change.dropped(), "8 recent and 2 kept leave room for 2 more");
		CopyHistory.Change next = copies(change.history(), 1).add("Newer", 1, SIZE, 1);
		assertEquals(List.of(2), next.dropped(), "the oldest not kept drops, past the kept Hut");
		assertEquals(List.of(13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 1), files(next.history()));
	}

	@Test
	void aRenamedCopyStaysThroughAnyNumberOfNewCopies() {
		CopyHistory history = copies(CopyHistory.EMPTY, 1).rename(1, "Hut");
		history = copies(history, 50);
		assertTrue(history.find(1).orElseThrow().kept());
		assertEquals(11, history.copies().size(), "the Hut and the 10 most recent");
		assertEquals(1, files(history).getLast());
	}

	@Test
	void renamingAKeptCopyAgainKeepsIt() {
		CopyHistory history = copies(CopyHistory.EMPTY, 1).rename(1, "Hut").rename(1, "Big hut");
		assertEquals(new CopyHistory.Saved(1, "Big hut", history.find(1).orElseThrow().time(), true, SIZE, 120),
			history.find(1).orElseThrow());
	}

	@Test
	void aDeleteDropsTheCopyAndItsFile() {
		CopyHistory history = copies(CopyHistory.EMPTY, 3).rename(2, "Hut");

		CopyHistory.Change kept = history.delete(2);
		assertEquals(List.of(3, 1), files(kept.history()));
		assertEquals(List.of(2), kept.dropped());

		CopyHistory.Change recent = kept.history().delete(3);
		assertEquals(List.of(1), files(recent.history()));
		assertEquals(List.of(3), recent.dropped());

		CopyHistory.Change unknown = recent.history().delete(7);
		assertEquals(recent.history(), unknown.history());
		assertEquals(List.of(), unknown.dropped());
	}

	@Test
	void theFilesNumbersAreNeverUsedTwice() {
		Set<Integer> used = new HashSet<>();
		CopyHistory history = CopyHistory.EMPTY;
		for (int i = 0; i < 30; i++) {
			history = history.add("Copy", i, SIZE, 1).history();
			assertTrue(used.add(history.newest().orElseThrow().file()));
			if (i % 4 == 0) {
				history = history.delete(history.newest().orElseThrow().file()).history();
			}
		}
		assertEquals(31, history.next());
		assertFalse(history.find(31).isPresent());
	}

	@Test
	void numberingStartsPastFilesInTheFolder() {
		assertEquals(8, CopyHistory.EMPTY.numberedFrom(8).next());
		CopyHistory history = copies(CopyHistory.EMPTY, 5);
		assertEquals(6, history.numberedFrom(3).next(), "a lower number leaves next as it is");
	}
}
