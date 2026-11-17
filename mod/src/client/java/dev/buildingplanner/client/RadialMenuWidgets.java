package dev.buildingplanner.client;

import dev.buildingplanner.input.RepeatSpeed;
import dev.buildingplanner.look.SmallText;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Building Planner's widgets in Effortless Building's radial menu (hooks E11 and E12), placed from the menu's middle
 * as its own buttons are. The menu's left-hand buttons (the wrench, undo and redo, and the row under them) are 20
 * across with their middles 26 apart, the leftmost 157 left of the menu's middle, in two rows 13 above and 13 below
 * it, so they span 167 to 95 left of the middle and end 23 below it. The two speed sliders sit just under them, 6 below
 * the lower row as that row is below the upper, and end above a mode's description, which the menu draws 100 below
 * the middle. Their labels draw on one line at {@link SmallText}'s size, full size at GUI scales 1 to 3, where the
 * longest label, "Break speed: 3.33 blocks/s", is 137 across: more than the 102 between the buttons' left edge and the
 * menu's ring (65 from the middle at most). So the sliders end 65 left of the middle, outside the ring, and are as wide
 * as the longest label either shows needs at the current size, plus {@link #PAD} on each side, reaching further left
 * than the buttons. Each is {@link #HEIGHT} high, and they end 55 below the middle.
 *
 * <p>Building Planner's buttons ({@link RadialButton}) are 20 across with their middles 26 apart, as the menu's own.
 * Geo is choosing where they go from four drafts, picked by the development system property {@value #LAYOUT_PROPERTY}
 * and read each time the menu opens, each slot listed in the buttons' order (show or hide, Copy, Paste, "Copy real
 * blocks", "Material list"):
 * <ul>
 * <li>{@code A}, right: in the left-hand buttons' columns mirrored to the right (middles 105, 131 and 157 right of the
 * middle), in rows 52 and 78 below it, under the options the menu shows there for some modes (Wall, Floor, Cube,
 * Slope Floor, Circle, Cylinder and Sphere), whose buttons fill the rows 13 above and 26 below the middle;
 * <li>{@code B}, left and above (the layout unless the property says otherwise): in the left-hand buttons' own columns,
 * in rows 39 and 65 above the middle, over the wrench, undo and redo;
 * <li>{@code C}, top: one row over the ring, its middle 95 above the menu's middle;
 * <li>{@code D}, under the sliders, where the design first put them: in the left-hand buttons' columns, in rows 67 and
 * 93 below the middle, the first 2 under the sliders as they are under each other; the lower row stays left of the
 * widest mode description, which starts 100 below the middle.
 * </ul>
 * Copy, Paste, "Copy real blocks" and "Material list" come with build steps 10 and 11; until then they show, doing
 * nothing, only while the development system property {@value #DRAFT_PROPERTY} is {@code true}. The icons are
 * Lucide's ({@link LucideIcons}), or vanilla items while the development system property {@value #ICONS_PROPERTY} is
 * {@code items}, for Geo's choice.
 */
public final class RadialMenuWidgets {
	static final String LAYOUT_PROPERTY = "buildingplanner.buttonLayout";
	static final String DRAFT_PROPERTY = "buildingplanner.draftButtons";
	static final String ICONS_PROPERTY = "buildingplanner.buttonIcons";
	/** Where the sliders end, from the middle: just outside the menu's ring. */
	private static final int RIGHT = -65;
	private static final int SLIDERS_TOP = 29;
	private static final int PAD = 4;
	private static final int HEIGHT = 12;
	private static final int SLIDER_GAP = 2;
	private static final String BUILD = "buildingplanner.ui.radial.build_speed";
	private static final String BREAK = "buildingplanner.ui.radial.break_speed";
	/** Each layout's button middles from the menu's middle, {x, y}, in the buttons' order. */
	private static final Map<String, int[][]> LAYOUTS = Map.of(
		"A", new int[][] {{105, 52}, {131, 52}, {157, 52}, {105, 78}, {131, 78}},
		"B", new int[][] {{-157, -39}, {-131, -39}, {-105, -39}, {-157, -65}, {-131, -65}},
		"C", new int[][] {{-52, -95}, {-26, -95}, {0, -95}, {26, -95}, {52, -95}},
		"D", new int[][] {{-157, 67}, {-131, 67}, {-105, 67}, {-157, 93}, {-131, 93}});

	private static final RadialButton.Icon EYE = RadialButton.lucide("eye", LucideIcons.EYE);
	private static final RadialButton.Icon EYE_OFF = RadialButton.lucide("eye_off", LucideIcons.EYE_OFF);
	private static final RadialButton.Icon COPY = RadialButton.lucide("copy", LucideIcons.COPY);
	private static final RadialButton.Icon PASTE = RadialButton.lucide("clipboard_paste", LucideIcons.CLIPBOARD_PASTE);
	private static final RadialButton.Icon COPY_REAL = RadialButton.lucide("brick_wall", LucideIcons.BRICK_WALL);
	private static final RadialButton.Icon MATERIAL_LIST = RadialButton.lucide("list_checks", LucideIcons.LIST_CHECKS);

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
		int[][] slots = LAYOUTS.getOrDefault(System.getProperty(LAYOUT_PROPERTY, "B"), LAYOUTS.get("B"));
		boolean items = "items".equals(System.getProperty(ICONS_PROPERTY));
		List<RadialButton> buttons = new ArrayList<>();
		buttons.add(button(width, height, slots[0], Component.translatable(BuildingPlannerClient.SHOW_HIDE.getName()),
			BuildingPlannerClient.SHOW_HIDE, () -> BuildingPlannerClient.ghostsShown()
				? items ? RadialButton.item(new ItemStack(Items.ENDER_EYE)) : EYE
				: items ? RadialButton.item(new ItemStack(Items.ENDER_PEARL)) : EYE_OFF,
			BuildingPlannerClient::toggleGhosts));
		if (Boolean.getBoolean(DRAFT_PROPERTY)) {
			buttons.add(button(width, height, slots[1], Component.translatable("buildingplanner.ui.radial.copy"),
				BuildingPlannerClient.COPY, icon(items, Items.PAPER, COPY), () -> {
				}));
			buttons.add(button(width, height, slots[2], Component.translatable("buildingplanner.ui.radial.paste"),
				BuildingPlannerClient.PASTE, icon(items, Items.SLIME_BALL, PASTE), () -> {
				}));
			buttons.add(button(width, height, slots[3], Component.translatable("buildingplanner.ui.radial.copy_real"),
				null, icon(items, Items.BRICKS, COPY_REAL), () -> {
				}));
			buttons.add(button(width, height, slots[4],
				Component.translatable("buildingplanner.ui.radial.material_list"), null,
				icon(items, Items.WRITABLE_BOOK, MATERIAL_LIST), () -> {
				}));
		}
		widgets.addAll(buttons);
		return widgets;
	}

	private static RadialButton button(int width, int height, int[] middle, Component name, KeyMapping key,
		Supplier<RadialButton.Icon> icon, Runnable action) {
		Supplier<String> keyName = key == null || key.isUnbound() ? () -> null
			: () -> key.getTranslatedKeyMessage().getString();
		return new RadialButton(width / 2 + middle[0] - RadialButton.SIZE / 2,
			height / 2 + middle[1] - RadialButton.SIZE / 2, name, keyName, icon, action);
	}

	private static Supplier<RadialButton.Icon> icon(boolean items, Item item, RadialButton.Icon lucide) {
		RadialButton.Icon icon = items ? RadialButton.item(new ItemStack(item)) : lucide;
		return () -> icon;
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
