package dev.buildingplanner.client;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/** The kinds of Effortless Building packet this client has sent to the server, for the driver's {@code ebsent}. */
public final class SentPackets {
	private static final Set<String> EFFORTLESS = new ConcurrentSkipListSet<>();

	private SentPackets() {
	}

	/** A packet with this namespace and path went out on the connection. */
	public static void sent(String namespace, String path) {
		if (namespace.equals("effortlessbuilding")) {
			EFFORTLESS.add(path);
		}
	}

	/** The paths of Effortless Building's packets sent so far, comma-separated and sorted, or "none". */
	public static String effortless() {
		return EFFORTLESS.isEmpty() ? "none" : String.join(",", EFFORTLESS);
	}
}
