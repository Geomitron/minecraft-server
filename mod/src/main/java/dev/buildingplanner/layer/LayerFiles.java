package dev.buildingplanner.layer;

import com.mojang.serialization.Dynamic;
import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.plan.LitematicBlockStates;
import dev.buildingplanner.plan.LitematicFormat;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The layer's files under {@code <world>/building-planner/layer/}: {@code <dimension namespace>/<dimension
 * path>/<x>.<z>.dat} for each area of {@value Layer#AREA_SIDE} by {@value Layer#AREA_SIDE} columns that holds ghost
 * blocks, where x and z are the area's coordinates.
 *
 * <p>Each file is gzip-compressed NBT: {@code version} ({@value #VERSION}), {@code data_version} (the game's data
 * version when it was written; older block states go through the game's data fixer when read), and {@code columns},
 * each with {@code x} and {@code z} (chunk coordinates) and {@code sections}, each with {@code y} (its height),
 * {@code palette} (block states as Litematica's palettes store them, air first) and {@code blocks}
 * ({@link LayerSection#pack}).
 *
 * <p>Calls come from one thread. {@link #save} builds a changed area's contents there and one background thread
 * writes them in call order, each through a temporary file and a rename, or deletes the file of an area left empty.
 * A file that can't be read is logged and left as it is, and its area is never written while this object lives, so
 * no edit overwrites it. {@link #close} waits for every write.
 */
public final class LayerFiles implements AutoCloseable {
	static final int VERSION = 1;
	static final String EXTENSION = ".dat";

	private final Path root;
	private final Map<Identifier, LongSet> unreadable = new HashMap<>();
	private final ExecutorService writer = Executors.newSingleThreadExecutor(task -> {
		Thread thread = new Thread(task, "Building Planner layer writer");
		thread.setDaemon(true);
		return thread;
	});

	/** {@code root} is {@code <world>/building-planner/layer}. */
	public LayerFiles(Path root) {
		this.root = root;
	}

	public Path areaFile(Identifier dimension, int areaX, int areaZ) {
		return root.resolve(dimension.getNamespace()).resolve(dimension.getPath()).resolve(areaX + "." + areaZ + EXTENSION);
	}

	/**
	 * Reads every area file into one layer for each dimension that has one. A file that can't be read adds nothing,
	 * and its area is never written; a file whose name or folder isn't an area's is logged and left alone.
	 */
	public Map<Identifier, Layer> readAll() throws IOException {
		Map<Identifier, Layer> layers = new HashMap<>();
		if (!Files.isDirectory(root)) {
			return layers;
		}
		List<Path> files;
		try (Stream<Path> walk = Files.walk(root)) {
			files = walk.filter(path -> path.getFileName().toString().endsWith(EXTENSION) && Files.isRegularFile(path))
				.sorted().toList();
		}
		for (Path file : files) {
			Path relative = root.relativize(file);
			String name = relative.getFileName().toString();
			String[] coordinates = name.substring(0, name.length() - EXTENSION.length()).split("\\.", -1);
			Identifier dimension = relative.getNameCount() < 3 ? null : Identifier.tryBuild(relative.getName(0).toString(),
				relative.subpath(1, relative.getNameCount() - 1).toString().replace(relative.getFileSystem().getSeparator(), "/"));
			Integer areaX = coordinates.length == 2 ? parseInt(coordinates[0]) : null;
			Integer areaZ = coordinates.length == 2 ? parseInt(coordinates[1]) : null;
			if (dimension == null || areaX == null || areaZ == null || !name.equals(areaX + "." + areaZ + EXTENSION)) {
				BuildingPlanner.LOGGER.warn("Building Planner skipped {}, which isn't a layer file's name", file);
				continue;
			}
			Layer layer = layers.computeIfAbsent(dimension, key -> new Layer());
			try {
				readArea(NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()), areaX, areaZ, layer);
			} catch (IOException | RuntimeException e) {
				unreadable.computeIfAbsent(dimension, key -> new LongOpenHashSet()).add(Layer.key(areaX, areaZ));
				BuildingPlanner.LOGGER.error("Building Planner couldn't read {}; its ghost blocks are left out, and the "
					+ "file is left for repair by hand and won't be written until the server restarts", file, e);
			}
		}
		layers.values().removeIf(Layer::isEmpty);
		return layers;
	}

	/** Writes each area the layer changed since the last save, or deletes its file when it holds no ghost block. */
	public void save(Identifier dimension, Layer layer) {
		LongSet skipped = unreadable.getOrDefault(dimension, LongSet.of());
		for (long area : layer.takeChangedAreas()) {
			if (skipped.contains(area)) {
				continue;
			}
			Path file = areaFile(dimension, Layer.keyX(area), Layer.keyZ(area));
			Optional<CompoundTag> contents = writeArea(layer, Layer.keyX(area), Layer.keyZ(area));
			writer.execute(() -> {
				try {
					if (contents.isPresent()) {
						Files.createDirectories(file.getParent());
						LitematicFormat.writeFile(contents.get(), file);
					} else {
						Files.deleteIfExists(file);
					}
				} catch (IOException | RuntimeException e) {
					BuildingPlanner.LOGGER.error("Building Planner couldn't write {}", file, e);
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
				BuildingPlanner.LOGGER.error("Building Planner's layer files took over two minutes to write; some may be missing");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/** An area's file contents, or empty when the area holds no ghost block. */
	static Optional<CompoundTag> writeArea(Layer layer, int areaX, int areaZ) {
		ListTag columns = new ListTag();
		for (int x = areaX << Layer.AREA_SHIFT; x < (areaX + 1) << Layer.AREA_SHIFT; x++) {
			for (int z = areaZ << Layer.AREA_SHIFT; z < (areaZ + 1) << Layer.AREA_SHIFT; z++) {
				LayerColumn column = layer.column(x, z);
				if (column == null) {
					continue;
				}
				ListTag sections = new ListTag();
				for (PackedSection section : column.pack()) {
					CompoundTag sectionTag = new CompoundTag();
					sectionTag.putInt("y", section.y());
					ListTag palette = new ListTag();
					for (BlockState state : section.palette()) {
						palette.add(LitematicBlockStates.write(state));
					}
					sectionTag.put("palette", palette);
					sectionTag.put("blocks", new LongArrayTag(section.blocks()));
					sections.add(sectionTag);
				}
				CompoundTag columnTag = new CompoundTag();
				columnTag.putInt("x", x);
				columnTag.putInt("z", z);
				columnTag.put("sections", sections);
				columns.add(columnTag);
			}
		}
		if (columns.isEmpty()) {
			return Optional.empty();
		}
		CompoundTag contents = new CompoundTag();
		contents.putInt("version", VERSION);
		contents.putInt("data_version", currentDataVersion());
		contents.put("columns", columns);
		return Optional.of(contents);
	}

	/**
	 * Reads an area's file contents into the layer. Anything damaged (another version, a missing entry, a column
	 * outside the area or listed twice, a damaged section) is an error, and then nothing goes into the layer.
	 */
	static void readArea(CompoundTag contents, int areaX, int areaZ, Layer layer) {
		int version = require(contents.getInt("version"), "version");
		if (version != VERSION) {
			throw new IllegalArgumentException("Unknown layer file version " + version);
		}
		int dataVersion = require(contents.getInt("data_version"), "data_version");
		Map<Long, LayerColumn> columns = new HashMap<>();
		for (Tag entry : require(contents.getList("columns"), "columns")) {
			CompoundTag columnTag = compound(entry);
			int x = require(columnTag.getInt("x"), "x");
			int z = require(columnTag.getInt("z"), "z");
			if (x >> Layer.AREA_SHIFT != areaX || z >> Layer.AREA_SHIFT != areaZ) {
				throw new IllegalArgumentException("Column " + x + ", " + z + " is outside area " + areaX + ", " + areaZ);
			}
			List<PackedSection> sections = new ArrayList<>();
			for (Tag sectionEntry : require(columnTag.getList("sections"), "sections")) {
				CompoundTag sectionTag = compound(sectionEntry);
				List<BlockState> palette = new ArrayList<>();
				for (Tag state : require(sectionTag.getList("palette"), "palette")) {
					palette.add(LitematicBlockStates.read(fixed(compound(state), dataVersion)));
				}
				sections.add(new PackedSection(require(sectionTag.getInt("y"), "y"), palette,
					require(sectionTag.getLongArray("blocks"), "blocks")));
			}
			if (columns.put(Layer.key(x, z), LayerColumn.unpack(sections)) != null) {
				throw new IllegalArgumentException("Column " + x + ", " + z + " is listed twice");
			}
		}
		columns.forEach((key, column) -> layer.putColumn(Layer.keyX(key), Layer.keyZ(key), column));
	}

	private static CompoundTag fixed(CompoundTag state, int dataVersion) {
		if (dataVersion >= currentDataVersion()) {
			return state;
		}
		return (CompoundTag) DataFixers.getDataFixer().update(References.BLOCK_STATE,
			new Dynamic<Tag>(NbtOps.INSTANCE, state), dataVersion, currentDataVersion()).getValue();
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

	private static Integer parseInt(String text) {
		try {
			return Integer.parseInt(text);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int currentDataVersion() {
		return SharedConstants.getCurrentVersion().dataVersion().version();
	}
}
