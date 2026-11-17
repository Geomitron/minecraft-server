package dev.buildingplanner.look;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.ClientConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LookDefaultsTest {
	// Litematica's defaults: renderBlocksAsTranslucent off, ghostBlockAlpha 0.5.
	private static final boolean LITEMATICA_TRANSLUCENT = false;
	private static final double LITEMATICA_ALPHA = 0.5;

	@Test
	void ourValuesAreTheOnesGeoChose() {
		assertTrue(LookDefaults.TRANSLUCENT);
		assertEquals(0.8, LookDefaults.GHOST_ALPHA);
	}

	@Test
	void anOptionLeftAtLitematicasDefaultTakesOurs() {
		assertTrue(LookDefaults.choose(LITEMATICA_TRANSLUCENT, LITEMATICA_TRANSLUCENT, LookDefaults.TRANSLUCENT));
		assertEquals(0.8, LookDefaults.choose(LITEMATICA_ALPHA, LITEMATICA_ALPHA, LookDefaults.GHOST_ALPHA));
	}

	@Test
	void anOptionThePlayerChangedKeepsTheirValue() {
		// The player turned translucent on already, or moved the alpha.
		assertTrue(LookDefaults.choose(true, LITEMATICA_TRANSLUCENT, false));
		assertEquals(0.3, LookDefaults.choose(0.3, LITEMATICA_ALPHA, LookDefaults.GHOST_ALPHA));
		assertEquals(1.0, LookDefaults.choose(1.0, LITEMATICA_ALPHA, LookDefaults.GHOST_ALPHA));
	}

	@Test
	void eachOptionIsDecidedOnItsOwn() {
		assertEquals(0.3, LookDefaults.choose(0.3, LITEMATICA_ALPHA, LookDefaults.GHOST_ALPHA));
		assertTrue(LookDefaults.choose(LITEMATICA_TRANSLUCENT, LITEMATICA_TRANSLUCENT, LookDefaults.TRANSLUCENT));
	}

	@Test
	void aMissingFileMeansTheDefaultsWereNotSet(@TempDir Path dir) {
		assertFalse(LookDefaults.applied(ClientConfig.load(dir.resolve("config/buildingplanner-client.properties"))));
	}

	@Test
	void theMarkIsRecordedAndReadBack(@TempDir Path dir) throws IOException {
		Path file = dir.resolve("config/buildingplanner-client.properties");
		LookDefaults.markApplied(ClientConfig.load(file));
		assertTrue(LookDefaults.applied(ClientConfig.load(file)));
		LookDefaults.markApplied(ClientConfig.load(file));
		assertTrue(LookDefaults.applied(ClientConfig.load(file)));
	}

	@Test
	void theMarkKeepsTheOtherSettings(@TempDir Path dir) throws IOException {
		Path file = dir.resolve("client.properties");
		Files.writeString(file, "place-repeat-ticks=2\n");
		LookDefaults.markApplied(ClientConfig.load(file));
		assertEquals("2", ClientConfig.load(file).get("place-repeat-ticks"));
	}

	@Test
	void anUnreadableOrUnrelatedFileMeansTheDefaultsWereNotSet(@TempDir Path dir) throws IOException {
		Path file = dir.resolve("client.properties");
		Files.writeString(file, "something-else=true\n");
		assertFalse(LookDefaults.applied(ClientConfig.load(file)));
		Files.writeString(file, "look-defaults-applied=maybe\n");
		assertFalse(LookDefaults.applied(ClientConfig.load(file)));
		Files.writeString(file, "\\uZZZZ=1\n");
		assertFalse(LookDefaults.applied(ClientConfig.load(file)));
	}
}
