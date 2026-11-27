package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.input.CopyColour.Refusal;
import dev.buildingplanner.input.CopyColour.Shown;
import dev.buildingplanner.input.CopyPlacement.Spot;
import dev.buildingplanner.input.RealPlace.Inventory;
import dev.buildingplanner.input.RealPlace.Slot;
import dev.buildingplanner.layer.Layer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CopyColourTest {
	/** A build height of -64 to 319 and a border from -100 to 99 on x and z, as blocks inside it. */
	private static final CopyColour.World WORLD = new CopyColour.World() {
		@Override
		public boolean inBuildHeight(int y) {
			return y >= -64 && y <= 319;
		}

		@Override
		public boolean insideBorder(BlockPos pos) {
			return pos.getX() >= -100 && pos.getX() <= 99 && pos.getZ() >= -100 && pos.getZ() <= 99;
		}
	};
	/** A copy 2 by 2 by 2 blocks: a stone at its lowest corner and a glass at its highest, so its box is the smallest. */
	private static final Vec3i SIZE = new Vec3i(2, 2, 2);
	private static BlockState stone;
	private static BlockState glass;
	private static BlockState dirt;
	private static Map<BlockPos, BlockState> blocks;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		stone = Blocks.STONE.defaultBlockState();
		glass = Blocks.GLASS.defaultBlockState();
		dirt = Blocks.DIRT.defaultBlockState();
		blocks = Map.of(BlockPos.ZERO, stone, new BlockPos(1, 1, 1), glass);
	}

	private static List<Spot> at(int x, int y, int z) {
		return List.of(new Spot(new BlockPos(x, y, z), SIZE));
	}

	private static Shown colour(List<Spot> set, Layer layer, int maxEditBlocks) {
		return CopyColour.of(set, blocks.size(), WORLD, maxEditBlocks, () -> CopyColour.overGhosts(set, blocks, layer));
	}

	@Test
	void aCopyOnClearGroundIsBlue() {
		assertEquals(Shown.BLUE, colour(at(0, 0, 0), new Layer(), 100));
	}

	@Test
	void aCopyBesideGhostBlocksIsStillBlue() {
		Layer layer = new Layer();
		// In the copy's box but in its empty space, and just outside its box.
		layer.set(new BlockPos(1, 0, 0), dirt);
		layer.set(new BlockPos(2, 0, 0), dirt);
		assertEquals(Shown.BLUE, colour(at(0, 0, 0), layer, 100));
	}

	@Test
	void aCopyWhoseBlockWouldReplaceAGhostBlockIsYellow() {
		Layer layer = new Layer();
		layer.set(new BlockPos(11, 1, 1), dirt);
		assertEquals(Shown.YELLOW, colour(at(10, 0, 0), layer, 100));
	}

	@Test
	void aGhostBlockTheSameAsTheCopysBlockThereIsntReplaced() {
		Layer layer = new Layer();
		layer.set(new BlockPos(10, 0, 0), stone);
		layer.set(new BlockPos(11, 1, 1), glass);
		assertEquals(Shown.BLUE, colour(at(10, 0, 0), layer, 100));
	}

	@Test
	void aSetIsYellowWhenAnyOfItsCopiesWouldReplaceAGhostBlock() {
		List<Spot> set = List.of(new Spot(new BlockPos(0, 0, 0), SIZE), new Spot(new BlockPos(2, 0, 0), SIZE),
			new Spot(new BlockPos(4, 0, 0), SIZE));
		Layer layer = new Layer();
		layer.set(new BlockPos(4, 0, 0), dirt);
		assertEquals(Shown.YELLOW, colour(set, layer, 100));
		assertEquals(Shown.BLUE, colour(set.subList(0, 2), layer, 100));
	}

	@Test
	void aCopyPartlyAboveOrBelowTheBuildHeightIsRed() {
		assertEquals(Shown.red(Refusal.BUILD_HEIGHT), colour(at(0, 319, 0), new Layer(), 100));
		assertEquals(Shown.red(Refusal.BUILD_HEIGHT), colour(at(0, -65, 0), new Layer(), 100));
		assertEquals(Shown.BLUE, colour(at(0, 318, 0), new Layer(), 100));
		assertEquals(Shown.BLUE, colour(at(0, -64, 0), new Layer(), 100));
	}

	@Test
	void aCopyPartlyOutsideTheWorldBorderIsRed() {
		assertEquals(Shown.red(Refusal.WORLD_BORDER), colour(at(99, 0, 0), new Layer(), 100));
		assertEquals(Shown.red(Refusal.WORLD_BORDER), colour(at(0, 0, -101), new Layer(), 100));
		assertEquals(Shown.BLUE, colour(at(98, 0, -100), new Layer(), 100));
	}

	@Test
	void redWinsOverYellow() {
		Layer layer = new Layer();
		layer.set(new BlockPos(99, 0, 0), dirt);
		assertEquals(Shown.red(Refusal.WORLD_BORDER), colour(at(99, 0, 0), layer, 100));
	}

	@Test
	void aSetOverTheBlockCapIsRed() {
		// 4 copies of 2 blocks: 8, against caps of 7 and 8.
		List<Spot> set = List.of(new Spot(new BlockPos(0, 0, 0), SIZE), new Spot(new BlockPos(2, 0, 0), SIZE),
			new Spot(new BlockPos(4, 0, 0), SIZE), new Spot(new BlockPos(6, 0, 0), SIZE));
		assertEquals(Shown.red(Refusal.TOO_MANY_BLOCKS), colour(set, new Layer(), 7));
		assertEquals(Shown.BLUE, colour(set, new Layer(), 8));
		assertEquals(Shown.BLUE, colour(set.subList(0, 3), new Layer(), 7));
	}

	@Test
	void aRowReachingPastTheBuildHeightIsRedThoughItsFirstCopyIsnt() {
		List<Spot> set = List.of(new Spot(new BlockPos(0, 317, 0), SIZE), new Spot(new BlockPos(0, 319, 0), SIZE));
		assertEquals(Shown.red(Refusal.BUILD_HEIGHT), colour(set, new Layer(), 100));
		assertEquals(Shown.BLUE, colour(set.subList(0, 1), new Layer(), 100));
	}

	@Test
	void theBlockCapComesFirstAsOnTheServer() {
		assertEquals(Shown.red(Refusal.TOO_MANY_BLOCKS), colour(at(99, 319, 0), new Layer(), 1));
	}

	@Test
	void theOneGhostBlockIsBlueYellowOrRedAsA1x1x1Spot() {
		BlockPos at = new BlockPos(5, 0, 5);
		Layer layer = new Layer();
		assertEquals(Shown.BLUE, CopyColour.ofBlocks(List.of(at), Map.of(at, stone), WORLD, 100, layer));
		layer.set(at, stone);
		assertEquals(Shown.BLUE, CopyColour.ofBlocks(List.of(at), Map.of(at, stone), WORLD, 100, layer));
		layer.set(at, dirt);
		assertEquals(Shown.YELLOW, CopyColour.ofBlocks(List.of(at), Map.of(at, stone), WORLD, 100, layer));
		BlockPos below = new BlockPos(5, -65, 5);
		assertEquals(Shown.red(Refusal.BUILD_HEIGHT),
			CopyColour.ofBlocks(List.of(below), Map.of(below, stone), WORLD, 100, new Layer()));
		BlockPos outside = new BlockPos(100, 0, 5);
		assertEquals(Shown.red(Refusal.WORLD_BORDER),
			CopyColour.ofBlocks(List.of(outside), Map.of(outside, stone), WORLD, 100, new Layer()));
	}

	@Test
	void aRowOfOneBlocksIsYellowWhereAnyOfItsBlocksWouldReplaceADifferentGhostBlockAndRedWherePartIsOutside() {
		List<BlockPos> set = List.of(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0), new BlockPos(2, 0, 0));
		// A mix's blocks along the set: the ghost glass at the middle is the glass placed there, the dirt at the end isn't.
		Map<BlockPos, BlockState> placed = Map.of(set.get(0), stone, set.get(1), glass, set.get(2), stone);
		Layer layer = new Layer();
		layer.set(set.get(1), glass);
		assertEquals(Shown.BLUE, CopyColour.ofBlocks(set, placed, WORLD, 100, layer));
		layer.set(set.get(2), dirt);
		assertEquals(Shown.YELLOW, CopyColour.ofBlocks(set, placed, WORLD, 100, layer));
		List<BlockPos> crossing = List.of(new BlockPos(98, 0, 0), new BlockPos(99, 0, 0), new BlockPos(100, 0, 0));
		assertEquals(Shown.red(Refusal.WORLD_BORDER), CopyColour.ofBlocks(crossing,
			Map.of(crossing.get(0), stone, crossing.get(1), stone), WORLD, 100, new Layer()));
	}

	@Test
	void ghostBlocksAreReadOnlyWhileARedCheckPasses() {
		boolean[] asked = new boolean[1];
		CopyColour.of(at(0, 319, 0), 2, WORLD, 100, () -> asked[0] = true);
		assertFalse(asked[0]);
		CopyColour.of(at(0, 0, 0), 2, WORLD, 100, () -> asked[0] = true);
		assertTrue(asked[0]);
	}

	/** Eyes at the middle of the lowest block of a copy at {@code at(0, 0, 0)}. */
	private static final Vec3 EYES = new Vec3(0.5, 0.5, 0.5);

	/**
	 * The world for real blocks: {@link #WORLD}'s build height and border, air but the given real blocks, and an entity
	 * in the way at each of {@code entities}.
	 */
	private static RealPlace.World real(Map<BlockPos, BlockState> real, Set<BlockPos> entities) {
		return new RealPlace.World() {
			@Override
			public boolean allows(BlockPos pos) {
				return WORLD.inBuildHeight(pos.getY()) && WORLD.insideBorder(pos);
			}

			@Override
			public BlockState at(BlockPos pos) {
				return real.getOrDefault(pos, Blocks.AIR.defaultBlockState());
			}

			@Override
			public boolean unobstructed(RealPlace.Entry block) {
				return !entities.contains(block.pos());
			}
		};
	}

	private static Shown real(List<Spot> set, Map<BlockPos, BlockState> real, Vec3 eyes) {
		return CopyColour.ofReal(set, blocks, real(real, Set.of()), eyes, Inventory.CREATIVE);
	}

	/** A survival inventory holding these items, one stack each. */
	private static Inventory holding(Map<Item, Integer> items) {
		return new Inventory(items.entrySet().stream().map(item -> new Slot(item.getKey(), item.getValue())).toList(), 0,
			false);
	}

	@Test
	void withRealBlocksACopyOnClearGroundIsBlue() {
		assertEquals(Shown.BLUE, real(at(0, 0, 0), Map.of(), EYES));
	}

	@Test
	void withRealBlocksGhostBlocksUnderTheCopyDontCount() {
		// The colour for real blocks reads no layer: the spot over a different ghost block that is yellow for ghost blocks
		// is blue for real ones, since real blocks don't replace ghost blocks.
		Layer layer = new Layer();
		layer.set(new BlockPos(1, 1, 1), dirt);
		assertEquals(Shown.YELLOW, colour(at(0, 0, 0), layer, 100));
		assertEquals(Shown.BLUE, real(at(0, 0, 0), Map.of(), EYES));
	}

	@Test
	void withRealBlocksARealBlockInACopysBlockMakesItYellow() {
		assertEquals(Shown.YELLOW, real(at(0, 0, 0), Map.of(new BlockPos(1, 1, 1), dirt), EYES));
		// In the copy's empty space it's in no block's way, and a space that takes a block, such as water, isn't either.
		assertEquals(Shown.BLUE, real(at(0, 0, 0), Map.of(new BlockPos(1, 0, 0), dirt), EYES));
		assertEquals(Shown.BLUE, real(at(0, 0, 0), Map.of(new BlockPos(1, 1, 1), Blocks.WATER.defaultBlockState()), EYES));
	}

	@Test
	void withRealBlocksABlockPastTheReachMakesTheCopyYellow() {
		// The stone 63.5 blocks from the eyes, the glass just over 64.
		assertEquals(Shown.YELLOW, real(at(64, 0, 0), Map.of(), EYES));
		assertEquals(Shown.BLUE, real(at(63, 0, 0), Map.of(), EYES));
		// With every block past the reach the server would place nothing and refuse nothing, and the copy is yellow too.
		assertEquals(Shown.YELLOW, real(at(70, 0, 0), Map.of(), EYES));
	}

	@Test
	void withRealBlocksPartOutsideTheBuildHeightOrTheBorderIsYellowNotRed() {
		assertEquals(Shown.YELLOW, real(at(0, 319, 0), Map.of(), new Vec3(0.5, 319.5, 0.5)));
		assertEquals(Shown.BLUE, real(at(0, 318, 0), Map.of(), new Vec3(0.5, 318.5, 0.5)));
		assertEquals(Shown.YELLOW, real(at(0, -65, 0), Map.of(), new Vec3(0.5, -64.5, 0.5)));
		assertEquals(Shown.YELLOW, real(at(99, 0, 0), Map.of(), new Vec3(99.5, 0.5, 0.5)));
		assertEquals(Shown.BLUE, real(at(98, 0, 0), Map.of(), new Vec3(98.5, 0.5, 0.5)));
	}

	@Test
	void withRealBlocksABlockNoItemPlacesOrAnEntityInTheWayMakesTheCopyYellow() {
		Map<BlockPos, BlockState> withWater = Map.of(BlockPos.ZERO, stone, new BlockPos(1, 1, 1),
			Blocks.WATER.defaultBlockState());
		assertEquals(Shown.YELLOW, CopyColour.ofReal(at(0, 0, 0), withWater, real(Map.of(), Set.of()), EYES,
			Inventory.CREATIVE));
		assertEquals(Shown.YELLOW, CopyColour.ofReal(at(0, 0, 0), blocks, real(Map.of(), Set.of(new BlockPos(1, 1, 1))),
			EYES, Inventory.CREATIVE));
	}

	@Test
	void withRealBlocksPast2000BlocksIsRedCheckedFirst() {
		// 1,001 copies of 2 blocks, in survival with an empty inventory, and past the reach: too many blocks comes first.
		List<Spot> over = Collections.nCopies(1001, new Spot(new BlockPos(70, 0, 0), SIZE));
		assertEquals(Shown.red(Refusal.TOO_MANY_BLOCKS),
			CopyColour.ofReal(over, blocks, real(Map.of(), Set.of()), EYES, holding(Map.of())));
		List<Spot> atTheCap = Collections.nCopies(1000, new Spot(BlockPos.ZERO, SIZE));
		assertEquals(Shown.BLUE, real(atTheCap, Map.of(), EYES));
	}

	@Test
	void withRealBlocksInSurvivalAnInventoryOneItemShortIsRed() {
		assertEquals(Shown.red(Refusal.NOT_ENOUGH_BLOCKS), CopyColour.ofReal(at(0, 0, 0), blocks,
			real(Map.of(), Set.of()), EYES, holding(Map.of(Items.STONE, 1))));
		assertEquals(Shown.BLUE, CopyColour.ofReal(at(0, 0, 0), blocks, real(Map.of(), Set.of()), EYES,
			holding(Map.of(Items.STONE, 1, Items.GLASS, 1))));
		// Two copies need 2 of each.
		List<Spot> two = List.of(new Spot(BlockPos.ZERO, SIZE), new Spot(new BlockPos(2, 0, 0), SIZE));
		assertEquals(Shown.red(Refusal.NOT_ENOUGH_BLOCKS), CopyColour.ofReal(two, blocks, real(Map.of(), Set.of()), EYES,
			holding(Map.of(Items.STONE, 2, Items.GLASS, 1))));
		// A block that doesn't go in needs no item.
		assertEquals(Shown.YELLOW, CopyColour.ofReal(at(0, 0, 0), blocks,
			real(Map.of(new BlockPos(1, 1, 1), dirt), Set.of()), EYES, holding(Map.of(Items.STONE, 1))));
	}

	@Test
	void withRealBlocksTheClicksPanelCountsWhatTheServerCounts() {
		// Two copies use 2 of each item, though the inventory holds less; the server counts them before it places any.
		List<Spot> two = List.of(new Spot(BlockPos.ZERO, SIZE), new Spot(new BlockPos(2, 0, 0), SIZE));
		CopyColour.Counted short1 = CopyColour.counted(two, blocks, real(Map.of(), Set.of()), EYES,
			holding(Map.of(Items.STONE, 1)));
		assertEquals(Shown.red(Refusal.NOT_ENOUGH_BLOCKS), short1.shown());
		assertEquals(Map.of(Items.STONE, 2, Items.GLASS, 2), short1.needs());
		// A block that doesn't go in uses no item, and the count says so when the inventory covers the rest.
		CopyColour.Counted partly = CopyColour.counted(at(0, 0, 0), blocks,
			real(Map.of(new BlockPos(1, 1, 1), dirt), Set.of()), EYES, holding(Map.of(Items.STONE, 1)));
		assertEquals(Shown.YELLOW, partly.shown());
		assertEquals(Map.of(Items.STONE, 1), partly.needs());
		// Past the cap the server refuses the paste before it counts, and so does the panel.
		List<Spot> over = Collections.nCopies(1001, new Spot(BlockPos.ZERO, SIZE));
		assertNull(CopyColour.counted(over, blocks, real(Map.of(), Set.of()), EYES, holding(Map.of())).needs());
	}

	@Test
	void withRealBlocksADoorsUpperHalfNeedsNoItemOfItsOwn() {
		BlockState lower = Blocks.OAK_DOOR.defaultBlockState();
		Map<BlockPos, BlockState> door = Map.of(BlockPos.ZERO, lower, BlockPos.ZERO.above(),
			lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		assertEquals(Shown.BLUE, CopyColour.ofReal(List.of(new Spot(BlockPos.ZERO, new Vec3i(1, 2, 1))), door,
			real(Map.of(), Set.of()), EYES, holding(Map.of(Items.OAK_DOOR, 1))));
	}
}
