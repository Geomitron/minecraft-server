package dev.buildingplanner.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import nl.requios.effortlessbuilding.buildmode.BuildModeEnum;
import nl.requios.effortlessbuilding.buildmode.BuildModes;
import nl.requios.effortlessbuilding.buildmode.ModeOptions;
import nl.requios.effortlessbuilding.buildpipeline.BuildPipeline;
import nl.requios.effortlessbuilding.buildpipeline.BuildPipelineClient;
import nl.requios.effortlessbuilding.modifier.ArrayModifier;
import nl.requios.effortlessbuilding.modifier.ModifierSystem;
import nl.requios.effortlessbuilding.utilities.BreakDisplayTracker;

/**
 * The driver's commands for Effortless Building, in a class of their own so the driver still loads on a client without
 * that mod.
 */
final class EffortlessDriver {
	private EffortlessDriver() {
	}

	/** Effortless Building's build mode, as its radial menu sets it. */
	static void mode(String mode) {
		BuildModes.CLIENT.setBuildMode(BuildModeEnum.valueOf(mode));
	}

	/** Effortless Building's build mode. */
	static String mode() {
		return BuildModes.CLIENT.getBuildMode().name();
	}

	/** The shape under way (none, PLACING or BREAKING) and how many kinds of real block its break list holds. */
	static String breakList() {
		BuildPipeline.BuildState state = BuildPipelineClient.getBuildState();
		BreakDisplayTracker list = BuildPipelineClient.BREAK_DISPLAY;
		return (state == null ? "none" : state.name()) + " " + (list.breakable.size() + list.rejected.size());
	}

	/** Effortless Building's array modifier alone; a count of 0 clears it. */
	static void array(int count, int dx, int dy, int dz) {
		ModifierSystem.CLIENT.clearModifiers();
		if (count > 0) {
			ArrayModifier array = new ArrayModifier();
			array.count = count;
			array.offsetX = dx;
			array.offsetY = dy;
			array.offsetZ = dz;
			ModifierSystem.CLIENT.addModifier(array);
		}
	}

	/** A box on the screen, in GUI pixels, with what it is. */
	private record Box(String what, double left, double top, double right, double bottom) {
		boolean meets(Box other) {
			return left < other.right && other.left < right && top < other.bottom && other.top < bottom;
		}

		@Override
		public String toString() {
			return what + " " + (int) left + "," + (int) top + ".." + (int) right + "," + (int) bottom;
		}
	}

	/**
	 * What Building Planner's buttons in the open radial menu meet, as "clear" or each meeting: the screen's edges,
	 * each other, the speed sliders, and everything Effortless Building 4.3's {@code RadialMenu} draws, wherever the
	 * pointer is and whichever mode is chosen. Its numbers are that class's: the ring's outer edge 65 from the middle,
	 * the side buttons 20 across at {@code buttonDistance} 105, the left-hand ones in rows 13 above and below the
	 * middle, each mode's options at 105 + 26 j right of the middle in rows -13 + 39 i, with their names 96 right and
	 * -37 + 39 i down, a hovered mode's name 75 out along its slice, as {@code drawTexts} places it, its description 100
	 * below the middle, and the "Effortless Building" mark in the bottom right corner. The menu has reached its full
	 * size, as it does a moment after it opens.
	 */
	static String radialConflicts(Screen screen) {
		Font font = Minecraft.getInstance().font;
		double mx = screen.width / 2.0;
		double my = screen.height / 2.0;
		int ix = (int) mx;
		int iy = (int) my;
		List<Box> ours = new ArrayList<>();
		List<Box> theirs = new ArrayList<>();
		for (GuiEventListener child : screen.children()) {
			if (child instanceof AbstractWidget w && w.visible) {
				(w instanceof RadialButton ? ours : theirs).add(new Box(w.getMessage().getString(), w.getX(), w.getY(),
					w.getX() + w.getWidth(), w.getY() + w.getHeight()));
			}
		}
		for (int[] button : new int[][] {{-157, -13}, {-131, -13}, {-105, -13}, {-157, 13}, {-131, 13}, {-105, 13}}) {
			theirs.add(new Box("Effortless Building's button at " + button[0] + "," + button[1], mx + button[0] - 10,
				my + button[1] - 10, mx + button[0] + 10, my + button[1] + 10));
		}
		double slice = 2 * Math.PI / BuildModeEnum.values().length;
		for (BuildModeEnum mode : BuildModeEnum.values()) {
			ModeOptions.OptionEnum[] options = mode.options;
			for (int i = 0; i < options.length; i++) {
				for (int j = 0; j < options[i].actions.length; j++) {
					double x = mx + 105 + 26 * j;
					double y = my - 13 + 39 * i;
					theirs.add(new Box(mode + "'s option button", x - 10, y - 10, x + 10, y + 10));
				}
				String label = I18n.get(options[i].name);
				int lx = (int) (mx + 105 - 9);
				int ly = iy - 37 + i * 39;
				theirs.add(new Box(mode + "'s option name", lx, ly, lx + font.width(label), ly + font.lineHeight));
			}
			int index = mode.ordinal();
			double begin = index * slice - Math.PI / 2;
			double end = (index + 1) * slice - Math.PI / 2;
			double x = (Math.cos(begin) + Math.cos(end)) * 0.5;
			double y = (Math.sin(begin) + Math.sin(end)) * 0.5;
			String name = I18n.get(mode.getNameKey());
			int nx = (int) (x * 75);
			int ny = (int) (y * 75) - font.lineHeight / 2;
			if (x <= -0.2) {
				nx -= font.width(name);
			} else if (x <= 0.2) {
				nx -= font.width(name) / 2;
			}
			theirs.add(new Box(mode + "'s name", ix + nx, iy + ny, ix + nx + font.width(name), iy + ny + font.lineHeight));
			String description = I18n.get(mode.getDescriptionKey());
			if (!description.isEmpty()) {
				int dx = (int) (mx - font.width(description) / 2f);
				theirs.add(new Box(mode + "'s description", dx, iy + 100, dx + font.width(description),
					iy + 100 + font.lineHeight));
			}
		}
		String credits = "Effortless Building";
		int cx = screen.width - font.width(credits) - 4;
		theirs.add(new Box("the Effortless Building mark", cx, screen.height - 10, cx + font.width(credits),
			screen.height - 10 + font.lineHeight));
		List<String> meetings = new ArrayList<>();
		for (int k = 0; k < ours.size(); k++) {
			Box box = ours.get(k);
			if (box.left() < 0 || box.top() < 0 || box.right() > screen.width || box.bottom() > screen.height) {
				meetings.add(box + " leaves the screen");
			}
			double nearX = Math.clamp(mx, box.left(), box.right());
			double nearY = Math.clamp(my, box.top(), box.bottom());
			if (Math.hypot(nearX - mx, nearY - my) < 65) {
				meetings.add(box + " meets the ring");
			}
			for (Box other : theirs) {
				if (box.meets(other)) {
					meetings.add(box + " meets " + other);
				}
			}
			for (Box other : ours.subList(k + 1, ours.size())) {
				if (box.meets(other)) {
					meetings.add(box + " meets " + other);
				}
			}
		}
		return meetings.isEmpty() ? "clear" : String.join("; ", meetings);
	}
}
