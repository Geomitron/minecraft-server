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
	// Litematica's defaults: renderBlocksAsTranslucent off, ghostBlockAlpha 0.5, enableAreaSelectionBoxesRendering on.
	private static final boolean LITEMATICA_TRANSLUCENT = false;
	private static final double LITEMATICA_ALPHA = 0.5;
	private static final boolean LITEMATICA_AREA_SELECTION = true;

	@Test
	void ourValuesAreTheOnesGeoChose() {
		assertTrue(LookDefaults.TRANSLUCENT);
		assertEquals(0.8, LookDefaults.GHOST_ALPHA);
		assertFalse(LookDefaults.TOOL_ITEM);
		assertFalse(LookDefaults.AREA_SELECTION_RENDERING);
	}

	@Test
	void theRecordsAreTheOnesEachVersionWrote() {
		// 0.3.0 and 0.4.0 wrote look-defaults-applied for every setting they changed once.
		assertEquals("look-defaults-applied", LookDefaults.LOOK_APPLIED);
		assertEquals("area-selection-off-applied", LookDefaults.AREA_SELECTION_APPLIED);
	}

	@Test
	void theAreaSelectionDrawingIsTurnedOffOnlyWhileItIsAtLitematicasDefault() {
		// Litematica draws area selections by default; a player who turned it off already has it off.
		assertFalse(LookDefaults.choose(LITEMATICA_AREA_SELECTION, LITEMATICA_AREA_SELECTION,
			LookDefaults.AREA_SELECTION_RENDERING));
		assertFalse(LookDefaults.choose(false, LITEMATICA_AREA_SELECTION, true));
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
	void aMissingFileMeansNoGroupWasSet(@TempDir Path dir) {
		ClientConfig config = ClientConfig.load(dir.resolve("config/buildingplanner-client.properties"));
		assertFalse(LookDefaults.lookApplied(config));
		assertFalse(LookDefaults.areaSelectionApplied(config));
	}

	@Test
	void theLookRecordIsWrittenAndReadBack(@TempDir Path dir) throws IOException {
		Path file = dir.resolve("config/buildingplanner-client.properties");
		LookDefaults.markLookApplied(ClientConfig.load(file));
		assertTrue(LookDefaults.lookApplied(ClientConfig.load(file)));
		LookDefaults.markLookApplied(ClientConfig.load(file));
		assertTrue(LookDefaults.lookApplied(ClientConfig.load(file)));
	}

	@Test
	void theAreaSelectionRecordIsWrittenAndReadBack(@TempDir Path dir) throws IOException {
		Path file = dir.resolve("config/buildingplanner-client.properties");
		LookDefaults.markAreaSelectionApplied(ClientConfig.load(file));
		assertTrue(LookDefaults.areaSelectionApplied(ClientConfig.load(file)));
		LookDefaults.markAreaSelectionApplied(ClientConfig.load(file));
		assertTrue(LookDefaults.areaSelectionApplied(ClientConfig.load(file)));
	}

	@Test
	void eachGroupHasARecordOfItsOwn(@TempDir Path dir) throws IOException {
		Path file = dir.resolve("client.properties");
		LookDefaults.markLookApplied(ClientConfig.load(file));
		assertFalse(LookDefaults.areaSelectionApplied(ClientConfig.load(file)));
		LookDefaults.markAreaSelectionApplied(ClientConfig.load(file));
		assertTrue(LookDefaults.lookApplied(ClientConfig.load(file)));
		assertTrue(LookDefaults.areaSelectionApplied(ClientConfig.load(file)));
	}

	@Test
	void aConfigFromVersion030Or040GetsTheAreaSelectionGroupAndNotTheLookGroupAgain(@TempDir Path dir)
		throws IOException {
		// What 0.3.0 and 0.4.0 left: the look record alone.
		Path file = dir.resolve("client.properties");
		Files.writeString(file, "look-defaults-applied=true\n");
		ClientConfig config = ClientConfig.load(file);
		assertTrue(LookDefaults.lookApplied(config));
		assertFalse(LookDefaults.areaSelectionApplied(config));
	}

	@Test
	void theRecordsKeepTheOtherSettings(@TempDir Path dir) throws IOException {
		Path file = dir.resolve("client.properties");
		Files.writeString(file, "place-repeat-ticks=2\n");
		LookDefaults.markLookApplied(ClientConfig.load(file));
		LookDefaults.markAreaSelectionApplied(ClientConfig.load(file));
		assertEquals("2", ClientConfig.load(file).get("place-repeat-ticks"));
		assertTrue(LookDefaults.lookApplied(ClientConfig.load(file)));
	}

	@Test
	void anUnreadableOrUnrelatedFileMeansNoGroupWasSet(@TempDir Path dir) throws IOException {
		Path file = dir.resolve("client.properties");
		Files.writeString(file, "something-else=true\n");
		assertFalse(LookDefaults.lookApplied(ClientConfig.load(file)));
		assertFalse(LookDefaults.areaSelectionApplied(ClientConfig.load(file)));
		Files.writeString(file, "look-defaults-applied=maybe\narea-selection-off-applied=maybe\n");
		assertFalse(LookDefaults.lookApplied(ClientConfig.load(file)));
		assertFalse(LookDefaults.areaSelectionApplied(ClientConfig.load(file)));
		Files.writeString(file, "\\uZZZZ=1\n");
		assertFalse(LookDefaults.lookApplied(ClientConfig.load(file)));
		assertFalse(LookDefaults.areaSelectionApplied(ClientConfig.load(file)));
	}
}
