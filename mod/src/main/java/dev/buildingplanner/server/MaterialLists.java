package dev.buildingplanner.server;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.sync.ListData;
import dev.buildingplanner.sync.ListLimits;
import dev.buildingplanner.sync.Payloads;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * The server's store of each player's material lists: read from the player's file when they first join after the
 * server starts, kept in memory until it stops, and written whole at most every {@link #SAVE_INTERVAL_MILLIS} while
 * they've changed, and when the server stops. It answers the game's four list messages, dropping one that breaks a
 * limit ({@link ListLimits}) with a warning that names the player. Every method runs on the server thread.
 */
public final class MaterialLists {
	/** A player's changed lists are written at most this often. */
	static final long SAVE_INTERVAL_MILLIS = 30_000;

	/** Where a player's lists are read from and written to; {@link ListFiles} on a server. */
	public interface Store {
		PlayerLists read(UUID player);

		void write(UUID player, PlayerLists lists);
	}

	private final Store store;
	private final LayerSync.Outbox outbox;
	private final Predicate<Identifier> hasDimension;
	private final Map<UUID, PlayerLists> players = new HashMap<>();
	private final Set<UUID> changed = new HashSet<>();
	/** The times of each player's latest list messages, at most {@link ListLimits#MAX_MESSAGES}, oldest first. */
	private final Map<UUID, Deque<Long>> messageTimes = new HashMap<>();
	private long lastSaveMillis;

	/** {@code hasDimension} says whether the server has a dimension; {@code nowMillis} starts the first save interval. */
	public MaterialLists(Store store, LayerSync.Outbox outbox, Predicate<Identifier> hasDimension, long nowMillis) {
		this.store = store;
		this.outbox = outbox;
		this.hasDimension = hasDimension;
		this.lastSaveMillis = nowMillis;
	}

	/**
	 * At join, after {@code hello}: reads the player's file if it hasn't yet, and returns {@code lists} and
	 * {@code current} to send them, in that order.
	 */
	public List<CustomPacketPayload> join(UUID player) {
		PlayerLists lists = lists(player);
		return List.of(index(lists), new Payloads.Current(lists.current(), lists.panel()));
	}

	/** {@code save_list}: keeps the list under the name, saved now, replacing a kept list of that name. */
	public void save(UUID player, String playerName, Payloads.SaveList message, long nowMillis) {
		if (!accept(player, playerName, "save_list", nowMillis)) {
			return;
		}
		Optional<String> problem = message.tooLarge() ? Optional.of(tooLarge())
			: ListLimits.nameProblem(message.name()).or(() -> ListLimits.listProblem(message.list(), hasDimension));
		if (problem.isPresent()) {
			drop(playerName, "save_list", problem.get());
			return;
		}
		PlayerLists lists = lists(player);
		lists.save(message.list().named(message.name(), nowMillis));
		changed.add(player);
		sendIndex(player, lists);
	}

	/** {@code delete_list}: deletes the kept list of that name. */
	public void delete(UUID player, String playerName, Payloads.DeleteList message, long nowMillis) {
		if (!accept(player, playerName, "delete_list", nowMillis)) {
			return;
		}
		Optional<String> problem = ListLimits.nameProblem(message.name());
		if (problem.isPresent()) {
			drop(playerName, "delete_list", problem.get());
			return;
		}
		PlayerLists lists = lists(player);
		if (lists.delete(message.name())) {
			changed.add(player);
		}
		sendIndex(player, lists);
	}

	/**
	 * {@code open_list}: sends the kept list of that name, or "Last counted" for an empty name; for a list no longer
	 * kept, sends the index again instead.
	 */
	public void open(UUID player, String playerName, Payloads.OpenList message, long nowMillis) {
		if (!accept(player, playerName, "open_list", nowMillis)) {
			return;
		}
		if (!message.name().isEmpty()) {
			Optional<String> problem = ListLimits.nameProblem(message.name());
			if (problem.isPresent()) {
				drop(playerName, "open_list", problem.get());
				return;
			}
		}
		PlayerLists lists = lists(player);
		Optional<ListData> list = message.name().isEmpty() ? lists.lastCounted() : lists.find(message.name());
		if (list.isPresent()) {
			outbox.send(player, new Payloads.OpenedList(list.get()));
		} else {
			sendIndex(player, lists);
		}
	}

	/**
	 * {@code current_list}: keeps the current list and the panel's switch; a list just counted from a box also
	 * becomes "Last counted", counted now.
	 */
	public void current(UUID player, String playerName, Payloads.CurrentList message, long nowMillis) {
		if (!accept(player, playerName, "current_list", nowMillis)) {
			return;
		}
		Optional<String> problem = message.tooLarge() ? Optional.of(tooLarge())
			: message.list().flatMap(list -> ListLimits.listProblem(list, hasDimension));
		if (problem.isPresent()) {
			drop(playerName, "current_list", problem.get());
			return;
		}
		PlayerLists lists = lists(player);
		if (message.counted() && message.list().isPresent()) {
			lists.setLastCounted(Optional.of(message.list().get().counted(nowMillis)));
			changed.add(player);
		}
		if (!lists.current().equals(message.list()) || lists.panel() != message.panel()) {
			lists.setCurrent(message.list());
			lists.setPanel(message.panel());
			changed.add(player);
		}
	}

	/** Every {@link #SAVE_INTERVAL_MILLIS}, writes the files of the players whose lists changed since the last save. */
	public void tick(long nowMillis) {
		if (nowMillis - lastSaveMillis >= SAVE_INTERVAL_MILLIS) {
			lastSaveMillis = nowMillis;
			save();
		}
	}

	/** Writes the files of the players whose lists changed since the last save. */
	public void save() {
		for (UUID player : changed) {
			store.write(player, players.get(player));
		}
		changed.clear();
	}

	/** The player's lists, read from their file the first time. */
	PlayerLists lists(UUID player) {
		return players.computeIfAbsent(player, store::read);
	}

	/**
	 * Whether a message is within {@link ListLimits#MAX_MESSAGES} in any {@link ListLimits#MESSAGE_WINDOW_MILLIS}; a
	 * message past that is dropped with a warning and doesn't count.
	 */
	private boolean accept(UUID player, String playerName, String kind, long nowMillis) {
		Deque<Long> times = messageTimes.computeIfAbsent(player, key -> new ArrayDeque<>());
		while (!times.isEmpty() && nowMillis - times.peekFirst() >= ListLimits.MESSAGE_WINDOW_MILLIS) {
			times.removeFirst();
		}
		if (times.size() >= ListLimits.MAX_MESSAGES) {
			drop(playerName, kind, "more than " + ListLimits.MAX_MESSAGES + " list messages in "
				+ ListLimits.MESSAGE_WINDOW_MILLIS / 1000 + " seconds");
			return false;
		}
		times.addLast(nowMillis);
		return true;
	}

	private void sendIndex(UUID player, PlayerLists lists) {
		outbox.send(player, index(lists));
	}

	private static Payloads.Lists index(PlayerLists lists) {
		return new Payloads.Lists(lists.lastCounted().map(Payloads.ListSummary::of),
			lists.saved().stream().map(Payloads.ListSummary::of).toList());
	}

	private static String tooLarge() {
		return "a list over " + ListLimits.MAX_LIST_BYTES + " bytes";
	}

	private static void drop(String playerName, String kind, String problem) {
		BuildingPlanner.LOGGER.warn("Building Planner dropped {}'s {}: {}", playerName, kind, problem);
	}
}
