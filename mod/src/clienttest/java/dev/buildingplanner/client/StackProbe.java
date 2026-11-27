package dev.buildingplanner.client;

import com.mojang.blaze3d.platform.Window;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * The item icons Effortless Building drew beside the crosshair in the last whole frame (its
 * {@code RenderHandler.drawItemStack}), for the driver's {@code stacks}.
 */
public final class StackProbe {
	private static List<String> drawing = new ArrayList<>();
	private static List<String> drawn = List.of();

	private StackProbe() {
	}

	/** A frame's icons begin: what the one before drew is the last whole frame's. */
	public static void beforeFrame() {
		drawn = drawing;
		drawing = new ArrayList<>();
	}

	/** The frame draws this icon, in red where {@code missing}. */
	public static void drawing(ItemStack stack, boolean missing) {
		drawing.add(BuiltInRegistries.ITEM.getKey(stack.getItem()) + " " + stack.getCount() + (missing ? " red" : ""));
	}

	/**
	 * The box the last whole frame's icons covered in the window's pixels (x y width height), as Effortless Building lays
	 * them out: from 10 right of the screen's middle and 8 above it, 20 apart, each 16 high with its count under it; or
	 * none.
	 */
	static String box() {
		if (drawn.isEmpty()) {
			return "none";
		}
		Window window = Minecraft.getInstance().getWindow();
		int scale = window.getGuiScale();
		int x = window.getGuiScaledWidth() / 2 + 10;
		int y = window.getGuiScaledHeight() / 2 - 8;
		return x * scale + " " + y * scale + " " + 20 * drawn.size() * scale + " " + 26 * scale;
	}

	/** The last whole frame's icons in the order drawn, each as its item, count and "red" where missing, or none. */
	static String drawn() {
		return drawn.isEmpty() ? "none" : String.join(", ", drawn);
	}
}
