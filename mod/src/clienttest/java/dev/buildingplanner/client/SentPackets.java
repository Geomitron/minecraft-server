package dev.buildingplanner.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * The custom payloads this client has sent to the server, for the driver's {@code ebsent} (the kinds of Effortless
 * Building packet) and {@code holdlog} (the client ticks Building Planner's edits went out on, and how long each mouse
 * button was down).
 */
public final class SentPackets {
	private static final Set<String> EFFORTLESS = new ConcurrentSkipListSet<>();
	private static final List<Integer> EDITS = new ArrayList<>();
	private static volatile int tick;
	private static int useDown;
	private static int attackDown;

	private SentPackets() {
	}

	/** A packet with this namespace and path went out on the connection; an edit is either protocol's ({@code edit}, {@code plan_edit}). */
	public static void sent(String namespace, String path) {
		if (namespace.equals("effortlessbuilding")) {
			EFFORTLESS.add(path);
		} else if (namespace.equals("buildingplanner") && (path.equals("edit") || path.equals("plan_edit"))) {
			synchronized (EDITS) {
				EDITS.add(tick);
			}
		}
	}

	/** A client tick starts, before any mod's end-of-tick work, so a send's tick doesn't depend on listener order. */
	static void startTick() {
		tick++;
	}

	/** Says whether the use and attack buttons were down in this client tick. */
	static void buttons(boolean use, boolean attack) {
		synchronized (EDITS) {
			useDown += use ? 1 : 0;
			attackDown += attack ? 1 : 0;
		}
	}

	/** Starts the hold log again. */
	static void resetHoldLog() {
		synchronized (EDITS) {
			EDITS.clear();
			useDown = 0;
			attackDown = 0;
		}
	}

	/**
	 * The hold log: ticks the use and attack buttons were down, how many edits went out, the gaps in ticks between them,
	 * for example {@code use=41 attack=0 edits=11 gaps=4,4,4,4,4,4,4,4,4,4}.
	 */
	static String holdLog() {
		synchronized (EDITS) {
			List<String> gaps = new ArrayList<>();
			for (int i = 1; i < EDITS.size(); i++) {
				gaps.add(Integer.toString(EDITS.get(i) - EDITS.get(i - 1)));
			}
			return "use=" + useDown + " attack=" + attackDown + " edits=" + EDITS.size() + " gaps=" + String.join(",", gaps);
		}
	}

	/** The paths of Effortless Building's packets sent so far, comma-separated and sorted, or "none". */
	public static String effortless() {
		return EFFORTLESS.isEmpty() ? "none" : String.join(",", EFFORTLESS);
	}
}
