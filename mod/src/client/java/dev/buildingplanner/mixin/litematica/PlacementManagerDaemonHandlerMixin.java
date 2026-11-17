package dev.buildingplanner.mixin.litematica;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.buildingplanner.client.RebuildOrder;
import fi.dy.masa.litematica.schematic.placement.PlacementManagerDaemonHandler;
import fi.dy.masa.litematica.schematic.placement.PlacementManagerTask;
import java.util.concurrent.LinkedBlockingQueue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Hook L9: a running rebuild drops only the queued rebuilds of its column made before it began, whose changes it reads
 * (see RebuildOrder). Elsewhere, as when a change queues a new rebuild in place of the queued ones, these drop every
 * rebuild of the column, as before.
 */
@Mixin(value = PlacementManagerDaemonHandler.class, remap = false)
abstract class PlacementManagerDaemonHandlerMixin {
	@Shadow
	@Final
	private LinkedBlockingQueue<PlacementManagerTask> queueRebuild;

	@Shadow
	@Final
	private LinkedBlockingQueue<PlacementManagerTask> deferredQueue;

	@WrapMethod(method = "removeRebuildTasksFor")
	private void buildingplanner$keepNewerRebuilds(int x, int z, Operation<Void> original) {
		if (RebuildOrder.inRebuild()) {
			queueRebuild.removeIf(task -> RebuildOrder.drops(task, x, z));
		} else {
			original.call(x, z);
		}
	}

	@WrapMethod(method = "removeDeferredTasksFor")
	private void buildingplanner$keepNewerDeferred(int x, int z, Operation<Void> original) {
		if (RebuildOrder.inRebuild()) {
			deferredQueue.removeIf(task -> RebuildOrder.drops(task, x, z));
		} else {
			original.call(x, z);
		}
	}
}
