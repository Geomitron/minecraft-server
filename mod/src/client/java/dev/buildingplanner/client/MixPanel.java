package dev.buildingplanner.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The mix panel at the right edge of the screen: while the block mix applies, it shows the mix's blocks (each block's
 * icon and its percentage) and the line naming the mix key, so the player always knows they're placing a mix. It hides
 * while the mix doesn't apply, while a screen is open, and while the HUD is hidden (F1). It draws at {@link #SCALE} of
 * the game's own size, as narrow as its widest line.
 */
final class MixPanel implements HudElement {
	/**
	 * The panel's size beside the game's own: 0.7, as the radial menu's slider labels are drawn, where a line of text is
	 * still about 13 pixels high on the screen at GUI scale 2 and readable.
	 */
	private static final float SCALE = 0.7f;
	private static final int ROW = 18;
	private static final int PAD = 4;
	private static final int MARGIN = 2;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int GREY = 0xFFA0A0A0;
	private static final int BACKGROUND = 0xA0000000;

	private final BlockMix mix;
	private final BooleanSupplier applies;
	private final Supplier<Component> key;

	/** {@code applies} is the mix rule now; {@code key} is the mix key's name as the player has bound it. */
	MixPanel(BlockMix mix, BooleanSupplier applies, Supplier<Component> key) {
		this.mix = mix;
		this.applies = applies;
		this.key = key;
	}

	boolean shows() {
		Minecraft mc = Minecraft.getInstance();
		return mc.gui.screen() == null && !mc.gui.hud.isHidden() && applies.getAsBoolean();
	}

	private Component title() {
		return Component.translatable("buildingplanner.ui.block_mix.title");
	}

	private Component keyLine() {
		return Component.translatable("buildingplanner.ui.mix_panel.keys", key.get());
	}

	/**
	 * The panel's lines as the player reads them, separated by " | ", or {@code hidden}; for the in-game checks. A
	 * block's row reads as its percentage, beside the block's icon.
	 */
	String text() {
		if (!shows()) {
			return "hidden";
		}
		List<String> lines = new ArrayList<>();
		lines.add(title().getString());
		int total = mix.total();
		for (BlockMix.Part part : mix.parts()) {
			lines.add(BlockMix.percent(part.weight(), total));
		}
		lines.add(keyLine().getString());
		return String.join(" | ", lines);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		if (!shows()) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		List<BlockMix.Part> parts = mix.parts();
		int total = mix.total();
		Component title = title();
		Component keyLine = keyLine();
		int percentWidth = 0;
		for (BlockMix.Part part : parts) {
			percentWidth = Math.max(percentWidth, font.width(BlockMix.percent(part.weight(), total)));
		}
		int width = Math.max(Math.max(font.width(title), font.width(keyLine)), ROW + percentWidth);
		int height = font.lineHeight + 2 + parts.size() * ROW + 2 + font.lineHeight;

		// Drawn from the panel's right edge, halfway down the screen, at the panel's scale.
		graphics.pose().pushMatrix();
		graphics.pose().translate(graphics.guiWidth() - MARGIN, graphics.guiHeight() / 2f);
		graphics.pose().scale(SCALE);
		int right = 0;
		int left = right - width - 2 * PAD;
		int top = -(height + 2 * PAD) / 2;
		graphics.fill(left, top, right, top + height + 2 * PAD, BACKGROUND);

		int x = left + PAD;
		int y = top + PAD;
		graphics.text(font, title, x, y, TEXT);
		y += font.lineHeight + 2;
		for (BlockMix.Part part : parts) {
			graphics.item(new ItemStack(part.block()), x, y + 1);
			int textY = y + (ROW - font.lineHeight) / 2 + 1;
			String percent = BlockMix.percent(part.weight(), total);
			graphics.text(font, percent, x + ROW + percentWidth - font.width(percent), textY, TEXT);
			y += ROW;
		}
		graphics.text(font, keyLine, x, y + 2, GREY);
		graphics.pose().popMatrix();
	}
}
