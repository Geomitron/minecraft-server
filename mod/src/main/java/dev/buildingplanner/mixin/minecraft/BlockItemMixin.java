package dev.buildingplanner.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.buildingplanner.input.BlockTurn;
import dev.buildingplanner.server.PlaceTurns;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hook S2: every block item's right click places through {@code BlockItem.place}, which asks the item for the state
 * to place ({@code getPlacementState}, after the item's {@code updatePlacementContext}), on the server and in the
 * client's guess of the block alike. While the player has a turn for the item in the main hand (see "Turning real
 * blocks in Disable"), the state takes it, its neighbours set its shape again, and it places only where the turned
 * block can stay: otherwise {@code place} gets null and places nothing, and the player keeps the item.
 */
@Mixin(BlockItem.class)
abstract class BlockItemMixin {
	@Shadow
	protected abstract boolean canPlace(BlockPlaceContext context, BlockState state);

	@WrapOperation(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BlockItem;"
		+ "getPlacementState(Lnet/minecraft/world/item/context/BlockPlaceContext;)"
		+ "Lnet/minecraft/world/level/block/state/BlockState;"))
	private BlockState buildingplanner$turn(BlockItem item, BlockPlaceContext context, Operation<BlockState> original) {
		BlockState placed = original.call(item, context);
		Player player = context.getPlayer();
		if (placed == null || player == null) {
			return placed;
		}
		BlockTurn.Held turn = context.getLevel().isClientSide() ? PlaceTurns.local() : PlaceTurns.of(player.getUUID());
		PlaceTurns.Placed turned = PlaceTurns.place(context, placed, turn, state -> canPlace(context, state));
		return turned.stays() ? turned.state() : null;
	}
}
