package dev.buildingplanner.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * The block-mix picker: every block in the game, with search and a toggle that limits the list to the blocks in the
 * player's inventory. Clicking a block adds it to the mix (up to nine), each with a share; "Use mix" turns the mix on.
 */
final class BlockMixScreen extends Screen {
	private static final int CELL = 18;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int GREY = 0xFFA0A0A0;

	private final BlockMix mix;
	private final Runnable onUse;
	/** The mix being edited; the player's mix changes only on "Use mix". */
	private final List<BlockMix.Part> parts;
	/** Every block that has an item of its own, in the registry's order. */
	private final List<Block> allBlocks;
	private List<Block> shown;
	private String search = "";
	private boolean inventoryOnly;
	private Button use;

	private BlockMixScreen(BlockMix mix, Runnable onUse) {
		super(Component.translatable("buildingplanner.ui.block_mix.title"));
		this.mix = mix;
		this.onUse = onUse;
		this.parts = new ArrayList<>(mix.parts());
		this.allBlocks = BuiltInRegistries.BLOCK.stream()
			.filter(block -> block.asItem() instanceof BlockItem item && item.getBlock() == block)
			.toList();
		this.shown = allBlocks;
	}

	/** Opens the picker on the player's mix; {@code onUse} runs after "Use mix" turns it on. */
	static void open(BlockMix mix, Runnable onUse) {
		Minecraft.getInstance().setScreenAndShow(new BlockMixScreen(mix, onUse));
	}

	private int contentWidth() {
		return Math.min(width - 16, 340);
	}

	private int left() {
		return (width - contentWidth()) / 2;
	}

	@Override
	protected void init() {
		int left = left();
		int contentWidth = contentWidth();
		int toggleWidth = 140;

		EditBox searchBox = new EditBox(font, left, 18, contentWidth - toggleWidth - 4, 20,
			Component.translatable("buildingplanner.ui.block_mix.search"));
		searchBox.setHint(Component.translatable("buildingplanner.ui.block_mix.search"));
		searchBox.setValue(search);
		searchBox.setResponder(value -> {
			search = value;
			refilter();
		});
		addRenderableWidget(searchBox);
		addRenderableWidget(CycleButton.onOffBuilder(inventoryOnly).create(left + contentWidth - toggleWidth, 18,
			toggleWidth, 20, Component.translatable("buildingplanner.ui.block_mix.inventory_only"), (button, value) -> {
				inventoryOnly = value;
				refilter();
			}));

		int gridTop = 42;
		int gridRows = Math.max(1, (height - gridTop - 80) / CELL);
		addRenderableWidget(new BlockGrid(left, gridTop, contentWidth / CELL, gridRows));

		int slotWidth = contentWidth / BlockMix.MAX_BLOCKS;
		int mixTop = height - 74;
		for (int i = 0; i < parts.size(); i++) {
			int index = i;
			int x = left + i * slotWidth;
			addRenderableWidget(new BlockIcon(x + (slotWidth - CELL) / 2, mixTop, parts.get(i).block(),
				Component.translatable("buildingplanner.ui.block_mix.remove"), () -> {
					parts.remove(index);
					rebuildWidgets();
				}));
			EditBox share = new EditBox(font, x + 2, mixTop + CELL + 1, slotWidth - 4, 14,
				Component.translatable("buildingplanner.ui.block_mix.share"));
			share.setMaxLength(3);
			share.setValue(Integer.toString(parts.get(i).weight()));
			share.setTooltip(Tooltip.create(Component.translatable("buildingplanner.ui.block_mix.share")));
			share.setResponder(digitsOnly(share, weight -> {
				parts.set(index, new BlockMix.Part(parts.get(index).block(), weight));
				use.active = canUse();
			}));
			addRenderableWidget(share);
		}

		int buttonWidth = (contentWidth - 8) / 3;
		int buttonTop = height - 24;
		use = addRenderableWidget(Button.builder(Component.translatable("buildingplanner.ui.block_mix.use"),
			button -> {
				mix.use(parts.stream().filter(part -> part.weight() > 0).toList());
				onClose();
				onUse.run();
			}).bounds(left, buttonTop, buttonWidth, 20).build());
		use.active = canUse();
		addRenderableWidget(Button.builder(Component.translatable("buildingplanner.ui.block_mix.clear"), button -> {
			parts.clear();
			rebuildWidgets();
		}).bounds(left + buttonWidth + 4, buttonTop, buttonWidth, 20).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
			.bounds(left + 2 * (buttonWidth + 4), buttonTop, buttonWidth, 20).build());
		setInitialFocus(searchBox);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(font, title, width / 2, 6, TEXT);
		int left = left();
		int mixTop = height - 74;
		if (parts.isEmpty()) {
			graphics.text(font, Component.translatable("buildingplanner.ui.block_mix.empty"), left, mixTop + 4, GREY);
			return;
		}
		int total = parts.stream().mapToInt(BlockMix.Part::weight).sum();
		int slotWidth = contentWidth() / BlockMix.MAX_BLOCKS;
		for (int i = 0; i < parts.size(); i++) {
			String percent = total == 0 ? "0%" : Math.round(100.0 * parts.get(i).weight() / total) + "%";
			graphics.centeredText(font, percent, left + i * slotWidth + slotWidth / 2, mixTop + CELL + 18, GREY);
		}
	}

	private boolean canUse() {
		return parts.stream().anyMatch(part -> part.weight() > 0);
	}

	private void add(Block block) {
		if (parts.size() < BlockMix.MAX_BLOCKS && parts.stream().noneMatch(part -> part.block() == block)) {
			parts.add(new BlockMix.Part(block, 1));
			rebuildWidgets();
		}
	}

	private void refilter() {
		String query = search.strip().toLowerCase(Locale.ROOT);
		Set<Item> held = new HashSet<>();
		if (inventoryOnly && minecraft.player != null) {
			Inventory inventory = minecraft.player.getInventory();
			for (int i = 0; i < inventory.getContainerSize(); i++) {
				held.add(inventory.getItem(i).getItem());
			}
		}
		shown = allBlocks.stream()
			.filter(block -> !inventoryOnly || held.contains(block.asItem()))
			.filter(block -> query.isEmpty() || block.getName().getString().toLowerCase(Locale.ROOT).contains(query)
				|| BuiltInRegistries.BLOCK.getKey(block).toString().contains(query))
			.toList();
	}

	/** A responder that keeps only digits in {@code box} and passes the number on, 0 when empty. */
	private static Consumer<String> digitsOnly(EditBox box, java.util.function.IntConsumer onNumber) {
		return value -> {
			String digits = value.replaceAll("[^0-9]", "");
			if (!digits.equals(value)) {
				box.setValue(digits);
				return;
			}
			onNumber.accept(digits.isEmpty() ? 0 : Math.min(Integer.parseInt(digits), BlockMix.MAX_WEIGHT));
		};
	}

	/** The scrolling grid of blocks to pick from. */
	private final class BlockGrid extends AbstractWidget {
		private final int columns;
		private final int rows;
		private int firstRow;

		BlockGrid(int x, int y, int columns, int rows) {
			super(x, y, columns * CELL, rows * CELL, Component.empty());
			this.columns = columns;
			this.rows = rows;
		}

		private int lastFirstRow() {
			return Math.max(0, (shown.size() + columns - 1) / columns - rows);
		}

		private int indexAt(double mouseX, double mouseY) {
			int column = (int) ((mouseX - getX()) / CELL);
			int row = (int) ((mouseY - getY()) / CELL);
			if (mouseX < getX() || mouseY < getY() || column >= columns || row >= rows) {
				return -1;
			}
			int index = (firstRow + row) * columns + column;
			return index < shown.size() ? index : -1;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			firstRow = Math.min(firstRow, lastFirstRow());
			graphics.fill(getX() - 1, getY() - 1, getX() + getWidth() + 1, getY() + getHeight() + 1, 0x80000000);
			if (shown.isEmpty()) {
				graphics.text(font, Component.translatable("buildingplanner.ui.block_mix.none"), getX() + 4, getY() + 4,
					GREY);
				return;
			}
			int hovered = isHovered() ? indexAt(mouseX, mouseY) : -1;
			for (int row = 0; row < rows; row++) {
				for (int column = 0; column < columns; column++) {
					int index = (firstRow + row) * columns + column;
					if (index >= shown.size()) {
						break;
					}
					Block block = shown.get(index);
					int x = getX() + column * CELL;
					int y = getY() + row * CELL;
					if (parts.stream().anyMatch(part -> part.block() == block)) {
						graphics.fill(x, y, x + CELL, y + CELL, 0x8040A040);
					}
					if (index == hovered) {
						graphics.fill(x, y, x + CELL, y + CELL, 0x80FFFFFF);
					}
					graphics.item(new ItemStack(block), x + 1, y + 1);
				}
			}
			if (hovered >= 0) {
				graphics.setTooltipForNextFrame(font, new ItemStack(shown.get(hovered)), mouseX, mouseY);
			}
			int last = lastFirstRow();
			if (last > 0) {
				int barHeight = Math.max(8, getHeight() * rows / (rows + last));
				int barTop = getY() + (getHeight() - barHeight) * firstRow / last;
				graphics.fill(getX() + getWidth() - 2, barTop, getX() + getWidth(), barTop + barHeight, 0xFFC0C0C0);
			}
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			int index = indexAt(event.x(), event.y());
			if (index >= 0) {
				add(shown.get(index));
			}
		}

		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
			firstRow = Math.clamp(firstRow - (long) Math.signum(scrollY), 0, lastFirstRow());
			return true;
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
		}
	}

	/** A block's icon that does something when clicked. */
	private final class BlockIcon extends AbstractWidget {
		private final ItemStack stack;
		private final Runnable onClick;

		BlockIcon(int x, int y, Block block, Component tooltip, Runnable onClick) {
			super(x, y, CELL, CELL, block.getName());
			this.stack = new ItemStack(block);
			this.onClick = onClick;
			setTooltip(Tooltip.create(block.getName().copy().append("\n").append(tooltip)));
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			graphics.fill(getX(), getY(), getX() + CELL, getY() + CELL, isHovered() ? 0x80FFFFFF : 0x80000000);
			graphics.item(stack, getX() + 1, getY() + 1);
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			onClick.run();
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			defaultButtonNarrationText(output);
		}
	}
}
