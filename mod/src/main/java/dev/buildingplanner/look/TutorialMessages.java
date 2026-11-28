package dev.buildingplanner.look;

import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Effortless Building's two tutorial messages, which Building Planner keeps out of chat wherever they come from (Geo,
 * 1479 Q1 C and Q2 C): its welcome message, at a player's first join of a world, and its build mode hint, at their first
 * choice of a mode. Its server part sends each as a translation of its key, which reaches the game from any server.
 */
public final class TutorialMessages {
	/** The two messages' keys in Effortless Building 4.3's language file. */
	static final Set<String> KEYS = Set.of("effortlessbuilding.message.welcome_message",
		"effortlessbuilding.message.build_mode_hint");

	private TutorialMessages() {
	}

	/** Whether the game message is one of the two: a translation of either key. */
	public static boolean dropped(Component message) {
		return message.getContents() instanceof TranslatableContents translation && KEYS.contains(translation.getKey());
	}
}
