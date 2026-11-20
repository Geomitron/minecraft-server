package dev.buildingplanner.client;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListHudRenderer;
import fi.dy.masa.malilib.config.HudAlignment;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Litematica's Info HUD for Building Planner's material list ({@link GhostMaterialList#getHudRenderer}): its switch is
 * the material list panel's, which the session's {@link ListSync} keeps, and it draws nothing, since the panel
 * ({@link MaterialPanel}) draws the list. So the screen's "Info HUD" button turns the panel on and off, and its label
 * ("Info HUD: ON" or "OFF") shows the panel's state. Without a session the switch reads off and doesn't turn.
 */
final class PanelSwitch extends MaterialListHudRenderer {
	PanelSwitch(MaterialListBase list) {
		super(list);
	}

	@Override
	public boolean getShouldRenderCustom() {
		ClientSession session = BuildingPlannerClient.session();
		return session != null && session.lists().panel();
	}

	@Override
	public void toggleShouldRender() {
		ClientSession session = BuildingPlannerClient.session();
		if (session != null) {
			session.lists().setPanel(!session.lists().panel());
		}
	}

	@Override
	public int render(GuiGraphicsExtractor graphics, int xOffset, int yOffset, HudAlignment alignment) {
		return 0;
	}
}
