package dev.buildingplanner.mixin.effortless;

import dev.buildingplanner.client.BuildingPlannerClient;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import nl.requios.effortlessbuilding.buildpipeline.BuildPipeline;
import nl.requios.effortlessbuilding.buildpipeline.RandomizerSystem;
import nl.requios.effortlessbuilding.utilities.BlockEntry;
import nl.requios.effortlessbuilding.utilities.BlockSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hook E5: while the mix is on, each position of a shape gets its block from the mix, by a fixed hash of the
 * position, so the preview and the plan agree.
 */
@Mixin(value = RandomizerSystem.class, remap = false)
abstract class RandomizerSystemMixin {
	@Inject(method = "processBlocks", at = @At("HEAD"), cancellable = true)
	private void buildingplanner$mix(BlockSet blocks, Player player, BuildPipeline.BuildState action,
		CallbackInfo ci) {
		if (action != BuildPipeline.BuildState.PLACING || !BuildingPlannerClient.mixOn()) {
			return;
		}
		for (BlockEntry entry : blocks.values()) {
			Block block = BuildingPlannerClient.mixBlockAt(entry.blockPos);
			entry.item = block.asItem();
			entry.blockState = block.defaultBlockState();
		}
		ci.cancel();
	}
}
