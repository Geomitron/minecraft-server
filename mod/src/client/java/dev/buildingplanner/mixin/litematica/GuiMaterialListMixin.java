package dev.buildingplanner.mixin.litematica;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.buildingplanner.client.MaterialListButtons;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Hooks L10 and L12: for Building Planner's material list only, the screen has no "Export" button, no export type
 * button ("Write to file") and no "Litematica menu" button; "Save list" and "Saved lists" go where the first two were,
 * in the same row, which {@code createButtons} moves to the bottom left on a narrow screen, and "Close" where
 * "Litematica menu" was. Its (i) icon shows Building Planner's text. Other material lists keep their buttons and text.
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
	private ButtonBase buildingplanner$closeButton(GuiMaterialList gui, ButtonBase button, IButtonActionListener listener,
		Operation<ButtonBase> original) {
		return MaterialListButtons.replaceMenu(gui, button) ? button : original.call(gui, button, listener);
	}

	/** The translation key of the (i) icon's text, which {@code createMultiplier} gives the icon. */
	@ModifyArg(method = "createMultiplier", at = @At(value = "INVOKE",
		target = "Lfi/dy/masa/malilib/gui/widgets/WidgetInfoIcon;<init>(IILfi/dy/masa/malilib/gui/interfaces/IGuiIcon;Ljava/lang/String;[Ljava/lang/Object;)V"),
		index = 3)
	private String buildingplanner$infoText(String key) {
		return MaterialListButtons.ours((GuiMaterialList) (Object) this) ? MaterialListButtons.INFO : key;
	}
}
