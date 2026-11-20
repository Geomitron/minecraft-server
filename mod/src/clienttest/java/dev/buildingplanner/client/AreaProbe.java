package dev.buildingplanner.client;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.AreaSelectionSimple;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import net.minecraft.core.BlockPos;

/**
 * The driver's commands for Litematica's simple area selection and for telling whether it's drawn (step9.sh), in a
 * class of their own so the driver's other commands still load without Litematica.
 */
final class AreaProbe {
	private AreaProbe() {
	}

	/**
	 * Litematica's simple area selection becomes the one box between the corners, with the origin of its own when one
	 * is given, saved as Litematica saves it.
	 */
	static String selectBox(BlockPos first, BlockPos second, BlockPos origin) {
		AreaSelectionSimple area = DataManager.getSimpleArea();
		if (area.getAllSubRegionBoxes().isEmpty()) {
			area.createNewSubRegionBox(first, "Area");
		}
		area.setSelectedSubRegionCornerPos(first, Corner.CORNER_1);
		area.setSelectedSubRegionCornerPos(second, Corner.CORNER_2);
		if (origin != null) {
			area.setExplicitOrigin(origin);
		}
		DataManager.save(true);
		return selection();
	}

	/**
	 * Litematica's simple area selection becomes a box far from everything, with its origin far away too, saved, so no
	 * view has either in sight.
	 */
	static String moveAway() {
		return selectBox(new BlockPos(0, -64, -1000), new BlockPos(0, -64, -1000), new BlockPos(0, -64, -1000));
	}

	/**
	 * Litematica's selection mode, then the boxes of its simple area selection as {@code corner..corner}, or none, then
	 * {@code origin x,y,z} when the selection has an origin of its own.
	 */
	static String selection() {
		List<String> boxes = new ArrayList<>();
		for (Box box : DataManager.getSimpleArea().getAllSubRegionBoxes()) {
			boxes.add(corner(box.getPos1()) + ".." + corner(box.getPos2()));
		}
		BlockPos origin = DataManager.getSimpleArea().getExplicitOrigin();
		return DataManager.getSelectionManager().getSelectionMode() + " "
			+ (boxes.isEmpty() ? "none" : String.join(" ", boxes)) + (origin == null ? "" : " origin " + corner(origin));
	}

	private static String corner(BlockPos pos) {
		return pos.toShortString().replace(" ", "");
	}

	/**
	 * How many pixels of the upper part of two screenshots (in the folder, without {@code .png}) differ by more than 60
	 * across their three colour channels together; screenshots of a view with and without something drawn differ by
	 * many, and two of the same view by few.
	 */
	static String shotDifference(Path folder, String first, String second) throws IOException {
		BufferedImage a = ImageIO.read(folder.resolve(first + ".png").toFile());
		BufferedImage b = ImageIO.read(folder.resolve(second + ".png").toFile());
		if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
			return "sizes differ";
		}
		int differing = 0;
		// Only the view: the chat, the hotbar and Litematica's lines of text at the bottom change with the game's own
		// messages.
		for (int y = 0; y < a.getHeight() * 6 / 10; y++) {
			for (int x = 0; x < a.getWidth(); x++) {
				int p = a.getRGB(x, y);
				int q = b.getRGB(x, y);
				int distance = 0;
				for (int shift = 0; shift < 24; shift += 8) {
					distance += Math.abs((p >> shift & 0xFF) - (q >> shift & 0xFF));
				}
				if (distance > 60) {
					differing++;
				}
			}
		}
		return String.valueOf(differing);
	}
}
