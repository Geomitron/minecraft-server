package dev.buildingplanner.mixin.effortless;

import nl.requios.effortlessbuilding.buildmode.TwoClicksBuildMode;
import nl.requios.effortlessbuilding.utilities.BlockEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Hook E33: the position the first click of a mode of two clicks (Line, Wall, Floor, Circle) has set, which the nudge
 * keys move while the lock is on during the shape (see "Locking and the first click").
 */
@Mixin(value = TwoClicksBuildMode.class, remap = false)
public interface TwoClicksBuildModeAccessor {
	@Accessor("firstBlockEntry")
	BlockEntry buildingplanner$firstBlockEntry();

	@Accessor("firstBlockEntry")
	void buildingplanner$setFirstBlockEntry(BlockEntry entry);
}
