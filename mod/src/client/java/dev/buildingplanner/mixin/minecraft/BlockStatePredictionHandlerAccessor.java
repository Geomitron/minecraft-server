package dev.buildingplanner.mixin.minecraft;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The positions of the blocks the game placed that the server hasn't answered for yet, by {@code BlockPos.asLong},
 * each with the server's last state there; {@code endPredictionsUpTo} removes each as the server's answer arrives. Its
 * values are of a private class, so the map is named here without them.
 */
@Mixin(BlockStatePredictionHandler.class)
public interface BlockStatePredictionHandlerAccessor {
	@Accessor("serverVerifiedStates")
	Long2ObjectOpenHashMap<?> buildingplanner$serverVerifiedStates();
}
