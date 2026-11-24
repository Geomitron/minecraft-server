package dev.buildingplanner.mixin.effortless;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.buildingplanner.client.RadialMenuWidgets;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import nl.requios.effortlessbuilding.screen.RadialMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks E11, E12, E16, E18, E19, E20 and E34: Building Planner's widgets in Effortless Building's radial menu, and its name
 * in the menu's corner (E18). Minecraft clears a screen's widgets and calls {@code init} each time it opens, so E11
 * adds them there; as children of the screen they
 * get the clicks and drags the menu passes on to {@code Screen}. The menu draws itself without {@code Screen}'s own
 * drawing, so E12 places them for the menu's opening animation, read from its {@code visibility} (E20), and draws them,
 * after the menu's ring and buttons and before its texts, so a hovered mode's name, which can fall on the sliders,
 * shows over them. The menu has no widgets of its own. The menu acts on its own button under the pointer in
 * {@code onClose}, which its {@code tick} calls once its key is let go, after closing so the action can open another
 * screen; E16 lets Building Planner's buttons act there too, after the menu's own action. E19 leaves every mode segment
 * unselected while Building Planner's mouse mode is on. E34 leaves out the "Functional Settings" button the menu shows
 * operators under its wrench during a session, where "Show or hide ghost blocks" sits.
 */
@Mixin(value = RadialMenu.class, remap = false)
abstract class RadialMenuMixin extends Screen {
	@Shadow
	private float visibility;

	/** The action of Building Planner's button under the pointer as {@code onClose} began, or null. */
	@Unique
	private Runnable buildingplanner$closingAction;

	private RadialMenuMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void buildingplanner$addWidgets(CallbackInfo ci) {
		RadialMenuWidgets.create().forEach(this::addRenderableWidget);
		RadialMenuWidgets.place(children(), width, height, visibility);
	}

	@Inject(method = "onClose", at = @At("HEAD"))
	private void buildingplanner$findClosingAction(CallbackInfo ci) {
		buildingplanner$closingAction =
			RadialMenuWidgets.menuClosing(children(), ((RadialMenu) (Object) this).performedActionUsingMouse);
	}

	@Inject(method = "onClose", at = @At("TAIL"))
	private void buildingplanner$actOnClose(CallbackInfo ci) {
		Runnable action = buildingplanner$closingAction;
		buildingplanner$closingAction = null;
		if (action != null) {
			action.run();
		}
	}

	@Inject(method = "extractRenderState", at = @At(value = "INVOKE",
		target = "Lnl/requios/effortlessbuilding/screen/RadialMenu;drawTexts"))
	private void buildingplanner$drawWidgets(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks,
		CallbackInfo ci) {
		RadialMenuWidgets.place(children(), width, height, visibility);
		for (GuiEventListener child : children()) {
			if (child instanceof Renderable widget) {
				widget.extractRenderState(graphics, mouseX, mouseY, partialTicks);
			}
		}
	}

	@ModifyExpressionValue(method = "drawRadialButtonBackgrounds", at = @At(value = "INVOKE",
		target = "Lnl/requios/effortlessbuilding/buildmode/BuildModeEnum;ordinal()I"))
	private int buildingplanner$selectedSegment(int modeIndex) {
		return RadialMenuWidgets.selectedSegment(modeIndex);
	}

	@ModifyExpressionValue(method = "extractRenderState", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/server/permissions/PermissionSet;hasPermission(Lnet/minecraft/server/permissions/Permission;)Z"))
	private boolean buildingplanner$functionalSettings(boolean operator) {
		return RadialMenuWidgets.showFunctionalSettings(operator);
	}

	@ModifyConstant(method = "drawTexts", constant = @Constant(stringValue = "Effortless Building"))
	private String buildingplanner$credits(String effortlessBuilding) {
		return I18n.get("buildingplanner.ui.radial.credits");
	}
}
