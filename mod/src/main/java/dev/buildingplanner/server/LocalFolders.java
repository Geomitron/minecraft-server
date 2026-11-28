package dev.buildingplanner.server;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/**
 * Where a local session keeps a player's ghost blocks and material lists for a server that doesn't run Building
 * Planner: {@code <game folder>/building-planner/servers/<server>/<player UUID>/}, with {@code layer/} and
 * {@code material-lists/} in it, in the formats the server uses (see "Servers without Building Planner": "The files").
 */
public final class LocalFolders {
	private LocalFolders() {
	}

	/**
	 * The server's folder name, as MaLiLib names Litematica's folders for it ({@code StringUtils.getWorldOrServerName},
	 * with its default settings): {@code realms} for a Realm, and otherwise the address the player joined with each
	 * {@code :} turned into {@code _}. Empty for an address that can't be one folder's name.
	 */
	public static Optional<String> server(String address, boolean realm) {
		String name = realm ? "realms" : address.replace(':', '_');
		if (name.isEmpty() || name.equals(".") || name.equals("..") || name.contains("/") || name.contains("\\")) {
			return Optional.empty();
		}
		return Optional.of(name);
	}

	/** The player's folder for that server, under the game folder. */
	public static Path player(Path gameDir, String server, UUID player) {
		return gameDir.resolve("building-planner").resolve("servers").resolve(server).resolve(player.toString());
	}

	/** The layer's files in the player's folder ({@link LayerStore}'s root). */
	public static Path layer(Path playerDir) {
		return playerDir.resolve("layer");
	}

	/** The material lists' file's folder in the player's folder ({@link ListFiles}' root). */
	public static Path materialLists(Path playerDir) {
		return playerDir.resolve("material-lists");
	}
}
