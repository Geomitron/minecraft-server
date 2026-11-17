package dev.buildingplanner.mixin.litematica;

import dev.buildingplanner.client.RebuildOrder;
import fi.dy.masa.litematica.schematic.placement.PlacementManagerTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/** Hooks L8 and L9: numbers each task of Litematica's placement manager in the order they're made (see RebuildOrder). */
@Mixin(value = PlacementManagerTask.class, remap = false)
abstract class PlacementManagerTaskMixin implements RebuildOrder.Numbered {
	@Unique
	private final long buildingplanner$number = RebuildOrder.next();

	@Shadow
	protected abstract int cx();

	@Shadow
	protected abstract int cz();

	@Override
	public int buildingplanner$chunkX() {
		return cx();
	}

	@Override
	public int buildingplanner$chunkZ() {
		return cz();
	}

	@Override
	public long buildingplanner$number() {
		return buildingplanner$number;
	}
}
