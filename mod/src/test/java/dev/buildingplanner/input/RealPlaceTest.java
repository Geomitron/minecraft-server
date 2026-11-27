package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.input.RealPlace.Entry;
import dev.buildingplanner.input.RealPlace.Inventory;
import dev.buildingplanner.input.RealPlace.Slot;
import dev.buildingplanner.input.RealPlace.Take;
import dev.buildingplanner.input.RealPlace.Tried;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The rule for each real block a shape or a paste places: when the setting acts, the order, the inventory, the reach, a
 * space that takes a block, a block an item places, and the cap.
 */
class RealPlaceTest {
	/** The slots of a player's inventory: the main inventory, the armour and the off hand. */
	private static final int SLOTS = 41;
	private static final Vec3 EYES = new Vec3(0.5, 1.62, 0.5);

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	/** A world of air with the given real blocks, letting a block in anywhere {@code allows} does. */
	private static RealPlace.World world(Map<BlockPos, BlockState> real, Predicate<BlockPos> allows) {
		return new RealPlace.World() {
			@Override
			public boolean allows(BlockPos pos) {
				return allows.test(pos);
			}

			@Override
			public BlockState at(BlockPos pos) {
				return real.getOrDefault(pos, Blocks.AIR.defaultBlockState());
			}
		};
	}

	private static RealPlace.World clearGround() {
		return world(Map.of(), pos -> true);
	}

	/** A Line of {@code length} blocks of {@code state} eastward from {@code start}. */
	private static List<Entry> line(BlockPos start, int length, BlockState state) {
		List<Entry> line = new ArrayList<>();
		for (int i = 0; i < length; i++) {
			line.add(new Entry(start.east(i), state));
		}
		return line;
	}

	/** An inventory of empty slots but the given ones, by slot, with the main hand at {@code mainHand}. */
	private static Inventory inventory(int mainHand, Map<Integer, Slot> stacks) {
		List<Slot> slots = new ArrayList<>(Collections.nCopies(SLOTS, new Slot(Items.AIR, 0)));
		stacks.forEach(slots::set);
		return new Inventory(slots, mainHand, false);
	}

	private static BlockState planks() {
		return Blocks.OAK_PLANKS.defaultBlockState();
	}

	@Test
	void theSettingActsOnlyDuringASessionWhoseServerTakesPlace() {
		assertTrue(RealPlace.acts(true, true, true));
		assertFalse(RealPlace.acts(false, true, true));
		assertFalse(RealPlace.acts(true, false, true));
		assertFalse(RealPlace.acts(true, true, false));
	}

	@Test
	void aShapePlacesRealBlocksInEveryModeButDisableWithAnItemThatPlacesABlock() {
		assertTrue(RealPlace.shapePlaces(true, false, Items.OAK_PLANKS));
		assertTrue(RealPlace.shapePlaces(true, false, Items.TORCH));
		assertFalse(RealPlace.shapePlaces(true, true, Items.OAK_PLANKS));
		assertFalse(RealPlace.shapePlaces(true, false, Items.STICK));
		assertFalse(RealPlace.shapePlaces(true, false, Items.WATER_BUCKET));
		assertFalse(RealPlace.shapePlaces(true, false, Items.AIR));
		assertFalse(RealPlace.shapePlaces(false, false, Items.OAK_PLANKS));
	}

	@Test
	void theServerPlacesFromTheLowestLevelUpInTheShapesOrderWithinALevel() {
		Entry high = new Entry(new BlockPos(0, 5, 0), planks());
		Entry lowA = new Entry(new BlockPos(3, 1, 0), planks());
		Entry lowB = new Entry(new BlockPos(1, 1, 0), planks());
		Entry middle = new Entry(new BlockPos(0, 3, 0), planks());
		List<Entry> tried = new ArrayList<>();
		List<Entry> placed = RealPlace.order(List.of(high, lowA, middle, lowB), block -> {
			tried.add(block);
			return Tried.PLACED;
		});
		assertEquals(List.of(lowA, lowB, middle, high), tried);
		assertEquals(tried, placed);
	}

	@Test
	void aBlockThatCantStayIsTriedAgainAfterTheRest() {
		// A lantern hanging from the roof, and a wall torch on the side of a wall block the shape places after it.
		Entry lantern = new Entry(new BlockPos(0, 2, 0),
			Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
		Entry roof = new Entry(new BlockPos(0, 3, 0), planks());
		Entry torch = new Entry(new BlockPos(1, 2, 0),
			Blocks.WALL_TORCH.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
		Entry wall = new Entry(new BlockPos(0, 2, 1), planks());
		Map<Entry, Entry> support = Map.of(lantern, roof, torch, wall);
		Set<Entry> in = new HashSet<>();
		List<Entry> tried = new ArrayList<>();
		List<Entry> placed = RealPlace.order(List.of(lantern, torch, wall, roof), block -> {
			tried.add(block);
			Entry needs = support.get(block);
			if (needs != null && !in.contains(needs)) {
				return Tried.CANT_STAY;
			}
			in.add(block);
			return Tried.PLACED;
		});
		assertEquals(List.of(lantern, torch, wall, roof, lantern, torch), tried);
		assertEquals(List.of(wall, roof, lantern, torch), placed);
	}

	@Test
	void theSecondTryGoesFromTheHighestLevelDownSoAHangingChainGoesIn() {
		BlockState dripstone = Blocks.POINTED_DRIPSTONE.defaultBlockState()
			.setValue(BlockStateProperties.VERTICAL_DIRECTION, Direction.DOWN);
		Entry roof = new Entry(new BlockPos(0, 4, 0), Blocks.STONE.defaultBlockState());
		Entry upper = new Entry(new BlockPos(0, 3, 0), dripstone);
		Entry lower = new Entry(new BlockPos(0, 2, 0), dripstone);
		Set<BlockPos> in = new HashSet<>();
		List<Entry> placed = RealPlace.order(List.of(roof, upper, lower), block -> {
			if (block != roof && !in.contains(block.pos().above())) {
				return Tried.CANT_STAY;
			}
			in.add(block.pos());
			return Tried.PLACED;
		});
		assertEquals(List.of(roof, upper, lower), placed);
	}

	@Test
	void aBlockLeftOutOrThatCantStayTwiceDoesntGoIn() {
		Entry leftOut = new Entry(new BlockPos(0, 1, 0), planks());
		Entry neverStays = new Entry(new BlockPos(1, 1, 0), Blocks.TORCH.defaultBlockState());
		Entry stays = new Entry(new BlockPos(2, 1, 0), planks());
		Map<Entry, Integer> tries = new HashMap<>();
		List<Entry> placed = RealPlace.order(List.of(leftOut, neverStays, stays), block -> {
			tries.merge(block, 1, Integer::sum);
			return block == leftOut ? Tried.LEFT_OUT : block == neverStays ? Tried.CANT_STAY : Tried.PLACED;
		});
		assertEquals(List.of(stays), placed);
		assertEquals(Map.of(leftOut, 1, neverStays, 2, stays, 1), tries);
	}

	@Test
	void eachBlockUsesItsOwnItem() {
		assertEquals(Items.OAK_PLANKS, RealPlace.item(planks()));
		assertEquals(Items.TORCH, RealPlace.item(Blocks.WALL_TORCH.defaultBlockState()));
		assertEquals(Items.OAK_SIGN, RealPlace.item(Blocks.OAK_WALL_SIGN.defaultBlockState()));
		assertEquals(Items.OAK_DOOR, RealPlace.item(Blocks.OAK_DOOR.defaultBlockState()));
	}

	@Test
	void noItemPlacesWaterLavaFireAPortalOrAPistonsHead() {
		for (BlockState state : List.of(Blocks.WATER.defaultBlockState(), Blocks.LAVA.defaultBlockState(),
			Blocks.FIRE.defaultBlockState(), Blocks.NETHER_PORTAL.defaultBlockState(),
			Blocks.PISTON_HEAD.defaultBlockState(), Blocks.AIR.defaultBlockState())) {
			assertNull(RealPlace.item(state), state.toString());
			assertFalse(RealPlace.goesIn(new Entry(BlockPos.ZERO, state), clearGround(), EYES), state.toString());
		}
		assertEquals(Map.of(), RealPlace.needs(List.of(new Entry(BlockPos.ZERO, Blocks.WATER.defaultBlockState()))));
	}

	@Test
	void aBlockGoesIntoASpaceARightClickCouldPlaceInto() {
		BlockState snow = Blocks.SNOW.defaultBlockState();
		for (BlockState there : List.of(Blocks.AIR.defaultBlockState(), Blocks.CAVE_AIR.defaultBlockState(),
			Blocks.WATER.defaultBlockState(), Blocks.LAVA.defaultBlockState(), Blocks.SHORT_GRASS.defaultBlockState(),
			Blocks.TALL_GRASS.defaultBlockState(), Blocks.FIRE.defaultBlockState(), Blocks.VINE.defaultBlockState(),
			snow.setValue(BlockStateProperties.LAYERS, 1))) {
			assertTrue(RealPlace.spaceTakes(there, planks()), there.toString());
		}
	}

	@Test
	void aSpaceThatDoesntTakeABlockKeepsItsRealBlock() {
		BlockState bottomSlab = Blocks.OAK_SLAB.defaultBlockState();
		BlockState candle = Blocks.CANDLE.defaultBlockState();
		for (BlockState there : List.of(Blocks.STONE.defaultBlockState(), planks(), Blocks.OAK_STAIRS.defaultBlockState(),
			bottomSlab, Blocks.TORCH.defaultBlockState(), candle,
			Blocks.SNOW.defaultBlockState().setValue(BlockStateProperties.LAYERS, 2))) {
			assertFalse(RealPlace.spaceTakes(there, planks()), there.toString());
		}
		// A right click with the block's own item would add to it, or leave it as it is.
		assertFalse(RealPlace.spaceTakes(bottomSlab, bottomSlab));
		assertFalse(RealPlace.spaceTakes(candle, candle));
		assertFalse(RealPlace.spaceTakes(Blocks.SNOW.defaultBlockState(), Blocks.SNOW.defaultBlockState()));
		assertFalse(RealPlace.spaceTakes(Blocks.SHORT_GRASS.defaultBlockState(), Blocks.SHORT_GRASS.defaultBlockState()));
		assertFalse(RealPlace.goesIn(new Entry(BlockPos.ZERO, planks()),
			world(Map.of(BlockPos.ZERO, Blocks.STONE.defaultBlockState()), pos -> true), EYES));
	}

	@Test
	void theReachIs64BlocksFromTheEyesToTheBlockAlongEachAxis() {
		// Eyes at a block's corner, so each face along an axis is a whole number of blocks from them.
		Vec3 eyes = new Vec3(0, 70, 0);
		for (Direction direction : Direction.values()) {
			for (int distance : new int[] {63, 64, 65}) {
				// The face of the block nearest the eyes is distance blocks from them.
				Vec3 face = eyes.relative(direction, distance);
				BlockPos pos = BlockPos.containing(face.relative(direction, 0.5));
				assertEquals(distance <= 64, RealPlace.inReach(pos, eyes), direction + " " + distance);
			}
		}
	}

	@Test
	void theReachIs64BlocksFromTheEyesAlongADiagonal() {
		// The block's nearest corner, at the origin, is the distance from the eyes along the diagonal down every axis.
		double tiny = 1e-9;
		for (double distance : new double[] {63, 64 - tiny, 64 + tiny, 65}) {
			double side = distance / Math.sqrt(3);
			Vec3 eyes = new Vec3(-side, -side, -side);
			assertEquals(distance < 64, RealPlace.inReach(BlockPos.ZERO, eyes), String.valueOf(distance));
		}
		assertFalse(RealPlace.goesIn(new Entry(new BlockPos(65, 1, 0), planks()), clearGround(), EYES));
		assertTrue(RealPlace.goesIn(new Entry(new BlockPos(64, 1, 0), planks()), clearGround(), EYES));
	}

	@Test
	void theWorldsOwnChecksComeBeforeTheSpaceIsRead() {
		BlockPos outside = new BlockPos(5, 1, 0);
		RealPlace.World world = new RealPlace.World() {
			@Override
			public boolean allows(BlockPos pos) {
				return !pos.equals(outside);
			}

			@Override
			public BlockState at(BlockPos pos) {
				if (pos.equals(outside)) {
					throw new AssertionError("read a block the world's checks leave out");
				}
				return Blocks.AIR.defaultBlockState();
			}
		};
		assertFalse(RealPlace.goesIn(new Entry(outside, planks()), world, EYES));
		assertTrue(RealPlace.goesIn(new Entry(outside.east(), planks()), world, EYES));
	}

	@Test
	void eachMixBlockCountsAsItsOwnItem() {
		List<Entry> blocks = new ArrayList<>(line(new BlockPos(0, 1, 0), 16, Blocks.COBBLESTONE.defaultBlockState()));
		blocks.addAll(line(new BlockPos(16, 1, 0), 4, Blocks.MOSSY_COBBLESTONE.defaultBlockState()));
		assertEquals(Map.of(Items.COBBLESTONE, 16, Items.MOSSY_COBBLESTONE, 4), RealPlace.needs(blocks));
	}

	@Test
	void everyBlockOfTheMirrorsAndTheArraysCopiesCounts() {
		List<Entry> blocks = new ArrayList<>(line(new BlockPos(1, 1, 0), 5, planks()));
		// The mirror's copy across x = 0, and two copies of the array 10 blocks north.
		blocks.addAll(line(new BlockPos(-5, 1, 0), 5, planks()));
		blocks.addAll(line(new BlockPos(1, 1, -10), 5, planks()));
		blocks.addAll(line(new BlockPos(1, 1, -20), 5, planks()));
		assertEquals(Map.of(Items.OAK_PLANKS, 20), RealPlace.needs(RealPlace.goingIn(blocks, clearGround(), EYES)));
	}

	@Test
	void aBlockTheChecksLeaveOutNeedsNoItem() {
		BlockPos stone = new BlockPos(1, 1, 0);
		BlockPos outside = new BlockPos(2, 1, 0);
		RealPlace.World world = world(Map.of(stone, Blocks.STONE.defaultBlockState()), pos -> !pos.equals(outside));
		List<Entry> blocks = new ArrayList<>(line(new BlockPos(0, 1, 0), 4, planks()));
		blocks.add(new Entry(new BlockPos(70, 1, 0), planks()));
		blocks.add(new Entry(new BlockPos(0, 2, 0), Blocks.WATER.defaultBlockState()));
		List<Entry> going = RealPlace.goingIn(blocks, world, EYES);
		assertEquals(List.of(blocks.get(0), blocks.get(3)), going);
		assertEquals(Map.of(Items.OAK_PLANKS, 2), RealPlace.needs(going));
	}

	@Test
	void aBlockMadeOfSeveralItemsUsesThatMany() {
		BlockState slab = Blocks.STONE_SLAB.defaultBlockState();
		assertEquals(1, RealPlace.items(slab));
		assertEquals(1, RealPlace.items(slab.setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP)));
		assertEquals(2, RealPlace.items(slab.setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE)));
		assertEquals(3, RealPlace.items(Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.CANDLES, 3)));
		assertEquals(4, RealPlace.items(Blocks.SEA_PICKLE.defaultBlockState().setValue(BlockStateProperties.PICKLES, 4)));
		assertEquals(5, RealPlace.items(Blocks.SNOW.defaultBlockState().setValue(BlockStateProperties.LAYERS, 5)));
		assertEquals(2, RealPlace.items(Blocks.TURTLE_EGG.defaultBlockState().setValue(BlockStateProperties.EGGS, 2)));
		assertEquals(3,
			RealPlace.items(Blocks.PINK_PETALS.defaultBlockState().setValue(BlockStateProperties.FLOWER_AMOUNT, 3)));
		assertEquals(4,
			RealPlace.items(Blocks.LEAF_LITTER.defaultBlockState().setValue(BlockStateProperties.SEGMENT_AMOUNT, 4)));
		assertEquals(1, RealPlace.items(Blocks.OAK_DOOR.defaultBlockState()));
		assertEquals(1, RealPlace.items(Blocks.BED.pick(DyeColor.RED).defaultBlockState()));
		assertEquals(Map.of(Items.STONE_SLAB, 3, Items.CANDLE, 3), RealPlace.needs(List.of(
			new Entry(new BlockPos(0, 1, 0), slab.setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE)),
			new Entry(new BlockPos(1, 1, 0), slab),
			new Entry(new BlockPos(2, 1, 0), Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.CANDLES, 3)))));
	}

	@Test
	void nothingGoesInWhenOneItemIsShort() {
		Map<Item, Integer> needs = Map.of(Items.COBBLESTONE, 16, Items.MOSSY_COBBLESTONE, 4);
		Inventory enough = inventory(0, Map.of(0, new Slot(Items.COBBLESTONE, 10), 5,
			new Slot(Items.COBBLESTONE, 6), 7, new Slot(Items.MOSSY_COBBLESTONE, 4)));
		assertTrue(enough.covers(needs));
		assertEquals(Map.of(), enough.shortOf(needs));
		Inventory oneShort = inventory(0, Map.of(0, new Slot(Items.COBBLESTONE, 10), 5,
			new Slot(Items.COBBLESTONE, 6), 7, new Slot(Items.MOSSY_COBBLESTONE, 3)));
		assertFalse(oneShort.covers(needs));
		assertEquals(Map.of(Items.MOSSY_COBBLESTONE, 1), oneShort.shortOf(needs));
		assertEquals(Map.of(Items.COBBLESTONE, 16, Items.MOSSY_COBBLESTONE, 4),
			inventory(0, Map.of()).shortOf(needs));
	}

	@Test
	void theMainHandsStackGoesFirstThenTheSlotsInOrder() {
		Inventory inventory = inventory(3, Map.of(1, new Slot(Items.OAK_PLANKS, 5), 3,
			new Slot(Items.OAK_PLANKS, 10), 20, new Slot(Items.OAK_PLANKS, 64), 2,
			new Slot(Items.COBBLESTONE, 64)));
		assertEquals(List.of(new Take(3, 4)), inventory.takes(Items.OAK_PLANKS, 4));
		assertEquals(List.of(new Take(3, 10), new Take(1, 5), new Take(20, 3)), inventory.takes(Items.OAK_PLANKS, 18));
		// A main hand without the item takes from the slots in order.
		assertEquals(List.of(new Take(2, 2)), inventory.takes(Items.COBBLESTONE, 2));
		// A double slab's 2 slabs from the main hand's last one and the next stack.
		Inventory slabs = inventory(0, Map.of(0, new Slot(Items.STONE_SLAB, 1), 9,
			new Slot(Items.STONE_SLAB, 8)));
		assertEquals(List.of(new Take(0, 1), new Take(9, 1)), slabs.takes(Items.STONE_SLAB, 2));
		assertEquals(List.of(new Take(3, 10), new Take(1, 5), new Take(20, 64)),
			inventory.takes(Items.OAK_PLANKS, 100));
	}

	@Test
	void creativeUsesNothing() {
		Map<Item, Integer> needs = Map.of(Items.OAK_PLANKS, RealPlace.MAX_BLOCKS);
		assertTrue(Inventory.CREATIVE.covers(needs));
		assertEquals(List.of(), Inventory.CREATIVE.takes(Items.OAK_PLANKS, 1));
		Inventory creative = new Inventory(List.of(new Slot(Items.OAK_PLANKS, 64)), 0, true);
		assertEquals(List.of(), creative.takes(Items.OAK_PLANKS, 1));
		assertTrue(creative.covers(needs));
	}

	@Test
	void theCapIs2000Blocks() {
		assertEquals(2_000, RealPlace.MAX_BLOCKS);
		assertFalse(RealPlace.tooMany(1));
		assertFalse(RealPlace.tooMany(2_000));
		assertTrue(RealPlace.tooMany(2_001));
	}
}
