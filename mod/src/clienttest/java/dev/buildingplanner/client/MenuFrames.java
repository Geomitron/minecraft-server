package dev.buildingplanner.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

/**
 * Shots of Effortless Building's radial menu 1 and 2 frames after it opens, during its opening animation, with where
 * Building Planner's buttons were drawn in each (step9.sh). The game draws a frame at the end of each pass of its loop,
 * so at the start of the next pass its main render target still holds that frame, which is when the shot is taken and
 * the buttons' places are read; a client tick, where the driver's own commands run, can come only every few frames.
 */
public final class MenuFrames {
	private static final String RADIAL_MENU = "nl.requios.effortlessbuilding.screen.RadialMenu";
	private static final int FRAMES = 2;

	/** The shots' name while armed, else null. */
	private static String name;
	/** How many frames in a row, up to the last, have drawn the radial menu. */
	private static int menuFrames;
	private static final List<Frame> frames = new ArrayList<>();

	/** A frame's buttons, by name, with their middles from the screen's middle and their sizes, in GUI pixels. */
	private record Frame(int number, double factor, List<String> names, List<double[]> places, String boxes) {
	}

	private MenuFrames() {
	}

	/** Shoots the radial menu's next opening, 1 and 2 frames after it, as {@code <name>-frame1} and {@code -frame2}. */
	static void arm(String shots) {
		name = shots;
		frames.clear();
	}

	/** At the start of the game loop's pass: the last frame is in the main render target. */
	public static void beforeFrame(Minecraft mc) {
		if (name == null || menuFrames < 1 || menuFrames > FRAMES || frames.size() >= menuFrames) {
			return;
		}
		Screen screen = mc.gui.screen();
		Screenshot.grab(mc.gameDirectory, name + "-frame" + menuFrames + ".png", mc.gameRenderer.mainRenderTarget(), 1,
			c -> {
			});
		List<String> names = new ArrayList<>();
		List<double[]> places = new ArrayList<>();
		double factor = 0;
		for (RadialButton button : buttons(screen)) {
			names.add(button.getMessage().getString());
			places.add(new double[] {Math.hypot(button.middleX() - screen.width / 2.0,
				button.middleY() - screen.height / 2.0), RadialButton.SIZE * button.factor()});
			factor = button.factor();
		}
		frames.add(new Frame(menuFrames, factor, names, places, Driver.buttonBoxes(mc)));
		if (menuFrames == FRAMES) {
			name = null;
		}
	}

	/** At the end of the game loop's pass, which drew the screen open now. */
	public static void afterFrame(Minecraft mc) {
		Screen screen = mc.gui.screen();
		boolean menu = screen != null && screen.getClass().getName().equals(RADIAL_MENU);
		menuFrames = menu ? menuFrames + 1 : 0;
	}

	/**
	 * The frames shot since {@link #arm}, each with the menu's factor then and its buttons' window boxes, and whether
	 * every button in each was nearer the middle and smaller than in the open menu now, which has reached full size:
	 * "nearer and smaller" ends the text when each was.
	 */
	static String report(Minecraft mc) {
		if (frames.size() < FRAMES) {
			return "only " + frames.size() + " frames shot";
		}
		Screen screen = mc.gui.screen();
		List<RadialButton> now = buttons(screen);
		List<String> parts = new ArrayList<>();
		List<String> failures = new ArrayList<>();
		for (Frame frame : frames) {
			parts.add(String.format(Locale.ROOT, "frame %d at %.3f: %s", frame.number(), frame.factor(), frame.boxes()));
			for (int i = 0; i < frame.names().size(); i++) {
				RadialButton open = now.get(i);
				double distance = Math.hypot(open.middleX() - screen.width / 2.0, open.middleY() - screen.height / 2.0);
				double[] then = frame.places().get(i);
				if (then[0] >= distance || then[1] >= RadialButton.SIZE * open.factor()) {
					failures.add(String.format(Locale.ROOT, "%s in frame %d at %.1f, %.1f across, open at %.1f, %.1f",
						frame.names().get(i), frame.number(), then[0], then[1], distance, RadialButton.SIZE * open.factor()));
				}
			}
		}
		return String.join(" ; ", parts) + " ; " + (failures.isEmpty() ? "nearer and smaller"
			: "not nearer and smaller: " + String.join(", ", failures));
	}

	private static List<RadialButton> buttons(Screen screen) {
		List<RadialButton> buttons = new ArrayList<>();
		for (GuiEventListener child : screen.children()) {
			if (child instanceof RadialButton button && button.visible) {
				buttons.add(button);
			}
		}
		return buttons;
	}
}
