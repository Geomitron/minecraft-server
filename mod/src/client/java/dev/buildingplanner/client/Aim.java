package dev.buildingplanner.client;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.input.AimSource;
import dev.buildingplanner.look.MenuKey;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Where the player aims from when editing ghost blocks (see "Editing from Freecam"): the eye position and look of the
 * game's camera entity, which is Freecam's camera while that is on and the player otherwise, or the player's while
 * Freecam's "Player control" is on ({@link AimSource}). Shapes (hooks E24 to E26), their block states (E1), boxes, the
 * copy's aim and its mirror read it. Freecam is read by reflection, as Building Planner never compiles against it.
 */
public final class Aim {
	/** Freecam's {@code Freecam.isPlayerControlEnabled()}, or null without Freecam. */
	private static final MethodHandle PLAYER_CONTROL = playerControlHandle();
	private static boolean warned;

	private Aim() {
	}

	/** The entity the player aims from, or null with no player. */
	public static Entity entity() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return null;
		}
		return AimSource.of(mc.getCameraEntity(), mc.player, playerControl());
	}

	/**
	 * The player a block placed from the aim is placed by, for its facing: the entity aimed from, which Freecam's camera
	 * is too, or the player when the camera is another entity (a spectated mob).
	 */
	public static Player placer() {
		return entity() instanceof Player p ? p : Minecraft.getInstance().player;
	}

	/**
	 * Hooks E24 to E26: the entity Effortless Building should aim from in place of {@code player}: the aim's, while
	 * Building Planner has a session and it asks for the player at this computer; {@code player} otherwise.
	 */
	public static Entity forEffortless(Player player) {
		if (!BuildingPlannerClient.active() || player != Minecraft.getInstance().player) {
			return player;
		}
		Entity aim = entity();
		return aim == null ? player : aim;
	}

	/** Hook E24: the player Effortless Building's preview places its first block by, for its facing. */
	public static Player placerForEffortless(Player player) {
		return forEffortless(player) instanceof Player p ? p : player;
	}

	private static boolean playerControl() {
		if (PLAYER_CONTROL == null) {
			return false;
		}
		try {
			return (boolean) PLAYER_CONTROL.invokeExact();
		} catch (Throwable t) {
			if (!warned) {
				warned = true;
				BuildingPlanner.LOGGER.warn("Couldn't read Freecam's Player control, so the aim follows the camera", t);
			}
			return false;
		}
	}

	private static MethodHandle playerControlHandle() {
		if (!FabricLoader.getInstance().isModLoaded(MenuKey.FREECAM_MOD)) {
			return null;
		}
		try {
			return MethodHandles.publicLookup().findStatic(Class.forName("net.xolt.freecam.Freecam"),
				"isPlayerControlEnabled", MethodType.methodType(boolean.class));
		} catch (ReflectiveOperationException | LinkageError e) {
			BuildingPlanner.LOGGER.warn("Freecam's Player control wasn't found, so the aim follows the camera", e);
			return null;
		}
	}
}
