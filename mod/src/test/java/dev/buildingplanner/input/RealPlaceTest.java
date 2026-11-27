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
import dev.buildingplanner.input.RealPlace.Try;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
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
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The rule for each real block a shape or a paste places: when the setting acts, the order, the inventory, the reach, a
 * space that takes a block, an entity in the way, a block an item places, a two-part block's second half, the state
 * hook S2 gives a block, and the cap.
 */
class RealPlaceTest {
	/** The slots of a player's inventory: the main inventory, the armour and the off hand. */
	private static final int SLOTS = 41;
	private static final Vec3 EYES = new Vec3(0.5, 1.62, 0.5);

	/**
	 * Minecraft without a server, each block and fluid given no tags, as no data pack loads any, so the rules for whether
	 * a block can stay read every tag as missing (a cactus stands here on a cactus, not on sand) rather than fail on tags
	 * not yet bound.
	 */
	@BeforeAll
	static void bootstrap() throws ReflectiveOperationException {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Method bindTags = Holder.Reference.class.getDeclaredMethod("bindTags", Collection.class);
		bindTags.setAccessible(true);
		for (Registry<?> registry : List.of(BuiltInRegistries.BLOCK, BuiltInRegistries.FLUID)) {
			for (Holder.Reference<?> holder : registry.listElements().toList()) {
				bindTags.invoke(holder, Set.of());
			}
		}
	}

	/** A world of air with the given real blocks, letting a block in anywhere {@code allows} does. */
	private static RealPlace.World world(Map<BlockPos, BlockState> real, Predicate<BlockPos> allows) {
		return world(real, allows, Set.of());
	}

	/** A world as {@link #world(Map, Predicate)} gives, with an entity in the way at each of {@code entities}. */
	private static RealPlace.World world(Map<BlockPos, BlockState> real, Predicate<BlockPos> allows,
		Set<BlockPos> entities) {
		return new RealPlace.World() {
			@Override
			public boolean allows(BlockPos pos) {
				return allows.test(pos);
			}

			@Override
			public BlockState at(BlockPos pos) {
				return real.getOrDefault(pos, Blocks.AIR.defaultBlockState());
			}

			@Override
			public boolean unobstructed(Entry block) {
				return !entities.contains(block.pos());
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
	void theSettingIsSavedAsTrueAndAMissingOrOtherValueReadsAsOff() {
		assertTrue(RealPlace.on(RealPlace.saved(true)));
		assertFalse(RealPlace.on(RealPlace.saved(false)));
		assertFalse(RealPlace.on(null));
		assertFalse(RealPlace.on("yes"));
		assertEquals("true", RealPlace.saved(true));
		assertEquals("false", RealPlace.saved(false));
	}

	@Test
	void thePreviewDrawsRedEachBlockThatDoesntGoInOrTheWholeShapeWhenAnItemIsShort() {
		List<BlockPos> shape = List.of(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0), new BlockPos(2, 0, 0));
		Set<BlockPos> going = Set.of(new BlockPos(0, 0, 0), new BlockPos(2, 0, 0));
		assertEquals(Set.of(new BlockPos(1, 0, 0)), RealPlace.red(shape, going, false));
		assertEquals(Set.copyOf(shape), RealPlace.red(shape, going, true));
		assertEquals(Set.of(), RealPlace.red(shape, Set.copyOf(shape), false));
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

	/**
	 * The world as Minecraft's own rule for whether a block can stay reads it ({@code BlockState.canSurvive}): air but
	 * for {@code real}, with the blocks {@code standing} over it, as hook M9 makes the player's level read while the
	 * preview works a shape out.
	 */
	private static LevelReader reader(Map<BlockPos, BlockState> real, Map<BlockPos, BlockState> standing) {
		InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
			case "getBlockState" -> standing.getOrDefault((BlockPos) args[0],
				real.getOrDefault((BlockPos) args[0], Blocks.AIR.defaultBlockState()));
			case "getFluidState" -> ((LevelReader) proxy).getBlockState((BlockPos) args[0]).getFluidState();
			case "getMinY" -> -64;
			case "getHeight" -> 384;
			default -> {
				if (method.isDefault()) {
					yield InvocationHandler.invokeDefault(proxy, method, args);
				}
				throw new UnsupportedOperationException(method.getName());
			}
		};
		return (LevelReader) Proxy.newProxyInstance(LevelReader.class.getClassLoader(),
			new Class<?>[] {LevelReader.class}, handler);
	}

	/**
	 * A try at placing {@code block} by Minecraft's own rule for whether it can stay, in {@code world}: placed in its
	 * state, scaffolding at the distance Minecraft gives it from the blocks around it
	 * ({@code ScaffoldingBlock.getStateForPlacement}), or one that can't stay.
	 */
	private static Try survives(Entry block, LevelReader world) {
		BlockState state = block.state();
		if (state.is(Blocks.SCAFFOLDING)) {
			state = state.setValue(ScaffoldingBlock.DISTANCE, ScaffoldingBlock.getDistance(world, block.pos()));
		}
		return state.canSurvive(world, block.pos()) ? new Try(Tried.PLACED, state) : Try.CANT_STAY;
	}

	/** The positions of {@code blocks} that can stay in the world {@code real} as it is, without the shape's blocks. */
	private static Set<BlockPos> stayNow(List<Entry> blocks, Map<BlockPos, BlockState> real) {
		Set<BlockPos> stay = new HashSet<>();
		for (Entry block : blocks) {
			if (survives(block, reader(real, Map.of())).tried() == Tried.PLACED) {
				stay.add(block.pos());
			}
		}
		return stay;
	}

	/** The positions of {@code blocks} that stay as the preview works the shape out over {@code real}. */
	private static Set<BlockPos> worked(List<Entry> blocks, Map<BlockPos, BlockState> real) {
		Map<BlockPos, BlockState> standing = new HashMap<>();
		LevelReader world = reader(real, standing);
		return RealPlace.inOrder(blocks, standing, block -> survives(block, world));
	}

	/** A Line of {@code length} blocks of {@code state} up from {@code start}. */
	private static List<Entry> upward(BlockPos start, int length, BlockState state) {
		List<Entry> line = new ArrayList<>();
		for (int i = 0; i < length; i++) {
			line.add(new Entry(start.above(i), state));
		}
		return line;
	}

	/** Stone at the origin, under the shapes that stand on the ground. */
	private static final Map<BlockPos, BlockState> GROUND = Map.of(BlockPos.ZERO, Blocks.STONE.defaultBlockState());

	@Test
	void scaffoldingInALineUpFromTheGroundStaysInTheServersOrderThoughTheWorldAsItIsHoldsOnlyTheFirst() {
		List<Entry> line = upward(BlockPos.ZERO.above(), 4, Blocks.SCAFFOLDING.defaultBlockState());
		Set<BlockPos> now = stayNow(line, GROUND);
		assertEquals(Set.of(new BlockPos(0, 1, 0)), now);
		assertTrue(RealPlace.needsOrder(line, now));
		Set<BlockPos> all = Set.of(new BlockPos(0, 1, 0), new BlockPos(0, 2, 0), new BlockPos(0, 3, 0),
			new BlockPos(0, 4, 0));
		assertEquals(all, worked(line, GROUND));
		// The server places the lowest level first, whatever the shape's own order.
		assertEquals(all, worked(line.reversed(), GROUND));
	}

	@Test
	void scaffoldingReachesSidewaysFromScaffoldingTheShapePlacesUpToSixBlocksOut() {
		List<Entry> blocks = line(new BlockPos(0, 1, 0), 8, Blocks.SCAFFOLDING.defaultBlockState());
		Set<BlockPos> stay = worked(blocks, GROUND);
		assertEquals(7, stay.size());
		assertFalse(stay.contains(new BlockPos(7, 1, 0)));
	}

	@Test
	void carpetsInALineUpStandOnOneAnother() {
		List<Entry> line = upward(BlockPos.ZERO.above(), 4, Blocks.CARPET.pick(DyeColor.WHITE).defaultBlockState());
		Set<BlockPos> now = stayNow(line, GROUND);
		assertEquals(Set.of(new BlockPos(0, 1, 0)), now);
		assertTrue(RealPlace.needsOrder(line, now));
		assertEquals(4, worked(line, GROUND).size());
	}

	@Test
	void aTorchOnABlockOfTheMixUnderItStays() {
		List<Entry> blocks = List.of(new Entry(new BlockPos(0, 2, 0), Blocks.TORCH.defaultBlockState()),
			new Entry(new BlockPos(0, 1, 0), Blocks.COBBLESTONE.defaultBlockState()));
		Set<BlockPos> now = stayNow(blocks, Map.of());
		assertEquals(Set.of(new BlockPos(0, 1, 0)), now);
		assertTrue(RealPlace.needsOrder(blocks, now));
		assertEquals(Set.of(new BlockPos(0, 1, 0), new BlockPos(0, 2, 0)), worked(blocks, Map.of()));
	}

	@Test
	void aBlockNothingOfTheShapeHoldsUpStillCantStay() {
		// A Line up of scaffolding in the air, nothing under it.
		List<Entry> line = upward(new BlockPos(0, 3, 0), 4, Blocks.SCAFFOLDING.defaultBlockState());
		Set<BlockPos> now = stayNow(line, GROUND);
		assertEquals(Set.of(), now);
		assertTrue(RealPlace.needsOrder(line, now));
		assertEquals(Set.of(), worked(line, GROUND));
		// Standing torches side by side in the air, as a Line aimed at a wall's side gives them: none holds another up.
		List<Entry> torches = line(new BlockPos(0, 5, 0), 3, Blocks.TORCH.defaultBlockState());
		assertEquals(Set.of(), worked(torches, Map.of()));
	}

	@Test
	void theWorkGoesOnlyToAShapeWithABlockThatCantStayNowBesideAnotherOfTheShape() {
		List<Entry> planks = line(new BlockPos(0, 1, 0), 3, planks());
		assertFalse(RealPlace.needsOrder(planks, Set.of(new BlockPos(0, 1, 0), new BlockPos(1, 1, 0),
			new BlockPos(2, 1, 0))));
		// A torch in the air on its own, as Single places it: no block of the shape is beside it.
		List<Entry> single = List.of(new Entry(new BlockPos(0, 5, 0), Blocks.TORCH.defaultBlockState()));
		assertFalse(RealPlace.needsOrder(single, Set.of()));
		// Two torches in the air, apart.
		List<Entry> apart = List.of(new Entry(new BlockPos(0, 5, 0), Blocks.TORCH.defaultBlockState()),
			new Entry(new BlockPos(2, 5, 0), Blocks.TORCH.defaultBlockState()));
		assertFalse(RealPlace.needsOrder(apart, Set.of()));
	}

	/**
	 * Real blocks of {@code state} in a row east from the origin, under a Line one level up: with no tags, a cactus stands
	 * here on a cactus and a chorus plant on a chorus plant, not on sand or end stone.
	 */
	private static Map<BlockPos, BlockState> row(int length, BlockState state) {
		Map<BlockPos, BlockState> row = new HashMap<>();
		for (int i = 0; i < length; i++) {
			row.put(BlockPos.ZERO.east(i), state);
		}
		return row;
	}

	@Test
	void aCactusBesideOneTheShapePlacesFirstDoesntStay() {
		BlockState cactus = Blocks.CACTUS.defaultBlockState();
		List<Entry> line = line(new BlockPos(0, 1, 0), 4, cactus);
		Map<BlockPos, BlockState> under = row(4, cactus);
		Set<BlockPos> now = stayNow(line, under);
		assertEquals(4, now.size());
		assertTrue(RealPlace.needsOrder(line, now));
		assertEquals(Set.of(new BlockPos(0, 1, 0), new BlockPos(2, 1, 0)), worked(line, under));
	}

	@Test
	void aChorusFlowerBesideABlockTheShapePlacesFirstDoesntStay() {
		// In the air beside a real chorus plant, which holds it up, with a block of the mix beside it on the other side.
		Map<BlockPos, BlockState> real = Map.of(new BlockPos(0, 5, 0), Blocks.CHORUS_PLANT.defaultBlockState());
		Entry block = new Entry(new BlockPos(1, 5, 1), Blocks.COBBLESTONE.defaultBlockState());
		Entry flower = new Entry(new BlockPos(1, 5, 0), Blocks.CHORUS_FLOWER.defaultBlockState());
		List<Entry> blocks = List.of(block, flower);
		Set<BlockPos> now = stayNow(blocks, real);
		assertEquals(Set.of(block.pos(), flower.pos()), now);
		assertTrue(RealPlace.needsOrder(blocks, now));
		assertEquals(Set.of(block.pos()), worked(blocks, real));
	}

	@Test
	void aChorusPlantBesideOneTheShapePlacesFirstWithBlocksAboveAndUnderItDoesntStay() {
		BlockState plant = Blocks.CHORUS_PLANT.defaultBlockState();
		Map<BlockPos, BlockState> real = new HashMap<>(row(2, plant));
		real.put(new BlockPos(0, 2, 0), Blocks.STONE.defaultBlockState());
		real.put(new BlockPos(1, 2, 0), Blocks.STONE.defaultBlockState());
		List<Entry> line = line(new BlockPos(0, 1, 0), 2, plant);
		Set<BlockPos> now = stayNow(line, real);
		assertEquals(2, now.size());
		assertTrue(RealPlace.needsOrder(line, now));
		assertEquals(Set.of(new BlockPos(0, 1, 0)), worked(line, real));
	}

	@Test
	void theWorkGoesToABlockANeighbourMayKeepOutOnlyBesideAnotherBlockOfTheShape() {
		BlockState cactus = Blocks.CACTUS.defaultBlockState();
		assertTrue(RealPlace.keptOutByNeighbours(cactus));
		assertTrue(RealPlace.keptOutByNeighbours(Blocks.CHORUS_FLOWER.defaultBlockState()));
		assertTrue(RealPlace.keptOutByNeighbours(Blocks.CHORUS_PLANT.defaultBlockState()));
		assertFalse(RealPlace.keptOutByNeighbours(Blocks.SCAFFOLDING.defaultBlockState()));
		assertFalse(RealPlace.keptOutByNeighbours(Blocks.TORCH.defaultBlockState()));
		assertFalse(RealPlace.keptOutByNeighbours(planks()));
		// Cactus apart, each able to stay in the world as it is.
		List<Entry> apart = List.of(new Entry(new BlockPos(0, 1, 0), cactus), new Entry(new BlockPos(2, 1, 0), cactus));
		assertFalse(RealPlace.needsOrder(apart, Set.of(new BlockPos(0, 1, 0), new BlockPos(2, 1, 0))));
		// A cactus beside a block of the mix, both able to stay in the world as it is.
		List<Entry> beside = List.of(new Entry(new BlockPos(0, 1, 0), Blocks.COBBLESTONE.defaultBlockState()),
			new Entry(new BlockPos(1, 1, 0), cactus));
		assertTrue(RealPlace.needsOrder(beside, Set.of(new BlockPos(0, 1, 0), new BlockPos(1, 1, 0))));
	}

	@Test
	void theOtherHalfMinecraftPlacesWithAFirstHalfCountsAsTheShapes() {
		// A carpet over a bed's head: the head is the shape's too, since Minecraft places it with the foot.
		BlockState foot = Blocks.STRAW_BED.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
			Direction.NORTH);
		List<Entry> blocks = List.of(new Entry(new BlockPos(0, 1, 0), foot),
			new Entry(new BlockPos(0, 2, -1), Blocks.CARPET.pick(DyeColor.WHITE).defaultBlockState()));
		assertTrue(RealPlace.needsOrder(blocks, Set.of(new BlockPos(0, 1, 0))));
		assertEquals(Set.of(new BlockPos(0, 1, 0), new BlockPos(0, 2, -1)), worked(blocks, GROUND));
	}

	@Test
	void theOtherHalfOfATwoPartBlockGoesAboveItOrTheWayItFaces() {
		BlockState door = Blocks.OAK_DOOR.defaultBlockState();
		assertEquals(new Entry(new BlockPos(0, 2, 0),
			door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)),
			RealPlace.otherHalf(new Entry(new BlockPos(0, 1, 0), door)));
		BlockState bed = Blocks.STRAW_BED.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
			Direction.EAST);
		assertEquals(new Entry(new BlockPos(1, 1, 0), bed.setValue(BlockStateProperties.BED_PART, BedPart.HEAD)),
			RealPlace.otherHalf(new Entry(new BlockPos(0, 1, 0), bed)));
		assertNull(RealPlace.otherHalf(new Entry(new BlockPos(0, 2, 0),
			door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER))));
		assertNull(RealPlace.otherHalf(new Entry(new BlockPos(0, 1, 0), planks())));
	}

	@Test
	void aBlockWhoseSpaceAnEarlierBlocksOtherHalfFillsDoesntGoIn() {
		Entry door = new Entry(new BlockPos(0, 1, 0), Blocks.OAK_DOOR.defaultBlockState());
		Entry above = new Entry(new BlockPos(0, 2, 0), planks());
		Map<BlockPos, BlockState> standing = new HashMap<>();
		Set<BlockPos> stay = RealPlace.inOrder(List.of(door, above), standing,
			block -> standing.containsKey(block.pos()) ? Try.LEFT_OUT : new Try(Tried.PLACED, block.state()));
		assertEquals(Set.of(door.pos()), stay);
		assertEquals(DoubleBlockHalf.UPPER,
			standing.get(above.pos()).getValue(BlockStateProperties.DOUBLE_BLOCK_HALF));
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
	void theWorldsOwnChecksComeFirstThenTheSpaceThenEntities() {
		BlockPos outside = new BlockPos(5, 1, 0);
		BlockPos stone = new BlockPos(7, 1, 0);
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
				return pos.equals(stone) ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
			}

			@Override
			public boolean unobstructed(Entry block) {
				if (block.pos().equals(outside) || block.pos().equals(stone)) {
					throw new AssertionError("looked for entities where the earlier checks leave the block out");
				}
				return true;
			}
		};
		assertFalse(RealPlace.goesIn(new Entry(outside, planks()), world, EYES));
		assertFalse(RealPlace.goesIn(new Entry(stone, planks()), world, EYES));
		assertTrue(RealPlace.goesIn(new Entry(outside.east(), planks()), world, EYES));
	}

	@Test
	void anEntityInTheWayKeepsABlockOutAndNeedsNoItem() {
		BlockPos cow = new BlockPos(1, 1, 0);
		RealPlace.World world = world(Map.of(), pos -> true, Set.of(cow));
		List<Entry> going = RealPlace.goingIn(line(new BlockPos(0, 1, 0), 3, planks()), world, EYES);
		assertEquals(List.of(new BlockPos(0, 1, 0), new BlockPos(2, 1, 0)), going.stream().map(Entry::pos).toList());
		assertEquals(Map.of(Items.OAK_PLANKS, 2), RealPlace.needs(going));
	}

	@Test
	void theSecondHalfOfATwoPartBlockIsLeftToItsFirst() {
		BlockState door = Blocks.OAK_DOOR.defaultBlockState();
		BlockState bed = Blocks.STRAW_BED.defaultBlockState();
		BlockState grass = Blocks.TALL_GRASS.defaultBlockState();
		Entry doorLower = new Entry(new BlockPos(0, 1, 0), door);
		Entry doorUpper = new Entry(new BlockPos(0, 2, 0),
			door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
		Entry bedFoot = new Entry(new BlockPos(2, 1, 0), bed.setValue(BlockStateProperties.BED_PART, BedPart.FOOT));
		Entry bedHead = new Entry(new BlockPos(2, 1, -1), bed.setValue(BlockStateProperties.BED_PART, BedPart.HEAD));
		Entry grassUpper = new Entry(new BlockPos(4, 2, 0),
			grass.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
		Entry plank = new Entry(new BlockPos(5, 1, 0), planks());
		List<Entry> own = RealPlace.withoutSecondHalves(List.of(doorLower, doorUpper, bedFoot, bedHead, grassUpper,
			plank));
		assertEquals(List.of(doorLower, bedFoot, plank), own);
		assertEquals(Map.of(Items.OAK_DOOR, 1, Blocks.STRAW_BED.asItem(), 1, Items.OAK_PLANKS, 1), RealPlace.needs(own));
	}

	@Test
	void hookS2GivesTheShapesOrientationAndKeepsMinecraftsOtherValues() {
		BlockState placed = Blocks.OAK_STAIRS.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
			.setValue(BlockStateProperties.HALF, Half.BOTTOM).setValue(BlockStateProperties.WATERLOGGED, true);
		BlockState shape = Blocks.OAK_STAIRS.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST).setValue(BlockStateProperties.HALF, Half.TOP);
		assertEquals(placed.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
			.setValue(BlockStateProperties.HALF, Half.TOP), RealPlace.formed(placed, shape, false));
		assertEquals(planks(), RealPlace.formed(planks(), planks(), false));
	}

	@Test
	void hookS2TakesTheShapesFormOfTheSameItem() {
		BlockState wallTorch = Blocks.WALL_TORCH.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
		assertEquals(wallTorch, RealPlace.formed(Blocks.TORCH.defaultBlockState(), wallTorch, false));
		BlockState wallSign = Blocks.OAK_WALL_SIGN.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
		assertEquals(wallSign, RealPlace.formed(Blocks.OAK_SIGN.defaultBlockState()
			.setValue(BlockStateProperties.ROTATION_16, 4), wallSign, false));
		assertEquals(Blocks.TORCH.defaultBlockState(),
			RealPlace.formed(wallTorch, Blocks.TORCH.defaultBlockState(), false));
	}

	@Test
	void hookS2PlacesNothingWhereMinecraftGivesAnotherBlockOrNone() {
		assertNull(RealPlace.formed(Blocks.STONE.defaultBlockState(), planks(), false));
		assertNull(RealPlace.formed(Blocks.SOUL_WALL_TORCH.defaultBlockState(),
			Blocks.WALL_TORCH.defaultBlockState(), false));
		assertNull(RealPlace.formed(null, planks(), false));
		assertNull(RealPlace.formed(Blocks.WATER.defaultBlockState(), Blocks.WATER.defaultBlockState(), false));
	}

	@Test
	void hookS2GivesTheShapesCountOfABlockMadeOfSeveralItems() {
		BlockState candle = Blocks.CANDLE.defaultBlockState();
		assertEquals(candle.setValue(BlockStateProperties.CANDLES, 3),
			RealPlace.formed(candle, candle.setValue(BlockStateProperties.CANDLES, 3), false));
		BlockState slab = Blocks.OAK_SLAB.defaultBlockState();
		assertEquals(slab.setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE),
			RealPlace.formed(slab.setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM),
				slab.setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE), false));
	}

	@Test
	void hookS2GivesAPastedDoorTheCopysHingeAndAShapesDoorMinecraftsOwn() {
		BlockState placed = Blocks.OAK_DOOR.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
			.setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.RIGHT);
		BlockState copy = Blocks.OAK_DOOR.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
			.setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.LEFT);
		BlockState facingNorth = placed.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
		assertEquals(facingNorth.setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.LEFT),
			RealPlace.formed(placed, copy, true));
		assertEquals(facingNorth, RealPlace.formed(placed, copy, false));
		assertEquals(planks(), RealPlace.formed(planks(), planks(), true));
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
		// The click's panel shows what all the slots hold together.
		assertEquals(16, oneShort.holds(Items.COBBLESTONE));
		assertEquals(0, oneShort.holds(Items.OAK_PLANKS));
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
