package dev.buildingplanner.mixin.minecraft;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.buildingplanner.input.BlockTurn;
import dev.buildingplanner.server.PlaceTurns;
import dev.buildingplanner.server.RealPlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hooks S2 and S3: every block item's right click places through {@code BlockItem.place}, which asks the item for the
 * state to place ({@code getPlacementState}, after the item's {@code updatePlacementContext}), on the server and in the
 * client's guess of the block alike, and so does the server's placing of a shape's or a paste's real blocks
 * ({@link RealPlacer}).
 */
@Mixin(BlockItem.class)
abstract class BlockItemMixin {
	@Shadow
	protected abstract boolean canPlace(BlockPlaceContext context, BlockState state);

	/**
	 * Hook S2. While the server places a real block of a shape or a paste, the state takes the shape's form,
	 * orientation and count ({@link RealPlacer#state}). Otherwise, while the player has a turn for the item in the main
	 * hand (see "Turning real blocks in Disable"), the state takes it. Either way, its neighbours set its shape again,
	 * and it places only where the block can stay: otherwise {@code place} gets null and places nothing, and the player
	 * keeps the item.
	 */
	@WrapOperation(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BlockItem;"
		+ "getPlacementState(Lnet/minecraft/world/item/context/BlockPlaceContext;)"
		+ "Lnet/minecraft/world/level/block/state/BlockState;"))
	private BlockState buildingplanner$turn(BlockItem item, BlockPlaceContext context, Operation<BlockState> original) {
		BlockState placed = original.call(item, context);
		Player player = context.getPlayer();
		if (player == null) {
			return placed;
		}
		boolean server = !context.getLevel().isClientSide();
		if (server && RealPlacer.places(player, context.getClickedPos())) {
			return RealPlacer.state(context, placed, state -> canPlace(context, state));
		}
		if (placed == null) {
			return null;
		}
		BlockTurn.Held turn = server ? PlaceTurns.of(player.getUUID()) : PlaceTurns.local();
		PlaceTurns.Placed turned = PlaceTurns.place(context, placed, turn, state -> canPlace(context, state));
		return turned.stays() ? turned.state() : null;
	}

	/** Hook S3: while the server places a shape or a paste, only its first block plays the place sound. */
	@WrapWithCondition(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;"
		+ "playSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;"
		+ "Lnet/minecraft/sounds/SoundSource;FF)V"))
	private boolean buildingplanner$oneSound(Level level, Entity entity, BlockPos pos, SoundEvent sound,
		SoundSource source, float volume, float pitch) {
		return level.isClientSide() || RealPlacer.playsSound(level, entity, pos, sound, source, volume, pitch);
	}
}
