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
 * The mix panel at the right edge of the screen: while the block mix applies, it shows the mix's blocks (icon and name)
 * with their percentages, and the line naming the mix key, so the player always knows they're placing a mix. It hides
 * while the mix doesn't apply, while a screen is open, and while the HUD is hidden (F1).
 */
final class MixPanel implements HudElement {
	private static final int ROW = 18;
	private static final int PAD = 4;
	private static final int MARGIN = 2;
	/** The least space between a block's name and its percentage. */
	private static final int GAP = 8;
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

	/** The panel's lines as the player reads them, separated by " | ", or {@code hidden}; for the in-game checks. */
	String text() {
		if (!shows()) {
			return "hidden";
		}
		List<String> lines = new ArrayList<>();
		lines.add(title().getString());
		int total = mix.total();
		for (BlockMix.Part part : mix.parts()) {
			lines.add(part.block().getName().getString() + " " + BlockMix.percent(part.weight(), total));
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
		int width = Math.max(font.width(title), font.width(keyLine));
		for (BlockMix.Part part : parts) {
			width = Math.max(width,
				ROW + font.width(part.block().getName()) + GAP + font.width(BlockMix.percent(part.weight(), total)));
		}
		int height = font.lineHeight + 2 + parts.size() * ROW + 2 + font.lineHeight;
		int right = graphics.guiWidth() - MARGIN;
		int left = right - width - 2 * PAD;
		int top = (graphics.guiHeight() - height) / 2 - PAD;
		graphics.fill(left, top, right, top + height + 2 * PAD, BACKGROUND);

		int x = left + PAD;
		int y = top + PAD;
		graphics.text(font, title, x, y, TEXT);
		y += font.lineHeight + 2;
		for (BlockMix.Part part : parts) {
			graphics.item(new ItemStack(part.block()), x, y + 1);
			int textY = y + (ROW - font.lineHeight) / 2 + 1;
			String percent = BlockMix.percent(part.weight(), total);
			graphics.text(font, part.block().getName(), x + ROW, textY, TEXT);
			graphics.text(font, percent, right - PAD - font.width(percent), textY, TEXT);
			y += ROW;
		}
		graphics.text(font, keyLine, x, y + 2, GREY);
	}
}
