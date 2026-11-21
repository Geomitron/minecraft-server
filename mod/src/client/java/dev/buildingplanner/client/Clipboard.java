package dev.buildingplanner.client;

import dev.buildingplanner.input.CopyPlacement;
import dev.buildingplanner.input.CopyPlacement.Spot;
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
 * The player's most recent copy: a Litematica placement of a schematic kept in memory, which only this player sees.
 * Litematica doesn't save it, since it has no file; hooks L4 and L5 keep it out of Litematica's selection and placement
 * list, as for tiles. At the aim, the placement is in Litematica, and its origin is set so that the copy, as it's
 * turned and mirrored, sits in the box {@link CopyPlacement} and {@code PasteBoxes} give for the player's aim. Put
 * away, it's out of Litematica, and the same placement, with its schematic, turn and mirror, comes back to the aim.
 * Locked, it stays where it is instead of following the aim, and the nudge keys move it.
 */
final class Clipboard {
	private static final String REGION = "Copy";

	/** The most recent copy, at the aim or put away; null before the first copy. */
	private SchematicPlacement placement;
	private boolean atAim;
	/** Whether the copy stays where it is, not following the aim; a paste keeps it locked. */
	private boolean locked;

	/** Whether there's a copy, at the aim or put away. */
	boolean exists() {
		return placement != null;
	}

	/** Whether the copy is at the aim. Cheap enough for the hooks, which ask several times a frame. */
	boolean atAim() {
		return atAim;
	}

	/** Whether the copy at the aim is locked where it is. */
	boolean locked() {
		return atAim && locked;
	}

	/** Locks the copy at the aim where it is, or unlocks it so it follows the aim again. */
	void toggleLock() {
		if (atAim) {
			locked = !locked;
		}
	}

	/** Moves the copy one block that way, as it's turned and mirrored. */
	void nudge(Direction direction) {
		moveTo(spot().min().relative(direction));
	}

	/** Once a tick: the copy counts as put away once Litematica no longer has its placement. */
	void tick() {
		if (atAim && !manager().getAllSchematicsPlacements().contains(placement)) {
			atAim = false;
		}
	}

	SchematicPlacement placement() {
		return placement;
	}

	/** Whether the placement is the copy's. */
	boolean holds(SchematicPlacement placement) {
		return placement != null && placement == this.placement;
	}

	/**
	 * Replaces any copy with one of {@code blocks} (world positions and states as the world shows them), at the aim, over
	 * the blocks until it follows the aim.
	 */
	void place(Map<BlockPos, BlockState> blocks) {
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
		placement = SchematicPlacement.createFor(schematic, min, name, true, true);
		manager().addSchematicPlacement(placement, false);
		atAim = true;
		locked = false;
	}

	/** Takes the copy out of Litematica, keeping it, as it's turned and mirrored, for {@link #bringBack}. */
	void putAway() {
		if (atAim()) {
			manager().removeSchematicPlacement(placement);
		}
		atAim = false;
	}

	/**
	 * Brings the copy back to the aim, unlocked, as it was turned and mirrored, at its last origin until it follows the
	 * aim. Its placement goes back into Litematica before anything about it changes, since changing a placement
	 * Litematica doesn't have would still draw it.
	 */
	void bringBack() {
		if (placement != null && !atAim()) {
			manager().addSchematicPlacement(placement, false);
			atAim = true;
			locked = false;
		}
	}

	/** The schematic's size, before it's turned. */
	Vec3i size() {
		return placement.getSchematic().getSubRegionContainer(REGION).getSize();
	}

	/** How the copy is mirrored and turned. */
	Turn turnNow() {
		return new Turn(placement.getMirror(), placement.getRotation());
	}

	/** The box the copy fills in the world, as it's turned and mirrored. */
	Spot spot() {
		Turn turn = turnNow();
		Vec3i size = size();
		return new Spot(placement.getOrigin().offset(CopyPlacement.cornerFromOrigin(size, turn)),
			CopyPlacement.turnedSize(size, turn));
	}

	/** Moves the copy so that, as it's turned and mirrored, its lowest corner is at {@code min}. */
	void moveTo(BlockPos min) {
		BlockPos origin = CopyPlacement.origin(min, size(), turnNow());
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

	/** Gives the copy a quarter turn around its middle, clockwise or counterclockwise seen from above. */
	void turn(boolean clockwise) {
		Turn turn = turnNow();
		set(clockwise ? turn.turnedClockwise() : turn.turnedCounterclockwise());
	}

	/** Flips the copy left to right for a player facing {@code facing}, around its middle. */
	void mirror(Direction facing) {
		set(turnNow().flippedLeftRight(facing));
	}

	/** Drops the copy. */
	void clear() {
		putAway();
		placement = null;
	}

	/**
	 * Adds the copy again, so Litematica, which draws placements in the order they were added, draws it over the tiles
	 * added since.
	 */
	void raise() {
		if (!atAim()) {
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

	/**
	 * Sets the copy's mirror and turn, keeping its middle block where it was. Litematica refuses to change a locked
	 * placement, with its own message.
	 */
	private void set(Turn turn) {
		Spot before = spot();
		placement.setMirror(turn.mirror(), LITEMATICA_MESSAGES);
		placement.setRotation(turn.rotation(), LITEMATICA_MESSAGES);
		moveTo(CopyPlacement.aroundMiddle(before, CopyPlacement.turnedSize(size(), turnNow())).min());
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
