package dev.buildingplanner.plan;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import sun.misc.Unsafe;

/**
 * Lets Litematica's own schematic writer and reader run in a unit test, without a game window.
 *
 * <p>Litematica reads block states through the registries of the client's world, and its config defaults read the
 * game directory from {@code Minecraft.getInstance()}. This bootstraps the vanilla registries, hands them to
 * Litematica, and installs a bare, never-started {@link Minecraft} whose only set field is the game directory.
 */
public final class LitematicaOutsideTheGame {
	private static boolean ready;

	private LitematicaOutsideTheGame() {
	}

	public static synchronized void setUp() throws Exception {
		if (ready) {
			return;
		}
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();

		Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
		unsafeField.setAccessible(true);
		Minecraft minecraft = (Minecraft) ((Unsafe) unsafeField.get(null)).allocateInstance(Minecraft.class);
		Field gameDirectory = Minecraft.class.getDeclaredField("gameDirectory");
		gameDirectory.setAccessible(true);
		// Gradle runs tests from the project directory, so this stays in the ignored build output.
		gameDirectory.set(minecraft, Files.createDirectories(Path.of("build", "tmp", "litematica-game")).toFile());
		Field instance = Minecraft.class.getDeclaredField("instance");
		instance.setAccessible(true);
		instance.set(null, minecraft);

		SchematicWorldHandler.INSTANCE.setDynamicRegistryManager(
			RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
		ready = true;
	}
}
