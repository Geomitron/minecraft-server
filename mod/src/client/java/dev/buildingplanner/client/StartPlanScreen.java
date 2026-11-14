package dev.buildingplanner.client;

import dev.buildingplanner.sync.PlayerMessage;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.util.FileNameUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Starts a plan: asks for its name, then makes an empty plan of the area the player marked with Litematica's area
 * tool and shares it to everyone through Syncmatica.
 */
final class StartPlanScreen extends Screen {
	/** Where new plans' files go, inside Litematica's schematics folder. */
	private static final String FOLDER = "Building Planner";
	private static final String EXTENSION = ".litematic";

	private final AreaSelection area;
	private EditBox name;
	private Button create;

	private StartPlanScreen(AreaSelection area) {
		super(Component.translatable("buildingplanner.ui.start_plan.title"));
		this.area = area;
	}

	/** Opens the screen, or tells the player to mark an area first. */
	static void open() {
		AreaSelection area = DataManager.getSelectionManager().getCurrentSelection();
		// Litematica's Simple mode keeps one box with both corners at 0,0,0 until the player marks an area
		// (AreaSelectionSimple.createDefaultBoxIfNeeded), so a box like that is no area.
		if (area == null || PositionUtils.getValidBoxes(area).stream()
			.allMatch(box -> BlockPos.ZERO.equals(box.getPos1()) && BlockPos.ZERO.equals(box.getPos2()))) {
			LitematicaPlans.showMessage(PlayerMessage.noAreaMarked());
			return;
		}
		Minecraft.getInstance().setScreenAndShow(new StartPlanScreen(area));
	}

	@Override
	protected void init() {
		int x = width / 2 - 100;
		int y = height / 2 - 40;
		addRenderableWidget(new StringWidget(x, y, 200, 12, title, font));
		name = new EditBox(font, x, y + 18, 200, 20, Component.translatable("buildingplanner.ui.start_plan.name"));
		name.setHint(Component.translatable("buildingplanner.ui.start_plan.name"));
		name.setMaxLength(64);
		name.setResponder(value -> create.active = !value.isBlank());
		addRenderableWidget(name);
		create = addRenderableWidget(Button.builder(Component.translatable("buildingplanner.ui.start_plan.create"),
			button -> create()).bounds(x, y + 46, 98, 20).build());
		create.active = false;
		addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
			.bounds(x + 102, y + 46, 98, 20).build());
		setInitialFocus(name);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isConfirmation() && create.active) {
			create();
			return true;
		}
		return super.keyPressed(event);
	}

	private void create() {
		String planName = name.getValue().strip();
		onClose();
		LitematicaSchematic empty = LitematicaSchematic.createEmptySchematic(area,
			minecraft.player == null ? "" : minecraft.player.getName().getString());
		if (empty == null) {
			return;
		}
		empty.getMetadata().setName(planName);
		Path folder = DataManager.getSchematicsBaseDirectory().resolve(FOLDER);
		String fileName = freeFileName(folder, planName);
		// Litematica reports its own errors when it can't write or read the file.
		if (!empty.writeToFile(folder, fileName, false)) {
			return;
		}
		LitematicaSchematic schematic = SchematicHolder.getInstance().getOrLoad(folder.resolve(fileName + EXTENSION));
		if (schematic == null) {
			return;
		}
		SchematicPlacement placement = SchematicPlacement.createFor(schematic, area.getEffectiveOrigin(), planName,
			true, true);
		DataManager.getSchematicPlacementManager().addSchematicPlacement(placement, false);
		ClientSession.share(placement);
	}

	/** A file name from the plan's name that no file in {@code folder} has yet. */
	private static String freeFileName(Path folder, String planName) {
		String base = FileNameUtils.generateSafeFileName(FileNameUtils.generateSimpleUnicodeSafeFileName(planName)).strip();
		if (base.isEmpty()) {
			base = "plan";
		}
		String fileName = base;
		for (int n = 2; Files.exists(folder.resolve(fileName + EXTENSION)); n++) {
			fileName = base + " " + n;
		}
		return fileName;
	}
}
