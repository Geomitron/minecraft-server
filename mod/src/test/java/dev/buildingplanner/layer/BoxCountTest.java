package dev.buildingplanner.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BoxCountTest {
	private static List<BlockState> kinds;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		kinds = List.of(Blocks.STONE.defaultBlockState(), Blocks.DIRT.defaultBlockState(),
			Blocks.GLASS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), Blocks.GOLD_BLOCK.defaultBlockState());
	}

	/**
	 * A world of 3 by 3 by 3 sections from section -1 to 1 on each axis: whole sections of stone, sections with a few
	 * scattered blocks, a section of air the game holds, and ghost blocks in some sections, over real blocks and air.
	 */
	private static final class World {
		final Map<BlockPos, BlockState> real = new HashMap<>();
		final Layer layer = new Layer();
		final BoxCount.Sections sections;

		World() {
			Random random = new Random(7);
			for (BlockPos pos : BlockPos.betweenClosed(-16, -16, -16, 31, -1, 31)) {
				// The bottom layer of sections: stone, whole.
				real.put(pos.immutable(), kinds.getFirst());
			}
			for (int i = 0; i < 3000; i++) {
				BlockPos pos = new BlockPos(random.nextInt(48) - 16, random.nextInt(32), random.nextInt(48) - 16);
				real.put(pos, kinds.get(random.nextInt(kinds.size())));
			}
			for (int i = 0; i < 1500; i++) {
				BlockPos pos = new BlockPos(random.nextInt(48) - 16, random.nextInt(48) - 16, random.nextInt(48) - 16);
				if (pos.getX() < 16 || pos.getZ() < 16) {
					layer.set(pos, kinds.get(random.nextInt(kinds.size())));
				}
			}
			BoxCount.Sections built = TestSections.of(real);
			LevelChunkSection air = TestSections.empty();
			sections = (x, y, z) -> {
				LevelChunkSection section = built.at(x, y, z);
				return section == null && x == 1 && y == 1 && z == 1 ? air : section;
			};
		}

		/** The copy's rule, read position by position. */
		Object2IntOpenHashMap<BlockState> countOneByOne(BlockPos min, BlockPos max, boolean ghostsShown) {
			Object2IntOpenHashMap<BlockState> counts = new Object2IntOpenHashMap<>();
			blocksOneByOne(min, max, ghostsShown).values().forEach(state -> counts.addTo(state, 1));
			return counts;
		}

		Map<BlockPos, BlockState> blocksOneByOne(BlockPos min, BlockPos max, boolean ghostsShown) {
			Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
			for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
				BlockState state = ghostsShown ? layer.get(pos) : Blocks.AIR.defaultBlockState();
				if (state.isAir()) {
					state = real.getOrDefault(pos, Blocks.AIR.defaultBlockState());
				}
				if (!state.isAir()) {
					blocks.put(pos.immutable(), state);
				}
			}
			return blocks;
		}
	}

	@Test
	void countsByWholeSectionsAsPositionByPosition() {
		World world = new World();
		List<BlockPos[]> boxes = List.of(
			// Every section whole.
			new BlockPos[] {new BlockPos(-16, -16, -16), new BlockPos(31, 31, 31)},
			// Cutting every section.
			new BlockPos[] {new BlockPos(-13, -11, -9), new BlockPos(27, 22, 30)},
			// Whole sections of stone with no ghost blocks, and cut ones above.
			new BlockPos[] {new BlockPos(16, -16, 16), new BlockPos(31, 4, 31)},
			// One block, and a flat box.
			new BlockPos[] {new BlockPos(5, 5, 5), new BlockPos(5, 5, 5)},
			new BlockPos[] {new BlockPos(-16, -1, -16), new BlockPos(31, 1, 31)});
		for (BlockPos[] box : boxes) {
			for (boolean shown : new boolean[] {true, false}) {
				assertEquals(world.countOneByOne(box[0], box[1], shown),
					BoxCount.of(box[0], box[1], world.layer, world.sections, shown));
			}
		}
	}

	@Test
	void takesTheSameBlocksAsPositionByPosition() {
		World world = new World();
		BlockPos min = new BlockPos(-13, -11, -9);
		BlockPos max = new BlockPos(27, 22, 30);
		for (boolean shown : new boolean[] {true, false}) {
			Map<BlockPos, BlockState> taken = new HashMap<>();
			BoxCount.forEach(min, max, world.layer, world.sections, shown, taken::put);
			assertEquals(world.blocksOneByOne(min, max, shown), taken);
		}
	}

	@Test
	void readsNoPositionOfAWholeSectionWithoutGhostBlocksOrOfOneOfOnlyAir() {
		LevelChunkSection stone = new LevelChunkSection(TestSections.empty().getStates(), null) {
			@Override
			public BlockState getBlockState(int x, int y, int z) {
				throw new AssertionError("read one by one");
			}
		};
		for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 15, 15, 15)) {
			stone.setBlockState(pos.getX(), pos.getY(), pos.getZ(), kinds.getFirst());
		}
		LevelChunkSection air = new LevelChunkSection(TestSections.empty().getStates(), null) {
			@Override
			public BlockState getBlockState(int x, int y, int z) {
				throw new AssertionError("read one by one");
			}
		};
		BoxCount.Sections world = (x, y, z) -> y == 0 ? stone : air;

		Object2IntOpenHashMap<BlockState> counts = BoxCount.of(new BlockPos(-16, 0, -16), new BlockPos(15, 31, 15),
			new Layer(), world, true);

		assertEquals(Map.of(kinds.getFirst(), 4 * 4096), counts);
	}
}
