package dev.buildingplanner.client;

import dev.buildingplanner.plan.CopyHistory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * "Rename copy", laid out as "Save list": a name field, filled in with the copy's name, which the player may type over,
 * and "Save" and "Cancel". The field takes at most {@value CopyHistory#MAX_NAME_LENGTH} characters, and "Save" is
 * greyed out while the name is empty or only spaces; spaces at either end are dropped. Enter and the keypad's Enter in
 * the name field do what "Save" does. "Save" keeps the copy until it's deleted, under the new name or the same one.
 * Both buttons, and Escape, go back to "Saved copies".
 */
final class RenameCopyScreen extends Screen {
	private static final int TEXT = 0xFFFFFFFF;
	private static final int WIDTH = 200;

	private final Screen parent;
	private final int file;
	private String name;
	private EditBox field;
	private Button save;

	private RenameCopyScreen(Screen parent, CopyHistory.Saved copy) {
		super(Component.translatable("buildingplanner.ui.rename_copy.title"));
		this.parent = parent;
		this.file = copy.file();
		this.name = copy.name();
	}

	/** Opens the screen for the copy over "Saved copies", to which it goes back. */
	static void open(Screen parent, CopyHistory.Saved copy) {
		Minecraft.getInstance().setScreenAndShow(new RenameCopyScreen(parent, copy));
	}

	@Override
	protected void init() {
		int left = (width - WIDTH) / 2;
		int top = height / 2 - 30;
		field = new EditBox(font, left, top + 12, WIDTH, 20,
			Component.translatable("buildingplanner.ui.rename_copy.name"));
		field.setMaxLength(CopyHistory.MAX_NAME_LENGTH);
		field.setValue(name);
		field.setResponder(value -> {
			name = value;
			save.active = !name.isBlank();
		});
		addRenderableWidget(field);
		int buttonWidth = (WIDTH - 4) / 2;
		save = addRenderableWidget(Button.builder(Component.translatable("buildingplanner.ui.rename_copy.save"),
			button -> save()).bounds(left, top + 40, buttonWidth, 20).build());
		save.active = !name.isBlank();
		addRenderableWidget(Button.builder(Component.translatable("buildingplanner.ui.rename_copy.cancel"),
			button -> onClose()).bounds(left + buttonWidth + 4, top + 40, buttonWidth, 20).build());
		setInitialFocus(field);
	}

	/** Enter and the keypad's Enter in the name field do what "Save" does, and nothing while "Save" is greyed out. */
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (getFocused() == field && event.isConfirmation()) {
			if (save.active) {
				save();
			}
			return true;
		}
		return super.keyPressed(event);
	}

	private void save() {
		SavedCopies copies = SavedCopies.get();
		String kept = name.strip();
		if (copies != null && !kept.isEmpty()) {
			copies.rename(file, kept);
		}
		onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		int top = height / 2 - 30;
		graphics.centeredText(font, title.getString(), width / 2, top - 16, TEXT);
		graphics.text(font, Component.translatable("buildingplanner.ui.rename_copy.name"), (width - WIDTH) / 2, top,
			TEXT);
	}

	@Override
	public void onClose() {
		minecraft.setScreenAndShow(parent);
	}
}
