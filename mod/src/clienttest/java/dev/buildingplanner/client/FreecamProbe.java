package dev.buildingplanner.client;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Freecam for the in-game checks, read and worked by reflection, as Freecam is on the dev clients' classpath only at
 * run time: its camera switched on and off as its key does, moved and turned, and its "Allow interaction" option read.
 */
final class FreecamProbe {
	private FreecamProbe() {
	}

	private static Class<?> freecam() throws ClassNotFoundException {
		return Class.forName("net.xolt.freecam.Freecam");
	}

	/** Switches Freecam's camera on or off, as its key does, unless it already is. */
	static String set(boolean on) throws Exception {
		if ((boolean) freecam().getMethod("isEnabled").invoke(null) != on) {
			freecam().getMethod("toggle").invoke(null);
		}
		return String.valueOf(freecam().getMethod("isEnabled").invoke(null));
	}

	/** Switches Freecam's "Player control" on or off, as its key does, unless it already is. */
	static String setPlayerControl(boolean on) throws Exception {
		if ((boolean) freecam().getMethod("isPlayerControlEnabled").invoke(null) != on) {
			freecam().getMethod("switchControls").invoke(null);
		}
		return String.valueOf(freecam().getMethod("isPlayerControlEnabled").invoke(null));
	}

	/** Whether Freecam's "Allow interaction" is on (its config's {@code shouldPreventInteractions} false). */
	static String allowInteraction() throws Exception {
		Class<?> modConfig = Class.forName("net.xolt.freecam.config.ModConfig");
		Object config = modConfig.getMethod("get").invoke(null);
		return String.valueOf(!(boolean) modConfig.getMethod("shouldPreventInteractions").invoke(config));
	}

	/** The game's camera entity: its class's simple name, its eye position to a tenth, and the way it faces. */
	static String camera() {
		Entity camera = Minecraft.getInstance().getCameraEntity();
		if (camera == null) {
			return "none";
		}
		Vec3 eye = camera.getEyePosition();
		return String.format(Locale.ROOT, "%s %.1f,%.1f,%.1f %s", camera.getClass().getSimpleName(), eye.x, eye.y, eye.z,
			camera.getDirection().getName());
	}

	/** Puts the camera entity's eyes at the point, facing the way given in degrees, as Freecam's flight would. */
	static String moveCamera(double x, double y, double z, float yRot, float xRot) {
		Entity camera = Minecraft.getInstance().getCameraEntity();
		camera.snapTo(x, y - camera.getEyeHeight(), z, yRot, xRot);
		camera.setOldPosAndRot();
		return camera();
	}

	/** Turns the camera entity to look at the point, as a mouse move would. */
	static String lookAt(double x, double y, double z) {
		Entity camera = Minecraft.getInstance().getCameraEntity();
		camera.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(x, y, z));
		camera.setOldPosAndRot();
		return camera();
	}
}
