package dev.buildingplanner.client;

import dev.buildingplanner.input.BlockTurn;
import dev.buildingplanner.input.CopyColour;
import dev.buildingplanner.input.CopyMode;
import dev.buildingplanner.input.DisablePreview;
import dev.buildingplanner.input.GhostMatch;
import dev.buildingplanner.input.MixRule;
import dev.buildingplanner.input.ShapeLock;
import dev.buildingplanner.layer.GhostErase;
import dev.buildingplanner.layer.OtherHalf;
import dev.buildingplanner.look.ControlsLine;
import dev.buildingplanner.look.GhostFrame;
import dev.buildingplanner.mixin.effortless.BuildPipelineClientAccessor;
import dev.buildingplanner.mixin.effortless.ThreeClicksBuildModeAccessor;
import dev.buildingplanner.mixin.effortless.TwoClicksBuildModeAccessor;
import dev.buildingplanner.mixin.minecraft.BlockItemInvoker;
import dev.buildingplanner.server.PlaceTurns;
import dev.buildingplanner.sync.EditKind;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.requios.effortlessbuilding.buildmode.BuildModeEnum;
import nl.requios.effortlessbuilding.buildmode.BuildModes;
import nl.requios.effortlessbuilding.buildmode.IBuildMode;
import nl.requios.effortlessbuilding.buildmode.ThreeClicksBuildMode;
import nl.requios.effortlessbuilding.buildmode.buildmodes.Floor;
import nl.requios.effortlessbuilding.buildmode.buildmodes.Line;
import nl.requios.effortlessbuilding.buildmode.buildmodes.Wall;
import nl.requios.effortlessbuilding.buildpipeline.BuildPipeline;
import nl.requios.effortlessbuilding.buildpipeline.BuildPipelineClient;
import nl.requios.effortlessbuilding.config.ServerConfig;
import nl.requios.effortlessbuilding.screen.RadialMenu;
import nl.requios.effortlessbuilding.utilities.BlockEntry;
import nl.requios.effortlessbuilding.utilities.BlockSet;

/**
 * Effortless Building's side of shapes: a finished shape's blocks for the layer, and aiming at ghost blocks. Only the
 * hooks into Effortless Building load this class, and only while Building Planner is on, so it's never loaded without
 * that mod, Litematica or MaLiLib: the verifier would load Litematica's classes with it.
 */
public final class EffortlessShapes {
	private EffortlessShapes() {
	}

	/**
	 * Hooks E5 and E6: whether the block mix applies now: the mix is on, the main hand holds a placeable item, and
	 * Effortless Building's mode isn't Disable (see {@link MixRule}).
	 */
	public static boolean mixApplies() {
		return MixRule.applies(BuildingPlannerClient.mixOn(), ClickRule.holdsPlaceable(),
			BuildModes.CLIENT.getBuildMode() == BuildModeEnum.DISABLED);
	}

	/** Whether the player has started a shape that isn't finished yet, read once a client tick. */
	public static boolean shapeUnderWay() {
		return BuildPipelineClient.getBuildState() != null;
	}

	/**
	 * The block a set of copies reaches in Line, Wall or Floor: the block Effortless Building's {@code Line.findLine},
	 * {@code Wall.findWall} or {@code Floor.findFloor} gives from {@code first}, the first copy's middle, with its reach
	 * and its aim from the camera (hooks E25 and E26), as its preview finds a shape's second position; or null where it
	 * finds none, and in Off.
	 */
	static BlockPos copyReach(CopyMode mode, BlockPos first) {
		Player player = Minecraft.getInstance().player;
		if (player == null) {
			return null;
		}
		return switch (mode) {
			case OFF -> null;
			case LINE -> Line.findLine(player, first, true);
			case WALL -> Wall.findWall(player, first, true);
			case FLOOR -> Floor.findFloor(player, first, true);
		};
	}

	/** Which shape is under way, for the build modes' line: none, one drawn with the place click, or the break click. */
	static ControlsLine.Underway underway() {
		BuildPipeline.BuildState state = BuildPipelineClient.getBuildState();
		if (state == null) {
			return ControlsLine.Underway.NONE;
		}
		return state == BuildPipeline.BuildState.BREAKING ? ControlsLine.Underway.BREAKING : ControlsLine.Underway.PLACING;
	}

	/**
	 * Hook E1: the player finished a shape with the place click. Each valid entry gets the block Effortless Building
	 * would place: the mix's (set by hook E5) or the held block, by its placement rules with the first click's face and
	 * the facing of the player at the aim (Freecam's camera while it's on, see {@link Aim}), then the turn of the block in
	 * hand while the turning controls are active ({@link HeldTurn}), then the shape's mirror and turn from the modifiers.
	 * {@code first} is the hit the states come from: the shape's first click (E1), which a locked position's hit is
	 * (E9); with none, the top face at the middle of the shape's first position. The click that finishes the shape ends the lock.
	 */
	public static void place(BlockSet blocks, BlockHitResult first) {
		BuildingPlannerClient.captureShape(EditKind.SHAPE, states(blocks, first));
		BuildingPlannerClient.shapeFinished();
	}

	/**
	 * The blocks a shape's valid entries get ({@link #place}), with the other half of each two-block one, by their
	 * positions in the world; package-private for the real blocks a shape places ({@link RealShape}).
	 */
	static Map<BlockPos, BlockState> states(BlockSet blocks, BlockHitResult first) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		Player placer = Aim.placer();
		Direction face = first != null ? first.getDirection() : Direction.UP;
		Vec3 hit = first != null ? first.getLocation() : Vec3.atCenterOf(blocks.firstPos);
		double yFraction = hit.y - Math.floor(hit.y);
		ItemStack held = player.getMainHandItem();
		boolean mix = mixApplies();
		boolean turning = BuildingPlannerClient.shapesTakeTurn();
		Map<BlockPos, BlockState> states = new LinkedHashMap<>();
		for (Map.Entry<BlockPos, BlockEntry> e : blocks.validEntries()) {
			BlockPos pos = e.getKey();
			BlockEntry entry = e.getValue();
			ItemStack stack = mix ? (entry.item == null ? ItemStack.EMPTY : new ItemStack(entry.item)) : held;
			if (!(stack.getItem() instanceof BlockItem item)) {
				continue;
			}
			BlockHitResult blockHit = new BlockHitResult(new Vec3(hit.x, pos.getY() + yFraction, hit.z), face, pos,
				false);
			BlockState state = placementState(item, stack, blockHit);
			states.put(pos, entry.applyTransforms(turning ? HeldTurn.apply(state, held.getItem()) : state));
		}
		Map<BlockPos, BlockState> otherHalves = new LinkedHashMap<>();
		states.forEach((pos, state) -> {
			// The placing rules give only the half placed first, which the game completes as it places it; a ghost
			// block gets both.
			BlockPos other = OtherHalf.of(pos, state);
			if (other != null) {
				otherHalves.put(other, OtherHalf.state(state));
			}
		});
		otherHalves.forEach(states::putIfAbsent);
		return states;
	}

	/**
	 * Hook E2: the player finished a shape with the break click, which erases the ghost blocks among its positions in
	 * the game's copy of the layer, with the other half of each two-block ghost block, as breaking either half of a real
	 * door or bed breaks both ({@link GhostErase#of}). A shape over no ghost blocks sends nothing. The click that
	 * finishes the shape ends the lock, as a placing one does (1239 Q1 A).
	 */
	public static void erase(BlockSet blocks) {
		ClientSession session = BuildingPlannerClient.session();
		if (session != null) {
			BuildingPlannerClient.captureShape(EditKind.ERASE,
				GhostErase.of(session.replica().layer(), blocks.validPositions()));
		}
		BuildingPlannerClient.shapeFinished();
	}

	/** Whether Effortless Building's mode is Single, whose line places and erases where the others start shapes. */
	static boolean singleMode() {
		return BuildModes.CLIENT.getBuildMode() == BuildModeEnum.SINGLE;
	}

	/** Whether Effortless Building's mode is Disable, where the player places real blocks. */
	static boolean disableMode() {
		return BuildModes.CLIENT.getBuildMode() == BuildModeEnum.DISABLED;
	}

	/**
	 * Esc's step out of a build mode (hook M7): Effortless Building's mode becomes Disable, with the radial menu's click
	 * sound, as choosing Disable in the menu does; choosing it ends the lock (hook E21).
	 */
	static void disable() {
		RadialMenu.playRadialMenuSound();
		BuildModes.CLIENT.setBuildMode(BuildModeEnum.DISABLED);
	}

	/** The name of Effortless Building's mode ("Line", say), or null in Disable. */
	static Component modeName() {
		BuildModeEnum mode = BuildModes.CLIENT.getBuildMode();
		return mode == BuildModeEnum.DISABLED ? null : Component.translatable(mode.getNameKey());
	}

	/** Whether a shape drawn with the break click is under way: it erases, so the turning controls aren't active. */
	static boolean breakShapeUnderWay() {
		return BuildPipelineClient.getBuildState() == BuildPipeline.BuildState.BREAKING;
	}

	/**
	 * The block Effortless Building's first click and its preview aim at now, unlocked: with its reach, at ghost blocks
	 * too (E9) and from Freecam's camera while it's on (E24). Null with nothing aimed at.
	 */
	private static BlockHitResult aimHit() {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null || mc.level == null) {
			return null;
		}
		Entity aim = Aim.forEffortless(player);
		Vec3 start = aim.getEyePosition();
		Vec3 end = start.add(aim.getLookAngle().scale(ServerConfig.INSTANCE.getReach(player)));
		ClipContext context = new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
		BlockHitResult hit = nearerHit(mc.level.clip(context), context);
		return hit.getType() == HitResult.Type.BLOCK ? hit : null;
	}

	/**
	 * Where a right click would start now, unlocked: beside the aimed face ({@link #aimHit}), at the position
	 * Effortless Building's preview draws its first block (its {@code resolveFirstClickPos}). Null with nothing aimed
	 * at.
	 */
	private static Aimed aimed() {
		BlockHitResult hit = aimHit();
		if (hit == null) {
			return null;
		}
		return new Aimed(BuildPipelineClientAccessor.buildingplanner$resolveFirstClickPos(hit,
			BuildPipeline.BuildState.PLACING, Minecraft.getInstance().level), hit);
	}

	/**
	 * Hook E38: the positions the red frame for the left click shows on now ({@link ShapeLock#framed}), from where a
	 * left click would start erasing ({@link #leftClickStart}); in Single, the ghost blocks among them, while ghost
	 * blocks show.
	 */
	public static List<BlockPos> leftClickFrame() {
		ClientSession session = BuildingPlannerClient.session();
		int step = step();
		return ShapeLock.framed(BuildingPlannerClient.lockActive(), singleMode(), step,
			step == 0 && session != null && session.lock().on(0), EffortlessShapes::leftClickStart,
			pos -> ghostShown(session, pos));
	}

	/**
	 * Hook E38: the ghost blocks among the positions the red frame shows on ({@link #leftClickFrame}), on which it draws
	 * more strongly ({@link GhostFrame#on}), while ghost blocks show.
	 */
	public static List<BlockPos> framedGhosts(List<BlockPos> framed) {
		ClientSession session = BuildingPlannerClient.session();
		return session == null ? List.of() : GhostFrame.on(framed, pos -> ghostShown(session, pos));
	}

	/** Whether the player's copy of the layer holds a ghost block at {@code pos} that shows now. */
	private static boolean ghostShown(ClientSession session, BlockPos pos) {
		return session.ghostsShown() && !session.replica().layer().get(pos).isAir();
	}

	/**
	 * Where a left click would start erasing now, unlocked: the aimed block itself ({@link #aimHit}), a ghost block or
	 * a real one, since Effortless Building's {@code resolveFirstClickPos} keeps the hit block for the break click,
	 * with its copies by the mirror, radial mirror and array, run through Effortless Building's modifiers and
	 * constraints ({@code processBlocks}) as a shape drawn with the break click is; only the valid ones, which a click
	 * erases at. None with nothing aimed at.
	 */
	private static List<BlockPos> leftClickStart() {
		BlockHitResult hit = aimHit();
		if (hit == null) {
			return List.of();
		}
		BlockPos pos = hit.getBlockPos();
		BlockSet blocks = new BlockSet();
		blocks.add(new BlockEntry(pos));
		blocks.firstPos = pos;
		blocks.lastPos = pos;
		BuildPipelineClient.CLIENT.processBlocks(blocks, Minecraft.getInstance().player,
			BuildPipeline.BuildState.BREAKING);
		return blocks.validPositions();
	}

	/** The position a right click would place at, and the hit that aims there. */
	private record Aimed(BlockPos pos, BlockHitResult hit) {
	}

	/**
	 * H, with the step under way unlocked ({@link #step}): before the first click, locks the first click's position
	 * where a right click would start now ({@link #aimed}); during a shape, locks the position the aim sets for the
	 * step now ({@link #found}). With nothing aimed at, it does nothing.
	 */
	static void lock(ShapeLock lock) {
		int step = step();
		if (step > 0) {
			BlockPos found = found(step);
			if (found != null) {
				lock.lockStep(step, found);
			}
			return;
		}
		Aimed aimed = aimed();
		if (aimed != null) {
			lock.lock(aimed.pos(), aimed.hit());
		}
	}

	/**
	 * The step under way (see {@link ShapeLock}): the number of positions the shape's clicks have set, 0 before the
	 * first click.
	 */
	static int step() {
		return shapeUnderWay() ? shapePositions().size() : 0;
	}

	/** How the step finds its position in Effortless Building's mode ({@link ShapeLock#finder}). */
	private static ShapeLock.Finder finder(int step) {
		BuildModeEnum mode = BuildModes.CLIENT.getBuildMode();
		return ShapeLock.finder(mode == BuildModeEnum.LINE, mode == BuildModeEnum.WALL, step);
	}

	/**
	 * The position the aim sets for a shape's step now: its finder's ({@link ShapeLock#finder}) from the positions the
	 * clicks have set, as Effortless Building's preview finds it, with the camera's line of sight while Freecam's is on
	 * (hook E26); or null where it finds none.
	 */
	private static BlockPos found(int step) {
		Player player = Minecraft.getInstance().player;
		List<BlockPos> set = shapePositions();
		if (player == null || set.size() != step) {
			return null;
		}
		ShapeLock.Finder finder = finder(step);
		BlockPos from = ShapeLock.from(finder, set);
		return switch (finder) {
			case LINE -> Line.findLine(player, from, true);
			case WALL -> Wall.findWall(player, from, true);
			case FLOOR -> Floor.findFloor(player, from, true);
			case HEIGHT -> ThreeClicksBuildMode.findHeight(player, from, true);
		};
	}

	/**
	 * Hook E37: the position a shape's step (1 or 2) takes ({@link ShapeLock#position}): while the lock's controls are
	 * active and the step is locked, its locked position, moved by the nudges; otherwise {@code found}, the one
	 * Effortless Building's aim found.
	 */
	public static BlockPos stepPosition(int step, BlockPos found) {
		return BuildingPlannerClient.lockActive() ? BuildingPlannerClient.session().lock().position(step, found)
			: found;
	}

	/**
	 * A nudge key while the step under way is locked ({@link ShapeLock#nudge}): before the first click, moves the
	 * locked position one block that way; during a shape, along a direction the step sets, the step's position, and
	 * along any other, the whole shape: the step's position, every position the clicks have set and the first click's
	 * hit with them (hook E33). Effortless Building's {@code findCoordinates} reads them each frame, so the preview and
	 * the finished shape follow.
	 */
	static void nudge(ShapeLock lock, Direction direction) {
		List<BlockPos> set = shapeUnderWay() ? shapePositions() : List.of();
		if (!lock.nudge(set, set.isEmpty() ? null : finder(set.size()), direction)) {
			return;
		}
		IBuildMode mode = BuildModes.CLIENT.getBuildMode().instance;
		if (mode instanceof TwoClicksBuildModeAccessor two) {
			two.buildingplanner$setFirstBlockEntry(moved(two.buildingplanner$firstBlockEntry(), direction));
		} else if (mode instanceof ThreeClicksBuildModeAccessor three) {
			three.buildingplanner$setFirstBlockEntry(moved(three.buildingplanner$firstBlockEntry(), direction));
			three.buildingplanner$setSecondBlockEntry(moved(three.buildingplanner$secondBlockEntry(), direction));
		}
		BlockHitResult hit = BuildPipelineClientAccessor.buildingplanner$firstClickHit();
		if (hit != null) {
			BuildPipelineClientAccessor.buildingplanner$setFirstClickHit(ShapeLock.moved(hit, direction));
		}
	}

	/** {@code entry} one block over that way, as a new entry, or null for none (a second click not made yet). */
	private static BlockEntry moved(BlockEntry entry, Direction direction) {
		return entry == null ? null : new BlockEntry(entry.blockPos.relative(direction));
	}

	/**
	 * The positions the clicks of the shape under way have set, its first first: the first in a mode of two clicks, and
	 * the second too after the second click in a mode of three.
	 */
	private static List<BlockPos> shapePositions() {
		IBuildMode mode = BuildModes.CLIENT.getBuildMode().instance;
		List<BlockPos> positions = new ArrayList<>(2);
		if (mode instanceof TwoClicksBuildModeAccessor two && two.buildingplanner$firstBlockEntry() != null) {
			positions.add(two.buildingplanner$firstBlockEntry().blockPos);
		} else if (mode instanceof ThreeClicksBuildModeAccessor three
			&& three.buildingplanner$firstBlockEntry() != null) {
			positions.add(three.buildingplanner$firstBlockEntry().blockPos);
			if (three.buildingplanner$secondBlockEntry() != null) {
				positions.add(three.buildingplanner$secondBlockEntry().blockPos);
			}
		}
		return positions;
	}

	/**
	 * The locked position the copy's box shows around (see "Locking and the first click": "Colours"), while the step
	 * under way is locked: before the first click, the locked first position; during a shape, the step's.
	 */
	static List<BlockPos> lockedPositions(ShapeLock lock) {
		int step = step();
		return lock.on(step) ? List.of(lock.pos(step)) : List.of();
	}

	/**
	 * The lock's colour over its locked positions {@code own} ({@link #lockedPositions}), with the locked position's hit
	 * before the first click or the first click's during a shape ({@link #colour}).
	 */
	static CopyColour.Shown lockColour(ShapeLock lock, List<BlockPos> own) {
		return colour(own, shapeUnderWay() ? BuildPipelineClient.getFirstClickHit() : lock.hit());
	}

	/**
	 * The locked positions as a set whose first and last positions are the list's ends, run through Effortless
	 * Building's modifiers, the block mix and its constraints ({@code processBlocks}), as its shapes are, so their colour
	 * covers what a right click places.
	 */
	private static BlockSet shownSet(List<BlockPos> positions) {
		BlockSet blocks = new BlockSet();
		positions.forEach(pos -> blocks.add(new BlockEntry(pos)));
		blocks.firstPos = positions.getFirst();
		blocks.lastPos = positions.getLast();
		BuildPipelineClient.CLIENT.processBlocks(blocks, Minecraft.getInstance().player,
			BuildPipeline.BuildState.PLACING);
		return blocks;
	}

	/**
	 * The colour of the locked positions (see "Locking and the first click": "Colours", {@link ClientSession#lockColour}):
	 * the positions {@code own}, and the blocks a right click
	 * would place there with {@code hit}, with the other half of a two-block one. The copies Effortless Building's
	 * modifiers make are left out, as its constraints leave out those outside the world.
	 */
	private static CopyColour.Shown colour(List<BlockPos> own, BlockHitResult hit) {
		Map<BlockPos, BlockState> all = states(shownSet(own), hit);
		Map<BlockPos, BlockState> placed = new LinkedHashMap<>();
		for (BlockPos pos : own) {
			BlockState state = all.get(pos);
			if (state == null) {
				continue;
			}
			placed.put(pos, state);
			BlockPos other = OtherHalf.of(pos, state);
			if (other != null && all.containsKey(other)) {
				placed.put(other, all.get(other));
			}
		}
		return BuildingPlannerClient.session().lockColour(own, placed);
	}

	/**
	 * The preview's state (hook E28): while the lock takes the first click, the held block's placement rules with the hit
	 * the position was locked with, moved by the nudges, and the player's facing now; otherwise the state Effortless Building found; then, while the shapes take the turn
	 * (every mode but Disable), the turn of the block in hand, so the preview shows the blocks a right click places. In
	 * Disable, while its preview shows, the state the right click would place ({@link #disableShown}).
	 */
	public static BlockState previewState(BlockItem item, ItemStack stack, BlockState aimed) {
		if (disableMode()) {
			DisableShown shown = disableShown();
			return shown == null ? aimed : shown.state();
		}
		BlockHitResult shown = shownHit();
		BlockState state = shown == null ? aimed : placementState(item, stack, shown);
		Player player = Minecraft.getInstance().player;
		return BuildingPlannerClient.shapesTakeTurn() && player != null
			? HeldTurn.apply(state, player.getMainHandItem().getItem()) : state;
	}

	/**
	 * The hit Building Planner gives the preview's block in place of the aim: the locked position's while the lock takes
	 * the first click; otherwise null.
	 */
	private static BlockHitResult shownHit() {
		return BuildingPlannerClient.lockTakesFirstClick() ? BuildingPlannerClient.session().lock().hit() : null;
	}

	/**
	 * The block's state by its placement rules for {@code hit}, as the player at the aim would place it (Freecam's camera
	 * while it's on, see {@link Aim}), before any turn.
	 */
	private static BlockState placementState(BlockItem item, ItemStack stack, BlockHitResult hit) {
		BlockState state = item.getBlock().getStateForPlacement(
			new BlockPlaceContext(Aim.placer(), InteractionHand.MAIN_HAND, stack, hit));
		return state != null ? state : item.getBlock().defaultBlockState();
	}

	/**
	 * The block in hand as Minecraft would place it now, before any turn, as the preview shows it: with the hit the
	 * first position was locked with, the shape's first click, or the aim; in Disable, as Minecraft's
	 * own right click would place it ({@link #placedByMinecraft}); aimed at nothing, the block's default state. Null
	 * without a block in hand.
	 */
	private static BlockState unturnedNow() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !(mc.player.getMainHandItem().getItem() instanceof BlockItem item)) {
			return null;
		}
		if (disableMode()) {
			return placedByMinecraft(item, mc.player.getMainHandItem());
		}
		BlockHitResult hit = clickHit();
		return hit == null ? item.getBlock().defaultBlockState() : placementState(item, mc.player.getMainHandItem(), hit);
	}

	/**
	 * The hit the place click's shape takes its blocks' states from now: the locked position's while the lock takes the
	 * first click, the shape's first click, or the aim ({@link #aimed}); null aimed at nothing.
	 */
	static BlockHitResult clickHit() {
		BlockHitResult shown = shownHit();
		BlockHitResult hit = shown != null ? shown : BuildPipelineClient.getFirstClickHit();
		if (hit == null) {
			Aimed aimed = aimed();
			return aimed == null ? null : aimed.hit();
		}
		return hit;
	}

	/** In Disable, the block Minecraft's own right click would place now ({@link #placedByMinecraft}), or null. */
	static Block placedInDisable() {
		BlockState state = unturnedNow();
		return state == null ? null : state.getBlock();
	}

	/**
	 * In Disable, whether the block Minecraft's own right click would place now hangs on the block behind it
	 * ({@link DisablePreview#hangs}), so Disable's line leaves out holding Left Control.
	 */
	static boolean placedHangs() {
		BlockState state = unturnedNow();
		return state != null && DisablePreview.hangs(state);
	}

	/**
	 * Disable's preview (hooks E28, E29 and E30, see "Turning real blocks in Disable": "The preview"): while it shows
	 * ({@link BuildingPlannerClient#disablePreviewOn}), the block a right click would place, from Minecraft's own aim
	 * ({@code Minecraft.hitResult}, at its reach), where the item's own placing puts it
	 * ({@code BlockPlaceContext.getClickedPos}, after {@code updatePlacementContext}), in the state hook S2 would give it
	 * with the turn of the block in hand, or the ghost block's turn that "Match ghost blocks" gives there
	 * ({@link #withGhostTurn}, {@link PlaceTurns#place}), and whether it would go in. Null while the preview
	 * doesn't show, the aim isn't a block, the player is beyond the reach the server places within
	 * ({@link DisablePreview#withinReach}, from the player, not Freecam's camera), or the block can't go there at all
	 * ({@code BlockPlaceContext.canPlace}).
	 */
	static DisableShown disableShown() {
		Minecraft mc = Minecraft.getInstance();
		if (!BuildingPlannerClient.disablePreviewOn() || !(mc.hitResult instanceof BlockHitResult hit)
			|| hit.getType() != HitResult.Type.BLOCK || !(mc.player.getMainHandItem().getItem() instanceof BlockItem item)
			|| !DisablePreview.withinReach(mc.player.getEyePosition(), hit.getBlockPos(),
				mc.player.blockInteractionRange())) {
			return null;
		}
		BlockPlaceContext context = item.updatePlacementContext(
			new BlockPlaceContext(mc.player, InteractionHand.MAIN_HAND, mc.player.getMainHandItem(), hit));
		if (context == null || !context.canPlace()) {
			return null;
		}
		BlockItemInvoker invoker = (BlockItemInvoker) item;
		BlockTurn.Turn turn = HeldTurn.of(item);
		BlockState placed = invoker.buildingplanner$placementState(context);
		if (placed == null) {
			// Minecraft's own right click places nothing here, so neither does the turned one.
			BlockState block = item.getBlock().defaultBlockState();
			return new DisableShown(context.getClickedPos(), turn == null ? block : BlockTurn.apply(block, turn), false);
		}
		BlockTurn.Held held = withGhostTurn(context, item, placed,
			turn == null ? BlockTurn.Held.NONE : new BlockTurn.Held(item, turn));
		PlaceTurns.Placed turned = PlaceTurns.place(context, placed, held,
			state -> invoker.buildingplanner$canPlace(context, state));
		// BlockItem.place checks a state the turn leaves as it was too (a player standing in its way, say).
		boolean goes = turned.stays() && invoker.buildingplanner$canPlace(context, turned.state());
		return new DisableShown(context.getClickedPos(), turned.state(), goes);
	}

	/** Disable's preview: where the block goes, its state, and whether it goes in, or is drawn red. */
	record DisableShown(BlockPos pos, BlockState state, boolean goes) {
	}

	/**
	 * Hook M6, in Disable: the turn a right click with {@code hit} takes, which is {@code otherwise} (Disable's turn,
	 * {@link dev.buildingplanner.input.DisableTurn#clickTakes}) unless "Match ghost blocks" gives the ghost block's turn
	 * there ({@link #withGhostTurn}). The click places where the item's own placing puts it
	 * ({@code BlockPlaceContext.getClickedPos}, after {@code updatePlacementContext}), as Disable's preview finds it.
	 */
	static BlockTurn.Held clickTurn(BlockHitResult hit, BlockTurn.Held otherwise) {
		Player player = Minecraft.getInstance().player;
		if (!(player.getMainHandItem().getItem() instanceof BlockItem item)) {
			return otherwise;
		}
		BlockPlaceContext context = item.updatePlacementContext(
			new BlockPlaceContext(player, InteractionHand.MAIN_HAND, player.getMainHandItem(), hit));
		if (context == null || !context.canPlace()) {
			return otherwise;
		}
		return withGhostTurn(context, item, ((BlockItemInvoker) item).buildingplanner$placementState(context),
			otherwise);
	}

	/**
	 * "Match ghost blocks" (see "Settings"): with the setting on and ghost blocks shown, where the player's copy of the layer has a ghost block
	 * of the block Minecraft would place ({@code placed}, {@link GhostMatch#turn}), the ghost block's turn when the block
	 * turned so can stay (hook S2's step, {@link PlaceTurns#place}, and {@code BlockItem.canPlace}); otherwise
	 * {@code otherwise}, the turn the click takes with the setting off.
	 */
	private static BlockTurn.Held withGhostTurn(BlockPlaceContext context, BlockItem item, BlockState placed,
		BlockTurn.Held otherwise) {
		ClientSession session = BuildingPlannerClient.session();
		if (session == null) {
			return otherwise;
		}
		BlockTurn.Held ghost = GhostMatch.turn(BuildingPlannerClient.matchGhost(), session.ghostsShown(),
			session.replica().layer().get(context.getClickedPos()), placed, item);
		if (ghost.turn() == null) {
			return otherwise;
		}
		BlockItemInvoker invoker = (BlockItemInvoker) item;
		PlaceTurns.Placed turned = PlaceTurns.place(context, placed, ghost,
			state -> invoker.buildingplanner$canPlace(context, state));
		return GhostMatch.clickTakes(ghost, turned.stays() && invoker.buildingplanner$canPlace(context, turned.state()),
			otherwise);
	}

	/**
	 * Hook E29: in Disable, while its preview shows, the preview draws the one block a right click would place
	 * ({@link #disableShown}), and no copies of the mirror or array, since Disable places only the clicked block; or
	 * nothing where it wouldn't place. A block that wouldn't go in is marked missing, which the preview draws red, as
	 * Effortless Building draws a block the player lacks (hook E8 clears the held block's marks, so it's the only one).
	 */
	public static BlockSet disablePreview(BlockSet aimed) {
		if (!BuildingPlannerClient.disablePreviewOn()) {
			return aimed;
		}
		DisableShown shown = disableShown();
		if (shown == null) {
			return null;
		}
		BlockSet blocks = new BlockSet();
		blocks.add(new BlockEntry(shown.pos()));
		blocks.firstPos = shown.pos();
		blocks.lastPos = shown.pos();
		BuildPipelineClient.ITEM_USAGE.initialize();
		if (!shown.goes()) {
			BuildPipelineClient.ITEM_USAGE.missingPositions.add(shown.pos());
		}
		return blocks;
	}

	/**
	 * The block in hand as Minecraft's own right click would place it now, before any turn: at Minecraft's aim
	 * ({@code Minecraft.hitResult}, at its reach, from Freecam's camera while it's on) and by the player's facing, through
	 * the item's own placing ({@code updatePlacementContext}, then {@code getPlacementState}), so a torch aimed at a wall
	 * is a wall torch. Aimed at nothing, or where it can't go, the block's default state.
	 */
	private static BlockState placedByMinecraft(BlockItem item, ItemStack stack) {
		Minecraft mc = Minecraft.getInstance();
		BlockState state = null;
		if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
			BlockPlaceContext context = item.updatePlacementContext(
				new BlockPlaceContext(mc.player, InteractionHand.MAIN_HAND, stack, hit));
			state = context == null ? null : ((BlockItemInvoker) item).buildingplanner$placementState(context);
		}
		return state != null ? state : item.getBlock().defaultBlockState();
	}

	/**
	 * Hook M5, with the sprint key held: a quarter turn of the block in hand, clockwise seen from above for a notch up
	 * ({@link HeldTurn#turn}).
	 */
	static void turnHeld(boolean clockwise) {
		BlockState now = unturnedNow();
		if (now != null) {
			HeldTurn.turn(Minecraft.getInstance().player.getMainHandItem().getItem(), now, toward(), clockwise);
		}
	}

	/** Hook M4, with the sprint key held: flips the block in hand to the next way it can sit ({@link HeldTurn#flip}). */
	static void flipHeld() {
		BlockState now = unturnedNow();
		if (now != null) {
			HeldTurn.flip(Minecraft.getInstance().player.getMainHandItem().getItem(), now, toward());
		}
	}

	/** The way a block set on its side faces when placed: toward the player at the aim, as a piston placed sideways. */
	private static Direction toward() {
		return Aim.placer().getDirection().getOpposite();
	}

	/**
	 * Hook E30: the preview skips a set of one block while no shape is under way, so it would draw nothing before a
	 * mode's first click; the first click's block, at the aim or at the locked position, is drawn in every mode but
	 * Disable while the lock's controls are active ({@link ShapeLock#drawnBeforeFirstClick}), and so is Disable's block
	 * while its preview shows. A set of copies, with the mirror or array on, is
	 * already more than one block and is drawn once.
	 */
	public static int previewSize(int size) {
		boolean drawn = ShapeLock.drawnBeforeFirstClick(BuildingPlannerClient.lockActive(), size)
			|| size == 1 && BuildingPlannerClient.disablePreviewOn();
		return size <= 1 && drawn ? 2 : size;
	}

	/**
	 * Hook E9, for Effortless Building's first click and its preview: while the lock takes the first click, the hit the
	 * position was locked with, moved by the nudges, whatever the player aims at, so that click starts from the locked
	 * position (hook E32) and {@code getFirstClickHit()} gives E1 that hit's face; otherwise {@link #nearerHit}.
	 */
	public static BlockHitResult shapeAim(BlockHitResult real, ClipContext context) {
		return BuildingPlannerClient.lockTakesFirstClick() ? BuildingPlannerClient.session().lock().hit()
			: nearerHit(real, context);
	}

	/**
	 * Hook E32: while the lock takes the first click, the locked position, for the place click and the break click
	 * alike; otherwise the position Effortless Building found, {@code found}.
	 */
	public static BlockPos firstClickPos(BlockPos found) {
		return BuildingPlannerClient.lockTakesFirstClick() ? BuildingPlannerClient.session().lock().pos(0) : found;
	}

	/**
	 * Hook E9: aims at the nearer of the real block Effortless Building found and a ghost block of the layer (not the
	 * copy, which floats where the player aims), so shapes can start on ghost blocks while they show.
	 */
	public static BlockHitResult nearerHit(BlockHitResult real, ClipContext context) {
		ClientSession session = BuildingPlannerClient.session();
		Level level = Minecraft.getInstance().level;
		if (session == null || level == null) {
			return real;
		}
		return GhostAim.nearer(real,
			GhostAim.ghostHit(session.replica().layer(), session.ghostsShown(), level, context), context.getFrom());
	}

	/**
	 * Hook E9: the block at a position as the start position sees it: a ghost block where the real one can be replaced
	 * (air, grass), so a shape started on a ghost block goes beside it, not into it.
	 */
	public static BlockState startBlock(Level level, BlockPos pos, BlockState real) {
		ClientSession session = BuildingPlannerClient.session();
		if (!real.canBeReplaced() || session == null || !session.ghostsShown()) {
			return real;
		}
		BlockState ghost = session.replica().layer().get(pos);
		return ghost.isAir() ? real : ghost;
	}
}
