package dev.buildingplanner.mixin.effortless;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import nl.requios.effortlessbuilding.buildpipeline.BuildPipeline;
import nl.requios.effortlessbuilding.buildpipeline.BuildPipelineClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Effortless Building's private {@code resolveFirstClickPos}, the position its preview draws its first block at for an
 * aim, which H locks the one ghost block at (see "One ghost block").
 */
@Mixin(value = BuildPipelineClient.class, remap = false)
public interface BuildPipelineClientAccessor {
	@Invoker("resolveFirstClickPos")
	static BlockPos buildingplanner$resolveFirstClickPos(BlockHitResult hit, BuildPipeline.BuildState action,
		Level level) {
		throw new AssertionError();
	}
}
