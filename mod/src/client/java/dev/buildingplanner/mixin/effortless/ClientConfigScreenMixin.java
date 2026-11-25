package dev.buildingplanner.mixin.effortless;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.buildingplanner.client.VisualSettingsRow;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nl.requios.effortlessbuilding.screen.ClientConfigScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hook E36: the "Match ghost blocks" row in Effortless Building's "Visual Settings" screen (see "Settings"). The screen
 * lays out its rows, draws their labels and shows their tooltips from its list of label keys ({@code LABEL_KEYS}), so
 * the row's key added to that list in each of those methods gives the row its place, label and tooltip; {@code init}
 * then adds the row's switch, drawn as the screen's own ("ON" or "OFF", {@code onOff}), below which "Save" and
 * "Cancel" move a row down. The switch changes only the screen's copy, which {@code save} saves with the screen's own
 * settings; "Cancel" leaves the setting as it was.
 */
@Mixin(value = ClientConfigScreen.class, remap = false)
abstract class ClientConfigScreenMixin extends Screen {
	@Shadow
	@Final
	private static String[] LABEL_KEYS;

	@Unique
	private boolean buildingplanner$matchGhost;

	private ClientConfigScreenMixin(Component title) {
		super(title);
	}

	@Shadow
	private static String onOff(boolean value) {
		throw new AssertionError();
	}

	@ModifyExpressionValue(method = {"init", "extractRenderState", "renderRowTooltips"}, at = @At(value = "FIELD",
		target = "Lnl/requios/effortlessbuilding/screen/ClientConfigScreen;LABEL_KEYS:[Ljava/lang/String;"))
	private String[] buildingplanner$addRow(String[] labels) {
		return VisualSettingsRow.withRow(labels);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void buildingplanner$addSwitch(CallbackInfo ci) {
		buildingplanner$matchGhost = VisualSettingsRow.saved();
		int rowY = VisualSettingsRow.rowY(height, LABEL_KEYS.length + 1);
		VisualSettingsRow.makeRoom(children(), rowY);
		addRenderableWidget(Button.builder(Component.literal(onOff(buildingplanner$matchGhost)), button -> {
			buildingplanner$matchGhost = !buildingplanner$matchGhost;
			button.setMessage(Component.literal(onOff(buildingplanner$matchGhost)));
		}).bounds(VisualSettingsRow.switchX(width), rowY, VisualSettingsRow.SWITCH_WIDTH, VisualSettingsRow.SWITCH_HEIGHT)
			.build());
	}

	@Inject(method = "save", at = @At("HEAD"))
	private void buildingplanner$save(CallbackInfo ci) {
		VisualSettingsRow.save(buildingplanner$matchGhost);
	}
}
