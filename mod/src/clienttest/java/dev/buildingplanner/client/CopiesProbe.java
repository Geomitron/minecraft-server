package dev.buildingplanner.client;

import dev.buildingplanner.plan.CopyFile;
import dev.buildingplanner.plan.CopyHistory;
import dev.buildingplanner.plan.CopyIndex;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

/**
 * The driver's view of the saved copies (saved.sh): the list the game shows, the index and the files on disk as
 * another reader finds them, and Litematica's own reading of a copy's file. Each copy shows as
 * {@code <file>:<name>:<kept or recent>:<x>x<y>x<z>:<blocks>}, newest first, separated by " ; ".
 */
final class CopiesProbe {
	private CopiesProbe() {
	}

	/** The saved copies as the game shows them, or {@code none}. */
	static String list() {
		List<String> rows = new ArrayList<>();
		for (CopyHistory.Saved copy : SavedCopies.get().list().copies()) {
			rows.add(row(copy.file(), copy.name(), copy.kept(), copy.size(), copy.blocks()));
		}
		return rows.isEmpty() ? "none" : String.join(" ; ", rows);
	}

	/**
	 * The index on disk, read as plain NBT: {@code missing}, {@code unreadable}, or its version and next number, then
	 * each copy with the time it was copied after an {@code @}.
	 */
	static String index() {
		Path file = SavedCopies.get().folder().resolve(CopyIndex.FILE);
		if (!Files.exists(file)) {
			return "missing";
		}
		CompoundTag root;
		try {
			root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
		} catch (IOException e) {
			return "unreadable";
		}
		List<String> rows = new ArrayList<>();
		ListTag copies = root.getListOrEmpty("copies");
		for (int i = 0; i < copies.size(); i++) {
			CompoundTag copy = copies.getCompoundOrEmpty(i);
			CompoundTag size = copy.getCompoundOrEmpty("size");
			rows.add(row(copy.getIntOr("file", 0), copy.getStringOr("name", ""), copy.getByteOr("kept", (byte) 0) == 1,
				new Vec3i(size.getIntOr("x", 0), size.getIntOr("y", 0), size.getIntOr("z", 0)), copy.getIntOr("blocks", 0))
				+ "@" + copy.getLongOr("time", 0));
		}
		return "version " + root.getIntOr("version", 0) + " next " + root.getIntOr("next", 0) + ": "
			+ (rows.isEmpty() ? "none" : String.join(" ; ", rows));
	}

	/** The names of the files in the player's copies folder, sorted, or {@code nofolder}. */
	static String files() throws IOException {
		Path folder = SavedCopies.get().folder();
		if (!Files.isDirectory(folder)) {
			return "nofolder";
		}
		try (Stream<Path> files = Files.list(folder)) {
			String names = files.map(path -> path.getFileName().toString()).sorted().collect(Collectors.joining(" "));
			return names.isEmpty() ? "empty" : names;
		}
	}

	/** The player's copies folder from the game folder. */
	static String folder() {
		return Minecraft.getInstance().gameDirectory.toPath().relativize(SavedCopies.get().folder()).toString()
			.replace('\\', '/');
	}

	/**
	 * The copy's file as Litematica's own reader finds it: its name, its regions with their sizes, and its block count,
	 * or {@code unreadable}.
	 */
	static String schematic(int file) {
		Path folder = SavedCopies.get().folder();
		LitematicaSchematic schematic = LitematicaSchematic.createFromFile(folder, Integer.toString(file));
		if (schematic == null || !Files.exists(CopyFile.path(folder, file))) {
			return "unreadable";
		}
		String regions = schematic.getAreas().keySet().stream().sorted().map(name -> {
			Vec3i size = schematic.getSubRegionContainer(name).getSize();
			return name + " " + size.getX() + "x" + size.getY() + "x" + size.getZ();
		}).collect(Collectors.joining(", "));
		return schematic.getMetadata().getName() + ": " + regions + ", " + schematic.getMetadata().getTotalBlocks()
			+ " blocks";
	}

	private static String row(int file, String name, boolean kept, Vec3i size, int blocks) {
		return file + ":" + name + ":" + (kept ? "kept" : "recent") + ":" + size.getX() + "x" + size.getY() + "x"
			+ size.getZ() + ":" + blocks;
	}
}
