package dev.buildingplanner.mixin.effortless;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.buildingplanner.client.ClickRule;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Hook E15: Effortless Building's own mixin lengthens the reach of {@code Minecraft.pick} for an empty hand, at its
 * tail, unless its mode is Disable. Wrapping the whole method, with that mixin's code inside, lets
 * {@link ClickRule#pick} show it Disable while the hand should aim as without mods (with {@link BuildModesMixin}).
 */
@Mixin(Minecraft.class)
abstract class MinecraftMixin {
	@WrapMethod(method = "pick(F)V")
	private void buildingplanner$vanillaReach(float partialTicks, Operation<Void> original) {
		ClickRule.pick(() -> original.call(partialTicks));
	}
}
