package dev.buildingplanner.look;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.Unpooled;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Effortless Building's welcome message and build mode hint, which never reach chat, and the messages that still do. */
class TutorialMessagesTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	/** A message as Effortless Building 4.3's server part makes it: the key, with its radial menu's key in blue. */
	private static Component effortless(String key) {
		return Component.translatable(key,
			Component.keybind("key.effortlessbuilding.open_radial_menu").withStyle(ChatFormatting.BLUE));
	}

	/** The message as a game reads it from a server: sent in a system chat packet and read back from its bytes. */
	private static Component fromServer(Component message) {
		RegistryFriendlyByteBuf bytes = new RegistryFriendlyByteBuf(Unpooled.buffer(),
			RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
		ClientboundSystemChatPacket.STREAM_CODEC.encode(bytes, new ClientboundSystemChatPacket(message, false));
		Component read = ClientboundSystemChatPacket.STREAM_CODEC.decode(bytes).content();
		assertEquals(0, bytes.readableBytes());
		return read;
	}

	@Test
	void theWelcomeMessageIsDropped() {
		assertTrue(TutorialMessages.dropped(effortless("effortlessbuilding.message.welcome_message")));
	}

	@Test
	void theBuildModeHintIsDropped() {
		assertTrue(TutorialMessages.dropped(effortless("effortlessbuilding.message.build_mode_hint")));
	}

	@Test
	void bothAreDroppedAsTheyArriveFromAServer() {
		Component welcome = fromServer(effortless("effortlessbuilding.message.welcome_message"));
		assertEquals(effortless("effortlessbuilding.message.welcome_message"), welcome);
		assertTrue(TutorialMessages.dropped(welcome));
		assertTrue(TutorialMessages.dropped(fromServer(effortless("effortlessbuilding.message.build_mode_hint"))));
	}

	@Test
	void buildingPlannersOwnLineAboutGhostBlocksShownAgainStays() {
		Component shownAgain = Component.translatable("buildingplanner.ghosts.shown_again",
			Component.keybind("key.buildingplanner.show_hide"));
		assertFalse(TutorialMessages.dropped(shownAgain));
		assertFalse(TutorialMessages.dropped(fromServer(shownAgain)));
	}

	@Test
	void effortlessBuildingsOtherMessagesStay() {
		assertFalse(TutorialMessages.dropped(Component.translatable("effortlessbuilding.message.not_operator")));
		assertFalse(TutorialMessages.dropped(Component.translatable("effortlessbuilding.message.undo")));
	}

	@Test
	void aTextWithTheWelcomeMessagesWordsStays() {
		assertFalse(TutorialMessages.dropped(Component.literal(
			"Thank you for using Effortless Building! To get started, hold Left Alt to open the build menu.")));
	}
}
