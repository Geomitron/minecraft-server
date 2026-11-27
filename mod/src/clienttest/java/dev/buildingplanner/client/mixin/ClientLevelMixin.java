package dev.buildingplanner.client.mixin;

import dev.buildingplanner.client.SoundProbe;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells the driver each sound the game plays at a position in its world: the sounds the server sends
 * ({@code playSeededSound}) and the game's own ({@code playLocalSound}) both play through the private
 * {@code playSound}.
 */
@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {
	@Inject(method = "playSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZJ)V",
		at = @At("HEAD"))
	private void buildingplanner_clienttest$played(double x, double y, double z, SoundEvent sound, SoundSource source,
		float volume, float pitch, boolean distanceDelay, long seed, CallbackInfo ci) {
		SoundProbe.played(sound.location().toString());
	}
}
