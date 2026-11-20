package dev.buildingplanner.sync;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * A material list as the game and the server keep it: its box (a dimension and its lowest and highest corners), the
 * screen's multiplier, sort, reverse and "Hide available", and its rows, with its name and time when it has them.
 * "Available" isn't kept: the game counts it from the player's inventory whenever the list shows.
 */
public record ListData(Optional<String> name, OptionalLong time, Identifier dimension, BlockPos min, BlockPos max,
	int multiplier, ListSort sort, boolean reverse, boolean hideAvailable, List<Row> rows) {
	/** One item's counts: its total, how many are still missing, and how many of those have the wrong block. */
	public record Row(Item item, int total, int missing, int wrong) {
	}

	public ListData {
		min = min.immutable();
		max = max.immutable();
		rows = List.copyOf(rows);
	}

	/** The box's size, x by y by z. */
	public Vec3i size() {
		return new Vec3i(max.getX() - min.getX() + 1, max.getY() - min.getY() + 1, max.getZ() - min.getZ() + 1);
	}

	/** The same list under a name, saved at {@code time}. */
	public ListData named(String name, long time) {
		return new ListData(Optional.of(name), OptionalLong.of(time), dimension, min, max, multiplier, sort, reverse,
			hideAvailable, rows);
	}

	/** The same list with no name, counted at {@code time}, as "Last counted" keeps it. */
	public ListData counted(long time) {
		return new ListData(Optional.empty(), OptionalLong.of(time), dimension, min, max, multiplier, sort, reverse,
			hideAvailable, rows);
	}
}
