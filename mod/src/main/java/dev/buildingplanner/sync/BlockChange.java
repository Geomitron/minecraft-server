package dev.buildingplanner.sync;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** One block of an edit: the region it belongs to, its plan position and its new state. */
public record BlockChange(String region, BlockPos pos, BlockState state) {
}
