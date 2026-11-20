package dev.buildingplanner.server;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.plan.LitematicFormat;
import dev.buildingplanner.sync.ListData;
import dev.buildingplanner.sync.ListSort;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * Each player's material lists in {@code <world>/building-planner/material-lists/<UUID>.dat}, gzip-compressed NBT. The
 * layout, and what makes a file unreadable, are in {@code docs/building-planner.md}, "The lists on the server".
 *
 * <p>Calls come from the server thread. {@link #write} builds a file's contents there, and one background thread writes
 * them in call order, each through a temporary file and a rename. {@link #close} waits for every write.
 */
public final class ListFiles implements MaterialLists.Store, AutoCloseable {
	static final int VERSION = 1;
	static final String EXTENSION = ".dat";
	static final String UNREADABLE = ".unreadable";

	private final Path root;
	private final ExecutorService writer = Executors.newSingleThreadExecutor(task -> {
		Thread thread = new Thread(task, "Building Planner material list writer");
		thread.setDaemon(true);
		return thread;
	});

	/** {@code root} is {@code <world>/building-planner/material-lists}. */
	public ListFiles(Path root) {
		this.root = root;
	}

	public Path file(UUID player) {
		return root.resolve(player + EXTENSION);
	}

	/**
	 * The player's lists, or none when they have no file. A file that can't be read is logged as an error and renamed
	 * with {@value #UNREADABLE} at its end, replacing an older one, and the player starts with no lists.
	 */
	@Override
	public PlayerLists read(UUID player) {
		Path file = file(player);
		if (!Files.exists(file)) {
			return new PlayerLists();
		}
		try {
			return fromTag(NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()));
		} catch (IOException | RuntimeException e) {
			Path renamed = file.resolveSibling(file.getFileName() + UNREADABLE);
			BuildingPlanner.LOGGER.error("Building Planner couldn't read {}; it's renamed to {}, and the player starts "
				+ "with no material lists", file, renamed.getFileName(), e);
			try {
				Files.move(file, renamed, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException moveError) {
				BuildingPlanner.LOGGER.error("Building Planner couldn't rename {}", file, moveError);
			}
			return new PlayerLists();
		}
	}

	/** Writes the player's lists whole, on the background thread. */
	@Override
	public void write(UUID player, PlayerLists lists) {
		Path file = file(player);
		CompoundTag contents = toTag(lists);
		writer.execute(() -> {
			try {
				Files.createDirectories(file.getParent());
				LitematicFormat.writeFile(contents, file);
			} catch (IOException | RuntimeException e) {
				BuildingPlanner.LOGGER.error("Building Planner couldn't write {}", file, e);
			}
		});
	}

	/** Waits for every write. */
	@Override
	public void close() {
		writer.shutdown();
		try {
			if (!writer.awaitTermination(2, TimeUnit.MINUTES)) {
				BuildingPlanner.LOGGER.error("Building Planner's material lists took over two minutes to write; some may be missing");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	static CompoundTag toTag(PlayerLists lists) {
		CompoundTag root = new CompoundTag();
		root.putInt("version", VERSION);
		root.putInt("data_version", SharedConstants.getCurrentVersion().dataVersion().version());
		ListTag saved = new ListTag();
		for (ListData list : lists.saved()) {
			CompoundTag tag = listTag(list);
			tag.putString("name", list.name().orElseThrow());
			tag.putLong("time", list.time().orElseThrow());
			saved.add(tag);
		}
		root.put("saved", saved);
		lists.lastCounted().ifPresent(list -> {
			CompoundTag tag = listTag(list);
			tag.putLong("time", list.time().orElseThrow());
			root.put("last_counted", tag);
		});
		lists.current().ifPresent(list -> {
			CompoundTag tag = listTag(list);
			list.name().ifPresent(name -> tag.putString("name", name));
			list.time().ifPresent(time -> tag.putLong("time", time));
			root.put("current", tag);
		});
		root.putBoolean("panel", lists.panel());
		return root;
	}

	/**
	 * The lists in a file's contents. Another version or a missing entry is an error; an item the game doesn't know
	 * drops from the list it's in.
	 */
	public static PlayerLists fromTag(CompoundTag root) {
		int version = require(root.getInt("version"), "version");
		if (version != VERSION) {
			throw new IllegalArgumentException("Unknown material list file version " + version);
		}
		require(root.getInt("data_version"), "data_version");
		PlayerLists lists = new PlayerLists();
		for (Tag entry : require(root.getList("saved"), "saved")) {
			CompoundTag tag = compound(entry);
			lists.save(readList(tag, Optional.of(require(tag.getString("name"), "name")),
				OptionalLong.of(require(tag.getLong("time"), "time"))));
		}
		Optional<CompoundTag> lastCounted = root.getCompound("last_counted");
		if (lastCounted.isPresent()) {
			lists.setLastCounted(Optional.of(readList(lastCounted.get(), Optional.empty(),
				OptionalLong.of(require(lastCounted.get().getLong("time"), "time")))));
		}
		Optional<CompoundTag> current = root.getCompound("current");
		if (current.isPresent()) {
			Optional<Long> time = current.get().getLong("time");
			lists.setCurrent(Optional.of(readList(current.get(), current.get().getString("name"),
				time.isPresent() ? OptionalLong.of(time.get()) : OptionalLong.empty())));
		}
		lists.setPanel(require(root.getBoolean("panel"), "panel"));
		return lists;
	}

	private static CompoundTag listTag(ListData list) {
		CompoundTag tag = new CompoundTag();
		tag.putString("dimension", list.dimension().toString());
		tag.put("min", pos(list.min()));
		tag.put("max", pos(list.max()));
		tag.putInt("multiplier", list.multiplier());
		tag.putString("sort", list.sort().name());
		tag.putBoolean("reverse", list.reverse());
		tag.putBoolean("hide_available", list.hideAvailable());
		ListTag rows = new ListTag();
		for (ListData.Row row : list.rows()) {
			CompoundTag rowTag = new CompoundTag();
			rowTag.putString("item", BuiltInRegistries.ITEM.getKey(row.item()).toString());
			rowTag.putInt("total", row.total());
			rowTag.putInt("missing", row.missing());
			rowTag.putInt("wrong", row.wrong());
			rows.add(rowTag);
		}
		tag.put("rows", rows);
		return tag;
	}

	private static ListData readList(CompoundTag tag, Optional<String> name, OptionalLong time) {
		Identifier dimension = Identifier.parse(require(tag.getString("dimension"), "dimension"));
		List<ListData.Row> rows = new ArrayList<>();
		for (Tag entry : require(tag.getList("rows"), "rows")) {
			CompoundTag row = compound(entry);
			String id = require(row.getString("item"), "item");
			int total = require(row.getInt("total"), "total");
			int missing = require(row.getInt("missing"), "missing");
			int wrong = require(row.getInt("wrong"), "wrong");
			Identifier itemId = Identifier.tryParse(id);
			Optional<Item> item = itemId == null ? Optional.empty()
				: BuiltInRegistries.ITEM.getOptional(itemId);
			if (item.isPresent()) {
				rows.add(new ListData.Row(item.get(), total, missing, wrong));
			} else {
				BuildingPlanner.LOGGER.warn("Building Planner dropped the unknown item {} from a material list", id);
			}
		}
		return new ListData(name, time, dimension, readPos(require(tag.getCompound("min"), "min")),
			readPos(require(tag.getCompound("max"), "max")), require(tag.getInt("multiplier"), "multiplier"),
			ListSort.valueOf(require(tag.getString("sort"), "sort")), require(tag.getBoolean("reverse"), "reverse"),
			require(tag.getBoolean("hide_available"), "hide_available"), rows);
	}

	private static CompoundTag pos(BlockPos pos) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("x", pos.getX());
		tag.putInt("y", pos.getY());
		tag.putInt("z", pos.getZ());
		return tag;
	}

	private static BlockPos readPos(CompoundTag tag) {
		return new BlockPos(require(tag.getInt("x"), "x"), require(tag.getInt("y"), "y"), require(tag.getInt("z"), "z"));
	}

	private static CompoundTag compound(Tag tag) {
		if (tag instanceof CompoundTag compound) {
			return compound;
		}
		throw new IllegalArgumentException("Expected a compound, found " + tag);
	}

	private static <T> T require(Optional<T> value, String key) {
		return value.orElseThrow(() -> new IllegalArgumentException("Missing " + key));
	}
}
