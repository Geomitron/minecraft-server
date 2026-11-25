package dev.buildingplanner.client.mixin;

import dev.buildingplanner.client.SentPackets;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells the driver each press of Esc the game handles, which reaches no key mapping, so a check can wait until a real
 * Esc has reached the game, as {@link KeyMappingMixin} does for the other keys.
 */
@Mixin(KeyboardHandler.class)
abstract class KeyboardHandlerMixin {
	@Inject(method = "keyPress", at = @At("HEAD"))
	private void buildingplanner_clienttest$countEsc(long window, int action, KeyEvent event, CallbackInfo ci) {
		if (action == 1 && event.isEscape()) {
			SentPackets.pressed();
		}
	}
}
