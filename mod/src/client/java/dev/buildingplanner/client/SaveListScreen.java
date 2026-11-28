package dev.buildingplanner.client;

import dev.buildingplanner.sync.ListLimits;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import java.time.ZonedDateTime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * "Save list": a name field, filled in to start with the date and time, which the player may type over, and "Save" and
 * "Cancel". The field takes at most {@value ListLimits#MAX_NAME_LENGTH} characters, and "Save" is greyed out while the
 * name is empty or only spaces; spaces at either end are dropped. Enter and the keypad's Enter in the name field do
 * what "Save" does. Saving with the name of a kept list replaces it. Both buttons, and Escape, go back to the list's
 * screen.
 */
final class SaveListScreen extends Screen {
	private static final int TEXT = 0xFFFFFFFF;
	private static final int WIDTH = 200;

	private final GuiMaterialList parent;
	private final GhostMaterialList list;
	private String name;
	private EditBox field;
	private Button save;

	private SaveListScreen(GuiMaterialList parent, GhostMaterialList list) {
		super(Component.translatable("buildingplanner.ui.save_list.title"));
		this.parent = parent;
		this.list = list;
		this.name = ListTexts.dateTime(ZonedDateTime.now());
	}

	/** Opens the screen for the list the material list screen shows; does nothing while Building Planner is off. */
	static void open(GuiMaterialList parent) {
		if (BuildingPlannerClient.session() != null && parent.getMaterialList() instanceof GhostMaterialList list) {
			Minecraft.getInstance().setScreenAndShow(new SaveListScreen(parent, list));
		}
	}

	@Override
	protected void init() {
		int left = (width - WIDTH) / 2;
		int top = height / 2 - 30;
		field = new EditBox(font, left, top + 12, WIDTH, 20,
			Component.translatable("buildingplanner.ui.save_list.name"));
		field.setMaxLength(ListLimits.MAX_NAME_LENGTH);
		field.setValue(name);
		field.setResponder(value -> {
			name = value;
			save.active = !name.isBlank();
		});
		addRenderableWidget(field);
		int buttonWidth = (WIDTH - 4) / 2;
		save = addRenderableWidget(Button.builder(Component.translatable("buildingplanner.ui.save_list.save"),
			button -> save()).bounds(left, top + 40, buttonWidth, 20).build());
		save.active = !name.isBlank();
		addRenderableWidget(Button.builder(Component.translatable("buildingplanner.ui.save_list.cancel"),
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
		ClientSession session = BuildingPlannerClient.session();
		String kept = name.strip();
		if (session != null && !kept.isEmpty()) {
			session.lists().save(kept, list.data());
		}
		onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		int top = height / 2 - 30;
		graphics.centeredText(font, title, width / 2, top - 16, TEXT);
		graphics.text(font, Component.translatable("buildingplanner.ui.save_list.name"), (width - WIDTH) / 2, top, TEXT);
	}

	@Override
	public void onClose() {
		minecraft.setScreenAndShow(parent);
	}
}
