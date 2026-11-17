package dev.buildingplanner.server;

import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.layer.LayerFiles;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * When the server reads and writes the layer's files ({@link LayerFiles}): every file at start, the areas that changed
 * at most every {@link #SAVE_INTERVAL_MILLIS} while the layer has changed, and the rest when the server stops. Calls
 * come from the server thread.
 */
public final class LayerStore implements AutoCloseable {
	/** A changed area is written at most this often. */
	static final long SAVE_INTERVAL_MILLIS = 30_000;

	private final LayerFiles files;
	private long lastSaveMillis;

	/** {@code root} is {@code <world>/building-planner/layer}; {@code nowMillis} starts the first save interval. */
	public LayerStore(Path root, long nowMillis) {
		this.files = new LayerFiles(root);
		this.lastSaveMillis = nowMillis;
	}

	/** Every dimension's layer from its files; see {@link LayerFiles#readAll}. */
	public Map<Identifier, Layer> load() throws IOException {
		return files.readAll();
	}

	/** Every {@link #SAVE_INTERVAL_MILLIS}, writes the areas that changed since the last save. */
	public void tick(LayerSync sync, long nowMillis) {
		if (nowMillis - lastSaveMillis >= SAVE_INTERVAL_MILLIS) {
			lastSaveMillis = nowMillis;
			save(sync);
		}
	}

	/** Writes the areas that changed since the last save. */
	public void save(LayerSync sync) {
		sync.layers().forEach(files::save);
	}

	/** Waits for every write. */
	@Override
	public void close() {
		files.close();
	}
}
