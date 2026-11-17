package dev.buildingplanner.mixin.effortless;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.buildingplanner.client.BuildingPlannerClient;
import dev.buildingplanner.client.ClickRule;
import dev.buildingplanner.client.EffortlessShapes;
import java.util.Collection;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import nl.requios.effortlessbuilding.buildpipeline.BuildPipelineClient;
import nl.requios.effortlessbuilding.network.BreakBuildModePacket;
import nl.requios.effortlessbuilding.network.PlaceBuildModePacket;
import nl.requios.effortlessbuilding.utilities.BlockSet;
import nl.requios.effortlessbuilding.utilities.ItemUsageTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Effortless Building's client pipeline, on a server with Building Planner: a finished shape goes into the layer
 * instead of to the server as real blocks (hooks E1 to E3), the mix starts shapes (E6), the preview has no "missing items"
 * tint (E8), shapes can start on ghost blocks (E9), and clicks without a placeable item stay vanilla's (E14).
 */
@Mixin(value = BuildPipelineClient.class, remap = false)
abstract class BuildPipelineClientMixin {
	/**
	 * Hook E1: the finished shape goes into the layer, not to the server as real blocks; with Building Planner off on
	 * a server, it goes nowhere.
	 */
	@WrapWithCondition(method = "handleClick", at = @At(value = "INVOKE",
		target = "Lnl/requios/effortlessbuilding/network/PacketHandler;sendToServer(Lnl/requios/effortlessbuilding/network/PlaceBuildModePacket;)V"))
	private static boolean buildingplanner$place(PlaceBuildModePacket packet, @Local BlockSet blocks) {
		if (!BuildingPlannerClient.active()) {
			return !BuildingPlannerClient.off();
		}
		EffortlessShapes.place(blocks);
		return false;
	}

	/** Hook E2: a shape drawn with the break click erases its ghost blocks. */
	@WrapWithCondition(method = "handleClick", at = @At(value = "INVOKE",
		target = "Lnl/requios/effortlessbuilding/network/PacketHandler;sendToServer(Lnl/requios/effortlessbuilding/network/BreakBuildModePacket;)V"))
	private static boolean buildingplanner$erase(BreakBuildModePacket packet, @Local BlockSet blocks) {
		if (!BuildingPlannerClient.active()) {
			return !BuildingPlannerClient.off();
		}
		EffortlessShapes.erase(blocks);
		return false;
	}

	/** Hook E3: ghost blocks aren't real blocks the player placed. */
	@WrapWithCondition(method = "handleClick", at = @At(value = "INVOKE",
		target = "Lnl/requios/effortlessbuilding/utilities/PlacedBlockTracker;clientTrackAll(Lnet/minecraft/resources/ResourceKey;Ljava/util/Collection;)V"))
	private static boolean buildingplanner$noTracking(ResourceKey<Level> dimension, Collection<BlockPos> positions) {
		return !BuildingPlannerClient.active();
	}

	/** Hook E6: while the mix applies, the held block counts as Effortless Building's Randomizer. */
	@ModifyExpressionValue(method = {"shouldInterceptPlacing", "updateDisplayTrackers"}, at = @At(value = "CONSTANT",
		args = "classValue=nl/requios/effortlessbuilding/item/RandomizerToolItem"), require = 2)
	private static boolean buildingplanner$mixIsRandomizer(boolean isRandomizer) {
		return isRandomizer || BuildingPlannerClient.active() && EffortlessShapes.mixApplies();
	}

	/**
	 * Hook E14: with anything but a placeable item in the main hand, and no shape under way, Effortless Building takes
	 * neither click, so both act as without mods (its client tick, its cancelling of vanilla's use, attack and held
	 * attack, and hold to repeat all ask these first). A shape under way still takes the click that finishes or
	 * cancels it.
	 */
	@ModifyReturnValue(method = {"shouldInterceptPlacing", "shouldInterceptBreaking"}, at = @At("RETURN"), require = 2)
	private static boolean buildingplanner$onlyWithPlaceables(boolean intercept) {
		return intercept && (!BuildingPlannerClient.ownsShapes() || BuildPipelineClient.getBuildState() != null
			|| ClickRule.holdsPlaceable());
	}

	/** Hook E8: ghost blocks don't use up items, so the preview shows none missing. */
	@WrapOperation(method = "updateDisplayTrackers", at = @At(value = "INVOKE",
		target = "Lnl/requios/effortlessbuilding/utilities/ItemUsageTracker;compute(Lnet/minecraft/world/entity/player/Player;Lnl/requios/effortlessbuilding/utilities/BlockSet;Z)V"))
	private static void buildingplanner$noMissingMix(ItemUsageTracker tracker, Player player, BlockSet blocks,
		boolean creative, Operation<Void> original) {
		if (BuildingPlannerClient.active()) {
			tracker.initialize();
		} else {
			original.call(tracker, player, blocks, creative);
		}
	}

	/** Hook E8, for the held block. */
	@WrapOperation(method = "updateDisplayTrackers", at = @At(value = "INVOKE",
		target = "Lnl/requios/effortlessbuilding/utilities/ItemUsageTracker;compute(Lnet/minecraft/world/entity/player/Player;Ljava/util/Collection;Lnet/minecraft/world/item/Item;Z)V"))
	private static void buildingplanner$noMissingHeld(ItemUsageTracker tracker, Player player,
		Collection<BlockPos> positions, Item item, boolean creative, Operation<Void> original) {
		if (BuildingPlannerClient.active()) {
			tracker.initialize();
		} else {
			original.call(tracker, player, positions, item, creative);
		}
	}

	/** Hook E9: aim at the nearer of the real block and a ghost block. */
	@WrapOperation(method = {"handleClick", "getPreviewBlocks"}, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/multiplayer/ClientLevel;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"),
		require = 2)
	private static BlockHitResult buildingplanner$aimAtGhosts(ClientLevel level, ClipContext context,
		Operation<BlockHitResult> original) {
		BlockHitResult real = original.call(level, context);
		return BuildingPlannerClient.active() ? EffortlessShapes.nearerHit(real, context) : real;
	}

	/** Hook E9: a shape started on a ghost block goes beside it, as on a real block. */
	@WrapOperation(method = "resolveFirstClickPos", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
	private static BlockState buildingplanner$startOnGhosts(Level level, BlockPos pos, Operation<BlockState> original) {
		BlockState real = original.call(level, pos);
		return BuildingPlannerClient.active() ? EffortlessShapes.startBlock(level, pos, real) : real;
	}
}
