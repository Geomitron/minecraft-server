package dev.buildingplanner.sync;

/**
 * Which session the game starts on a server (see "Servers without Building Planner": "Which servers"): at joining, a
 * local session on a server that takes none of the payloads a session sends; otherwise none until the server's
 * {@code hello}, which starts the server's session when it speaks this game's protocol version, in place of any local
 * session, and leaves Building Planner off when it speaks another.
 */
public final class JoinRule {
	/** A session: none (Building Planner off), a local one on the player's computer, or the server's. */
	public enum Session {
		NONE,
		LOCAL,
		SERVER
	}

	private JoinRule() {
	}

	/**
	 * At joining: {@code ownWorld} for the game's own world, singleplayer or a LAN host's, and {@code takenPayloads} how
	 * many of the payloads a session sends the server takes ({@code ClientPlayNetworking.canSend}). A server that takes
	 * any of them runs some version of Building Planner, so the game waits for its {@code hello}.
	 */
	public static Session atJoin(boolean ownWorld, int takenPayloads) {
		return ownWorld || takenPayloads > 0 ? Session.NONE : Session.LOCAL;
	}

	/** A {@code hello} from the server, whatever session runs: the server's session, or none for another version. */
	public static Session atHello(int protocolVersion) {
		return protocolVersion == Payloads.PROTOCOL_VERSION ? Session.SERVER : Session.NONE;
	}
}
