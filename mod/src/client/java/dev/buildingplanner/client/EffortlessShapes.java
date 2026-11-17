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
	 * Hooks E5 and E6: whether the block mix applies now: the mix is on, the main hand holds a block, and Effortless
	 * Building's mode isn't Disable (see {@link MixRule}).
	 */
	public static boolean mixApplies() {
		Player player = Minecraft.getInstance().player;
		return MixRule.applies(BuildingPlannerClient.mixOn(),
			player != null && player.getMainHandItem().getItem() instanceof BlockItem,
			BuildModes.CLIENT.getBuildMode() == BuildModeEnum.DISABLED);
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
		BuildingPlannerClient.captureShape(EditKind.SHAPE, states);
	}

	/** Hook E2: the player finished a shape with the break click, which erases its ghost blocks. */
	public static void erase(BlockSet blocks) {
		Map<BlockPos, BlockState> states = new LinkedHashMap<>();
		for (BlockPos pos : blocks.validPositions()) {
			states.put(pos, Blocks.AIR.defaultBlockState());
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
