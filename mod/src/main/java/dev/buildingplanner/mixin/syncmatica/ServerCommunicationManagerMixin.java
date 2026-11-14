package dev.buildingplanner.mixin.syncmatica;

import ch.endte.syncmatica.communication.ExchangeTarget;
import ch.endte.syncmatica.communication.ServerCommunicationManager;
import ch.endte.syncmatica.network.PacketType;
import dev.buildingplanner.server.ServerSync;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hook S1, the delete check: a player's delete reaches Syncmatica only if they may delete that plan. */
@Mixin(value = ServerCommunicationManager.class, remap = false)
abstract class ServerCommunicationManagerMixin {
	@Inject(method = "handle", at = @At("HEAD"), cancellable = true)
	private void buildingplanner$checkDelete(ExchangeTarget source, PacketType type, FriendlyByteBuf packetBuf,
		CallbackInfo ci) {
		if (type != PacketType.REMOVE_SYNCMATIC) {
			return;
		}
		// Read the plan's id and leave it for Syncmatica.
		int start = packetBuf.readerIndex();
		UUID placementId = packetBuf.readUUID();
		packetBuf.readerIndex(start);
		if (!ServerSync.mayDelete(((ServerCommunicationManager) (Object) this).getGameProfile(source), placementId)) {
			ci.cancel();
		}
	}
}
