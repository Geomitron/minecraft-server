package dev.buildingplanner.input;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Disable's preview and its line (see "Turning real blocks in Disable"), as pure functions: when the preview shows, the
 * reach the server places within, and which blocks hang on the block behind them, for which Disable's line leaves out
 * holding Left Control.
 */
public final class DisablePreview {
	/**
	 * How far past the player's block interaction range the server takes a right click on a block
	 * ({@code ServerGamePacketListenerImpl.handleUseItemOn} calls {@code isWithinBlockInteractionRange(pos, 1.0)}).
	 */
	public static final double SERVER_BUFFER = 1.0;

	private DisablePreview() {
	}

	/**
	 * Whether Disable's preview shows: Disable's turning controls are active, the sprint key is held, and a right click
	 * reaches the world, which it doesn't while Freecam keeps clicks from it ({@link #interactionBlocked}).
	 */
	public static boolean shows(boolean active, boolean sprint, boolean interactionBlocked) {
		return active && sprint && !interactionBlocked;
	}

	/**
	 * Whether Freecam keeps a click from the world, by Freecam's own rule: its camera is on, its "Player control" is
	 * off, and its "Allow interaction" is off.
	 */
	public static boolean interactionBlocked(boolean freecamOn, boolean playerControl, boolean allowInteraction) {
		return freecamOn && !playerControl && !allowInteraction;
	}

	/**
	 * Whether the server places a right click on the block at {@code clicked} by a player whose eyes are at {@code eye},
	 * with that block interaction range: the block's box within the range and {@link #SERVER_BUFFER} of the eyes, as the
	 * server checks it ({@code Player.isWithinBlockInteractionRange}). Freecam's camera aims, but the player clicks.
	 */
	public static boolean withinReach(Vec3 eye, BlockPos clicked, double range) {
		double reach = range + SERVER_BUFFER;
		return new AABB(clicked).distanceToSqr(eye) < reach * reach;
	}

	/**
	 * Whether the block hangs on the block behind it, as Minecraft's rule for that block requires, so it can't stay
	 * turned another way: a ladder, a torch, sign or banner on a wall, a button or a lever on a wall, a tripwire hook,
	 * a coral fan on a wall and cocoa. A grindstone on a wall needs nothing behind it.
	 */
	public static boolean hangs(BlockState state) {
		Block block = state.getBlock();
		if (block instanceof FaceAttachedHorizontalDirectionalBlock) {
			return !(block instanceof GrindstoneBlock)
				&& state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE) == AttachFace.WALL;
		}
		return block instanceof LadderBlock || block instanceof WallTorchBlock || block instanceof RedstoneWallTorchBlock
			|| block instanceof WallSignBlock || block instanceof WallBannerBlock || block instanceof TripWireHookBlock
			|| block instanceof BaseCoralWallFanBlock || block instanceof CocoaBlock;
	}
}
