package dev.buildingplanner.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The folders a local session keeps a player's ghost blocks and lists in, by server and player. */
class LocalFoldersTest {
	private static final Path GAME = Path.of("game");

	@Test
	void eachColonOfTheAddressBecomesAnUnderscore() {
		assertEquals(Optional.of("localhost_25565"), LocalFolders.server("localhost:25565", false));
		assertEquals(Optional.of("play.example.org"), LocalFolders.server("play.example.org", false));
		assertEquals(Optional.of("[__1]_25565"), LocalFolders.server("[::1]:25565", false));
	}

	@Test
	void aRealmIsRealms() {
		assertEquals(Optional.of("realms"), LocalFolders.server("realm.example.org:25565", true));
	}

	@Test
	void anAddressThatCantBeOneFolderHasNone() {
		assertEquals(Optional.empty(), LocalFolders.server("", false));
		assertEquals(Optional.empty(), LocalFolders.server("..", false));
		assertEquals(Optional.empty(), LocalFolders.server("example.org/x", false));
	}

	@Test
	void twoPlayersHaveTwoFolders() {
		UUID one = UUID.fromString("527e7f9d-ae90-3a44-aa88-30a47a9cdf34");
		UUID two = UUID.fromString("00000000-0000-0000-0000-000000000002");
		Path first = LocalFolders.player(GAME, "localhost_25565", one);
		Path second = LocalFolders.player(GAME, "localhost_25565", two);

		assertEquals(GAME.resolve("building-planner/servers/localhost_25565/527e7f9d-ae90-3a44-aa88-30a47a9cdf34"), first);
		assertNotEquals(first, second);
		assertEquals(first.resolve("layer"), LocalFolders.layer(first));
		assertEquals(first.resolve("material-lists"), LocalFolders.materialLists(first));
	}
}
