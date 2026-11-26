package dev.buildingplanner.mixin.effortless;

import dev.buildingplanner.client.SettingsName;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Hook E39: the radial menu's button that opens Effortless Building's settings screen is named "Settings" (see
 * "Settings"). A button reads its name from its action's key in its constructor, and its description from the
 * description's key; only the settings button's name key changes, and its description, which Effortless Building
 * leaves empty, stays empty.
 */
@Mixin(targets = "nl.requios.effortlessbuilding.screen.RadialMenu$MenuButton", remap = false)
abstract class RadialMenuButtonMixin {
	@ModifyArg(method = "<init>", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/resources/language/I18n;get(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"),
		index = 0)
	private static String buildingplanner$settingsName(String key) {
		return SettingsName.key(key);
	}
}
