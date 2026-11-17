package dev.buildingplanner.client;

import dev.buildingplanner.input.HoldRepeat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import nl.requios.effortlessbuilding.buildmode.BuildModeEnum;
import nl.requios.effortlessbuilding.buildmode.BuildModes;
import nl.requios.effortlessbuilding.buildpipeline.BuildPipeline;
import nl.requios.effortlessbuilding.buildpipeline.BuildPipelineClient;
import nl.requios.effortlessbuilding.item.RandomizerToolItem;

/**
 * Hold to repeat, in Effortless Building's Single mode: its client tick handles a click only on the tick a button goes
 * down, so while the button stays down this calls the same {@code BuildPipelineClient.handleRightClick} and
 * {@code handleLeftClick} again at the pace the game repeats a held button (hook E10). The checks before each call
 * are Effortless Building's own, from its client tick, so a repeat does what a fresh click would. Other modes and the
 * Disable mode have no repeat. Only called while Building Planner has a session and Effortless Building is loaded, so
 * this class is never loaded without that mod.
 */
final class HeldClicks {
	private final HoldRepeat use = new HoldRepeat(HoldRepeat.USE_PERIOD);
	private final HoldRepeat attack = new HoldRepeat(HoldRepeat.ATTACK_PERIOD);

	/** Once every client tick. */
	void tick(Minecraft client) {
		boolean active = client.player != null && client.level != null && client.gui.screen() == null
			&& BuildModes.CLIENT.getBuildMode() == BuildModeEnum.SINGLE && BuildPipelineClient.shouldInterceptPlacing();
		if (use.tick(active && client.options.keyUse.isDown()) && canPlace(client)) {
			BuildPipelineClient.handleRightClick(client);
		}
		if (attack.tick(active && client.options.keyAttack.isDown()) && canBreak(client)) {
			BuildPipelineClient.handleLeftClick(client);
		}
	}

	/** Effortless Building's conditions for a place click. */
	private static boolean canPlace(Minecraft client) {
		ItemStack held = client.player.getMainHandItem();
		if (client.player.isShiftKeyDown() && held.getItem() instanceof RandomizerToolItem) {
			return false;
		}
		return BuildPipelineClient.getBuildState() != BuildPipeline.BuildState.BREAKING
			&& (BuildPipeline.isBuildTriggerItem(held) || BuildPipelineClient.getBuildState() == BuildPipeline.BuildState.PLACING);
	}

	/** Effortless Building's conditions for a break click. */
	private static boolean canBreak(Minecraft client) {
		ItemStack held = client.player.getMainHandItem();
		return BuildPipelineClient.shouldInterceptBreaking()
			&& BuildPipelineClient.getBuildState() != BuildPipeline.BuildState.PLACING
			&& (held.isEmpty() || BuildPipeline.isBuildTriggerItem(held) || BuildPipelineClient.getBuildState() != null);
	}
}
