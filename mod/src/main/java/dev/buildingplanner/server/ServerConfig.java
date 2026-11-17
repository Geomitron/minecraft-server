package dev.buildingplanner.server;

import dev.buildingplanner.sync.SyncLimits;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * The server's config file, {@code config/buildingplanner.properties}: the cap on edits. A missing file is written
 * with the default; a missing or unreadable value keeps its default.
 */
public final class ServerConfig {
	static final String MAX_EDIT_BLOCKS = "max-edit-blocks";

	private ServerConfig() {
	}

	public static SyncLimits load(Path file) throws IOException {
		Properties properties = new Properties();
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				properties.load(reader);
			}
		} else {
			properties.setProperty(MAX_EDIT_BLOCKS, Integer.toString(SyncLimits.DEFAULT.maxEditBlocks()));
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				properties.store(writer, "Building Planner. " + MAX_EDIT_BLOCKS + ": most blocks one edit may change (1 to "
					+ SyncLimits.WIRE_MAX_EDIT_BLOCKS + ").");
			}
		}
		return new SyncLimits(intOr(properties, MAX_EDIT_BLOCKS, SyncLimits.DEFAULT.maxEditBlocks()));
	}

	private static int intOr(Properties properties, String key, int fallback) {
		try {
			return Integer.parseInt(properties.getProperty(key, "").trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
