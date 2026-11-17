package dev.buildingplanner.look;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * How Building Planner sets Litematica's ghost blocks to look on a player's computer, once: translucent, at 0.8
 * opacity. An option the player has changed from Litematica's default keeps the player's value, and once the defaults
 * are set, the file {@code config/buildingplanner-client.properties} records it, so a later change of the player's own
 * is never overwritten.
 */
public final class LookDefaults {
	/** What Building Planner sets {@code renderBlocksAsTranslucent} to. */
	public static final boolean TRANSLUCENT = true;
	/** What Building Planner sets {@code ghostBlockAlpha} to. */
	public static final double GHOST_ALPHA = 0.8;

	static final String APPLIED = "look-defaults-applied";

	private LookDefaults() {
	}

	/** The value an option takes: Building Planner's when the player left it at Litematica's default, else the player's. */
	public static boolean choose(boolean current, boolean litematicaDefault, boolean ours) {
		return current == litematicaDefault ? ours : current;
	}

	/** The value an option takes: Building Planner's when the player left it at Litematica's default, else the player's. */
	public static double choose(double current, double litematicaDefault, double ours) {
		return Double.compare(current, litematicaDefault) == 0 ? ours : current;
	}

	/** Whether {@code file} records that the defaults were set. A missing or unreadable file doesn't. */
	public static boolean applied(Path file) {
		if (!Files.exists(file)) {
			return false;
		}
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(file)) {
			properties.load(reader);
		} catch (IOException | IllegalArgumentException e) {
			return false;
		}
		return Boolean.parseBoolean(properties.getProperty(APPLIED, "").trim());
	}

	/** Records in {@code file} that the defaults were set. */
	public static void markApplied(Path file) throws IOException {
		Properties properties = new Properties();
		properties.setProperty(APPLIED, "true");
		Files.createDirectories(file.getParent());
		try (Writer writer = Files.newBufferedWriter(file)) {
			properties.store(writer, "Building Planner, on this computer. " + APPLIED
				+ ": Litematica's ghost block look was set once, so later changes of yours stay.");
		}
	}
}
