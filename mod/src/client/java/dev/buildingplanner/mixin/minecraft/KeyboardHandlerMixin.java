package dev.buildingplanner.mixin.minecraft;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import dev.buildingplanner.client.BuildingPlannerClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hook M7: during a session, Esc with no screen open takes the next step out of Building Planner's and Effortless
 * Building's modes and doesn't pause; with no step left, it pauses as usual (see "Esc"). {@code keyPress} calls
 * {@code pauseGame} only for Esc, with no screen open or a pause screen without its menu; {@code pauseOnly} is true while
 * the debug key (F3) is held, and {@code action}, the method's only int argument, is GLFW's press (1) or repeat (2).
 */
@Mixin(KeyboardHandler.class)
abstract class KeyboardHandlerMixin {
	@WrapWithCondition(method = "keyPress", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/Minecraft;pauseGame(Z)V"))
	private boolean buildingplanner$escSteps(Minecraft minecraft, boolean pauseOnly,
		@Local(argsOnly = true) int action) {
		return pauseOnly || BuildingPlannerClient.escape(action == 2);
	}
}
