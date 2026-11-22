package dev.buildingplanner.client;

import dev.buildingplanner.input.CopyMode;
import dev.buildingplanner.input.CopyPlacement;
import dev.buildingplanner.input.CopyRow;
import dev.buildingplanner.input.MixRule;
import dev.buildingplanner.input.SingleRule;
import dev.buildingplanner.layer.GhostErase;
import dev.buildingplanner.layer.OtherHalf;
import dev.buildingplanner.mixin.effortless.BuildPipelineClientAccessor;
import dev.buildingplanner.sync.EditKind;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.requios.effortlessbuilding.buildmode.BuildModeEnum;
import nl.requios.effortlessbuilding.buildmode.BuildModes;
import nl.requios.effortlessbuilding.buildpipeline.BuildPipeline;
import nl.requios.effortlessbuilding.buildpipeline.BuildPipelineClient;
import nl.requios.effortlessbuilding.config.ServerConfig;
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
	 * Hook E1: the player finished a shape with the place click. Each valid entry gets the block Effortless Building
	 * would place: the mix's (set by hook E5) or the held block, by its placement rules with the first click's face and
	 * the facing of the player at the aim (Freecam's camera while it's on, see {@link Aim}), then the turn of the block in
	 * hand while the turning controls are active ({@link HeldTurn}), then the shape's mirror and turn from the modifiers.
	 * {@code first} is the hit the states come from: the shape's first click (E1), or the locked block's
	 * ({@link SingleBlock#hit}); with none, the top face at the middle of the shape's first position.
	 */
	public static void place(BlockSet blocks, BlockHitResult first) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		Player placer = Aim.placer();
		Direction face = first != null ? first.getDirection() : Direction.UP;
		Vec3 hit = first != null ? first.getLocation() : Vec3.atCenterOf(blocks.firstPos);
		double yFraction = hit.y - Math.floor(hit.y);
		ItemStack held = player.getMainHandItem();
		boolean mix = mixApplies();
		boolean turning = BuildingPlannerClient.turningActive();
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
		BuildingPlannerClient.captureShape(EditKind.SHAPE, states);
	}

	/**
	 * Hook E2: the player finished a shape with the break click, which erases the ghost blocks among its positions in
	 * the game's copy of the layer, with the other half of each two-block ghost block, as breaking either half of a real
	 * door or bed breaks both ({@link GhostErase#of}). A shape over no ghost blocks sends nothing.
	 */
	public static void erase(BlockSet blocks) {
		ClientSession session = BuildingPlannerClient.session();
		if (session != null) {
			BuildingPlannerClient.captureShape(EditKind.ERASE,
				GhostErase.of(session.replica().layer(), blocks.validPositions()));
		}
	}

	/** Whether Effortless Building's mode is Single, where the one ghost block's controls act. */
	static boolean singleMode() {
		return BuildModes.CLIENT.getBuildMode() == BuildModeEnum.SINGLE;
	}

	/** Whether Effortless Building's mode is Disable, where the player places real blocks. */
	static boolean disableMode() {
		return BuildModes.CLIENT.getBuildMode() == BuildModeEnum.DISABLED;
	}

	/** Whether a shape drawn with the break click is under way: it erases, so the turning controls aren't active. */
	static boolean breakShapeUnderWay() {
		return BuildPipelineClient.getBuildState() == BuildPipeline.BuildState.BREAKING;
	}

	/**
	 * Where a right click would place the one ghost block now: beside the aimed face, at the position Effortless
	 * Building's preview draws its first block (its {@code resolveFirstClickPos}), aimed as its preview aims: with
	 * Single's reach, at ghost blocks too (E9) and from Freecam's camera while it's on (E24). Null with nothing aimed at.
	 */
	private static Aimed aimed() {
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
		if (hit.getType() != HitResult.Type.BLOCK) {
			return null;
		}
		return new Aimed(BuildPipelineClientAccessor.buildingplanner$resolveFirstClickPos(hit,
			BuildPipeline.BuildState.PLACING, mc.level), hit);
	}

	/** The position a right click would place at, and the hit that aims there. */
	private record Aimed(BlockPos pos, BlockHitResult hit) {
	}

	/**
	 * H: locks the one ghost block where a right click would place it now ({@link #aimed}). Returns whether anything was
	 * aimed at; with nothing, the block stays unlocked.
	 */
	static boolean lock(SingleBlock block) {
		Aimed aimed = aimed();
		if (aimed == null) {
			return false;
		}
		block.lock(aimed.pos(), aimed.hit());
		return true;
	}

	/**
	 * The block as Building Planner shows it, while it's locked or its row is fixed: the row's positions, or the locked
	 * block alone, as a set whose first and last positions are the row's ends, run through Effortless Building's
	 * modifiers, the block mix and its constraints ({@code processBlocks}), as its shapes are: what its preview draws
	 * (hook E27) and a right click places.
	 */
	static BlockSet shownSet(SingleBlock block) {
		List<BlockPos> positions = block.rowFixed() ? block.row() : List.of(block.pos());
		BlockSet blocks = new BlockSet();
		positions.forEach(pos -> blocks.add(new BlockEntry(pos)));
		blocks.firstPos = positions.getFirst();
		blocks.lastPos = positions.getLast();
		BuildPipelineClient.CLIENT.processBlocks(blocks, Minecraft.getInstance().player,
			BuildPipeline.BuildState.PLACING);
		return blocks;
	}

	/**
	 * Hook M2: the right click that's the block's ({@link SingleRule#takesPlace}). In Single, locked, it places the block
	 * where it's locked. In Zoop and Vertical the first click fixes the row's start where the block is (where it's
	 * locked, if it is, or where a right click would place it) and sends nothing; with nothing aimed at and the block
	 * unlocked, it does nothing. The second places the whole row as one shape edit and drops the row, and the block
	 * follows the aim again, or stays locked where it was.
	 */
	static void rightClick(SingleBlock block) {
		if (SingleBlock.mode() == CopyMode.SINGLE || block.rowFixed()) {
			placeShown(block);
			block.dropRow();
		} else if (block.locked()) {
			block.fixRow(block.pos(), block.hit());
		} else {
			Aimed aimed = aimed();
			if (aimed != null) {
				block.fixRow(aimed.pos(), aimed.hit());
			}
		}
	}

	/**
	 * Once a client tick while the block's row is fixed and its controls are active: lays the row from its start toward
	 * the aim ({@link CopyRow#row}, up to {@link CopyRow#MAX_BLOCKS}): in Zoop toward where a right click would place,
	 * leaving the row as it was while the player aims at nothing; in Vertical toward the view's height above or below the
	 * start ({@link CopyRow#verticalAim}), within Single's reach.
	 */
	static void followRow(SingleBlock block) {
		Player player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		CopyPlacement.Spot start = new CopyPlacement.Spot(block.rowStart(), new Vec3i(1, 1, 1));
		BlockPos toward;
		if (SingleBlock.mode() == CopyMode.VERTICAL) {
			Entity view = Aim.forEffortless(player);
			toward = CopyRow.verticalAim(start, view.getEyePosition(), view.getLookAngle(),
				ServerConfig.INSTANCE.getReach(player));
		} else {
			Aimed aimed = aimed();
			if (aimed == null) {
				return;
			}
			toward = aimed.pos();
		}
		block.showRow(CopyRow.row(SingleBlock.mode(), start, toward, CopyRow.MAX_BLOCKS).stream()
			.map(CopyPlacement.Spot::min).toList());
	}

	/**
	 * Places the block where it's locked, or its fixed row, as one shape edit, with the hit it was locked or fixed with
	 * and the player's facing now, as a Single click would place it there.
	 */
	private static void placeShown(SingleBlock block) {
		BlockSet blocks = shownSet(block);
		place(blocks, block.hit());
		Minecraft mc = Minecraft.getInstance();
		if (!blocks.validEntries().isEmpty() && mc.player.getMainHandItem().getItem() instanceof BlockItem item) {
			SoundType sound = item.getBlock().defaultBlockState().getSoundType();
			mc.level.playLocalSound(blocks.firstPos, sound.getPlaceSound(), SoundSource.BLOCKS, sound.getVolume(),
				sound.getPitch(), false);
		}
	}

	/**
	 * Hook M1: the left click that's the block's ({@link SingleRule#takesBreak}). With a row fixed, it drops the row and
	 * erases nothing. Otherwise the block is locked, and it erases every ghost block a right click would place
	 * ({@link #shownSet}: the locked position and the copies Effortless Building's mirror, radial mirror and array make
	 * of it, 1074 Q1 A), whatever the player aims at, with the other half of a two-block ghost block
	 * ({@link GhostErase#of}); with no ghost block there, it sends nothing.
	 */
	static void leftClick(SingleBlock block) {
		if (block.rowFixed()) {
			block.dropRow();
			return;
		}
		ClientSession session = BuildingPlannerClient.session();
		Map<BlockPos, BlockState> erased = GhostErase.of(session.replica().layer(), shownSet(block).validPositions());
		if (erased.isEmpty()) {
			return;
		}
		// The sound of the first ghost block erased, read before the erase empties it.
		BlockPos first = erased.keySet().iterator().next();
		SoundType sound = session.replica().layer().get(first).getSoundType();
		if (session.sendShape(EditKind.ERASE, erased)) {
			Minecraft.getInstance().level.playLocalSound(first, sound.getBreakSound(), SoundSource.BLOCKS,
				sound.getVolume(), sound.getPitch(), false);
		}
	}

	/**
	 * The preview's state (hook E28): for the one ghost block locked or in a fixed row, the held block's placement rules
	 * with the hit it was locked or its row fixed with and the player's facing now, and otherwise the state Effortless
	 * Building found; then, while the turning controls are active, the turn of the block in hand, so the preview shows
	 * the blocks a right click places.
	 */
	public static BlockState previewState(BlockItem item, ItemStack stack, BlockState aimed) {
		SingleBlock block = BuildingPlannerClient.shownSingle();
		BlockState state = block == null ? aimed : placementState(item, stack, block.hit());
		Player player = Minecraft.getInstance().player;
		return BuildingPlannerClient.turningActive() && player != null
			? HeldTurn.apply(state, player.getMainHandItem().getItem()) : state;
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
	 * one ghost block was locked or its row fixed with, the shape's first click, or the aim; aimed at nothing, the
	 * block's default state. Null without a block in hand.
	 */
	private static BlockState unturnedNow() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !(mc.player.getMainHandItem().getItem() instanceof BlockItem item)) {
			return null;
		}
		SingleBlock block = BuildingPlannerClient.shownSingle();
		BlockHitResult hit = block != null ? block.hit() : BuildPipelineClient.getFirstClickHit();
		if (hit == null) {
			Aimed aimed = aimed();
			if (aimed == null) {
				return item.getBlock().defaultBlockState();
			}
			hit = aimed.hit();
		}
		return placementState(item, mc.player.getMainHandItem(), hit);
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
	 * Hook E27: while the one ghost block is locked or its row is fixed, and its controls are active, the preview draws
	 * the locked block or the row ({@link #shownSet}) in place of the block at the aim.
	 */
	public static BlockSet previewBlocks(BlockSet aimed) {
		SingleBlock block = BuildingPlannerClient.shownSingle();
		return block == null ? aimed : shownSet(block);
	}

	/**
	 * Hook E30: the preview skips a set of one block while no shape is under way, so Effortless Building's Single mode
	 * draws nothing for the block at the aim; the locked block, which may be far from the aim, and a row of one are
	 * drawn.
	 */
	public static int previewSize(int size) {
		return size <= 1 && BuildingPlannerClient.shownSingle() != null ? 2 : size;
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
