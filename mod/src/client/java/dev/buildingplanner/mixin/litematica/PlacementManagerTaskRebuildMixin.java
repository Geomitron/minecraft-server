package dev.buildingplanner.mixin.litematica;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.buildingplanner.client.RebuildOrder;
import fi.dy.masa.litematica.schematic.placement.PlacementManagerTaskRebuild;
import org.spongepowered.asm.mixin.Mixin;

/** Hook L8: Litematica's rebuilds of one chunk column run one at a time (see RebuildOrder). */
@Mixin(value = PlacementManagerTaskRebuild.class, remap = false)
abstract class PlacementManagerTaskRebuildMixin {
	@WrapMethod(method = "run")
	private void buildingplanner$oneAtATime(Operation<Void> original) {
		RebuildOrder.Numbered task = (RebuildOrder.Numbered) this;
		RebuildOrder.run(task.buildingplanner$chunkX(), task.buildingplanner$chunkZ(), () -> original.call());
	}
}
