package dev.buildingplanner.mixin.minecraft;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The level's record of the blocks the game placed before the server answered, which is package-private. */
@Mixin(ClientLevel.class)
public interface ClientLevelInvoker {
	@Invoker("getBlockStatePredictionHandler")
	BlockStatePredictionHandler buildingplanner$predictions();
}
