package dev.buildingplanner.mixin.minecraft;

import dev.buildingplanner.client.BuildingPlannerClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hook C1: every change to a block in a chunk the game has loaded goes through {@code setBlockState}, whether the
 * server sent it or the game placed the block before the server answered. It returns the block it replaced, or null
 * when nothing changed. During a local session, the game's own level's changes go to its server part.
 */
@Mixin(LevelChunk.class)
abstract class ClientLevelChunkMixin {
	@Inject(method = "setBlockState", at = @At("RETURN"))
	private void buildingplanner$realChangedInGame(BlockPos pos, BlockState state, int flags,
		CallbackInfoReturnable<BlockState> cir) {
		if (cir.getReturnValue() != null && ((LevelChunk) (Object) this).getLevel() instanceof ClientLevel level) {
			BuildingPlannerClient.realChanged(level, pos, state);
		}
	}
}
