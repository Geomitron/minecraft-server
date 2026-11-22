package dev.buildingplanner.client;

import dev.buildingplanner.input.MixRule;
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
	 * the facing of the player at the aim (Freecam's camera while it's on, see {@link Aim}), then the shape's mirror and
	 * turn. {@code first} is the hit the states come from: the shape's first click (E1), or the locked block's
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
			BlockState state = item.getBlock().getStateForPlacement(
				new BlockPlaceContext(placer, InteractionHand.MAIN_HAND, stack, blockHit));
			states.put(pos, entry.applyTransforms(state != null ? state : item.getBlock().defaultBlockState()));
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

	/**
	 * H: locks the one ghost block where a right click would place it now, beside the aimed face, at the position
	 * Effortless Building's preview draws its first block (its {@code resolveFirstClickPos}), aimed as its preview aims:
	 * with Single's reach, at ghost blocks too (E9) and from Freecam's camera while it's on (E24). Returns whether
	 * anything was aimed at; with nothing, the block stays unlocked.
	 */
	static boolean lock(SingleBlock block) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null || mc.level == null) {
			return false;
		}
		Entity aim = Aim.forEffortless(player);
		Vec3 start = aim.getEyePosition();
		Vec3 end = start.add(aim.getLookAngle().scale(ServerConfig.INSTANCE.getReach(player)));
		ClipContext context = new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
		BlockHitResult hit = nearerHit(mc.level.clip(context), context);
		if (hit.getType() != HitResult.Type.BLOCK) {
			return false;
		}
		block.lock(BuildPipelineClientAccessor.buildingplanner$resolveFirstClickPos(hit,
			BuildPipeline.BuildState.PLACING, mc.level), hit);
		return true;
	}

	/**
	 * The locked block as a set of one, run through Effortless Building's modifiers, the block mix and its constraints
	 * ({@code processBlocks}), as its shapes are: what its preview draws (hook E27) and a right click places.
	 */
	static BlockSet lockedSet(SingleBlock block) {
		BlockSet blocks = new BlockSet();
		BlockPos pos = block.pos();
		blocks.add(new BlockEntry(pos));
		blocks.firstPos = pos;
		blocks.lastPos = pos;
		BuildPipelineClient.CLIENT.processBlocks(blocks, Minecraft.getInstance().player,
			BuildPipeline.BuildState.PLACING);
		return blocks;
	}

	/**
	 * Hook M2: the right click while the block is locked places it where it's locked, as one shape edit, with the hit it
	 * was locked with and the player's facing now, as a Single click would place it there.
	 */
	static void placeLocked(SingleBlock block) {
		BlockSet blocks = lockedSet(block);
		place(blocks, block.hit());
		Minecraft mc = Minecraft.getInstance();
		if (!blocks.validEntries().isEmpty() && mc.player.getMainHandItem().getItem() instanceof BlockItem item) {
			SoundType sound = item.getBlock().defaultBlockState().getSoundType();
			mc.level.playLocalSound(block.pos(), sound.getPlaceSound(), SoundSource.BLOCKS, sound.getVolume(),
				sound.getPitch(), false);
		}
	}

	/**
	 * Hook M1: the left click while the block is locked erases the ghost block at the locked position, whatever the
	 * player aims at, with the other half of a two-block ghost block ({@link GhostErase#of}). With no ghost block there,
	 * it sends nothing.
	 */
	static void eraseLocked(SingleBlock block) {
		ClientSession session = BuildingPlannerClient.session();
		Minecraft mc = Minecraft.getInstance();
		BlockState ghost = session.replica().layer().get(block.pos());
		if (session.sendShape(EditKind.ERASE, GhostErase.of(session.replica().layer(), List.of(block.pos())))) {
			SoundType sound = ghost.getSoundType();
			mc.level.playLocalSound(block.pos(), sound.getBreakSound(), SoundSource.BLOCKS, sound.getVolume(),
				sound.getPitch(), false);
		}
	}

	/**
	 * The locked block's state for the preview (hook E28): the held block's placement rules with the hit it was locked
	 * with and the player's facing now, as {@link #placeLocked} places it, so the preview shows the block a right click
	 * places. Otherwise the state Effortless Building found for the aim.
	 */
	public static BlockState previewState(BlockItem item, ItemStack stack, BlockState aimed) {
		SingleBlock block = BuildingPlannerClient.lockedSingle();
		if (block == null) {
			return aimed;
		}
		BlockState state = item.getBlock().getStateForPlacement(
			new BlockPlaceContext(Aim.placer(), InteractionHand.MAIN_HAND, stack, block.hit()));
		return state != null ? state : item.getBlock().defaultBlockState();
	}

	/**
	 * Hook E27: while the one ghost block is locked and its controls are active, the preview draws the locked block
	 * ({@link #lockedSet}) in place of the block at the aim.
	 */
	public static BlockSet previewBlocks(BlockSet aimed) {
		SingleBlock block = BuildingPlannerClient.lockedSingle();
		return block == null ? aimed : lockedSet(block);
	}

	/**
	 * Hook E30: the preview skips a set of one block while no shape is under way, so Effortless Building's Single mode
	 * draws nothing for the block at the aim; the locked block, which may be far from the aim, is drawn.
	 */
	public static int previewSize(int size) {
		return size <= 1 && BuildingPlannerClient.lockedSingle() != null ? 2 : size;
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
