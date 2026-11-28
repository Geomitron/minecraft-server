package dev.buildingplanner.client;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.sync.ListData;
import dev.buildingplanner.sync.ListLimits;
import dev.buildingplanner.sync.Payloads;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.malilib.gui.GuiBase;
import java.util.List;
import java.util.Optional;

/**
 * The game's side of the material list messages on one connection, sent through its session's {@link SessionLink}: it
 * keeps the server's index of the player's lists,
 * makes the {@code current} list the server sends at join the current list, and sends {@code current_list} when the
 * current list, its counts or the panel's switch change, at most every {@link #SEND_INTERVAL_MILLIS}, with the latest
 * state. It keeps to the server's limits ({@link ListLimits}), so the player never meets them. Runs on the client's
 * main thread.
 *
 * <p>It also keeps the material list panel's switch, which stays as it is when another list becomes current, and,
 * while the panel is on, counts the current list's box again every {@link #RECOUNT_INTERVAL_MILLIS}, so "Missing" drops
 * as the group builds, or, where the box can't be counted, its "Available".
 */
final class ListSync {
	/** {@code current_list} goes at most this often. */
	static final long SEND_INTERVAL_MILLIS = 2_000;
	/** While the panel is on, the current list is counted again this often, as Litematica's own Info HUD counts. */
	static final long RECOUNT_INTERVAL_MILLIS = 2_000;

	private final SessionLink link;
	private Payloads.Lists index = new Payloads.Lists(Optional.empty(), List.of());
	/** Whether the server's {@code current} has arrived; until then the game sends no {@code current_list}. */
	private boolean started;
	/** The panel's switch. */
	private boolean panel;
	/** The current list and the panel's switch as the server last had them. */
	private Optional<ListData> sentList = Optional.empty();
	private boolean sentPanel;
	/** The list that was current at the last tick, and whether it counted its box since the last send. */
	private GhostMaterialList seen;
	private boolean counted;
	private long lastSendMillis = Long.MIN_VALUE / 2;
	private long lastRecountMillis = Long.MIN_VALUE / 2;
	/** The name of the list "Open" asked the server for ("" for "Last counted"), until its answer arrives, or null. */
	private String opening;

	ListSync(SessionLink link) {
		this.link = link;
	}

	/** The server's index of the player's lists, with "Last counted" kept up to date as the game counts. */
	Payloads.Lists index() {
		return index;
	}

	/** Whether the material list panel is on. */
	boolean panel() {
		return panel;
	}

	/** Turns the panel on or off: the screen's "Info HUD" button, through {@link PanelSwitch}. */
	void setPanel(boolean on) {
		panel = on;
	}

	/**
	 * The server's index of the player's lists. The server sends it instead of the list "Open" asked for when that list
	 * is no longer kept, which ends the wait for it.
	 */
	void receive(Payloads.Lists lists) {
		index = lists;
		if (opening != null && (opening.isEmpty() ? lists.lastCounted().isEmpty()
			: lists.saved().stream().noneMatch(saved -> saved.name().equals(opening)))) {
			opening = null;
		}
	}

	/**
	 * The server's {@code current} at join: makes its list the current list, with its counts as kept, or, with none,
	 * takes away a list of Building Planner's that was current, which belongs to another connection.
	 */
	void receive(Payloads.Current current) {
		panel = current.panel();
		if (current.list().isPresent()) {
			makeCurrent(current.list().get());
		} else if (DataManager.getMaterialList() instanceof GhostMaterialList) {
			makeCurrent((MaterialListBase) null);
		}
		seen = currentList();
		sentList = seen == null ? Optional.empty() : Optional.of(seen.data());
		sentPanel = panel;
		started = true;
	}

	/**
	 * The server's answer to {@code open_list}: makes the list current, with its counts as kept, and opens its screen
	 * when "Open" asked for it.
	 */
	void receive(Payloads.OpenedList opened) {
		makeCurrent(opened.list());
		if (opening != null) {
			opening = null;
			GuiBase.openGui(new GuiMaterialList(currentList()));
		}
	}

	/** The radial menu's "Material lists" button: opens the current list's screen, or "Saved lists" without one. */
	void openCurrent() {
		GhostMaterialList list = currentList();
		if (list != null) {
			GuiBase.openGui(new GuiMaterialList(list));
		} else {
			SavedListsScreen.open(null);
		}
	}

	/** Keeps the list under the name on the server, replacing a kept list of that name. */
	void save(String name, ListData list) {
		if (ListLimits.nameProblem(name).isEmpty() && fits(list)) {
			link.send(new Payloads.SaveList(name, list));
		}
	}

	void delete(String name) {
		if (ListLimits.nameProblem(name).isEmpty()) {
			link.send(new Payloads.DeleteList(name));
		}
	}

	/**
	 * "Open": asks the server for the kept list of that name, or for "Last counted" with an empty name, and opens its
	 * screen when it arrives.
	 */
	void open(String name) {
		if (name.isEmpty() || ListLimits.nameProblem(name).isEmpty()) {
			link.send(new Payloads.OpenList(name));
			opening = name;
		}
	}

	/** Once a client tick: notes a count of the current list, and sends {@code current_list} when it's due. */
	void tick(long nowMillis) {
		if (!started) {
			return;
		}
		GhostMaterialList list = currentList();
		if (list != seen) {
			seen = list;
			counted = false;
		}
		if (panel && list != null && nowMillis - lastRecountMillis >= RECOUNT_INTERVAL_MILLIS) {
			lastRecountMillis = nowMillis;
			list.recount();
		}
		if (list != null && list.takeCounted()) {
			counted = true;
			ListData data = list.data();
			index = new Payloads.Lists(Optional.of(new Payloads.ListSummary("", nowMillis, data.size())), index.saved());
		}
		if (nowMillis - lastSendMillis < SEND_INTERVAL_MILLIS) {
			return;
		}
		Optional<ListData> data = list == null ? Optional.empty() : Optional.of(list.data());
		if (!counted && data.equals(sentList) && panel == sentPanel) {
			return;
		}
		if (data.isPresent() && !fits(data.get())) {
			BuildingPlanner.LOGGER.warn("Building Planner's current material list is over {} bytes, so the server "
				+ "doesn't keep it", ListLimits.MAX_LIST_BYTES);
			data = Optional.empty();
		}
		link.send(new Payloads.CurrentList(data, counted && data.isPresent(), panel));
		lastSendMillis = nowMillis;
		sentList = list == null ? Optional.empty() : Optional.of(list.data());
		sentPanel = panel;
		counted = false;
	}

	private void makeCurrent(ListData data) {
		makeCurrent(GhostMaterialList.kept(data));
	}

	/**
	 * Makes the list current, the one the screen, the panel and Litematica's own material list key show, or none. The
	 * panel stays on or off: {@code DataManager.setMaterialList} turns the old list's Info HUD off, which is the panel's
	 * switch, so it's put back after.
	 */
	void makeCurrent(MaterialListBase list) {
		boolean on = panel;
		DataManager.setMaterialList(list);
		panel = on;
	}

	private static GhostMaterialList currentList() {
		return DataManager.getMaterialList() instanceof GhostMaterialList list ? list : null;
	}

	private static boolean fits(ListData list) {
		return Payloads.listBytes(list) <= ListLimits.MAX_LIST_BYTES;
	}
}
