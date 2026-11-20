package dev.buildingplanner.client;

import dev.buildingplanner.server.ListFiles;
import dev.buildingplanner.server.PlayerLists;
import dev.buildingplanner.sync.ListData;
import fi.dy.masa.litematica.data.DataManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

/**
 * The driver's view of the material lists: the game's current list, and "Last counted" in a player's lists file on the
 * dev server, which shares the container with the clients. Each list reads as its box, the screen's settings and its
 * rows, without its name and time.
 */
final class ListProbe {
	private ListProbe() {
	}

	/** The game's current list, when it's Building Planner's, or "none". */
	static String current() {
		return DataManager.getMaterialList() instanceof GhostMaterialList list ? describe(list.data()) : "none";
	}

	/** "Last counted" in the lists file, or "no file", or "none". */
	static String lastCounted(Path file) throws Exception {
		if (!Files.exists(file)) {
			return "no file";
		}
		PlayerLists lists = ListFiles.fromTag(NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()));
		return lists.lastCounted().map(ListProbe::describe).orElse("none");
	}

	/** Whether the file's "Last counted" is the game's current list. */
	static boolean fileHasCurrent(Path file) {
		try {
			String current = current();
			return !current.equals("none") && lastCounted(file).equals(current);
		} catch (Exception e) {
			// A file the server is renaming into place reads again at the next tick.
			return false;
		}
	}

	static String describe(ListData list) {
		return list.dimension() + " " + pos(list.min()) + ".." + pos(list.max()) + " ×" + list.multiplier() + " "
			+ list.sort() + (list.reverse() ? " reversed" : "") + (list.hideAvailable() ? " hide available" : "")
			+ list.rows().stream().map(row -> " | " + BuiltInRegistries.ITEM.getKey(row.item()) + " " + row.total() + "/"
				+ row.missing() + "/" + row.wrong()).collect(Collectors.joining());
	}

	private static String pos(BlockPos pos) {
		return pos.getX() + "," + pos.getY() + "," + pos.getZ();
	}
}
