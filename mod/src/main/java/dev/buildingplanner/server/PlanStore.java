package dev.buildingplanner.server;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.plan.LitematicFormat;
import dev.buildingplanner.plan.Plan;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.minecraft.nbt.CompoundTag;

/**
 * The server's plan files under {@code <world>/building-planner/}: each live plan in {@code plans/<plan id>.litematic}
 * and each deleted one in {@code deleted/}.
 *
 * <p>Calls come from the server thread. A save builds the file's contents there, from the plan as it is at that
 * moment, and one background thread writes the files in call order, each through a temporary file and a rename.
 * {@link #close} waits for every write.
 */
public final class PlanStore implements AutoCloseable {
	/** A changed plan is written at most this often. */
	static final long SAVE_INTERVAL_MILLIS = 30_000;
	private static final String EXTENSION = ".litematic";

	private final Path plansDir;
	private final Path deletedDir;
	private final ExecutorService writer = Executors.newSingleThreadExecutor(task -> {
		Thread thread = new Thread(task, "Building Planner plan writer");
		thread.setDaemon(true);
		return thread;
	});
	private long lastSaveMillis;

	/** {@code root} is {@code <world>/building-planner}; {@code nowMillis} starts the first save interval. */
	public PlanStore(Path root, long nowMillis) {
		this.plansDir = root.resolve("plans");
		this.deletedDir = root.resolve("deleted");
		this.lastSaveMillis = nowMillis;
	}

	Path planFile(UUID planId) {
		return plansDir.resolve(planId + EXTENSION);
	}

	Path deletedFile(UUID planId) {
		return deletedDir.resolve(planId + EXTENSION);
	}

	/**
	 * Reads a plan from the add-on's own file or, when it has none, from {@code shared}: the file Syncmatica stored when
	 * the plan was shared (null when Syncmatica has none ready). Empty, with the reason logged, when neither reads.
	 * An unreadable add-on file stays where it is, for repair by hand, and isn't replaced by Syncmatica's.
	 */
	public Optional<Plan> load(UUID planId, Path shared) {
		Path own = planFile(planId);
		Path file = Files.exists(own) ? own : shared;
		if (file == null || !Files.exists(file)) {
			BuildingPlanner.LOGGER.error("No file holds plan {}, so it won't sync", planId);
			return Optional.empty();
		}
		try {
			return Optional.of(LitematicFormat.readFile(file));
		} catch (IOException | RuntimeException e) {
			BuildingPlanner.LOGGER.error("Couldn't read plan {} from {}, so it won't sync", planId, file, e);
			return Optional.empty();
		}
	}

	/** Every {@link #SAVE_INTERVAL_MILLIS}, saves the plans that changed since the last save. */
	public void tick(PlanSync sync, long nowMillis) {
		if (nowMillis - lastSaveMillis >= SAVE_INTERVAL_MILLIS) {
			lastSaveMillis = nowMillis;
			saveChanged(sync, nowMillis);
		}
	}

	/** Saves the plans that changed since the last save. */
	public void saveChanged(PlanSync sync, long nowMillis) {
		for (UUID planId : sync.takeChanged()) {
			sync.plan(planId).ifPresent(plan -> write(plan, planFile(planId), nowMillis));
		}
	}

	/**
	 * Keeps a deleted plan in {@code deleted/}: the plan as it is now when it was syncing, or else its last saved
	 * file. Its file in {@code plans/} goes.
	 */
	public void archive(UUID planId, Optional<Plan> plan, long nowMillis) {
		Path own = planFile(planId);
		Path archived = deletedFile(planId);
		if (plan.isPresent()) {
			write(plan.get(), archived, nowMillis);
			submit("delete " + own, () -> Files.deleteIfExists(own));
		} else {
			submit("move " + own, () -> {
				if (Files.exists(own)) {
					Files.createDirectories(deletedDir);
					Files.move(own, archived, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
				}
			});
		}
	}

	/** Waits for every write. */
	@Override
	public void close() {
		writer.shutdown();
		try {
			if (!writer.awaitTermination(2, TimeUnit.MINUTES)) {
				BuildingPlanner.LOGGER.error("Building Planner's plan files took over two minutes to write; some may be missing");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void write(Plan plan, Path file, long nowMillis) {
		plan.refreshMetadata(nowMillis);
		CompoundTag contents = LitematicFormat.write(plan);
		submit("write " + file, () -> {
			Files.createDirectories(file.getParent());
			LitematicFormat.writeFile(contents, file);
		});
	}

	private interface FileTask {
		void run() throws IOException;
	}

	private void submit(String description, FileTask task) {
		writer.execute(() -> {
			try {
				task.run();
			} catch (IOException | RuntimeException e) {
				BuildingPlanner.LOGGER.error("Building Planner couldn't {}", description, e);
			}
		});
	}
}
