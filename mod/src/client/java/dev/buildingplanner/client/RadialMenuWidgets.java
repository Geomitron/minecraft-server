package dev.buildingplanner.client;

import dev.buildingplanner.input.MouseMode;
import dev.buildingplanner.input.RepeatSpeed;
import dev.buildingplanner.look.SmallText;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Building Planner's widgets in Effortless Building's radial menu (hooks E11, E12, E16, E19 and E20), placed from the
 * menu's middle as its own buttons are. The menu's left-hand buttons (the wrench, undo and redo, and the row under
 * them) are 20 across with their middles 26 apart, the leftmost 157 left of the menu's middle, in two rows 13 above
 * and 13 below it, so they span 167 to 95 left of the middle and end 23 below it. The two speed sliders sit just under
 * them, 6 below the lower row as that row is below the upper, and end above a mode's description, which the menu draws
 * 100 below the middle. Their labels draw on one line at {@link SmallText}'s size, full size at GUI scales 1 to 3,
 * where the longest label, "Break speed: 3.33 blocks/s", is 137 across: more than the 102 between the buttons' left
 * edge and the menu's ring (65 from the middle at most). So the sliders end 65 left of the middle, outside the ring,
 * and are as wide as the longest label either shows needs at the current size, plus {@link #PAD} on each side, reaching
 * further left than the buttons. Each is {@link #HEIGHT} high, and they end 55 below the middle.
 *
 * <p>Building Planner's six buttons ({@link RadialButton}) sit in two rows above the wrench, undo and redo, in their
 * columns (middles 157, 131 and 105 left of the menu's middle): "Show or hide ghost blocks", "Block mix" and "Material
 * list" 39 above the middle, and Copy, Paste and "Material lists" 65 above it. "Show or hide ghost blocks" and "Block
 * mix" do what their keys do; Copy, Paste, "Material list" and "Material lists" have no keys, and Copy, Paste and
 * "Material list" each show selected while its mouse mode is on, when no mode segment does (E19). Each icon is a
 * texture of its own ({@link RadialButton#icon}), "Block mix" shows Effortless Building's Randomizer Bag, and
 * "Material lists" Minecraft's Book and Quill.
 *
 * <p>The widgets join the menu's opening animation (E20): the menu places its ring and buttons at {@link #factor} of
 * their distance and size from its middle, which grows from 0.8 to 1 over its first frames, and {@link #place} places
 * Building Planner's widgets by the same factor each frame. The sliders move with it and keep their size, as the
 * menu's texts do.
 */
public final class RadialMenuWidgets {
	/** Where the sliders end, from the middle: just outside the menu's ring. */
	private static final int RIGHT = -65;
	private static final int SLIDERS_TOP = 29;
	private static final int PAD = 4;
	private static final int HEIGHT = 12;
	private static final int SLIDER_GAP = 2;
	private static final String BUILD = "buildingplanner.ui.radial.build_speed";
	private static final String BREAK = "buildingplanner.ui.radial.break_speed";
	/** The buttons' middles from the menu's middle, {x, y}. */
	private static final int[] SHOW_HIDE_SLOT = {-157, -39};
	private static final int[] BLOCK_MIX_SLOT = {-131, -39};
	private static final int[] MATERIAL_LIST_SLOT = {-105, -39};
	private static final int[] COPY_SLOT = {-157, -65};
	private static final int[] PASTE_SLOT = {-131, -65};
	private static final int[] MATERIAL_LISTS_SLOT = {-105, -65};

	private static final Identifier SHOWN = RadialButton.icon("ghost_blocks_shown");
	private static final Identifier HIDDEN = RadialButton.icon("ghost_blocks_hidden");
	private static final Identifier COPY = RadialButton.icon("copy");
	private static final Identifier PASTE = RadialButton.icon("paste");
	private static final Identifier MATERIAL_LIST = RadialButton.icon("material_list");
	/** Effortless Building's Randomizer Bag item texture, drawn from its jar. */
	private static final Identifier RANDOMIZER_BAG =
		Identifier.fromNamespaceAndPath("effortlessbuilding", "textures/item/randomizerbag.png");
	/** Minecraft's Book and Quill item texture, drawn from the game's own. */
	private static final Identifier BOOK_AND_QUILL =
		Identifier.fromNamespaceAndPath("minecraft", "textures/item/writable_book.png");

	/** A widget the menu places from its middle each frame. */
	interface Placed {
		/** Places the widget for a menu whose middle is at {@code menuX}, {@code menuY}, at {@link #factor}. */
		void place(double menuX, double menuY, double factor);
	}

	private RadialMenuWidgets() {
	}

	/** The widgets for a radial menu, made afresh each time it opens; {@link #place} places them. */
	public static List<AbstractWidget> create() {
		int sliderWidth = widestLabel() + 2 * PAD;
		List<AbstractWidget> widgets = new ArrayList<>(List.of(
			new RepeatSlider(RIGHT, SLIDERS_TOP, sliderWidth, HEIGHT, RepeatSpeed.PLACING, BUILD,
				BuildingPlannerClient.config()),
			new RepeatSlider(RIGHT, SLIDERS_TOP + HEIGHT + SLIDER_GAP, sliderWidth, HEIGHT, RepeatSpeed.ERASING, BREAK,
				BuildingPlannerClient.config())));
		widgets.add(button(SHOW_HIDE_SLOT, BuildingPlannerClient.SHOW_HIDE,
			() -> BuildingPlannerClient.ghostsShown() ? SHOWN : HIDDEN, () -> false, BuildingPlannerClient::toggleGhosts));
		widgets.add(button(BLOCK_MIX_SLOT, BuildingPlannerClient.BLOCK_MIX, () -> RANDOMIZER_BAG, () -> false,
			BuildingPlannerClient::blockMix));
		widgets.add(button(MATERIAL_LIST_SLOT, "material_list", MATERIAL_LIST,
			() -> BuildingPlannerClient.drawingBoxFor(BoxDraw.Purpose.MATERIAL_LIST), BuildingPlannerClient::materialList));
		widgets.add(button(COPY_SLOT, "copy", COPY, () -> BuildingPlannerClient.drawingBoxFor(BoxDraw.Purpose.COPY),
			BuildingPlannerClient::copy));
		widgets.add(button(PASTE_SLOT, "paste", PASTE,
			() -> BuildingPlannerClient.mouseMode() == MouseMode.COPY_AT_AIM, BuildingPlannerClient::paste));
		widgets.add(button(MATERIAL_LISTS_SLOT, "material_lists", BOOK_AND_QUILL, () -> false,
			BuildingPlannerClient::materialLists));
		return widgets;
	}

	/**
	 * The menu's size for its opening animation (E20), from its {@code visibility}, which grows from 0 to 1 over its
	 * first frames: it places its ring and buttons at this share of their distance and size from its middle.
	 */
	public static double factor(float visibility) {
		return 0.8 + 0.2 * visibility;
	}

	/** Hook E12 (and E11, before the first frame): places the menu's widgets for this frame of its opening. */
	public static void place(List<? extends GuiEventListener> children, int width, int height, float visibility) {
		for (GuiEventListener child : children) {
			if (child instanceof Placed placed) {
				placed.place(width / 2.0, height / 2.0, factor(visibility));
			}
		}
	}

	/**
	 * Hook E19: the index of the mode segment the menu draws selected, given the mode's: none (-1) while a box is drawn
	 * or the copy is at the aim, since then Building Planner's button for that mode shows selected instead.
	 */
	public static int selectedSegment(int modeIndex) {
		return BuildingPlannerClient.mouseModeOn() ? -1 : modeIndex;
	}

	/**
	 * Hook E16: the radial menu is closing, by its key let go or by Escape. As the menu then acts on its own button
	 * under the pointer unless a click acted since it opened ({@code actedByClick}), so does the Building Planner button
	 * under the pointer now, which may have moved since the last frame. Returns that button's action, or null, for the
	 * hook to run once the menu has closed, so "Block mix" and "Material lists" can open their screens: closing grabs
	 * the mouse, which moves the pointer to the window's middle.
	 */
	public static Runnable menuClosing(List<? extends GuiEventListener> children, boolean actedByClick) {
		if (actedByClick) {
			return null;
		}
		Minecraft mc = Minecraft.getInstance();
		double x = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
		double y = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
		for (GuiEventListener child : children) {
			if (child instanceof RadialButton button && button.isMouseOver(x, y)) {
				return button::act;
			}
		}
		return null;
	}

	/** A button named and worked by its key: its hover text shows the key as the player has bound it. */
	private static RadialButton button(int[] slot, KeyMapping key, Supplier<Identifier> icon, BooleanSupplier selected,
		Runnable action) {
		Supplier<String> keyName = () -> key.isUnbound() ? null : key.getTranslatedKeyMessage().getString();
		return new RadialButton(slot[0], slot[1], Component.translatable(key.getName()), keyName, icon, selected, action);
	}

	/** A button without a key, named by {@code buildingplanner.ui.radial.<name>}. */
	private static RadialButton button(int[] slot, String name, Identifier icon, BooleanSupplier selected,
		Runnable action) {
		return new RadialButton(slot[0], slot[1], Component.translatable("buildingplanner.ui.radial." + name),
			() -> null, () -> icon, selected, action);
	}

	/** The width the longest label either slider shows takes at the current size, in GUI pixels. */
	private static int widestLabel() {
		Font font = Minecraft.getInstance().font;
		int widest = 0;
		for (int ticks = RepeatSpeed.FASTEST; ticks <= RepeatSpeed.SLOWEST; ticks++) {
			for (String key : List.of(BUILD, BREAK)) {
				widest = Math.max(widest,
					font.width(Component.translatable(key, RepeatSpeed.blocksPerSecond(ticks))));
			}
		}
		return (int) Math.ceil(widest * SmallText.scale(Minecraft.getInstance().getWindow().getGuiScale()));
	}
}
