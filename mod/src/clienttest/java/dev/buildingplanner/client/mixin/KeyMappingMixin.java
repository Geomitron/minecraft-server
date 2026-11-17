package dev.buildingplanner.client.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.buildingplanner.client.SentPackets;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells the driver each key or mouse button press the game handles, which it does once a frame, so a check can wait
 * until a real key press has reached the game before it looks at what the press did.
 */
@Mixin(KeyMapping.class)
abstract class KeyMappingMixin {
	@Inject(method = "click", at = @At("HEAD"))
	private static void buildingplanner_clienttest$count(InputConstants.Key key, CallbackInfo ci) {
		SentPackets.pressed();
	}
}
