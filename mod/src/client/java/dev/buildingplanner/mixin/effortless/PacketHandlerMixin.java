package dev.buildingplanner.mixin.effortless;

import dev.buildingplanner.client.BuildingPlannerClient;
import nl.requios.effortlessbuilding.network.PacketHandler;
import nl.requios.effortlessbuilding.network.RedoPacket;
import nl.requios.effortlessbuilding.network.UndoPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hook E4: Effortless Building's undo and redo (Ctrl+Z, Ctrl+Y and its radial menu's buttons) undo and redo the
 * player's edits to the layer instead, on a server with Building Planner, and send nothing on a server without it.
 */
@Mixin(value = PacketHandler.class, remap = false)
abstract class PacketHandlerMixin {
	@Inject(method = "sendToServer(Lnl/requios/effortlessbuilding/network/UndoPacket;)V", at = @At("HEAD"),
		cancellable = true)
	private static void buildingplanner$undo(UndoPacket packet, CallbackInfo ci) {
		if (BuildingPlannerClient.undo()) {
			ci.cancel();
		}
	}

	@Inject(method = "sendToServer(Lnl/requios/effortlessbuilding/network/RedoPacket;)V", at = @At("HEAD"),
		cancellable = true)
	private static void buildingplanner$redo(RedoPacket packet, CallbackInfo ci) {
		if (BuildingPlannerClient.redo()) {
			ci.cancel();
		}
	}
}
