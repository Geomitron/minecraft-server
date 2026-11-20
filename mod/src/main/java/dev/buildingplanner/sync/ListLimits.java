package dev.buildingplanner.sync;

import dev.buildingplanner.input.BoxCorners;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * The limits the server keeps material lists to, which the game keeps to itself, so a player never meets them. Each
 * check returns what's wrong, for the server's warning, or empty when the name or list keeps to them.
 */
public final class ListLimits {
	/** A name is 1 to this many characters. */
	public static final int MAX_NAME_LENGTH = 64;
	/** A list is at most this many bytes as sent. */
	public static final int MAX_LIST_BYTES = 64 * 1024;
	/** The server accepts at most this many of a player's list messages in any {@link #MESSAGE_WINDOW_MILLIS}. */
	public static final int MAX_MESSAGES = 20;
	public static final long MESSAGE_WINDOW_MILLIS = 10_000;

	private ListLimits() {
	}

	/** A name is 1 to {@value #MAX_NAME_LENGTH} characters, with no line breaks or other control characters. */
	public static Optional<String> nameProblem(String name) {
		if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
			return Optional.of("a name of " + name.length() + " characters");
		}
		if (name.codePoints().anyMatch(ListLimits::isControl)) {
			return Optional.of("a name with a control character");
		}
		return Optional.empty();
	}

	/**
	 * A list's name, when it has one, keeps to {@link #nameProblem}; it has at most one row for each item; in each row,
	 * no count is below 0, "Missing" is at most "Total" and the wrong blocks are at most "Missing"; the box is at most
	 * {@value BoxCorners#MAX_SIDE} blocks a side, in a dimension {@code hasDimension} accepts; and the multiplier is at
	 * least 1.
	 */
	public static Optional<String> listProblem(ListData list, Predicate<Identifier> hasDimension) {
		if (list.name().isPresent()) {
			Optional<String> name = nameProblem(list.name().get());
			if (name.isPresent()) {
				return name;
			}
		}
		Vec3i size = list.size();
		for (int side : new int[] {size.getX(), size.getY(), size.getZ()}) {
			if (side < 1 || side > BoxCorners.MAX_SIDE) {
				return Optional.of("a box of " + size.getX() + " × " + size.getY() + " × " + size.getZ());
			}
		}
		if (!hasDimension.test(list.dimension())) {
			return Optional.of("an unknown dimension " + list.dimension());
		}
		if (list.multiplier() < 1) {
			return Optional.of("a multiplier of " + list.multiplier());
		}
		Set<Item> items = new HashSet<>();
		for (ListData.Row row : list.rows()) {
			if (!items.add(row.item())) {
				return Optional.of("a second row for " + row.item());
			}
			if (row.wrong() < 0 || row.missing() < row.wrong() || row.total() < row.missing()) {
				return Optional.of("a row for " + row.item() + " of total " + row.total() + ", missing " + row.missing()
					+ " and wrong " + row.wrong());
			}
		}
		return Optional.empty();
	}

	private static boolean isControl(int codePoint) {
		int type = Character.getType(codePoint);
		return type == Character.CONTROL || type == Character.LINE_SEPARATOR || type == Character.PARAGRAPH_SEPARATOR;
	}
}
