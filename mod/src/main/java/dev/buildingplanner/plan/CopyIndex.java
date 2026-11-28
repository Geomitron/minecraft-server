package dev.buildingplanner.plan;

import dev.buildingplanner.BuildingPlanner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

/**
 * The saved copies' {@code index.dat}, gzip-compressed NBT, in the player's copies folder; the layout is in
 * {@code docs/building-planner.md}, "Saved copies": "The files".
 */
public final class CopyIndex {
	public static final String FILE = "index.dat";
	public static final String UNREADABLE = ".unreadable";
	static final int VERSION = 1;

	private CopyIndex() {
	}

	/**
	 * The copies in the folder's index, or none without one. An index that can't be read is logged as an error and
	 * renamed with {@value #UNREADABLE} at its end, replacing an older one, and gives none.
	 */
	public static CopyHistory read(Path folder) {
		Path file = folder.resolve(FILE);
		if (!Files.exists(file)) {
			return CopyHistory.EMPTY;
		}
		try {
			return fromTag(NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()));
		} catch (IOException | RuntimeException e) {
			Path renamed = unreadable(file);
			BuildingPlanner.LOGGER.error("Building Planner couldn't read {}; it's renamed to {}, and the game starts with "
				+ "no saved copies", file, renamed.getFileName(), e);
			return CopyHistory.EMPTY;
		}
	}

	/** Writes the index through a temporary file and a rename. */
	public static void write(Path folder, CopyHistory history) throws IOException {
		Files.createDirectories(folder);
		LitematicFormat.writeFile(toTag(history), folder.resolve(FILE));
	}

	/** Renames a file that can't be read with {@value #UNREADABLE} at its end, replacing an older one; returns the new path. */
	public static Path unreadable(Path file) {
		Path renamed = file.resolveSibling(file.getFileName() + UNREADABLE);
		try {
			Files.move(file, renamed, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			BuildingPlanner.LOGGER.error("Building Planner couldn't rename {}", file, e);
		}
		return renamed;
	}

	static CompoundTag toTag(CopyHistory history) {
		CompoundTag root = new CompoundTag();
		root.putInt("version", VERSION);
		root.putInt("next", history.next());
		ListTag copies = new ListTag();
		for (CopyHistory.Saved copy : history.copies()) {
			CompoundTag tag = new CompoundTag();
			tag.putInt("file", copy.file());
			tag.putString("name", copy.name());
			tag.putLong("time", copy.time());
			tag.putBoolean("kept", copy.kept());
			CompoundTag size = new CompoundTag();
			size.putInt("x", copy.size().getX());
			size.putInt("y", copy.size().getY());
			size.putInt("z", copy.size().getZ());
			tag.put("size", size);
			tag.putInt("blocks", copy.blocks());
			copies.add(tag);
		}
		root.put("copies", copies);
		return root;
	}

	/**
	 * The copies in an index's contents. Another version, a missing entry, a file number used twice or not below
	 * {@code next}, an empty name, a side under 1 or a negative block count is an error.
	 */
	static CopyHistory fromTag(CompoundTag root) {
		int version = require(root.getInt("version"), "version");
		if (version != VERSION) {
			throw new IllegalArgumentException("Unknown saved copies index version " + version);
		}
		int next = require(root.getInt("next"), "next");
		List<CopyHistory.Saved> copies = new ArrayList<>();
		Set<Integer> files = new HashSet<>();
		for (Tag entry : require(root.getList("copies"), "copies")) {
			if (!(entry instanceof CompoundTag tag)) {
				throw new IllegalArgumentException("Expected a compound, found " + entry);
			}
			int file = require(tag.getInt("file"), "file");
			if (file < 1 || file >= next || !files.add(file)) {
				throw new IllegalArgumentException("A copy's file number " + file + " with next " + next);
			}
			String name = require(tag.getString("name"), "name");
			if (name.isEmpty()) {
				throw new IllegalArgumentException("A copy with no name");
			}
			CompoundTag sizeTag = require(tag.getCompound("size"), "size");
			Vec3i size = new Vec3i(require(sizeTag.getInt("x"), "x"), require(sizeTag.getInt("y"), "y"),
				require(sizeTag.getInt("z"), "z"));
			int blocks = require(tag.getInt("blocks"), "blocks");
			if (size.getX() < 1 || size.getY() < 1 || size.getZ() < 1 || blocks < 0) {
				throw new IllegalArgumentException("A copy of size " + size + " with " + blocks + " blocks");
			}
			copies.add(new CopyHistory.Saved(file, name, require(tag.getLong("time"), "time"),
				require(tag.getBoolean("kept"), "kept"), size, blocks));
		}
		return new CopyHistory(next, copies);
	}

	private static <T> T require(Optional<T> value, String key) {
		return value.orElseThrow(() -> new IllegalArgumentException("Missing " + key));
	}
}
