package dev.buildingplanner.look;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.ClientConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GhostsShownTest {
	@Test
	void ghostBlocksShowAtFirst(@TempDir Path dir) {
		assertTrue(GhostsShown.get(ClientConfig.load(dir.resolve("buildingplanner-client.properties"))));
	}

	@Test
	void hidingLastsAcrossGameRuns(@TempDir Path dir) throws IOException {
		Path file = dir.resolve("buildingplanner-client.properties");
		GhostsShown.set(ClientConfig.load(file), false);
		assertFalse(GhostsShown.get(ClientConfig.load(file)));
		GhostsShown.set(ClientConfig.load(file), true);
		assertTrue(GhostsShown.get(ClientConfig.load(file)));
	}

	@Test
	void aValueThatIsntFalseShowsThem(@TempDir Path dir) throws IOException {
		Path file = dir.resolve("buildingplanner-client.properties");
		Files.writeString(file, GhostsShown.KEY + "=no\n");
		assertTrue(GhostsShown.get(ClientConfig.load(file)));
	}
}
