package dev.buildingplanner.client;

import dev.buildingplanner.plan.Turn;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.gui.interfaces.IMessageConsumer;
import fi.dy.masa.malilib.util.InfoUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The player's copy of part of the plans: a Litematica placement of a schematic kept in memory, which only this
 * player sees. Litematica doesn't save it, since it has no file, and Syncmatica doesn't share it.
 */
final class Clipboard {
	/** How far the copy can be placed where the player aims. */
	private static final double AIM_DISTANCE = 64;
	private static final String REGION = "Copy";

	private SchematicPlacement placement;
	/** The plan most of the copy's blocks came from. */
	private UUID source;

	/** Whether there's a copy; forgets it once Litematica no longer has its placement. */
	boolean exists() {
		if (placement != null && !manager().getAllSchematicsPlacements().contains(placement)) {
			placement = null;
			source = null;
		}
		return placement != null;
	}

	UUID source() {
		return source;
	}

	SchematicPlacement placement() {
		return placement;
	}

	/**
	 * Replaces any copy with one of {@code blocks} (world positions and states as the world shows them), with its
	 * origin on the block beside the face the player aims at, or else over the blocks; selects it, with Litematica's tool
	 * in its placement mode. Never the Paste tool, which places real blocks in creative mode.
	 */
	void place(Map<BlockPos, BlockState> blocks, UUID sourcePlan) {
		clear();
		BlockPos min = null;
		BlockPos max = null;
		for (BlockPos pos : blocks.keySet()) {
			min = min == null ? pos : PositionUtils.getMinCorner(min, pos);
			max = max == null ? pos : PositionUtils.getMaxCorner(max, pos);
		}
		AreaSelection area = new AreaSelection();
		area.addSubRegionBox(new Box(min, max, REGION), false);
		Minecraft mc = Minecraft.getInstance();
		LitematicaSchematic schematic = LitematicaSchematic.createEmptySchematic(area,
			mc.player == null ? "" : mc.player.getName().getString());
		if (schematic == null) {
			return;
		}
		String name = Component.translatable("buildingplanner.ui.copy.name").getString();
		schematic.getMetadata().setName(name);
		LitematicaBlockStateContainer container = schematic.getSubRegionContainer(REGION);
		// Structure void, not air, fills the copy's empty space: Litematica skips it when it draws placements into its
		// schematic world, so the plans' blocks there stay visible under the copy.
		Vec3i size = container.getSize();
		BlockState empty = Blocks.STRUCTURE_VOID.defaultBlockState();
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.ZERO,
			new BlockPos(size.getX() - 1, size.getY() - 1, size.getZ() - 1))) {
			container.set(pos.getX(), pos.getY(), pos.getZ(), empty);
		}
		BlockPos corner = min;
		blocks.forEach((pos, state) -> container.set(pos.getX() - corner.getX(), pos.getY() - corner.getY(),
			pos.getZ() - corner.getZ(), state));
		placement = SchematicPlacement.createFor(schematic, aimed(mc, min), name, true, true);
		source = sourcePlan;
		manager().addSchematicPlacement(placement, false);
		manager().setSelectedSchematicPlacement(placement);
		DataManager.setToolMode(ToolMode.SCHEMATIC_PLACEMENT);
	}

	/** The copy's blocks other than its empty space, at the world positions where Litematica draws them, with their states turned. */
	Map<BlockPos, BlockState> worldBlocks() {
		Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
		LitematicaBlockStateContainer container = placement.getSchematic().getSubRegionContainer(REGION);
		Vec3i size = container.getSize();
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.ZERO,
			new BlockPos(size.getX() - 1, size.getY() - 1, size.getZ() - 1))) {
			BlockState state = container.get(pos.getX(), pos.getY(), pos.getZ());
			if (!state.isAir() && !state.is(Blocks.STRUCTURE_VOID)) {
				BlockPos world = LitematicaPlans.worldPosition(placement, REGION, pos);
				if (world != null) {
					blocks.put(world, LitematicaPlans.transformed(placement, REGION, state));
				}
			}
		}
		return blocks;
	}

	/** Gives the copy a quarter turn clockwise, seen from above, around its origin. */
	void turn() {
		set(new Turn(placement.getMirror(), placement.getRotation()).turnedClockwise());
	}

	/** Flips the copy left to right for a player facing {@code facing}, around its origin. */
	void mirror(Direction facing) {
		set(new Turn(placement.getMirror(), placement.getRotation()).flippedLeftRight(facing));
	}

	void clear() {
		if (placement != null) {
			manager().removeSchematicPlacement(placement);
		}
		placement = null;
		source = null;
	}

	/** Litematica refuses to change a locked placement, with its own message. */
	private void set(Turn turn) {
		placement.setMirror(turn.mirror(), LITEMATICA_MESSAGES);
		placement.setRotation(turn.rotation(), LITEMATICA_MESSAGES);
	}

	/** Shows Litematica's messages as its own actions do. */
	private static final IMessageConsumer LITEMATICA_MESSAGES = new IMessageConsumer() {
		@Override
		public void addMessage(Message.MessageType type, String message, Object... args) {
			InfoUtils.showGuiOrInGameMessage(type, message, args);
		}

		@Override
		public void addMessage(Message.MessageType type, int lifeTime, String message, Object... args) {
			InfoUtils.showGuiOrInGameMessage(type, lifeTime, message, args);
		}
	};

	private static BlockPos aimed(Minecraft mc, BlockPos otherwise) {
		if (mc.level == null || mc.getCameraEntity() == null) {
			return otherwise;
		}
		HitResult hit = RayTraceUtils.getRayTraceFromEntity(mc.level, mc.getCameraEntity(), false, AIM_DISTANCE);
		return hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK
			? block.getBlockPos().relative(block.getDirection()) : otherwise;
	}

	private static SchematicPlacementManager manager() {
		return DataManager.getSchematicPlacementManager();
	}
}
