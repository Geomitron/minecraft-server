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

class MenuKeyTest {
	// Minecraft 26.3's code for M (InputConstants.KEY_M), which MaLiLib stores as the key's code; K is 14 and L is 15.
	private static final int KEY_M = 16;
	private static final int KEY_K = 14;
	private static final int KEY_L = 15;

	@Test
	void theKeysAreFreecamsConfigKeyAndMod() {
		assertEquals("key.freecam.config.open", MenuKey.FREECAM_KEY);
		assertEquals("freecam", MenuKey.FREECAM_MOD);
	}

	@Test
	void theGroupAppliesOnlyWithBothModsLoaded() {
		assertTrue(MenuKey.applies(true, true));
		assertFalse(MenuKey.applies(true, false));
		assertFalse(MenuKey.applies(false, true));
		assertFalse(MenuKey.applies(false, false));
	}

	@Test
	void freecamsKeyTakesMOnlyWhileItHasNoKey() {
		assertTrue(MenuKey.bindFreecam(true));
		// A key the player bound already, K say, stays.
		assertFalse(MenuKey.bindFreecam(false));
	}

	@Test
	void litematicasHotkeyIsClearedOnlyWhileItIsM() {
		assertTrue(MenuKey.clearLitematica(List.of(KEY_M), KEY_M));
		// A hotkey the player moved stays: another key, a chord that holds M, or none.
		assertFalse(MenuKey.clearLitematica(List.of(KEY_K), KEY_M));
		assertFalse(MenuKey.clearLitematica(List.of(KEY_M, KEY_L), KEY_M));
		assertFalse(MenuKey.clearLitematica(List.of(KEY_L, KEY_M), KEY_M));
		assertFalse(MenuKey.clearLitematica(List.of(), KEY_M));
	}

	@Test
	void eachKeyIsDecidedOnItsOwn() {
		// Freecam's key bound to K by the player, Litematica's hotkey still M: only Litematica's changes.
		assertFalse(MenuKey.bindFreecam(false));
		assertTrue(MenuKey.clearLitematica(List.of(KEY_M), KEY_M));
		// Freecam's key unbound, Litematica's hotkey moved to L: only Freecam's changes.
		assertTrue(MenuKey.bindFreecam(true));
		assertFalse(MenuKey.clearLitematica(List.of(KEY_L), KEY_M));
	}

	@Test
	void aMissingFileMeansTheGroupWasNotSet(@TempDir Path dir) {
		assertFalse(MenuKey.applied(ClientConfig.load(dir.resolve("config/buildingplanner-client.properties"))));
	}

	@Test
	void theRecordIsWrittenAndReadBack(@TempDir Path dir) throws IOException {
		Path file = dir.resolve("config/buildingplanner-client.properties");
		MenuKey.markApplied(ClientConfig.load(file));
		assertTrue(MenuKey.applied(ClientConfig.load(file)));
		MenuKey.markApplied(ClientConfig.load(file));
		assertTrue(MenuKey.applied(ClientConfig.load(file)));
		assertEquals("menu-key-applied", MenuKey.APPLIED);
	}

	@Test
	void theRecordIsItsOwnAndKeepsTheOtherSettings(@TempDir Path dir) throws IOException {
		// A config 0.4.0 wrote, and one with the records of the groups before this one.
		Path file = dir.resolve("client.properties");
		Files.writeString(file, "place-repeat-ticks=2\nlook-defaults-applied=true\narea-selection-off-applied=true\n");
		assertFalse(MenuKey.applied(ClientConfig.load(file)));
		MenuKey.markApplied(ClientConfig.load(file));
		ClientConfig config = ClientConfig.load(file);
		assertTrue(MenuKey.applied(config));
		assertEquals("2", config.get("place-repeat-ticks"));
		assertTrue(LookDefaults.lookApplied(config));
		assertTrue(LookDefaults.areaSelectionApplied(config));
	}

	@Test
	void anUnreadableOrUnrelatedFileMeansTheGroupWasNotSet(@TempDir Path dir) throws IOException {
		Path file = dir.resolve("client.properties");
		Files.writeString(file, "look-defaults-applied=true\n");
		assertFalse(MenuKey.applied(ClientConfig.load(file)));
		Files.writeString(file, "menu-key-applied=maybe\n");
		assertFalse(MenuKey.applied(ClientConfig.load(file)));
		Files.writeString(file, "\\uZZZZ=1\n");
		assertFalse(MenuKey.applied(ClientConfig.load(file)));
	}
}
