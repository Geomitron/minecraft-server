package dev.buildingplanner.mixin.minecraft;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.buildingplanner.client.ShapeOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hook M9: Minecraft's checks of where a block can go read the world through {@code getBlockState} and
 * {@code getFluidState}, so while the preview works a shape out in the server's order, the shape's blocks placed so far
 * stand there ({@link ShapeOverlay}).
 */
@Mixin(Level.class)
abstract class LevelMixin {
	@ModifyReturnValue(method = "getBlockState", at = @At("RETURN"))
	private BlockState buildingplanner$shapeBlock(BlockState real, @Local(argsOnly = true) BlockPos pos) {
		return ShapeOverlay.state((Level) (Object) this, pos, real);
	}

	@ModifyReturnValue(method = "getFluidState", at = @At("RETURN"))
	private FluidState buildingplanner$shapeFluid(FluidState real, @Local(argsOnly = true) BlockPos pos) {
		return ShapeOverlay.fluid((Level) (Object) this, pos, real);
	}
}
