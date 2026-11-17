package dev.buildingplanner;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Building Planner's settings on a player's computer, in {@code config/buildingplanner-client.properties}: read once
 * when the game starts, and written whole on each change, so each setting keeps the others.
 */
public final class ClientConfig {
	private final Path file;
	private final Properties properties;

	private ClientConfig(Path file, Properties properties) {
		this.file = file;
		this.properties = properties;
	}

	/** The settings in {@code file}. A missing or unreadable file has none. */
	public static ClientConfig load(Path file) {
		Properties properties = new Properties();
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				properties.load(reader);
			} catch (IOException | IllegalArgumentException e) {
				properties.clear();
			}
		}
		return new ClientConfig(file, properties);
	}

	/** The setting's value, trimmed, or null when it isn't set. */
	public String get(String key) {
		String value = properties.getProperty(key);
		return value == null ? null : value.trim();
	}

	/** Sets the setting and writes the file. */
	public void set(String key, String value) throws IOException {
		properties.setProperty(key, value);
		Files.createDirectories(file.getParent());
		try (Writer writer = Files.newBufferedWriter(file)) {
			properties.store(writer, "Building Planner's settings on this computer.");
		}
	}
}
