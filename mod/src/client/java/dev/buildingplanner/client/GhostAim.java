package dev.buildingplanner.client;

import dev.buildingplanner.layer.Layer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Aiming at the nearer of a real block and a ghost block, as shapes (hook E9) and the copy do. Ghost blocks are read
 * from the player's copy of the layer, not from Litematica's schematic world, which also holds the copy: the copy
 * floats where the player aims, so aiming at it would pull it toward the player. Ghost blocks count only while they
 * show.
 */
final class GhostAim {
	/** How far the copy follows the player's aim, from the camera while Freecam's is on. */
	static final double COPY_REACH = 64;

	private GhostAim() {
	}

	/** The ghost block a ray first meets, or null when it meets none or ghost blocks are hidden. */
	static BlockHitResult ghostHit(Layer layer, boolean shown, LevelHeightAccessor height, ClipContext context) {
		if (!shown) {
			return null;
		}
		BlockHitResult hit = new LayerBlocks(layer, height).clip(context);
		return hit.getType() == HitResult.Type.BLOCK ? hit : null;
	}

	/** The nearer of a real block hit and a ghost block hit, from where the ray starts; either may be a miss or null. */
	static BlockHitResult nearer(BlockHitResult real, BlockHitResult ghost, Vec3 from) {
		if (ghost == null) {
			return real;
		}
		if (real != null && real.getType() == HitResult.Type.BLOCK
			&& from.distanceToSqr(real.getLocation()) <= from.distanceToSqr(ghost.getLocation())) {
			return real;
		}
		return ghost;
	}

	/**
	 * Where the copy goes for the player's aim: the block beside the face they aim at, real or ghost, within {@link
	 * #COPY_REACH}; null when they aim at nothing.
	 */
	static BlockPos copyOrigin(Layer layer, boolean shown) {
		BlockHitResult hit = aim(layer, shown, COPY_REACH);
		return hit == null ? null : hit.getBlockPos().relative(hit.getDirection());
	}

	/**
	 * The block the player aims at within {@code reach} of the eyes they aim from ({@link Aim}), the nearer of a real block and a ghost block, with
	 * the face aimed at; null when they aim at nothing.
	 */
	static BlockHitResult aim(Layer layer, boolean shown, double reach) {
		Minecraft mc = Minecraft.getInstance();
		Entity from = Aim.entity();
		if (from == null || mc.level == null) {
			return null;
		}
		Vec3 eye = from.getEyePosition();
		ClipContext context = new ClipContext(eye, eye.add(from.getLookAngle().scale(reach)),
			ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player);
		BlockHitResult hit = nearer(mc.level.clip(context), ghostHit(layer, shown, mc.level, context), eye);
		return hit != null && hit.getType() == HitResult.Type.BLOCK ? hit : null;
	}

	/** The layer's ghost blocks as a world a ray can be traced through. */
	private record LayerBlocks(Layer layer, LevelHeightAccessor height) implements BlockGetter {
		@Override
		public BlockState getBlockState(BlockPos pos) {
			return layer.get(pos);
		}

		@Override
		public FluidState getFluidState(BlockPos pos) {
			return Fluids.EMPTY.defaultFluidState();
		}

		@Override
		public BlockEntity getBlockEntity(BlockPos pos) {
			return null;
		}

		@Override
		public int getHeight() {
			return height.getHeight();
		}

		@Override
		public int getMinY() {
			return height.getMinY();
		}
	}
}
