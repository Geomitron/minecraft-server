package dev.buildingplanner.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Vec3i;

/**
 * Which saved copies stay (see "Saved copies": "Which copies stay"): the saved copies, newest first, and the number the
 * next copy's file takes, so that no number is used twice. Each change gives the copies after it and the files it
 * drops. The {@value #RECENT} most recent copies the player hasn't renamed stay; a renamed copy is kept until it's
 * deleted and doesn't count among them.
 */
public record CopyHistory(int next, List<Saved> copies) {
	/** How many of the copies the player hasn't renamed stay. */
	public static final int RECENT = 10;
	public static final CopyHistory EMPTY = new CopyHistory(1, List.of());

	public CopyHistory {
		copies = List.copyOf(copies);
	}

	/**
	 * One saved copy: its file's number, its name, when it was copied (in milliseconds since 1970), whether it's kept
	 * (renamed), its size and its number of blocks.
	 */
	public record Saved(int file, String name, long time, boolean kept, Vec3i size, int blocks) {
	}

	/** The copies after a change, and the numbers of the files it drops, which are deleted once the index is written. */
	public record Change(CopyHistory history, List<Integer> dropped) {
	}

	/**
	 * A new copy, named {@code name}, at the top, taking the next file's number; the oldest copy not kept beyond the
	 * {@value #RECENT} most recent drops.
	 */
	public Change add(String name, long time, Vec3i size, int blocks) {
		List<Saved> after = new ArrayList<>(copies.size() + 1);
		after.add(new Saved(next, name, time, false, size, blocks));
		List<Integer> dropped = new ArrayList<>();
		int recent = 1;
		for (Saved copy : copies) {
			if (!copy.kept() && ++recent > RECENT) {
				dropped.add(copy.file());
			} else {
				after.add(copy);
			}
		}
		return new Change(new CopyHistory(next + 1, after), List.copyOf(dropped));
	}

	/** The copy renamed {@code name} and kept until it's deleted, in the same place; no other copy changes. */
	public CopyHistory rename(int file, String name) {
		List<Saved> after = new ArrayList<>(copies.size());
		for (Saved copy : copies) {
			after.add(copy.file() == file
				? new Saved(copy.file(), name, copy.time(), true, copy.size(), copy.blocks()) : copy);
		}
		return new CopyHistory(next, after);
	}

	/** The copy taken out of the list, dropping its file; an unknown number changes nothing. */
	public Change delete(int file) {
		List<Saved> after = copies.stream().filter(copy -> copy.file() != file).toList();
		return new Change(new CopyHistory(next, after), after.size() == copies.size() ? List.of() : List.of(file));
	}

	/** The copies with the next file's number at least {@code first}, past files already in the folder. */
	public CopyHistory numberedFrom(int first) {
		return first > next ? new CopyHistory(first, copies) : this;
	}

	public Optional<Saved> find(int file) {
		return copies.stream().filter(copy -> copy.file() == file).findFirst();
	}

	public Optional<Saved> newest() {
		return copies.stream().findFirst();
	}
}
