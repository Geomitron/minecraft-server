package dev.buildingplanner.server;

import dev.buildingplanner.sync.ListData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One player's material lists as the server keeps them: the saved lists by name, "Last counted", the current list and
 * the panel's switch.
 */
public final class PlayerLists {
	private final Map<String, ListData> saved = new LinkedHashMap<>();
	private Optional<ListData> lastCounted = Optional.empty();
	private Optional<ListData> current = Optional.empty();
	private boolean panel;

	/** The saved lists, newest first. */
	public List<ListData> saved() {
		List<ListData> lists = new ArrayList<>(saved.values());
		// Later saves are later in the map, so for equal times the later save comes first.
		Collections.reverse(lists);
		lists.sort(Comparator.comparingLong((ListData list) -> list.time().orElse(0)).reversed());
		return lists;
	}

	public Optional<ListData> find(String name) {
		return Optional.ofNullable(saved.get(name));
	}

	/** Keeps a named list, replacing a kept list of that name. */
	public void save(ListData list) {
		String name = list.name().orElseThrow();
		saved.remove(name);
		saved.put(name, list);
	}

	/** Deletes the kept list of that name; returns whether there was one. */
	public boolean delete(String name) {
		return saved.remove(name) != null;
	}

	public Optional<ListData> lastCounted() {
		return lastCounted;
	}

	public void setLastCounted(Optional<ListData> lastCounted) {
		this.lastCounted = lastCounted;
	}

	public Optional<ListData> current() {
		return current;
	}

	public void setCurrent(Optional<ListData> current) {
		this.current = current;
	}

	public boolean panel() {
		return panel;
	}

	public void setPanel(boolean panel) {
		this.panel = panel;
	}
}
