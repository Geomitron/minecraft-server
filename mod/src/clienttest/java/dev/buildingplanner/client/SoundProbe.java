package dev.buildingplanner.client;

import java.util.ArrayList;
import java.util.List;

/**
 * The sounds this game played at a position in its world since the driver's {@code soundreset}, the server's and its
 * own alike ({@code ClientLevel.playSound}, which {@code ClientLevelMixin} reads), and those the server sent
 * ({@code ClientPacketListenerMixin}), for the driver's {@code sounds} and {@code serversounds}. Effortless Building's
 * game plays a quiet place sound of its own as its preview changes. Read and written on the client thread.
 */
public final class SoundProbe {
	private static final List<String> played = new ArrayList<>();
	private static final List<String> sent = new ArrayList<>();

	private SoundProbe() {
	}

	/** The game plays the sound with this id, such as minecraft:block.wood.place. */
	public static void played(String id) {
		played.add(id);
	}

	/** The server sent the sound with this id to play. */
	public static void sent(String id) {
		sent.add(id);
	}

	static void reset() {
		played.clear();
		sent.clear();
	}

	/**
	 * How many sounds played since {@link #reset} have {@code text} in their id: of those the server sent, or with
	 * {@code fromServer} false, of all.
	 */
	static long count(String text, boolean fromServer) {
		return (fromServer ? sent : played).stream().filter(id -> id.contains(text)).count();
	}
}
