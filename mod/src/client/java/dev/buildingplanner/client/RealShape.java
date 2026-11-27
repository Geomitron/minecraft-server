package dev.buildingplanner.client;

import dev.buildingplanner.input.BlockTurn;
import dev.buildingplanner.input.GhostMatch;
import dev.buildingplanner.input.PlaceTurn;
import dev.buildingplanner.input.RealPlace;
import dev.buildingplanner.mixin.minecraft.BlockItemInvoker;
import dev.buildingplanner.server.PlaceTurns;
import dev.buildingplanner.sync.LayerChange;
import dev.buildingplanner.sync.Payloads;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.requios.effortlessbuilding.buildpipeline.BuildPipelineClient;
import nl.requios.effortlessbuilding.utilities.BlockEntry;
import nl.requios.effortlessbuilding.utilities.BlockSet;
import nl.requios.effortlessbuilding.utilities.ItemUsageTracker;

/**
 * "Place real blocks" for Effortless Building's shapes (see "Place real blocks"): while the setting acts, the place
 * click's shape goes to the server as one {@code place} (hook E1), the preview counts the inventory over the blocks
 * that go in (E8), draws red the blocks that wouldn't go in (E40) and in a ghost block's turn the blocks "Match ghost
 * blocks" turns (E43), and the click plays its place sound only when the inventory covers it (E41). Only the hooks
 * into Effortless Building load this class.
 */
public final class RealShape {
	/** The preview's blocks, checked once a client tick and again when its positions change. */
	private static Checked shown;

	private RealShape() {
	}

	/**
	 * A shape's blocks as the place click sends them ({@code blocks}: each in the state the preview shows, without the
	 * second half of a two-part block), the blocks among them that go in by the checks the player's game makes
	 * ({@code goingIn}), those of them that can stay ({@code stay}), and the states "Match ghost blocks" gave
	 * ({@code matched}). {@code positions} and {@code tick} say which preview it was checked for.
	 */
	private record Checked(List<BlockPos> positions, long tick, Direction face, Vec3 hit, List<RealPlace.Entry> blocks,
		List<RealPlace.Entry> goingIn, Set<BlockPos> stay, Map<BlockPos, BlockState> matched) {
	}

	/**
	 * Whether the place click's shape places real blocks now: the setting acts ({@link BuildingPlannerClient#realActs}),
	 * the mode isn't Disable, and the main hand holds an item that places a block ({@link RealPlace#shapePlaces}).
	 */
	public static boolean places() {
		Player player = Minecraft.getInstance().player;
		return player != null && RealPlace.shapePlaces(BuildingPlannerClient.realActs(), EffortlessShapes.disableMode(),
			player.getMainHandItem().getItem());
	}

	/**
	 * Hook E1: the finished shape goes to the server as one {@code place}, the first click's face and hit with each
	 * block in the state the preview shows. The click that finishes the shape ends the lock.
	 */
	public static void place(BlockSet blocks, BlockHitResult first) {
		Checked checked = check(blocks, first);
		ClientLevel level = Minecraft.getInstance().level;
		List<LayerChange> changes = checked.blocks().stream().map(block -> new LayerChange(block.pos(), block.state()))
			.toList();
		if (level != null && !changes.isEmpty()) {
			ClientPlayNetworking.send(new Payloads.Place(level.dimension().identifier(), checked.face(), checked.hit(),
				false, changes));
		}
		BuildingPlannerClient.shapeFinished();
	}

	/**
	 * Hook E8: the place click's tracker counts the blocks that go in, each block as its own item, as Effortless
	 * Building's own tracker counts a mix ({@code compute(Player, BlockSet, boolean)}), so the icons beside the
	 * crosshair show how many of each the click uses and how many are missing.
	 */
	public static void count(ItemUsageTracker tracker, Player player, BlockSet blocks, boolean creative) {
		BlockSet going = new BlockSet();
		for (RealPlace.Entry block : preview(blocks).goingIn()) {
			going.add(new BlockEntry(block.pos(), block.state(), RealPlace.item(block.state())));
		}
		tracker.compute(player, going, creative);
	}

	/**
	 * Hook E40: the preview draws red every block of the shape while the inventory lacks any item the click needs
	 * ({@code ItemUsageTracker.hasMissing}, after {@link #count}), since then nothing goes in; otherwise each block that
	 * wouldn't go in by the checks the player's game makes, or couldn't stay ({@link RealPlace#red}).
	 */
	public static void markRed(BlockSet blocks) {
		ItemUsageTracker tracker = BuildPipelineClient.ITEM_USAGE;
		Set<BlockPos> red = RealPlace.red(blocks.validPositions(), going(preview(blocks)), tracker.hasMissing());
		tracker.missingPositions.clear();
		tracker.missingPositions.addAll(red);
	}

	/**
	 * Hook E43: the state the preview draws at {@code pos}, {@code drawn} unless "Match ghost blocks" turns the block
	 * there to the ghost block's turn.
	 */
	public static BlockState drawn(BlockPos pos, BlockState drawn) {
		Checked checked = shown;
		BlockState matched = checked == null ? null : checked.matched().get(pos);
		return matched != null ? matched : drawn;
	}

	/**
	 * Hook E41: whether the click's place sound plays: only when the player's inventory covers the blocks that go in
	 * ({@link RealPlace.Inventory#covers}), since otherwise the server places nothing.
	 */
	public static boolean covered(BlockSet blocks) {
		Player player = Minecraft.getInstance().player;
		return player != null && inventory(player).covers(RealPlace.needs(check(blocks,
			BuildPipelineClient.getFirstClickHit()).goingIn()));
	}

	/** The preview's blocks checked: the last check while the positions and the client tick are the same. */
	private static Checked preview(BlockSet blocks) {
		ClientLevel level = Minecraft.getInstance().level;
		long tick = level == null ? 0 : level.getGameTime();
		List<BlockPos> positions = blocks.validPositions();
		Checked last = shown;
		if (last == null || last.tick() != tick || !last.positions().equals(positions)) {
			last = check(blocks, EffortlessShapes.clickHit());
			shown = last;
		}
		return last;
	}

	/** The positions of the blocks that go in and can stay. */
	private static Set<BlockPos> going(Checked checked) {
		Set<BlockPos> going = new HashSet<>();
		for (RealPlace.Entry block : checked.goingIn()) {
			if (checked.stay().contains(block.pos())) {
				going.add(block.pos());
			}
		}
		return going;
	}

	/**
	 * Checks a shape's blocks with the face and hit of {@code first} (with none, the top face at the middle of the
	 * shape's first position): each in the state the place click gives it ({@link EffortlessShapes#states}), or the
	 * ghost block's turn where "Match ghost blocks" gives it ({@link #ghostTurned}), then as the server checks them
	 * before placing ({@link RealPlace#goingIn}), from the player's eyes and not Freecam's camera, and whether each
	 * would stay ({@link #stays}).
	 */
	private static Checked check(BlockSet blocks, BlockHitResult first) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		ClientLevel level = mc.level;
		Direction face = first != null ? first.getDirection() : Direction.UP;
		Vec3 hit = first != null ? first.getLocation() : Vec3.atCenterOf(blocks.firstPos);
		List<RealPlace.Entry> placed = new ArrayList<>();
		EffortlessShapes.states(blocks, first).forEach((pos, state) -> placed.add(new RealPlace.Entry(pos, state)));
		List<RealPlace.Entry> sent = new ArrayList<>();
		Map<BlockPos, BlockState> matched = new HashMap<>();
		for (RealPlace.Entry block : RealPlace.withoutSecondHalves(placed)) {
			BlockState turned = ghostTurned(player, block, face, hit);
			if (turned != null) {
				matched.put(block.pos(), turned);
				block = new RealPlace.Entry(block.pos(), turned);
			}
			sent.add(block);
		}
		List<RealPlace.Entry> goingIn = RealPlace.goingIn(sent, world(level, player), player.getEyePosition());
		Set<BlockPos> stay = new HashSet<>();
		for (RealPlace.Entry block : goingIn) {
			if (stays(player, block, face, hit)) {
				stay.add(block.pos());
			}
		}
		return new Checked(blocks.validPositions(), level.getGameTime(), face, hit, sent, goingIn, stay, matched);
	}

	/**
	 * "Match ghost blocks" for a shape's real block (see "Place real blocks": "What a shape does"): with the setting on
	 * while ghost blocks show, where the layer has a ghost block of the block's own block ({@link GhostMatch#turn}),
	 * the block in the ghost block's turn, where it would stay so ({@link #stays}); otherwise null.
	 */
	private static BlockState ghostTurned(Player player, RealPlace.Entry block, Direction face, Vec3 hit) {
		ClientSession session = BuildingPlannerClient.session();
		Item item = RealPlace.item(block.state());
		if (session == null || item == null) {
			return null;
		}
		BlockTurn.Held ghost = GhostMatch.turn(BuildingPlannerClient.matchGhost(), session.ghostsShown(),
			session.replica().layer().get(block.pos()), block.state(), item);
		if (ghost.turn() == null) {
			return null;
		}
		BlockState turned = PlaceTurn.turned(block.state(), ghost, item, true);
		return !turned.equals(block.state()) && stays(player, new RealPlace.Entry(block.pos(), turned), face, hit)
			? turned : null;
	}

	/**
	 * Whether the block would stay where the server places it, as hook S2 checks it there: Minecraft's state for a
	 * right click with the shape's face and the hit at the block's own height ({@code BlockItem.place}'s steps), in the
	 * shape's form, orientation and count ({@link RealPlace#formed}), shaped by its neighbours and able to stay
	 * ({@link PlaceTurns#checked}). The world is as it is now, so a block that only another block of the same shape
	 * holds up shows as one that can't stay.
	 */
	private static boolean stays(Player player, RealPlace.Entry block, Direction face, Vec3 hit) {
		if (!(RealPlace.item(block.state()) instanceof BlockItem item)) {
			return false;
		}
		BlockPos pos = block.pos();
		double yFraction = hit.y - Math.floor(hit.y);
		BlockPlaceContext context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(item),
			new BlockHitResult(new Vec3(hit.x, pos.getY() + yFraction, hit.z), face, pos, false));
		if (!context.getClickedPos().equals(pos) || !context.canPlace()) {
			return false;
		}
		BlockPlaceContext updated = item.updatePlacementContext(context);
		if (updated == null) {
			return false;
		}
		BlockItemInvoker invoker = (BlockItemInvoker) item;
		BlockState placed = invoker.buildingplanner$placementState(updated);
		BlockState formed = RealPlace.formed(placed, block.state());
		return formed != null && PlaceTurns.checked(updated, placed, formed,
			state -> invoker.buildingplanner$canPlace(updated, state)).stays();
	}

	/** The world as the player's game checks it before placing: within the build height and inside the world border. */
	private static RealPlace.World world(ClientLevel level, Player player) {
		return new RealPlace.World() {
			@Override
			public boolean allows(BlockPos pos) {
				return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos);
			}

			@Override
			public BlockState at(BlockPos pos) {
				return level.getBlockState(pos);
			}

			@Override
			public boolean unobstructed(RealPlace.Entry block) {
				return level.isUnobstructed(block.state(), block.pos(), CollisionContext.placementContext(player));
			}
		};
	}

	/** The player's inventory as the server takes items from it, or a creative player's, which covers every block. */
	private static RealPlace.Inventory inventory(Player player) {
		if (player.hasInfiniteMaterials()) {
			return RealPlace.Inventory.CREATIVE;
		}
		Inventory inventory = player.getInventory();
		List<ItemStack> stacks = new ArrayList<>(inventory.getContainerSize());
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			stacks.add(inventory.getItem(slot));
		}
		return RealPlace.Inventory.of(stacks, inventory.getSelectedSlot(), false);
	}
}
