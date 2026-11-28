package dev.buildingplanner.client;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.plan.CopyFile;
import dev.buildingplanner.plan.CopyHistory;
import dev.buildingplanner.plan.CopyIndex;
import dev.buildingplanner.sync.PlayerMessage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The player's saved copies (see "Saved copies"), a set for each Minecraft account in
 * {@code <game folder>/building-planner/copies/<player UUID>/}, the same on every server. It lives outside the session,
 * as the copy does, so the list stays across servers.
 *
 * <p>Calls come from the game's thread. One background thread owns the list and does every change in call order:
 * it reads and writes the files, each write through a temporary file and a rename, and hands a copy the player picks
 * back to the game's thread. The game's thread reads the list as the background thread last left it ({@link #list}).
 */
final class SavedCopies {
	/** Null while Building Planner is off for want of Litematica or MaLiLib. */
	private static SavedCopies instance;

	private final Path folder;
	private final String author;
	private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
		Thread thread = new Thread(task, "Building Planner saved copies");
		thread.setDaemon(true);
		return thread;
	});
	/** The list; only the background thread reads or changes it. */
	private CopyHistory history = CopyHistory.EMPTY;
	/** The list as the background thread last left it. */
	private volatile CopyHistory shown = CopyHistory.EMPTY;
	/** Changes given to the background thread and not yet done, for the in-game checks. */
	private final AtomicInteger pending = new AtomicInteger();

	private SavedCopies(Path folder, String author) {
		this.folder = folder;
		this.author = author;
	}

	/** As the game starts: reads the player's index on the background thread. */
	static void start(Minecraft mc) {
		instance = new SavedCopies(folder(mc.gameDirectory.toPath(), mc.getUser().getProfileId()),
			mc.getUser().getName());
		instance.run(instance::readIndex);
	}

	static SavedCopies get() {
		return instance;
	}

	/** The player's copies folder under the game folder. */
	static Path folder(Path gameDir, UUID player) {
		return gameDir.resolve("building-planner").resolve("copies").resolve(player.toString());
	}

	Path folder() {
		return folder;
	}

	/** The saved copies, newest first, as the background thread last left them. */
	CopyHistory list() {
		return shown;
	}

	/** Whether every change given to the background thread is done. */
	boolean settled() {
		return pending.get() == 0;
	}

	/**
	 * Copy made a copy of {@code blocks} (at their world positions, which nothing changes from now on), in a box of
	 * {@code size}: saves it as the newest copy, named by the time, and drops the oldest copy not kept beyond the 10 most
	 * recent.
	 */
	void add(Map<BlockPos, BlockState> blocks, Vec3i size) {
		long time = System.currentTimeMillis();
		String name = ListTexts.dateTime(time);
		run(() -> {
			CopyHistory.Change change = history.add(name, time, size, blocks.size());
			Path file = CopyFile.path(folder, history.next());
			try {
				CopyFile.write(file, blocks, name, author, time);
			} catch (IOException | RuntimeException e) {
				BuildingPlanner.LOGGER.error("Building Planner couldn't save the copy to {}", file, e);
				return;
			}
			apply(change);
		});
	}

	/** Renames the copy, which then stays until it's deleted. */
	void rename(int file, String name) {
		run(() -> {
			history = history.rename(file, name);
			writeIndex();
		});
	}

	/** Deletes the copy and its file. */
	void delete(int file) {
		run(() -> apply(history.delete(file)));
	}

	/**
	 * The player picked the copy: reads its file and, on the game's thread, brings it to the aim. A file that can't be
	 * read is logged as an error and renamed with {@code .unreadable}, the copy drops from the list, and the player gets
	 * a message.
	 */
	void pick(int file) {
		run(() -> read(file));
	}

	/** The Paste button with no copy since the game started: picks the newest saved copy, or says there's none. */
	void pickNewest() {
		run(() -> history.newest().ifPresentOrElse(newest -> read(newest.file()),
			() -> Minecraft.getInstance().execute(() -> ClientSession.show(PlayerMessage.nothingToPaste()))));
	}

	/** As the game stops: waits for every change. */
	void close() {
		worker.shutdown();
		try {
			if (!worker.awaitTermination(2, TimeUnit.MINUTES)) {
				BuildingPlanner.LOGGER.error("Building Planner's saved copies took over two minutes to write; some may be missing");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void run(Runnable change) {
		pending.incrementAndGet();
		worker.execute(() -> {
			try {
				change.run();
			} catch (RuntimeException e) {
				BuildingPlanner.LOGGER.error("Building Planner's saved copies failed", e);
			} finally {
				shown = history;
				pending.decrementAndGet();
			}
		});
	}

	/**
	 * Reads the index; the next copy's file takes a number past every copy's file in the folder, so a copy whose index
	 * was lost isn't overwritten.
	 */
	private void readIndex() {
		history = CopyIndex.read(folder).numberedFrom(highestFile() + 1);
	}

	private int highestFile() {
		if (!Files.isDirectory(folder)) {
			return 0;
		}
		try (Stream<Path> files = Files.list(folder)) {
			return files.map(path -> path.getFileName().toString())
				.filter(name -> name.matches("[0-9]{1,9}" + CopyFile.EXTENSION.replace(".", "\\.")))
				.mapToInt(name -> Integer.parseInt(name.substring(0, name.length() - CopyFile.EXTENSION.length())))
				.max().orElse(0);
		} catch (IOException e) {
			BuildingPlanner.LOGGER.error("Building Planner couldn't list {}", folder, e);
			return 0;
		}
	}

	private void read(int file) {
		if (history.find(file).isEmpty()) {
			return;
		}
		Path path = CopyFile.path(folder, file);
		Map<BlockPos, BlockState> blocks;
		try {
			blocks = CopyFile.read(path);
		} catch (IOException | RuntimeException e) {
			Path renamed = Files.exists(path) ? CopyIndex.unreadable(path) : path;
			BuildingPlanner.LOGGER.error("Building Planner couldn't read the saved copy {}; it's renamed to {} and "
				+ "dropped from the list", path, renamed.getFileName(), e);
			history = history.delete(file).history();
			writeIndex();
			Minecraft.getInstance().execute(() -> ClientSession.show(PlayerMessage.copyUnreadable()));
			return;
		}
		Minecraft.getInstance().execute(() -> BuildingPlannerClient.pasteSaved(blocks));
	}

	/** Takes the change, writes the index, then deletes the files it drops. */
	private void apply(CopyHistory.Change change) {
		history = change.history();
		if (writeIndex()) {
			deleteFiles(change.dropped());
		}
	}

	private boolean writeIndex() {
		try {
			CopyIndex.write(folder, history);
			return true;
		} catch (IOException | RuntimeException e) {
			BuildingPlanner.LOGGER.error("Building Planner couldn't write {}", folder.resolve(CopyIndex.FILE), e);
			return false;
		}
	}

	private void deleteFiles(List<Integer> files) {
		for (int file : files) {
			Path path = CopyFile.path(folder, file);
			try {
				Files.deleteIfExists(path);
			} catch (IOException e) {
				BuildingPlanner.LOGGER.error("Building Planner couldn't delete {}", path, e);
			}
		}
	}
}
