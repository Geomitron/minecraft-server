package dev.buildingplanner.mixin.minecraft;

import dev.buildingplanner.server.ServerSync;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hook S1: every change to a block in a loaded chunk goes through {@code setBlockState}, whatever makes it (a player, a
 * command, a piston, flowing water, a growing plant). It returns the block it replaced, or null when nothing changed. It
 * acts for a server's levels alone, a dedicated server's or the game's own server's in the player's own world.
 */
@Mixin(LevelChunk.class)
abstract class LevelChunkMixin {
	@Inject(method = "setBlockState", at = @At("RETURN"))
	private void buildingplanner$realChanged(BlockPos pos, BlockState state, int flags,
		CallbackInfoReturnable<BlockState> cir) {
		if (cir.getReturnValue() != null && ((LevelChunk) (Object) this).getLevel() instanceof ServerLevel level) {
			ServerSync.realChanged(level, pos, state);
		}
	}
}
