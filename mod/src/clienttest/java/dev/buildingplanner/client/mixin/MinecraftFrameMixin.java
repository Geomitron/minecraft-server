package dev.buildingplanner.client.mixin;

import dev.buildingplanner.client.FrameProbe;
import dev.buildingplanner.client.MenuFrames;
import dev.buildingplanner.client.ScreenProbe;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells the driver where each pass of the game's loop begins and ends, for shots of single frames, turning the view
 * frame by frame and timing frames.
 */
@Mixin(Minecraft.class)
abstract class MinecraftFrameMixin {
	@Inject(method = "runTick", at = @At("HEAD"))
	private void buildingplanner_clienttest$beforeFrame(boolean advanceGameTime, CallbackInfo ci) {
		MenuFrames.beforeFrame((Minecraft) (Object) this);
		FrameProbe.beforeFrame((Minecraft) (Object) this);
	}

	@Inject(method = "runTick", at = @At("RETURN"))
	private void buildingplanner_clienttest$afterFrame(boolean advanceGameTime, CallbackInfo ci) {
		MenuFrames.afterFrame((Minecraft) (Object) this);
		ScreenProbe.frameDrawn();
	}
}
