package dev.buildingplanner.mixin.effortless;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.buildingplanner.client.SettingsName;
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
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hook E39: Effortless Building's settings screen is titled "Settings", the title its constructor passes on read from
 * Building Planner's key ({@link SettingsName}). Hook E36: the rows "Match ghost blocks" and "Place real blocks" in that
 * screen (see "Settings"). The screen lays out its rows, draws their labels and shows their tooltips from its list of
 * label keys ({@code LABEL_KEYS}), so the rows' keys added to that list in each of those methods give the rows their
 * places, labels and tooltips; {@code init} then adds each row's switch, drawn as the screen's own ("ON" or "OFF",
 * {@code onOff}), below which "Save" and "Cancel" move down by the rows. A switch changes only the screen's copy,
 * which {@code save} saves with the screen's own settings; "Cancel" leaves the settings as they were.
 */
@Mixin(value = ClientConfigScreen.class, remap = false)
abstract class ClientConfigScreenMixin extends Screen {
	@Shadow
	@Final
	private static String[] LABEL_KEYS;

	@Unique
	private final boolean[] buildingplanner$on = new boolean[VisualSettingsRow.rows().size()];

	private ClientConfigScreenMixin(Component title) {
		super(title);
	}

	@Shadow
	private static String onOff(boolean value) {
		throw new AssertionError();
	}

	@ModifyArg(method = "<init>", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"))
	private static String buildingplanner$settingsTitle(String key) {
		return SettingsName.key(key);
	}

	@ModifyExpressionValue(method = {"init", "extractRenderState", "renderRowTooltips"}, at = @At(value = "FIELD",
		target = "Lnl/requios/effortlessbuilding/screen/ClientConfigScreen;LABEL_KEYS:[Ljava/lang/String;"))
	private String[] buildingplanner$addRow(String[] labels) {
		return VisualSettingsRow.withRows(labels);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void buildingplanner$addSwitches(CallbackInfo ci) {
		VisualSettingsRow.makeRoom(children(), VisualSettingsRow.rows().getFirst().rowY(height, LABEL_KEYS.length));
		for (VisualSettingsRow row : VisualSettingsRow.rows()) {
			int i = row.ordinal();
			buildingplanner$on[i] = row.saved();
			addRenderableWidget(Button.builder(Component.literal(onOff(buildingplanner$on[i])), button -> {
				buildingplanner$on[i] = !buildingplanner$on[i];
				button.setMessage(Component.literal(onOff(buildingplanner$on[i])));
			}).bounds(VisualSettingsRow.switchX(width), row.rowY(height, LABEL_KEYS.length), VisualSettingsRow.SWITCH_WIDTH,
				VisualSettingsRow.SWITCH_HEIGHT).build());
		}
	}

	@Inject(method = "save", at = @At("HEAD"))
	private void buildingplanner$save(CallbackInfo ci) {
		for (VisualSettingsRow row : VisualSettingsRow.rows()) {
			row.save(buildingplanner$on[row.ordinal()]);
		}
	}
}
