package dev.buildingplanner.mixin.effortless;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.buildingplanner.client.Aim;
import dev.buildingplanner.client.BuildingPlannerClient;
import dev.buildingplanner.client.ClickRule;
import dev.buildingplanner.client.EffortlessShapes;
import dev.buildingplanner.client.RealShape;
import java.util.Collection;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.requios.effortlessbuilding.buildpipeline.BuildPipeline;
import nl.requios.effortlessbuilding.buildpipeline.BuildPipelineClient;
import nl.requios.effortlessbuilding.network.BreakBuildModePacket;
import nl.requios.effortlessbuilding.network.PlaceBuildModePacket;
import nl.requios.effortlessbuilding.utilities.BlockSet;
import nl.requios.effortlessbuilding.utilities.BreakDisplayTracker;
import nl.requios.effortlessbuilding.utilities.ItemUsageTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Effortless Building's client pipeline, on a server with Building Planner: a finished shape goes into the layer
 * instead of to the server as real blocks (hooks E1 to E3), or with "Place real blocks" to the server as a
 * {@code place} (E1, with its preview's count and red blocks, E8 and E40, and its sound, E41), the mix starts shapes
 * (E6), the preview of ghost blocks has no "missing items" tint and a break shape no list of real blocks (E8), shapes can start on ghost blocks (E9), and clicks without
 * a placeable item stay vanilla's and a mouse mode's go to it (E14). A shape's first click and its preview aim from Freecam's camera while it's on
 * (E24), and take a locked first position (E9, E32). In Disable, the preview draws the real block a right click would
 * place while Left Ctrl turns it (E29).
 */
@Mixin(value = BuildPipelineClient.class, remap = false)
abstract class BuildPipelineClientMixin {
	/**
	 * Hook E1: the finished shape goes into the layer, not to the server as real blocks, or while "Place real blocks"
	 * acts, to the server as a {@code place}; with Building Planner off on a server, it goes nowhere.
	 */
	@WrapWithCondition(method = "handleClick", at = @At(value = "INVOKE",
		target = "Lnl/requios/effortlessbuilding/network/PacketHandler;sendToServer(Lnl/requios/effortlessbuilding/network/PlaceBuildModePacket;)V"))
	private static boolean buildingplanner$place(PlaceBuildModePacket packet, @Local BlockSet blocks) {
		if (!BuildingPlannerClient.active()) {
			return !BuildingPlannerClient.off();
		}
		if (RealShape.places()) {
			RealShape.place(blocks, BuildPipelineClient.getFirstClickHit());
		} else {
			EffortlessShapes.place(blocks, BuildPipelineClient.getFirstClickHit());
		}
		return false;
	}

	/**
	 * Hook E41: while "Place real blocks" acts, the place click's sound plays only when the inventory covers the
	 * shape's blocks that go in, since otherwise the server places nothing.
	 */
	@WrapWithCondition(method = "handleClick", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/multiplayer/ClientLevel;playLocalSound(Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V",
		ordinal = 0))
	private static boolean buildingplanner$placeSound(ClientLevel level, BlockPos pos, SoundEvent sound,
		SoundSource source, float volume, float pitch, boolean distanceDelay, @Local BlockSet blocks) {
		return !BuildingPlannerClient.active() || !RealShape.places() || RealShape.covered(blocks);
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
	 * Hook E14: while a mouse mode takes the clicks, Effortless Building takes neither, in any mode, so hooks M1 to M5
	 * have them. With anything but a placeable item in the main hand, and no shape under way, Effortless Building takes
	 * neither click either, so both act as without mods (its client tick, its cancelling of vanilla's use, attack and
	 * held attack, and hold to repeat all ask these first). A shape under way still takes the click that finishes or
	 * cancels it. A click on a locked position stays Effortless Building's.
	 */
	@ModifyReturnValue(method = "shouldInterceptPlacing", at = @At("RETURN"))
	private static boolean buildingplanner$placingOnlyWithPlaceables(boolean intercept) {
		return intercept && onlyWithPlaceables();
	}

	/** Hook E14, for the break click. */
	@ModifyReturnValue(method = "shouldInterceptBreaking", at = @At("RETURN"))
	private static boolean buildingplanner$breakingOnlyWithPlaceables(boolean intercept) {
		return intercept && onlyWithPlaceables();
	}

	private static boolean onlyWithPlaceables() {
		return !BuildingPlannerClient.mouseTaken() && (!BuildingPlannerClient.ownsShapes()
			|| BuildPipelineClient.getBuildState() != null || ClickRule.holdsPlaceable());
	}

	/**
	 * Hook E29: in Disable, while Left Ctrl is held with a block that turns or flips, the preview draws the one block a
	 * right click would place, red where the turned block can't stay, or nothing where it wouldn't place.
	 */
	@ModifyReturnValue(method = "getPreviewBlocks", at = @At("RETURN"))
	private static BlockSet buildingplanner$disablePreview(BlockSet blocks) {
		return BuildingPlannerClient.active() ? EffortlessShapes.disablePreview(blocks) : blocks;
	}

	/**
	 * Hook E8: ghost blocks don't use up items, so the preview shows none missing. While "Place real blocks" acts, the
	 * tracker counts the blocks that go in instead, each as its own item ({@link RealShape#count}).
	 */
	@WrapOperation(method = "updateDisplayTrackers", at = @At(value = "INVOKE",
		target = "Lnl/requios/effortlessbuilding/utilities/ItemUsageTracker;compute(Lnet/minecraft/world/entity/player/Player;Lnl/requios/effortlessbuilding/utilities/BlockSet;Z)V"))
	private static void buildingplanner$noMissingMix(ItemUsageTracker tracker, Player player, BlockSet blocks,
		boolean creative, Operation<Void> original) {
		if (!BuildingPlannerClient.active()) {
			original.call(tracker, player, blocks, creative);
		} else if (RealShape.places()) {
			RealShape.count(tracker, player, blocks, creative);
		} else {
			tracker.initialize();
		}
	}

	/** Hook E8, for the held block. */
	@WrapOperation(method = "updateDisplayTrackers", at = @At(value = "INVOKE",
		target = "Lnl/requios/effortlessbuilding/utilities/ItemUsageTracker;compute(Lnet/minecraft/world/entity/player/Player;Ljava/util/Collection;Lnet/minecraft/world/item/Item;Z)V"))
	private static void buildingplanner$noMissingHeld(ItemUsageTracker tracker, Player player,
		Collection<BlockPos> positions, Item item, boolean creative, Operation<Void> original,
		@Local(argsOnly = true) BlockSet blocks) {
		if (!BuildingPlannerClient.active()) {
			original.call(tracker, player, positions, item, creative);
		} else if (RealShape.places()) {
			RealShape.count(tracker, player, blocks, creative);
		} else {
			tracker.initialize();
		}
	}

	/**
	 * Hook E40: while "Place real blocks" acts, the place click's preview draws red the whole shape when the inventory
	 * lacks an item it needs, and otherwise each block that wouldn't go in ({@link RealShape#markRed}).
	 */
	@Inject(method = "updateDisplayTrackers", at = @At("TAIL"))
	private static void buildingplanner$realRed(Player player, BlockSet blocks, CallbackInfo ci) {
		if (BuildingPlannerClient.active() && BuildPipelineClient.getBuildState() != BuildPipeline.BuildState.BREAKING
			&& RealShape.places()) {
			RealShape.markRed(blocks);
		}
	}

	/**
	 * Hook E8, for a shape drawn with the break click: it erases ghost blocks and leaves the real blocks under it, so
	 * Effortless Building's list of them, drawn in one row that never wraps, shows nothing.
	 */
	@WrapOperation(method = "updateDisplayTrackers", at = @At(value = "INVOKE",
		target = "Lnl/requios/effortlessbuilding/utilities/BreakDisplayTracker;compute(Lnet/minecraft/world/entity/player/Player;Lnl/requios/effortlessbuilding/utilities/BlockSet;)V"))
	private static void buildingplanner$noBreakList(BreakDisplayTracker tracker, Player player, BlockSet blocks,
		Operation<Void> original) {
		if (BuildingPlannerClient.active()) {
			tracker.initialize();
		} else {
			original.call(tracker, player, blocks);
		}
	}

	/**
	 * Hook E9: aim at the nearer of the real block and a ghost block; while a first position is locked, at the hit it
	 * was locked with, moved by the nudges.
	 */
	@WrapOperation(method = {"handleClick", "getPreviewBlocks"}, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/multiplayer/ClientLevel;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"),
		require = 2)
	private static BlockHitResult buildingplanner$aimAtGhosts(ClientLevel level, ClipContext context,
		Operation<BlockHitResult> original) {
		BlockHitResult real = original.call(level, context);
		return BuildingPlannerClient.active() ? EffortlessShapes.shapeAim(real, context) : real;
	}

	/**
	 * Hook E32: while a first position is locked ({@code ShapeLock}), the first click and the first click's preview take
	 * the locked position.
	 */
	@WrapOperation(method = {"handleClick", "getPreviewBlocks"}, at = @At(value = "INVOKE",
		target = "Lnl/requios/effortlessbuilding/buildpipeline/BuildPipelineClient;resolveFirstClickPos(Lnet/minecraft/world/phys/BlockHitResult;Lnl/requios/effortlessbuilding/buildpipeline/BuildPipeline$BuildState;Lnet/minecraft/world/level/Level;)Lnet/minecraft/core/BlockPos;"),
		require = 2)
	private static BlockPos buildingplanner$lockedFirstClick(BlockHitResult hit, BuildPipeline.BuildState action,
		Level level, Operation<BlockPos> original) {
		BlockPos found = original.call(hit, action, level);
		return BuildingPlannerClient.active() ? EffortlessShapes.firstClickPos(found) : found;
	}

	/** Hook E9: a shape started on a ghost block goes beside it, as on a real block. */
	@WrapOperation(method = "resolveFirstClickPos", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
	private static BlockState buildingplanner$startOnGhosts(Level level, BlockPos pos, Operation<BlockState> original) {
		BlockState real = original.call(level, pos);
		return BuildingPlannerClient.active() ? EffortlessShapes.startBlock(level, pos, real) : real;
	}

	/** Hook E24: a shape's first click and its preview aim from the camera while Freecam's is on ({@link Aim}). */
	@WrapOperation(method = {"handleClick", "getPreviewBlocks"}, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/player/Player;getEyePosition()Lnet/minecraft/world/phys/Vec3;"), require = 2)
	private static Vec3 buildingplanner$eyeAtAim(Player player, Operation<Vec3> original) {
		Entity aim = Aim.forEffortless(player);
		return aim == player ? original.call(player) : aim.getEyePosition();
	}

	/** Hook E24, for the look. */
	@WrapOperation(method = {"handleClick", "getPreviewBlocks"}, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/player/Player;getLookAngle()Lnet/minecraft/world/phys/Vec3;"), require = 2)
	private static Vec3 buildingplanner$lookAtAim(Player player, Operation<Vec3> original) {
		Entity aim = Aim.forEffortless(player);
		return aim == player ? original.call(player) : aim.getLookAngle();
	}
}
