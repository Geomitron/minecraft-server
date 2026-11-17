package dev.buildingplanner.client;

import dev.buildingplanner.input.MixRule;
import dev.buildingplanner.sync.EditKind;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.requios.effortlessbuilding.buildmode.BuildModeEnum;
import nl.requios.effortlessbuilding.buildmode.BuildModes;
import nl.requios.effortlessbuilding.buildpipeline.BuildPipelineClient;
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
	 * would place: the mix's (set by hook E5) or the held block, by its placement rules with the first click's face,
	 * then the shape's mirror and turn.
	 */
	public static void place(BlockSet blocks) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		BlockHitResult first = BuildPipelineClient.getFirstClickHit();
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
				new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, blockHit));
			states.put(pos, entry.applyTransforms(state != null ? state : item.getBlock().defaultBlockState()));
		}
		Map<BlockPos, BlockState> otherHalves = new LinkedHashMap<>();
		states.forEach((pos, state) -> {
			BlockPos other = otherHalf(pos, state);
			if (other != null) {
				otherHalves.put(other, otherHalfState(state));
			}
		});
		otherHalves.forEach(states::putIfAbsent);
		BuildingPlannerClient.captureShape(EditKind.SHAPE, states);
	}

	/**
	 * Where the other half of a two-block block is (a door's or a tall plant's upper half, a bed's head), or null for a
	 * block of one. The placing rules give only the half placed first, which the game completes as it places it; a
	 * ghost block gets both.
	 */
	private static BlockPos otherHalf(BlockPos pos, BlockState state) {
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
			return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
		}
		if (state.hasProperty(BlockStateProperties.BED_PART) && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
			return state.getValue(BlockStateProperties.BED_PART) == BedPart.FOOT ? pos.relative(facing)
				: pos.relative(facing.getOpposite());
		}
		return null;
	}

	/** The state of the other half, for a state {@link #otherHalf} finds one for. */
	private static BlockState otherHalfState(BlockState state) {
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
			return state.cycle(BlockStateProperties.DOUBLE_BLOCK_HALF);
		}
		return state.cycle(BlockStateProperties.BED_PART);
	}

	/**
	 * Hook E2: the player finished a shape with the break click, which erases its ghost blocks, with the other
	 * half of each two-block ghost block, as breaking either half of a real door or bed breaks both.
	 */
	public static void erase(BlockSet blocks) {
		Map<BlockPos, BlockState> states = new LinkedHashMap<>();
		Level ghosts = SchematicWorldHandler.getSchematicWorld();
		for (BlockPos pos : blocks.validPositions()) {
			states.put(pos, Blocks.AIR.defaultBlockState());
			BlockPos other = ghosts == null ? null : otherHalf(pos, ghosts.getBlockState(pos));
			if (other != null) {
				states.put(other, Blocks.AIR.defaultBlockState());
			}
		}
		BuildingPlannerClient.captureShape(EditKind.ERASE, states);
	}

	/**
	 * Hook E9: aims at the nearer of the real block Effortless Building found and a ghost block, so shapes can
	 * start on ghost blocks.
	 */
	public static BlockHitResult nearerHit(BlockHitResult real, ClipContext context) {
		Player player = Minecraft.getInstance().player;
		if (player == null) {
			return real;
		}
		double range = context.getFrom().distanceTo(context.getTo());
		BlockHitResult ghost = RayTraceUtils.traceToSchematicWorld(player, range, true, false);
		if (ghost == null || ghost.getType() != HitResult.Type.BLOCK) {
			return real;
		}
		if (real.getType() == HitResult.Type.BLOCK
			&& context.getFrom().distanceToSqr(real.getLocation()) <= context.getFrom().distanceToSqr(ghost.getLocation())) {
			return real;
		}
		return ghost;
	}

	/**
	 * Hook E9: the block at a position as the start position sees it: a ghost block where the real one can be replaced
	 * (air, grass), so a shape started on a ghost block goes beside it, not into it.
	 */
	public static BlockState startBlock(Level level, BlockPos pos, BlockState real) {
		if (!real.canBeReplaced()) {
			return real;
		}
		Level schematic = SchematicWorldHandler.getSchematicWorld();
		if (schematic == null) {
			return real;
		}
		BlockState ghost = schematic.getBlockState(pos);
		return ghost.isAir() ? real : ghost;
	}
}
