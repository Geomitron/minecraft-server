package dev.buildingplanner.client;

import dev.buildingplanner.plan.Turn;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.gui.interfaces.IMessageConsumer;
import fi.dy.masa.malilib.util.InfoUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The player's copy of some blocks: a Litematica placement of a schematic kept in memory, which only this player sees.
 * Litematica doesn't save it, since it has no file; hooks L4 and L5 keep it out of Litematica's selection and placement
 * list, as for tiles. Its origin, the corner with the lowest coordinates when it's made, follows where the player
 * aims.
 */
final class Clipboard {
	private static final String REGION = "Copy";

	private SchematicPlacement placement;

	/** Whether there's a copy; forgets it once Litematica no longer has its placement. */
	boolean exists() {
		if (placement != null && !manager().getAllSchematicsPlacements().contains(placement)) {
			placement = null;
		}
		return placement != null;
	}

	SchematicPlacement placement() {
		return placement;
	}

	/** Whether the placement is the copy's. */
	boolean holds(SchematicPlacement placement) {
		return placement != null && placement == this.placement;
	}

	/**
	 * Replaces any copy with one of {@code blocks} (world positions and states as the world shows them), with its
	 * origin at {@code origin}, or over the blocks when that's null.
	 */
	void place(Map<BlockPos, BlockState> blocks, BlockPos origin) {
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
		// schematic world, so the ghost blocks there stay visible under the copy.
		Vec3i size = container.getSize();
		BlockState empty = Blocks.STRUCTURE_VOID.defaultBlockState();
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.ZERO,
			new BlockPos(size.getX() - 1, size.getY() - 1, size.getZ() - 1))) {
			container.set(pos.getX(), pos.getY(), pos.getZ(), empty);
		}
		BlockPos corner = min;
		blocks.forEach((pos, state) -> container.set(pos.getX() - corner.getX(), pos.getY() - corner.getY(),
			pos.getZ() - corner.getZ(), state));
		// Known as the copy before it's added, so hook L4 keeps Litematica from selecting it.
		placement = SchematicPlacement.createFor(schematic, origin != null ? origin : min, name, true, true);
		manager().addSchematicPlacement(placement, false);
	}

	/** Moves the copy's origin to {@code origin}, when it isn't there already. */
	void follow(BlockPos origin) {
		if (!origin.equals(placement.getOrigin())) {
			placement.setOrigin(origin,
				message -> InfoUtils.showGuiOrInGameMessage(Message.MessageType.WARNING, message));
		}
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
				BlockPos world = worldPosition(placement, REGION, pos);
				if (world != null) {
					blocks.put(world, transformed(placement, REGION, state));
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
	}

	/**
	 * Adds the copy again, so Litematica, which draws placements in the order they were added, draws it over the tiles
	 * added since.
	 */
	void raise() {
		if (!exists()) {
			return;
		}
		manager().removeSchematicPlacement(placement, false);
		manager().addSchematicPlacement(placement, false);
	}

	/** The world position of a container position, as Litematica places a region's blocks. */
	private static BlockPos worldPosition(SchematicPlacement placement, String regionName, BlockPos containerPos) {
		SubRegionPlacement region = placement.getRelativeSubRegionPlacement(regionName);
		BlockPos size = placement.getSchematic().getAreaSize(regionName);
		if (region == null || size == null) {
			return null;
		}
		BlockPos regionPos = region.getPos();
		BlockPos minRel = PositionUtils.getMinCorner(regionPos,
			regionPos.offset(PositionUtils.getRelativeEndPositionFromAreaSize(size)));
		BlockPos withinRegion = minRel.subtract(regionPos).offset(containerPos);
		return PositionUtils.getTransformedPlacementPosition(withinRegion, placement, region)
			.offset(PositionUtils.getTransformedBlockPos(regionPos, placement.getMirror(), placement.getRotation()))
			.offset(placement.getOrigin());
	}

	/**
	 * A block state as a placement shows it in the world: the inverse of Litematica's
	 * {@code SchematicUtils.getUntransformedBlockState}, which mirrors by the placement, then by the sub-region (on the
	 * other axis while the placement is turned a quarter), then turns by both.
	 */
	private static BlockState transformed(SchematicPlacement placement, String region, BlockState state) {
		SubRegionPlacement sub = placement.getRelativeSubRegionPlacement(region);
		Mirror subMirror = sub == null ? Mirror.NONE : sub.getMirror();
		Rotation rotation = sub == null ? placement.getRotation() : placement.getRotation().getRotated(sub.getRotation());
		if (subMirror != Mirror.NONE && (placement.getRotation() == Rotation.CLOCKWISE_90
			|| placement.getRotation() == Rotation.COUNTERCLOCKWISE_90)) {
			subMirror = subMirror == Mirror.FRONT_BACK ? Mirror.LEFT_RIGHT : Mirror.FRONT_BACK;
		}
		return state.mirror(placement.getMirror()).mirror(subMirror).rotate(rotation);
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

	private static SchematicPlacementManager manager() {
		return DataManager.getSchematicPlacementManager();
	}
}
