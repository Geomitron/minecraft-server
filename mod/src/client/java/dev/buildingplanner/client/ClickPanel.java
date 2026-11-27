package dev.buildingplanner.client;

import com.mojang.blaze3d.platform.Window;
import dev.buildingplanner.input.RealPlace;
import dev.buildingplanner.look.ClickItems;
import dev.buildingplanner.look.MaterialPanelRows;
import dev.buildingplanner.look.PanelRoom;
import dev.buildingplanner.mixin.litematica.OverlayRendererAccessor;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.render.RenderUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The click's panel (see "Place real blocks": "The click's panel"): in survival, while a shape that places real blocks
 * is under way and while a copy is at the aim with "Place real blocks" acting, a panel drawn as the material list
 * panel's ({@link CountPanel}) lists each item the click uses, how many it uses, how many the player holds and how many
 * are missing, in red. It counts what the server's all-or-nothing check counts: the shape's preview's count
 * ({@link RealShape#needs}) or the paste's ({@link ClientSession#copyNeeds}). Where it goes, the order of its rows, how
 * many it shows and whether Effortless Building's icons beside the crosshair still show are {@link #layout}'s, which
 * the in-game checks switch. It keeps out of the screen's other parts ({@link PanelRoom}), Litematica's lines about
 * the ghost block at the crosshair among them, and hides while a screen is open, while the HUD is hidden (F1), without
 * a session, and while the click uses no items.
 */
final class ClickPanel implements HudElement {
	/** From the screen's edges to the panel, in GUI pixels, as the material list panel's. */
	private static final int EDGE = MaterialPanel.EDGE;
	private static final String[] COLUMNS = {"buildingplanner.ui.click_panel.uses",
		"buildingplanner.ui.click_panel.holds", "buildingplanner.ui.click_panel.missing"};

	/** The panel's layout: the recommendation for each of Geo's open choices, which the in-game checks switch. */
	static ClickItems.Layout layout = ClickItems.Layout.RECOMMENDED;

	/**
	 * How long after Litematica last wrote its lines about the block at the crosshair they count as showing: it writes
	 * them every 50 ms while it draws them, and a slow frame takes longer.
	 */
	private static final long LINES_SHOWN_MS = 250;

	private final Supplier<Map<Item, Integer>> shapeNeeds;
	private final Supplier<PanelRoom.Box> mixBox;
	private final IntSupplier controlsTop;
	/**
	 * Where Litematica's lines showed since the panel last appeared, or null: the panel stays clear of them until it
	 * hides, so it doesn't jump up and down as the crosshair crosses ghost blocks.
	 */
	private PanelRoom.Box linesSeen;

	/**
	 * {@code shapeNeeds} is the items the place click of the shape under way uses (null when none is, or it places no
	 * real blocks), {@code mixBox} the mix panel's box while it shows, and {@code controlsTop} the top of the controls
	 * lines above the hotbar.
	 */
	ClickPanel(Supplier<Map<Item, Integer>> shapeNeeds, Supplier<PanelRoom.Box> mixBox, IntSupplier controlsTop) {
		this.shapeNeeds = shapeNeeds;
		this.mixBox = mixBox;
		this.controlsTop = controlsTop;
	}

	/** Where the panel goes, and its rows, in GUI pixels. */
	private record Placed(CountPanel.Layout layout, float x, float y) {
	}

	/** The items the click uses now, or null while the panel hides. */
	private Map<Item, Integer> needs() {
		Minecraft mc = Minecraft.getInstance();
		ClientSession session = BuildingPlannerClient.session();
		Player player = mc.player;
		if (session == null || player == null || player.hasInfiniteMaterials() || mc.gui.screen() != null
			|| mc.gui.hud.isHidden()) {
			return null;
		}
		Map<Item, Integer> needs = shapeNeeds.get();
		if (needs == null) {
			needs = session.copyNeeds();
		}
		return needs == null || needs.isEmpty() ? null : needs;
	}

	/** Whether Effortless Building's icons beside the crosshair show while the panel does. */
	boolean hidesIcons() {
		return !layout.iconsShow() && needs() != null;
	}

	private static CountPanel.Texts texts() {
		Component[] headings = new Component[COLUMNS.length];
		for (int i = 0; i < COLUMNS.length; i++) {
			headings[i] = Component.translatable(COLUMNS[i]);
		}
		return new CountPanel.Texts(Component.translatable("buildingplanner.ui.click_panel.title"), headings,
			Component.empty(), MaterialPanel::more);
	}

	/** The panel's rows before the room it has cuts them: at most {@link ClickItems.Layout#maxRows}, in its order. */
	private static MaterialPanelRows<ItemStack> rows(Map<Item, Integer> needs, Player player) {
		RealPlace.Inventory inventory = RealPlace.Inventory.of(player);
		List<ClickItems.Entry<ItemStack>> entries = new ArrayList<>();
		needs.forEach((item, uses) -> entries.add(new ClickItems.Entry<>(new ItemStack(item),
			BuiltInRegistries.ITEM.getKey(item).toString(), uses, inventory.holds(item))));
		return ClickItems.rows(entries, layout.order(), layout.maxRows());
	}

	/**
	 * Where the panel goes and the rows it shows: at its spot, then down as far as it has room, below what the screen
	 * shows above it and above what it shows below; rows that don't fit go to "+N more".
	 */
	private Placed placed() {
		Map<Item, Integer> needs = needs();
		if (needs == null) {
			linesSeen = null;
			return null;
		}
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		Window window = mc.getWindow();
		int width = window.getGuiScaledWidth();
		int height = window.getGuiScaledHeight();
		CountPanel.Texts texts = texts();
		MaterialPanelRows<ItemStack> rows = rows(needs, mc.player);
		CountPanel.Layout full = CountPanel.Layout.of(font, texts, rows);
		int panelWidth = (int) Math.ceil(full.guiWidth());
		int left = left(width, panelWidth);
		PanelRoom.Room room = PanelRoom.below(left, left + panelWidth, EDGE, height - ContextualBar.MARGIN_BOTTOM,
			boxes(mc, width, height, needs.size()));
		int units = (int) (room.height() / CountPanel.scale());
		CountPanel.Layout cut = CountPanel.Layout.of(font, texts,
			rows.within(units - CountPanel.fixedHeight(font), CountPanel.ROW, CountPanel.moreHeight(font)));
		return new Placed(cut, left(width, (int) Math.ceil(cut.guiWidth())), room.top());
	}

	/** The panel's left edge at its spot, for a panel {@code panelWidth} wide. */
	private static int left(int width, int panelWidth) {
		return switch (layout.spot()) {
			case TOP_RIGHT -> width - EDGE - panelWidth;
			case TOP_MIDDLE -> (width - panelWidth) / 2;
			case TOP_LEFT -> EDGE;
		};
	}

	/** What else the screen shows, which the panel keeps out of. */
	private List<PanelRoom.Box> boxes(Minecraft mc, int width, int height, int items) {
		List<PanelRoom.Box> boxes = new ArrayList<>();
		PanelRoom.Box material = MaterialPanel.guiBox();
		if (material != null) {
			boxes.add(material);
		}
		PanelRoom.Box mix = mixBox.get();
		if (mix != null) {
			boxes.add(mix);
		}
		PanelRoom.Box effects = effects(mc, width);
		if (effects != null) {
			boxes.add(effects);
		}
		int middleX = width / 2;
		int middleY = height / 2;
		// The crosshair, and Effortless Building's icons, which start 10 right of the middle, 20 apart, with their counts
		// under them.
		boxes.add(new PanelRoom.Box(middleX - 8, middleY - 8, middleX + 8, middleY + 8));
		if (layout.iconsShow()) {
			boxes.add(new PanelRoom.Box(middleX + 10, middleY - 8, middleX + 10 + 20 * items, middleY + 16));
		}
		boxes.add(chat(mc, height));
		boxes.add(new PanelRoom.Box(0, controlsTop.getAsInt(), width, height));
		PanelRoom.Box lines = litematicaLines(mc, width);
		if (lines != null) {
			linesSeen = linesSeen == null ? lines : new PanelRoom.Box(Math.min(linesSeen.left(), lines.left()),
				Math.min(linesSeen.top(), lines.top()), Math.max(linesSeen.right(), lines.right()),
				Math.max(linesSeen.bottom(), lines.bottom()));
		}
		if (linesSeen != null) {
			boxes.add(linesSeen);
		}
		return boxes;
	}

	/**
	 * Litematica's lines about the ghost block at the crosshair, on by default at the top right in half-size text, where
	 * they show now, or null; laid out as MaLiLib's {@code RenderUtils.renderText} lays them out, in its scaled units:
	 * lines 11 apart, each on a background 2 wider than its text on each side and 2 above it, kept below the effect icons
	 * at the top right.
	 */
	private static PanelRoom.Box litematicaLines(Minecraft mc, int width) {
		OverlayRendererAccessor overlay = (OverlayRendererAccessor) OverlayRenderer.getInstance();
		List<String> lines = overlay.buildingplanner$blockInfoLines();
		if (!Configs.InfoOverlays.BLOCK_INFO_LINES_ENABLED.getBooleanValue() || lines.isEmpty()
			|| System.currentTimeMillis() - overlay.buildingplanner$infoUpdateTime() > LINES_SHOWN_MS) {
			return null;
		}
		double scale = Configs.InfoOverlays.BLOCK_INFO_LINES_FONT_SCALE.getDoubleValue();
		if (scale < 0.0125) {
			return null;
		}
		int offsetX = Configs.InfoOverlays.BLOCK_INFO_LINES_OFFSET_X.getIntegerValue();
		int offsetY = Configs.InfoOverlays.BLOCK_INFO_LINES_OFFSET_Y.getIntegerValue();
		HudAlignment alignment = (HudAlignment) Configs.InfoOverlays.BLOCK_INFO_LINES_ALIGNMENT.getOptionListValue();
		int widest = 0;
		for (String line : lines) {
			widest = Math.max(widest, mc.font.width(line));
		}
		int contentHeight = lines.size() * 11 - 2;
		double scaledWidth = width / scale;
		double left = switch (alignment) {
			case TOP_RIGHT, BOTTOM_RIGHT -> scaledWidth - widest - offsetX - 2;
			case CENTER -> scaledWidth / 2 - widest / 2.0 - offsetX;
			default -> offsetX + 2;
		};
		double top = RenderUtils.getHudPosY(offsetY + 2, offsetY, contentHeight, scale, alignment)
			+ RenderUtils.getHudOffsetForPotions(alignment, scale, mc.player);
		return new PanelRoom.Box((int) Math.floor((left - 2) * scale), (int) Math.floor((top - 2) * scale),
			(int) Math.ceil((left + widest + 2) * scale), (int) Math.ceil((top + contentHeight) * scale));
	}

	/**
	 * The effect icons in the top-right corner, as the game lays them out: 25 apart from the right edge, the beneficial
	 * ones in the top row and the others in a second row under it; or null with none shown.
	 */
	private static PanelRoom.Box effects(Minecraft mc, int width) {
		int good = 0;
		int bad = 0;
		for (MobEffectInstance effect : mc.player.getActiveEffects()) {
			if (effect.showIcon()) {
				if (effect.getEffect().value().isBeneficial()) {
					good++;
				} else {
					bad++;
				}
			}
		}
		if (good + bad == 0) {
			return null;
		}
		return new PanelRoom.Box(width - 25 * Math.max(good, bad), 0, width, bad > 0 ? 27 + 24 : 1 + 24);
	}

	/** Chat at the bottom-left, at its width and its unfocused height from the player's chat settings. */
	private static PanelRoom.Box chat(Minecraft mc, int height) {
		double scale = mc.options.chatScale().get();
		int chatWidth = (int) Math.ceil(ChatComponent.getWidth(mc.options.chatWidth().get()) * scale) + 12;
		int chatHeight = (int) Math.ceil(ChatComponent.getHeight(mc.options.chatHeightUnfocused().get()) * scale);
		int bottom = height - 40;
		return new PanelRoom.Box(0, bottom - chatHeight, chatWidth, bottom);
	}

	/**
	 * The panel's lines as the player reads them, separated by " | ", or {@code hidden}; for the in-game checks. A row
	 * reads as its item's id, then how many the click uses, how many the player holds and how many are missing, with
	 * the last one's colour.
	 */
	String text() {
		Placed placed = placed();
		return placed == null ? "hidden" : CountPanel.text(placed.layout());
	}

	/**
	 * Litematica's lines about the ghost block at the crosshair, as the panel keeps clear of them, in the window's pixels
	 * (x y width height), or {@code none} while they don't show; for the in-game checks and for outlining them in
	 * screenshots.
	 */
	static String linesBox() {
		Minecraft mc = Minecraft.getInstance();
		Window window = mc.getWindow();
		PanelRoom.Box box = mc.player == null ? null : litematicaLines(mc, window.getGuiScaledWidth());
		int scale = window.getGuiScale();
		return box == null ? "none" : box.left() * scale + " " + box.top() * scale + " "
			+ (box.right() - box.left()) * scale + " " + (box.bottom() - box.top()) * scale;
	}

	/**
	 * The panel's box in the window's pixels (x y width height), or {@code hidden}; for the in-game checks and for
	 * outlining it in screenshots.
	 */
	String box() {
		Placed placed = placed();
		return placed == null ? "hidden" : CountPanel.windowBox(placed.layout(), placed.x(), placed.y());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Placed placed = placed();
		if (placed != null) {
			CountPanel.draw(graphics, Minecraft.getInstance().font, placed.layout(), placed.x(), placed.y());
		}
	}
}
