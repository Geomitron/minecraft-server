package dev.buildingplanner.mixin.litematica;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.buildingplanner.client.MaterialListButtons;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hook L10: for Building Planner's material list only, the screen has no "Export" button, no export type button
 * ("Write to file") and no "Litematica menu" button; "Save list" and "Saved lists" go where the first two were, in the
 * same row, which {@code createButtons} moves to the bottom left on a narrow screen. Other material lists keep their
 * buttons.
 */
@Mixin(value = GuiMaterialList.class, remap = false)
abstract class GuiMaterialListMixin {
	/** The export buttons, which {@code createButton} adds; it names their type only in a listener of its own. */
	@WrapOperation(method = "createButton", at = @At(value = "INVOKE",
		target = "Lfi/dy/masa/litematica/gui/GuiMaterialList;addButton(Lfi/dy/masa/malilib/gui/button/ButtonBase;Lfi/dy/masa/malilib/gui/button/IButtonActionListener;)Lfi/dy/masa/malilib/gui/button/ButtonBase;"))
	private ButtonBase buildingplanner$listButtons(GuiMaterialList gui, ButtonBase button, IButtonActionListener listener,
		Operation<ButtonBase> original) {
		return MaterialListButtons.replaceExport(gui, button, listener) ? button : original.call(gui, button, listener);
	}

	/** The "Litematica menu" button, the only button {@code createButtons} adds itself. */
	@WrapOperation(method = "createButtons", at = @At(value = "INVOKE",
		target = "Lfi/dy/masa/litematica/gui/GuiMaterialList;addButton(Lfi/dy/masa/malilib/gui/button/ButtonBase;Lfi/dy/masa/malilib/gui/button/IButtonActionListener;)Lfi/dy/masa/malilib/gui/button/ButtonBase;"))
	private ButtonBase buildingplanner$noMenuButton(GuiMaterialList gui, ButtonBase button, IButtonActionListener listener,
		Operation<ButtonBase> original) {
		return MaterialListButtons.ours(gui) ? button : original.call(gui, button, listener);
	}
}
