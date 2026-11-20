package dev.buildingplanner.look;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.ClientConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LayerKeysTest {
	// Stand-ins for Minecraft 26.3's codes for Page Up and Page Down (InputConstants.KEY_PAGEUP and KEY_PAGEDOWN), and
	// for other keys; the rules compare codes and never read their values.
	private static final int PAGE_UP = 201;
	private static final int PAGE_DOWN = 202;
	private static final int KEY_K = 14;
	private static final int KEY_L = 15;

	@Test
	void theGroupAppliesOnlyWithLitematicaLoaded() {
		assertTrue(LayerKeys.applies(true));
		assertFalse(LayerKeys.applies(false));
	}

	@Test
	void eachHotkeyIsClearedWhileItIsItsDefaultKey() {
		assertTrue(LayerKeys.clear(List.of(PAGE_UP), PAGE_UP));
		assertTrue(LayerKeys.clear(List.of(PAGE_DOWN), PAGE_DOWN));
	}

	@Test
	void aHotkeyThePlayerMovedStays() {
		// Another key, the other layer key, a chord that holds the default key, or no key.
		assertFalse(LayerKeys.clear(List.of(KEY_K), PAGE_UP));
		assertFalse(LayerKeys.clear(List.of(PAGE_DOWN), PAGE_UP));
		assertFalse(LayerKeys.clear(List.of(PAGE_UP), PAGE_DOWN));
		assertFalse(LayerKeys.clear(List.of(PAGE_UP, KEY_L), PAGE_UP));
		assertFalse(LayerKeys.clear(List.of(KEY_L, PAGE_UP), PAGE_UP));
		assertFalse(LayerKeys.clear(List.of(), PAGE_UP));
	}

	@Test
	void eachHotkeyIsDecidedOnItsOwn() {
		// layerNext still Page Up, layerPrevious moved to K: only layerNext is cleared.
		assertTrue(LayerKeys.clear(List.of(PAGE_UP), PAGE_UP));
		assertFalse(LayerKeys.clear(List.of(KEY_K), PAGE_DOWN));
	}

	@Test
	void aMissingFileMeansTheGroupWasNotSet(@TempDir Path dir) {
		assertFalse(LayerKeys.applied(ClientConfig.load(dir.resolve("config/buildingplanner-client.properties"))));
	}

	@Test
	void theRecordIsWrittenAndReadBack(@TempDir Path dir) throws IOException {
		Path file = dir.resolve("config/buildingplanner-client.properties");
		LayerKeys.markApplied(ClientConfig.load(file));
		assertTrue(LayerKeys.applied(ClientConfig.load(file)));
		LayerKeys.markApplied(ClientConfig.load(file));
		assertTrue(LayerKeys.applied(ClientConfig.load(file)));
		assertEquals("layer-keys-applied", LayerKeys.APPLIED);
	}

	@Test
	void theRecordIsItsOwnAndKeepsTheOtherSettings(@TempDir Path dir) throws IOException {
		// A config 0.5.0 wrote, with the records of the groups before this one.
		Path file = dir.resolve("client.properties");
		Files.writeString(file, "place-repeat-ticks=2\nlook-defaults-applied=true\narea-selection-off-applied=true\n"
			+ "menu-key-applied=true\n");
		assertFalse(LayerKeys.applied(ClientConfig.load(file)));
		LayerKeys.markApplied(ClientConfig.load(file));
		ClientConfig config = ClientConfig.load(file);
		assertTrue(LayerKeys.applied(config));
		assertEquals("2", config.get("place-repeat-ticks"));
		assertTrue(LookDefaults.lookApplied(config));
		assertTrue(LookDefaults.areaSelectionApplied(config));
		assertTrue(MenuKey.applied(config));
	}

	@Test
	void anUnreadableOrUnrelatedFileMeansTheGroupWasNotSet(@TempDir Path dir) throws IOException {
		Path file = dir.resolve("client.properties");
		Files.writeString(file, "menu-key-applied=true\n");
		assertFalse(LayerKeys.applied(ClientConfig.load(file)));
		Files.writeString(file, "layer-keys-applied=maybe\n");
		assertFalse(LayerKeys.applied(ClientConfig.load(file)));
		Files.writeString(file, "\\uZZZZ=1\n");
		assertFalse(LayerKeys.applied(ClientConfig.load(file)));
	}
}
