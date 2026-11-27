package dev.buildingplanner.client.mixin;

import dev.buildingplanner.client.SentPackets;
import dev.buildingplanner.sync.Payloads;
import java.util.List;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tells the driver which custom payloads leave the client, so a check can prove that one never did. */
@Mixin(Connection.class)
abstract class ConnectionMixin {
	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
		at = @At("HEAD"))
	private void buildingplanner_clienttest$count(Packet<?> packet, ChannelFutureListener listener, boolean flush,
		CallbackInfo ci) {
		if (packet instanceof ServerboundCustomPayloadPacket custom) {
			var id = custom.payload().type().id();
			SentPackets.sent(id.getNamespace(), id.getPath(),
				custom.payload() instanceof Payloads.Edit edit ? edit.changes()
					: custom.payload() instanceof Payloads.Place place ? place.blocks() : List.of());
		}
	}
}
