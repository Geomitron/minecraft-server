package dev.buildingplanner.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentSkipListSet;

import dev.buildingplanner.sync.LayerChange;
import net.minecraft.core.BlockPos;

/**
 * The custom payloads this client has sent to the server, for the driver's {@code ebsent} (the kinds of Effortless
 * Building packet) and {@code holdlog} (the client ticks Building Planner's edits went out on, and how long each mouse
 * button was down, and how many different blocks the edits changed), and {@code placesent} (the blocks of each
 * {@code place} since then).
 */
public final class SentPackets {
	private static final Set<String> EFFORTLESS = new ConcurrentSkipListSet<>();
	/** The paths of Building Planner's payloads sent so far, for the driver's {@code bpsent}. */
	private static final Set<String> PLANNER = new ConcurrentSkipListSet<>();
	private static final List<Integer> EDITS = new ArrayList<>();
	/** The number of blocks each {@code place} sent since the hold log started carried, in order. */
	private static final List<Integer> PLACES = new ArrayList<>();
	/** The blocks the edits sent so far changed. */
	private static final Set<BlockPos> CHANGED = new HashSet<>();
	private static volatile int tick;
	private static int useDown;
	private static int attackDown;
	/** Key and mouse button presses the game has handled, on the render thread. */
	private static int presses;

	private SentPackets() {
	}

	/** A packet with this namespace and path went out on the connection. */
	public static void sent(String namespace, String path, List<LayerChange> changes) {
		if (namespace.equals("buildingplanner")) {
			PLANNER.add(path);
		}
		if (namespace.equals("effortlessbuilding")) {
			EFFORTLESS.add(path);
		} else if (namespace.equals("buildingplanner") && path.equals("edit")) {
			synchronized (EDITS) {
				EDITS.add(tick);
				for (LayerChange change : changes) {
					CHANGED.add(change.pos());
				}
			}
		} else if (namespace.equals("buildingplanner") && path.equals("place")) {
			synchronized (EDITS) {
				PLACES.add(changes.size());
			}
		}
	}

	/**
	 * The {@code place} payloads sent since the hold log started, for the driver's {@code placesent}: each one's number of
	 * blocks, comma-separated, or "none".
	 */
	static String places() {
		synchronized (EDITS) {
			return PLACES.isEmpty() ? "none" : String.join(",", PLACES.stream().map(String::valueOf).toList());
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

	/** Whether the use (or else the attack) button was down in a client tick since the hold log started. */
	static boolean held(boolean use) {
		synchronized (EDITS) {
			return (use ? useDown : attackDown) > 0;
		}
	}

	/** The game handled a key or mouse button press (KeyMappingMixin). */
	public static void pressed() {
		presses++;
	}

	/** How many key and mouse button presses the game has handled since it started. */
	static int presses() {
		return presses;
	}

	/** Starts the hold log again. */
	static void resetHoldLog() {
		synchronized (EDITS) {
			EDITS.clear();
			PLACES.clear();
			CHANGED.clear();
			useDown = 0;
			attackDown = 0;
		}
	}

	/**
	 * The hold log: ticks the use and attack buttons were down, how many edits went out, the gaps in ticks between them,
	 * for example {@code use=41 attack=0 edits=11 blocks=11 gaps=4,4,4,4,4,4,4,4,4,4}. {@code blocks} is the number of
	 * different blocks the edits changed: it falls short of {@code edits} when two repeats edit the same block, as a
	 * walk paced unevenly by the game's commands lets them.
	 */
	static String holdLog() {
		synchronized (EDITS) {
			List<String> gaps = new ArrayList<>();
			for (int i = 1; i < EDITS.size(); i++) {
				gaps.add(Integer.toString(EDITS.get(i) - EDITS.get(i - 1)));
			}
			return "use=" + useDown + " attack=" + attackDown + " edits=" + EDITS.size() + " blocks=" + CHANGED.size()
				+ " gaps=" + String.join(",", gaps);
		}
	}

	/** The paths of Building Planner's payloads sent so far, comma-separated and sorted, or "none". */
	static String planner() {
		return PLANNER.isEmpty() ? "none" : String.join(",", PLANNER);
	}

	/** The paths of Effortless Building's packets sent so far, comma-separated and sorted, or "none". */
	public static String effortless() {
		return EFFORTLESS.isEmpty() ? "none" : String.join(",", EFFORTLESS);
	}
}
