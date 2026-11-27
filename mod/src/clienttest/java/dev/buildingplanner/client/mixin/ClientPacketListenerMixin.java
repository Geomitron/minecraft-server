package dev.buildingplanner.client.mixin;

import dev.buildingplanner.client.SoundProbe;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells the driver each sound the server sends to play at a position, once it reaches the game's own thread
 * ({@code ensureRunningOnSameThread} returns there, and throws on the network thread that hands the packet over).
 */
@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
	@Inject(method = "handleSoundEvent", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
		target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V"))
	private void buildingplanner_clienttest$sent(ClientboundSoundPacket packet, CallbackInfo ci) {
		SoundProbe.sent(packet.getSound().value().location().toString());
	}
}
