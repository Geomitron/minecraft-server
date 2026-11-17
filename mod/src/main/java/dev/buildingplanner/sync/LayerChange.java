package dev.buildingplanner.sync;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** One block of an edit to the layer: its world position and its new state, air for no ghost block. */
public record LayerChange(BlockPos pos, BlockState state) {
}
