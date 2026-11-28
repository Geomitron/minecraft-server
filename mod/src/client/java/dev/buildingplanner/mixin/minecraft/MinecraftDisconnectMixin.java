package dev.buildingplanner.mixin.minecraft;

import dev.buildingplanner.client.BuildingPlannerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hook M10: every leave, the player's own or after the server closes the connection, runs this method on the render
 * thread, which ends the session at its head, before it drops the game's queued tasks (among them the end that Fabric
 * API's {@code DISCONNECT} queued from the network thread) and takes the level away.
 */
@Mixin(Minecraft.class)
abstract class MinecraftDisconnectMixin {
	@Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("HEAD"))
	private void buildingplanner$endSession(Screen next, boolean keepResourcePacks, boolean stopSound,
		CallbackInfo ci) {
		BuildingPlannerClient.disconnecting();
	}
}
