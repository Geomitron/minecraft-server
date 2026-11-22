package dev.buildingplanner.mixin.minecraft;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * The item's own placing state, which {@code BlockItem.place} asks for, so a turn in Disable starts from the block as
 * Minecraft's right click would place it (see "Turning real blocks in Disable"), and its check that a state can go
 * where it's placed, which Disable's preview asks of the turned state as hook S2 does. Called here, outside
 * {@code place}, hook S2 doesn't turn it.
 */
@Mixin(BlockItem.class)
public interface BlockItemInvoker {
	@Invoker("getPlacementState")
	BlockState buildingplanner$placementState(BlockPlaceContext context);

	@Invoker("canPlace")
	boolean buildingplanner$canPlace(BlockPlaceContext context, BlockState state);
}
