package dev.buildingplanner.client;

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
 * Building Planner's widgets in Effortless Building's radial menu (hooks E11, E12 and E16), placed from the menu's
 * middle as its own buttons are. The menu's left-hand buttons (the wrench, undo and redo, and the row under them) are 20
 * across with their middles 26 apart, the leftmost 157 left of the menu's middle, in two rows 13 above and 13 below
 * it, so they span 167 to 95 left of the middle and end 23 below it. The two speed sliders sit just under them, 6 below
 * the lower row as that row is below the upper, and end above a mode's description, which the menu draws 100 below
 * the middle. Their labels draw on one line at {@link SmallText}'s size, full size at GUI scales 1 to 3, where the
 * longest label, "Break speed: 3.33 blocks/s", is 137 across: more than the 102 between the buttons' left edge and the
 * menu's ring (65 from the middle at most). So the sliders end 65 left of the middle, outside the ring, and are as wide
 * as the longest label either shows needs at the current size, plus {@link #PAD} on each side, reaching further left
 * than the buttons. Each is {@link #HEIGHT} high, and they end 55 below the middle.
 *
 * <p>Building Planner's buttons ({@link RadialButton}) sit above the wrench, undo and redo, in the same columns (middles
 * 157, 131 and 105 left of the menu's middle), in rows 39 and 65 above the middle: "Show or hide ghost blocks", Copy and
 * Paste, then "Material list" in the left column; and in the row 91 above the middle, Turn, Mirror and Clear, which
 * show only while a copy exists, or while the development system property {@value #ALL_BUTTONS_PROPERTY} is
 * {@code true}, so the in-game checks can place all seven. Each does what its key does; "Material list" has no key.
 * Each icon is a texture of its own ({@link RadialButton#icon}).
 */
public final class RadialMenuWidgets {
	static final String ALL_BUTTONS_PROPERTY = "buildingplanner.allButtons";
	/** Where the sliders end, from the middle: just outside the menu's ring. */
	private static final int RIGHT = -65;
	private static final int SLIDERS_TOP = 29;
	private static final int PAD = 4;
	private static final int HEIGHT = 12;
	private static final int SLIDER_GAP = 2;
	private static final String BUILD = "buildingplanner.ui.radial.build_speed";
	private static final String BREAK = "buildingplanner.ui.radial.break_speed";
	/** The buttons' middles from the menu's middle, {x, y}, in the buttons' order. */
	private static final int[][] SLOTS = {{-157, -39}, {-131, -39}, {-105, -39}, {-157, -65},
		{-157, -91}, {-131, -91}, {-105, -91}};

	private static final Identifier SHOWN = RadialButton.icon("ghost_blocks_shown");
	private static final Identifier HIDDEN = RadialButton.icon("ghost_blocks_hidden");
	private static final Identifier COPY = RadialButton.icon("copy");
	private static final Identifier PASTE = RadialButton.icon("paste");
	private static final Identifier MATERIAL_LIST = RadialButton.icon("material_list");
	private static final Identifier TURN = RadialButton.icon("turn");
	private static final Identifier MIRROR = RadialButton.icon("mirror");
	private static final Identifier CLEAR = RadialButton.icon("clear");

	private RadialMenuWidgets() {
	}

	/** The widgets for a radial menu of that size, made afresh each time it opens. */
	public static List<AbstractWidget> create(int width, int height) {
		int sliderWidth = widestLabel() + 2 * PAD;
		int x = width / 2 + RIGHT - sliderWidth;
		int top = height / 2 + SLIDERS_TOP;
		List<AbstractWidget> widgets = new ArrayList<>(List.of(
			new RepeatSlider(x, top, sliderWidth, HEIGHT, RepeatSpeed.PLACING, BUILD, BuildingPlannerClient.config()),
			new RepeatSlider(x, top + HEIGHT + SLIDER_GAP, sliderWidth, HEIGHT, RepeatSpeed.ERASING, BREAK,
				BuildingPlannerClient.config())));
		BooleanSupplier always = () -> true;
		BooleanSupplier withCopy = Boolean.getBoolean(ALL_BUTTONS_PROPERTY) ? always
			: BuildingPlannerClient::copyExists;
		widgets.add(button(width, height, SLOTS[0], Component.translatable(BuildingPlannerClient.SHOW_HIDE.getName()),
			BuildingPlannerClient.SHOW_HIDE, () -> BuildingPlannerClient.ghostsShown() ? SHOWN : HIDDEN,
			BuildingPlannerClient::toggleGhosts, always));
		widgets.add(button(width, height, SLOTS[1], Component.translatable("buildingplanner.ui.radial.copy"),
			BuildingPlannerClient.COPY, () -> COPY, BuildingPlannerClient::copy, always));
		widgets.add(button(width, height, SLOTS[2], Component.translatable("buildingplanner.ui.radial.paste"),
			BuildingPlannerClient.PASTE, () -> PASTE, BuildingPlannerClient::paste, always));
		widgets.add(button(width, height, SLOTS[3], Component.translatable("buildingplanner.ui.radial.material_list"),
			null, () -> MATERIAL_LIST, BuildingPlannerClient::materialList, always));
		widgets.add(button(width, height, SLOTS[4], Component.translatable(BuildingPlannerClient.TURN.getName()),
			BuildingPlannerClient.TURN, () -> TURN, BuildingPlannerClient::turnCopy, withCopy));
		widgets.add(button(width, height, SLOTS[5], Component.translatable(BuildingPlannerClient.MIRROR.getName()),
			BuildingPlannerClient.MIRROR, () -> MIRROR, BuildingPlannerClient::mirrorCopy, withCopy));
		widgets.add(button(width, height, SLOTS[6], Component.translatable(BuildingPlannerClient.CLEAR.getName()),
			BuildingPlannerClient.CLEAR, () -> CLEAR, BuildingPlannerClient::clearCopy, withCopy));
		return widgets;
	}

	/**
	 * Hook E12, before each frame of the menu: shows each button by its condition, so Turn, Mirror and Clear go with the
	 * copy, even after Clear in the open menu.
	 */
	public static void beforeDraw(List<? extends GuiEventListener> children) {
		for (GuiEventListener child : children) {
			if (child instanceof RadialButton button) {
				button.refresh();
			}
		}
	}

	/**
	 * Hook E16: the radial menu is closing, by its key let go or by Escape. As the menu then acts on its own button
	 * under the pointer unless a click acted since it opened ({@code actedByClick}), so does the Building Planner button
	 * under the pointer.
	 */
	public static void menuClosing(List<? extends GuiEventListener> children, boolean actedByClick) {
		if (actedByClick) {
			return;
		}
		for (GuiEventListener child : children) {
			if (child instanceof RadialButton button && button.visible && button.isHovered()) {
				button.act();
			}
		}
	}

	private static RadialButton button(int width, int height, int[] middle, Component name, KeyMapping key,
		Supplier<Identifier> icon, Runnable action, BooleanSupplier shown) {
		Supplier<String> keyName = key == null || key.isUnbound() ? () -> null
			: () -> key.getTranslatedKeyMessage().getString();
		return new RadialButton(width / 2 + middle[0] - RadialButton.SIZE / 2,
			height / 2 + middle[1] - RadialButton.SIZE / 2, name, keyName, icon, action, shown);
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
