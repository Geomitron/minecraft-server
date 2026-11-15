package dev.buildingplanner.mixin.syncmatica;

import ch.endte.syncmatica.network.SyncmaticaPacket;
import ch.endte.syncmatica.network.handler.ClientPlayHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hook S3: Syncmatica's client handles each of its packets on the client's main thread. Its own mixin catches some
 * packets on the network thread, and handling them there loads and unloads plans in Litematica while the render thread
 * draws their boxes, which crashes the game. Such a packet goes back into the game's packet queue, as the game does
 * with its own packets, so it's handled on the main thread in the order it arrived. Each packet carries its own copy
 * of its bytes, so it can wait.
 */
@Mixin(value = ClientPlayHandler.class, remap = false)
abstract class ClientPlayHandlerMixin {
	@Inject(method = "decodeSyncData", at = @At("HEAD"), cancellable = true)
	private static void buildingplanner$onMainThread(SyncmaticaPacket data, ClientPacketListener handler,
		CallbackInfo ci) {
		Minecraft minecraft = Minecraft.getInstance();
		PacketProcessor packets = minecraft.packetProcessor();
		if (packets.isSameThread()) {
			return;
		}
		if (handler != null) {
			packets.scheduleIfPossible(handler, new ClientboundCustomPayloadPacket(new SyncmaticaPacket.Payload(data)));
		} else {
			minecraft.execute(() -> ClientPlayHandler.decodeSyncData(data, null));
		}
		ci.cancel();
	}
}
