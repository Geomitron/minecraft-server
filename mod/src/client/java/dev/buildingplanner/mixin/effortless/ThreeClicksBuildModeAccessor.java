package dev.buildingplanner.mixin.effortless;

import nl.requios.effortlessbuilding.buildmode.ThreeClicksBuildMode;
import nl.requios.effortlessbuilding.utilities.BlockEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Hook E33: the positions the first and second clicks of a mode of three clicks (Cube, Cylinder, Sphere, Slope Floor,
 * Diagonal Line, Diagonal Wall) have set, which the nudge keys move together while the lock is on during the shape
 * (see "Locking and the first click").
 */
@Mixin(value = ThreeClicksBuildMode.class, remap = false)
public interface ThreeClicksBuildModeAccessor {
	@Accessor("firstBlockEntry")
	BlockEntry buildingplanner$firstBlockEntry();

	@Accessor("firstBlockEntry")
	void buildingplanner$setFirstBlockEntry(BlockEntry entry);

	@Accessor("secondBlockEntry")
	BlockEntry buildingplanner$secondBlockEntry();

	@Accessor("secondBlockEntry")
	void buildingplanner$setSecondBlockEntry(BlockEntry entry);
}
