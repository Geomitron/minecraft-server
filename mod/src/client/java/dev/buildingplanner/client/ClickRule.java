package dev.buildingplanner.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import nl.requios.effortlessbuilding.buildmode.BuildModeEnum;
import nl.requios.effortlessbuilding.buildpipeline.BuildPipelineClient;

/**
 * Which clicks Effortless Building takes on a server (hooks E13 to E15): only those made with an item that places a
 * block in the main hand, or that finish a shape under way. Only the hooks into Effortless Building load this class,
 * so it's never loaded without that mod. Unlike {@link EffortlessShapes}, it uses none of Litematica's classes, since
 * the hooks ask it whether or not Building Planner is on.
 */
public final class ClickRule {
	/** Whether the pick under way leaves Effortless Building's reach out (hook E15). Only the client thread uses it. */
	private static boolean vanillaPick;
	/**
	 * Whether Copy, "Copy real blocks" or the material list waits for a box drawn with Effortless Building's "Cube"
	 * shape, which any held item, or none, then draws (hooks E13 to E15).
	 */
	private static boolean boxAwaited;

	private ClickRule() {
	}

	/**
	 * Whether an item places a block, so it draws and erases ghost blocks: Minecraft's block items (blocks, and redstone
	 * dust, fences, doors, signs, seeds, string and the like). Anything else (a tool, food, and items that place an
	 * entity or a fluid: buckets, item frames, armor stands, boats) clicks as without mods (hooks E13 and E14), and the
	 * mix applies only with a placeable item in hand.
	 */
	public static boolean placeable(ItemStack stack) {
		return stack.getItem() instanceof BlockItem;
	}

	/** Whether the player's main hand holds a placeable item. */
	public static boolean holdsPlaceable() {
		Player player = Minecraft.getInstance().player;
		return player != null && placeable(player.getMainHandItem());
	}

	/** Whether a box is awaited, so any held item, or none, draws the cube. */
	public static boolean boxAwaited() {
		return boxAwaited;
	}

	static void setBoxAwaited(boolean awaited) {
		boxAwaited = awaited;
	}

	/** Hooks E13 to E15: whether Effortless Building's clicks are taken now: a placeable item in hand, or a box awaited. */
	public static boolean startsShapes() {
		return boxAwaited || holdsPlaceable();
	}

	/**
	 * Hook E15: runs the game's pick of what the crosshair aims at. With anything but a placeable item in the main hand
	 * and no shape under way, Effortless Building's longer reach, which it gives an empty hand and which aims through
	 * an entity at the block behind it, is left out, so the hand aims as without mods.
	 */
	public static void pick(Runnable pick) {
		vanillaPick = BuildingPlannerClient.ownsShapes() && !startsShapes() && BuildPipelineClient.getBuildState() == null;
		try {
			pick.run();
		} finally {
			vanillaPick = false;
		}
	}

	/** Hook E15: Effortless Building's mode, as Disable while the pick leaves its reach out. */
	public static BuildModeEnum modeForPick(BuildModeEnum mode) {
		return vanillaPick ? BuildModeEnum.DISABLED : mode;
	}
}
