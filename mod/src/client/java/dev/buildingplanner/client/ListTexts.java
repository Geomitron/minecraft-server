package dev.buildingplanner.client;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;

/**
 * The material lists' dates and sizes as the screens show them. The date's pattern is in the language file
 * ({@code buildingplanner.ui.lists.date_time}, a {@link DateTimeFormatter} pattern), so its wording is edited there.
 */
final class ListTexts {
	private ListTexts() {
	}

	/** The date and time, on this computer's clock, as "Nov 19, 6:21 PM". */
	static String dateTime(ZonedDateTime time) {
		return DateTimeFormatter.ofPattern(Component.translatable("buildingplanner.ui.lists.date_time").getString(),
			Locale.US).format(time);
	}

	/** The time in milliseconds since 1970 as {@link #dateTime(ZonedDateTime)} shows it. */
	static String dateTime(long millis) {
		return dateTime(ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()));
	}

	/** A box's size, as "12 × 5 × 8". */
	static Component size(Vec3i size) {
		return Component.translatable("buildingplanner.ui.saved_lists.size", size.getX(), size.getY(), size.getZ());
	}
}
