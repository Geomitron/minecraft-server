package dev.buildingplanner.input;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The rule for each real block a shape or a paste places while "Place real blocks" acts (see "Place real blocks"),
 * which the game's preview, the copy's colour and the server's placing share: when the setting acts, which blocks go
 * in, the items they use, the order the server places them in, and the cap.
 */
public final class RealPlace {
	/**
	 * The most blocks a {@code place} carries, and the most real blocks a player places in a second: Effortless
	 * Building's own cap on a survival player's shape ({@code ServerConfig.survivalMaxBlocksPlaced}).
	 */
	public static final int MAX_BLOCKS = 2_000;
	/** How far from the player's eyes a real block goes in (1372 Q2 A). */
	public static final double REACH = 64;
	/** The setting's key in {@code config/buildingplanner-client.properties}. */
	public static final String SETTING = "place-real-blocks";

	/** The properties that count the items a block is made of, one item each (a slab's type aside). */
	private static final List<IntegerProperty> ITEM_COUNTS = List.of(BlockStateProperties.CANDLES,
		BlockStateProperties.PICKLES, BlockStateProperties.LAYERS, BlockStateProperties.EGGS,
		BlockStateProperties.FLOWER_AMOUNT, BlockStateProperties.SEGMENT_AMOUNT);

	private RealPlace() {
	}

	/** A block of a shape or a paste: where it goes, in the state the preview shows. */
	public record Entry(BlockPos pos, BlockState state) {
	}

	/** Whether the setting's saved value is on: {@code true}; a missing or other value reads as off, as it starts. */
	public static boolean on(String saved) {
		return "true".equals(saved);
	}

	/** The value the "Settings" screen's "Save" button saves for the setting. */
	public static String saved(boolean on) {
		return String.valueOf(on);
	}

	/**
	 * Whether the setting acts: it's on, Building Planner has a session, and the server takes the payload {@code place}
	 * ({@code ClientPlayNetworking.canSend}). Otherwise shapes and pastes edit ghost blocks, as with it off.
	 */
	public static boolean acts(boolean on, boolean session, boolean serverTakesPlace) {
		return on && session && serverTakesPlace;
	}

	/**
	 * Whether the place click's shape places real blocks: while the setting acts, in every build mode but Disable, whose
	 * right click is Minecraft's already, with an item that places a block in the main hand.
	 */
	public static boolean shapePlaces(boolean acts, boolean modeDisabled, Item inHand) {
		return acts && !modeDisabled && inHand instanceof BlockItem;
	}

	/**
	 * The item that places a block in {@code state}, by Minecraft's {@code Block.asItem} (a torch for a wall torch), or
	 * null where no item places it: water, lava, fire, a portal and a piston's head have none.
	 */
	public static Item item(BlockState state) {
		Item item = state.getBlock().asItem();
		return item instanceof BlockItem ? item : null;
	}

	/**
	 * How many items a block in {@code state} is made of, as by hand: a double slab two slabs, and candles, sea pickles,
	 * snow layers, turtle eggs, petals and leaf litter as many as it holds; any other block one, a door or a bed too,
	 * whose other half Minecraft places with it.
	 */
	public static int items(BlockState state) {
		if (state.hasProperty(BlockStateProperties.SLAB_TYPE)) {
			return state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE ? 2 : 1;
		}
		for (IntegerProperty count : ITEM_COUNTS) {
			if (state.hasProperty(count)) {
				return state.getValue(count);
			}
		}
		return 1;
	}

	/**
	 * Whether a space holding the real block {@code there} takes {@code placed}, as a right click places into it without
	 * changing a real block (1372 Q3 A): a space Minecraft marks as one any block replaces
	 * ({@code BlockState.canBeReplaced}: air, water, lava, grass, fire, a vine), a snow layer only while it's one layer,
	 * and never one holding the placed block's own item, which a right click would add to (a slab to a slab, a candle to
	 * candles) or leave as it is.
	 */
	public static boolean spaceTakes(BlockState there, BlockState placed) {
		Item item = item(placed);
		if (item != null && there.getBlock().asItem() == item) {
			return false;
		}
		if (there.hasProperty(BlockStateProperties.LAYERS)) {
			return there.getValue(BlockStateProperties.LAYERS) == 1;
		}
		return there.canBeReplaced();
	}

	/**
	 * Whether {@code pos} is within {@link #REACH} of the player's {@code eyes} (the player's own, not Freecam's camera),
	 * measured to the nearest point of the block's cube, as Minecraft measures a right click's reach
	 * ({@code Player.isWithinBlockInteractionRange}).
	 */
	public static boolean inReach(BlockPos pos, Vec3 eyes) {
		return new AABB(pos).distanceToSqr(eyes) <= REACH * REACH;
	}

	/** The world as the checks before placing see it, on the player's computer or on the server. */
	public interface World {
		/**
		 * Whether the caller's own checks let a block in at {@code pos}: the build height and the world border, and on the
		 * server a loaded chunk and spawn protection too. Asked before {@link #at}, so the server never loads a chunk.
		 */
		boolean allows(BlockPos pos);

		/** The real block at {@code pos}. */
		BlockState at(BlockPos pos);

		/**
		 * Whether no entity is in the way of {@code block}, as Minecraft checks it as it places the block
		 * ({@code Level.isUnobstructed} with the player's {@code CollisionContext.placementContext}). Asked last.
		 */
		boolean unobstructed(Entry block);
	}

	/**
	 * Whether {@code block} goes in, as far as the checks before placing can tell: an item places it, it's within the
	 * reach, the world allows it there, its space takes it, and no entity is in its way, which Minecraft checks again
	 * as it places it. Whether it can stay Minecraft checks as it places it, after the blocks under it.
	 */
	public static boolean goesIn(Entry block, World world, Vec3 eyes) {
		return item(block.state()) != null && inReach(block.pos(), eyes) && world.allows(block.pos())
			&& spaceTakes(world.at(block.pos()), block.state()) && world.unobstructed(block);
	}

	/**
	 * Whether {@code state} is the part of a two-part block that Minecraft places with the other
	 * ({@code Block.setPlacedBy}): a door's or a tall plant's upper half, or a bed's head.
	 */
	public static boolean secondHalf(BlockState state) {
		return state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
			&& state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER
			|| state.hasProperty(BlockStateProperties.BED_PART)
			&& state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD;
	}

	/**
	 * The blocks of a shape or a paste the server places itself, in their order: all but the {@link #secondHalf} of a
	 * two-part block, which a paste carries and which uses no item of its own.
	 */
	public static List<Entry> withoutSecondHalves(List<Entry> blocks) {
		List<Entry> own = new ArrayList<>(blocks.size());
		for (Entry block : blocks) {
			if (!secondHalf(block.state())) {
				own.add(block);
			}
		}
		return own;
	}

	/**
	 * Hook S2's state for a real block of a shape or a paste: {@code placed}, the state Minecraft gives the block's item
	 * at its position, in the form of {@code shape} where Minecraft gives the other form of the same item (a standing
	 * torch for a wall torch, a standing sign for a wall sign: the shape's block with {@code placed}'s other values,
	 * {@code Block.withPropertiesOf}), with the shape's orientation (its values of {@link BlockTurn}'s properties,
	 * through {@link PlaceTurn#accepted}) and the shape's count of a block made of several items (3 candles; a double
	 * slab is a slab's type, which the orientation sets). Null when Minecraft gives no state or another block.
	 */
	public static BlockState formed(BlockState placed, BlockState shape) {
		Item item = item(shape);
		if (placed == null || item == null) {
			return null;
		}
		BlockState form;
		if (placed.getBlock() == shape.getBlock()) {
			form = placed;
		} else if (item(placed) == item) {
			form = shape.getBlock().withPropertiesOf(placed);
		} else {
			return null;
		}
		BlockState turned = PlaceTurn.turned(form, PlaceTurn.accepted(item, BlockTurn.values(shape)), item, true);
		for (IntegerProperty count : ITEM_COUNTS) {
			if (turned.hasProperty(count) && shape.hasProperty(count)) {
				turned = turned.setValue(count, shape.getValue(count));
			}
		}
		return turned;
	}

	/** The blocks of {@code blocks} that go in ({@link #goesIn}), in their order. */
	public static List<Entry> goingIn(List<Entry> blocks, World world, Vec3 eyes) {
		List<Entry> going = new ArrayList<>(blocks.size());
		for (Entry block : blocks) {
			if (goesIn(block, world, eyes)) {
				going.add(block);
			}
		}
		return going;
	}

	/**
	 * The blocks of {@code shape} the preview draws red (hook E40): all of them while the inventory lacks an item the
	 * blocks going in need ({@code lacking}), since then nothing goes in; otherwise each one not among {@code goingIn},
	 * the blocks that pass the checks the player's computer can make and can stay where they go.
	 */
	public static Set<BlockPos> red(Collection<BlockPos> shape, Set<BlockPos> goingIn, boolean lacking) {
		Set<BlockPos> red = new HashSet<>();
		for (BlockPos pos : shape) {
			if (lacking || !goingIn.contains(pos)) {
				red.add(pos);
			}
		}
		return red;
	}

	/**
	 * The items {@code blocks} need, by item in the order first needed: each block's {@link #item}, as many as it's made
	 * of ({@link #items}). Every block counts, the mirror's and the array's copies too, and each block of the mix as its
	 * own item; give it the blocks {@link #goingIn}, since a block left out needs none. A block no item places needs
	 * nothing.
	 */
	public static Map<Item, Integer> needs(List<Entry> blocks) {
		Map<Item, Integer> needs = new LinkedHashMap<>();
		for (Entry block : blocks) {
			Item item = item(block.state());
			if (item != null) {
				needs.merge(item, items(block.state()), Integer::sum);
			}
		}
		return needs;
	}

	/** Items taken from one slot: its index in {@link Inventory#slots} and how many. */
	public record Take(int slot, int count) {
	}

	/** One slot of a player's inventory: its stack's item (air when empty) and count. */
	public record Slot(Item item, int count) {
		public static Slot of(ItemStack stack) {
			return new Slot(stack.getItem(), stack.getCount());
		}
	}

	/**
	 * A player's inventory as the server takes items from it (1372 Q1 C): every slot in order ({@code Inventory.getItem}
	 * for each slot below {@code getContainerSize}), the main hand's slot ({@code getSelectedSlot}), and whether the
	 * player is in creative, where the server places from stacks it makes and uses none.
	 */
	public record Inventory(List<Slot> slots, int mainHand, boolean creative) {
		/** A creative player's inventory, which covers every block. */
		public static final Inventory CREATIVE = new Inventory(List.of(), 0, true);

		/** The inventory of a player whose slots hold {@code stacks}, in order. */
		public static Inventory of(List<ItemStack> stacks, int mainHand, boolean creative) {
			return new Inventory(stacks.stream().map(Slot::of).toList(), mainHand, creative);
		}

		/**
		 * The items the inventory falls short of for {@code needs}: each item it has fewer of than needed, and how many
		 * fewer, in the order of {@code needs}. Nothing goes in unless this is empty; in creative it always is.
		 */
		public Map<Item, Integer> shortOf(Map<Item, Integer> needs) {
			Map<Item, Integer> lacking = new LinkedHashMap<>();
			if (creative) {
				return lacking;
			}
			needs.forEach((item, needed) -> {
				int has = 0;
				for (Slot slot : slots) {
					if (slot.item() == item) {
						has += slot.count();
					}
				}
				if (has < needed) {
					lacking.put(item, needed - has);
				}
			});
			return lacking;
		}

		/** Whether the inventory has every item {@code needs} counts. */
		public boolean covers(Map<Item, Integer> needs) {
			return shortOf(needs).isEmpty();
		}

		/**
		 * Where {@code count} of {@code item} come from: the main hand's stack first, then the slots in order, as Effortless
		 * Building's server takes them ({@code InventoryHelper.consumeItems}); none in creative. Fewer than {@code count}
		 * where the inventory has fewer. The slots stay as they are: the server uses the items up as it places.
		 */
		public List<Take> takes(Item item, int count) {
			List<Take> takes = new ArrayList<>();
			if (creative) {
				return takes;
			}
			int left = take(mainHand, item, count, takes);
			for (int slot = 0; slot < slots.size() && left > 0; slot++) {
				if (slot != mainHand) {
					left = take(slot, item, left, takes);
				}
			}
			return takes;
		}

		private int take(int slot, Item item, int left, List<Take> takes) {
			if (left <= 0 || slot < 0 || slot >= slots.size() || slots.get(slot).item() != item) {
				return left;
			}
			int taken = Math.min(left, slots.get(slot).count());
			if (taken > 0) {
				takes.add(new Take(slot, taken));
			}
			return left - taken;
		}
	}

	/** Whether a {@code place} of {@code blocks} blocks is past the cap: the server refuses it whole, and the copy is red. */
	public static boolean tooMany(int blocks) {
		return blocks > MAX_BLOCKS;
	}

	/** How one try at placing a block went ({@link #order}). */
	public enum Tried {
		PLACED,
		/** It couldn't stay there, or an entity was in its way: it gets one more try after the rest. */
		CANT_STAY,
		/** It didn't go in for any other reason, and gets no other try. */
		LEFT_OUT
	}

	/**
	 * Places {@code blocks} in the server's order, trying each with {@code place}: from the lowest level up, in their own
	 * order within a level, so a block goes in after the blocks under it; then each block that couldn't stay once more,
	 * after the rest, from the highest level down, so a torch on a wall, a lantern hanging from a roof, or a chain of
	 * blocks hanging from one another goes in once the same {@code place} has put in its support. Returns the blocks
	 * placed, in the order they went in.
	 */
	public static List<Entry> order(List<Entry> blocks, Function<Entry, Tried> place) {
		List<Entry> lowestFirst = new ArrayList<>(blocks);
		lowestFirst.sort(Comparator.comparingInt(block -> block.pos().getY()));
		List<Entry> placed = new ArrayList<>(blocks.size());
		List<Entry> again = new ArrayList<>();
		for (Entry block : lowestFirst) {
			switch (place.apply(block)) {
				case PLACED -> placed.add(block);
				case CANT_STAY -> again.add(block);
				case LEFT_OUT -> {
				}
			}
		}
		again.sort(Comparator.comparingInt((Entry block) -> block.pos().getY()).reversed());
		for (Entry block : again) {
			if (place.apply(block) == Tried.PLACED) {
				placed.add(block);
			}
		}
		return placed;
	}
}
