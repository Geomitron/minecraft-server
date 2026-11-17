package dev.buildingplanner.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.plan.PackedBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LayerTest {
	private static BlockState air;
	private static BlockState stone;
	private static BlockState dirt;
	private static BlockState glass;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		air = Blocks.AIR.defaultBlockState();
		stone = Blocks.STONE.defaultBlockState();
		dirt = Blocks.DIRT.defaultBlockState();
		glass = Blocks.GLASS.defaultBlockState();
	}

	@Test
	void forEachInVisitsTheGhostBlocksInsideABoxAcrossColumnsAndSections() {
		Layer layer = new Layer();
		List<BlockPos> inside = List.of(new BlockPos(-1, 15, 3), new BlockPos(-1, 16, 3), new BlockPos(16, 20, 5));
		inside.forEach(pos -> layer.set(pos, stone));
		layer.set(new BlockPos(-2, 16, 3), dirt);
		layer.set(new BlockPos(17, 20, 5), dirt);
		layer.set(new BlockPos(0, 21, 3), dirt);

		List<BlockPos> found = new ArrayList<>();
		layer.forEachIn(new BlockPos(-1, 10, 0), new BlockPos(16, 20, 5), (pos, state) -> {
			assertEquals(stone, state);
			found.add(pos);
		});
		assertEquals(Set.copyOf(inside), Set.copyOf(found));
		assertEquals(inside.size(), found.size());
	}

	@Test
	void keepsBlocksOnEitherSideOfSectionColumnAndAreaEdges() {
		Layer layer = new Layer();
		List<BlockPos> edges = new ArrayList<>();
		for (int x : new int[] {-513, -512, -17, -16, -1, 0, 15, 16, 511, 512}) {
			edges.add(new BlockPos(x, x, -x));
		}
		for (int i = 0; i < edges.size(); i++) {
			assertEquals(air, layer.set(edges.get(i), i % 2 == 0 ? stone : dirt));
		}
		for (int i = 0; i < edges.size(); i++) {
			BlockPos pos = edges.get(i);
			assertEquals(i % 2 == 0 ? stone : dirt, layer.get(pos), pos.toString());
			assertEquals(air, layer.get(pos.above()), pos.above().toString());
		}
		// (-1, -1, 1) is in chunk -1, 0, section -1, at index (15, 15, 1) of it.
		LayerColumn column = layer.column(-1, 0);
		assertNotNull(column);
		assertEquals(stone, column.sections().get(-1).get(LayerSection.index(15, 15, 1)));
		assertEquals(stone, layer.column(-33, 32).get(-513, -513, 513));
	}

	@Test
	void keepsBlocksAtTheBuildHeightLimits() {
		Layer layer = new Layer();
		// The overworld's lowest and highest blocks, then the lowest and highest any dimension type allows.
		int[] heights = {-64, 319, -2032, 2031};
		for (int y : heights) {
			layer.set(new BlockPos(3, y, 4), glass);
		}
		for (int y : heights) {
			assertEquals(glass, layer.get(new BlockPos(3, y, 4)));
		}
		assertEquals(List.of(-127, -4, 19, 126), new ArrayList<>(layer.column(0, 0).sections().keySet()));
	}

	@Test
	void freesASectionThenAColumnLeftEmpty() {
		Layer layer = new Layer();
		BlockPos low = new BlockPos(-5, -60, 7);
		BlockPos high = new BlockPos(-5, 100, 7);
		layer.set(low, stone);
		layer.set(high, stone);
		layer.set(high.east(), dirt);
		assertEquals(2, layer.column(-1, 0).sections().size());

		assertEquals(stone, layer.set(low, air));
		assertEquals(List.of(6), new ArrayList<>(layer.column(-1, 0).sections().keySet()));
		layer.set(high, Blocks.CAVE_AIR.defaultBlockState());
		assertEquals(1, layer.column(-1, 0).sections().get(6).blocks());
		layer.set(high.east(), air);

		assertNull(layer.column(-1, 0));
		assertTrue(layer.isEmpty());
	}

	@Test
	void erasingNothingAllocatesNothingAndChangesNoArea() {
		Layer layer = new Layer();
		assertEquals(air, layer.set(new BlockPos(1, 2, 3), air));
		assertTrue(layer.isEmpty());
		assertTrue(layer.takeChangedAreas().isEmpty());
	}

	@Test
	void notesTheAreasThatChanged() {
		Layer layer = new Layer();
		layer.set(new BlockPos(0, 0, 0), stone);
		layer.set(new BlockPos(-1, 0, 511), stone);
		layer.set(new BlockPos(512, 0, -513), stone);
		assertEquals(java.util.Set.of(Layer.key(0, 0), Layer.key(-1, 0), Layer.key(1, -2)), layer.takeChangedAreas());
		assertTrue(layer.takeChangedAreas().isEmpty());

		layer.set(new BlockPos(0, 0, 0), stone);
		assertTrue(layer.takeChangedAreas().isEmpty(), "setting a block to what it is changes nothing");
		layer.set(new BlockPos(0, 0, 0), air);
		assertEquals(java.util.Set.of(Layer.key(0, 0)), layer.takeChangedAreas());
	}

	@Test
	void paletteGrowsAndShrinksWithTheStatesInUse() {
		LayerSection section = new LayerSection();
		section.set(0, stone);
		section.set(1, dirt);
		section.set(2, glass);
		assertEquals(3, section.paletteSize());

		section.set(1, stone);
		assertEquals(2, section.paletteSize());
		assertEquals(List.of(air, stone, glass), section.pack(0).palette());

		// The freed entry is reused for the next new state.
		section.set(3, dirt);
		assertEquals(3, section.paletteSize());
		assertEquals(List.of(air, stone, dirt, glass), section.pack(0).palette());
		assertEquals(4, section.blocks());
	}

	@Test
	void aSectionOfEveryDifferentStateRoundTrips() {
		LayerSection section = new LayerSection();
		List<BlockState> states = new ArrayList<>();
		for (BlockState state : Block.BLOCK_STATE_REGISTRY) {
			if (!state.isAir() && states.size() < LayerSection.VOLUME) {
				states.add(state);
			}
		}
		assertEquals(LayerSection.VOLUME, states.size());
		for (int index = 0; index < LayerSection.VOLUME; index++) {
			section.set(index, states.get(index));
		}
		assertEquals(LayerSection.VOLUME, section.paletteSize());

		PackedSection packed = section.pack(-3);
		assertEquals(LayerSection.VOLUME + 1, packed.palette().size());
		assertEquals(PackedBlocks.longsFor(LayerSection.VOLUME, 13), packed.blocks().length);
		LayerSection unpacked = LayerSection.unpack(packed);
		for (int index = 0; index < LayerSection.VOLUME; index++) {
			assertEquals(states.get(index), unpacked.get(index));
		}

		for (int index = 0; index < LayerSection.VOLUME; index++) {
			section.set(index, stone);
		}
		assertEquals(1, section.paletteSize());
		assertEquals(List.of(air, stone), section.pack(-3).palette());
	}

	@Test
	void aColumnRoundTripsThroughItsPackedSections() {
		Layer layer = new Layer();
		layer.set(new BlockPos(-16, -64, -16), stone);
		layer.set(new BlockPos(-1, 0, -1), glass);
		layer.set(new BlockPos(-9, 200, -2), dirt);
		List<PackedSection> packed = layer.column(-1, -1).pack();
		assertEquals(List.of(-4, 0, 12), packed.stream().map(PackedSection::y).toList());

		Layer copy = new Layer();
		copy.replaceColumn(-1, -1, packed);
		assertEquals(stone, copy.get(new BlockPos(-16, -64, -16)));
		assertEquals(glass, copy.get(new BlockPos(-1, 0, -1)));
		assertEquals(dirt, copy.get(new BlockPos(-9, 200, -2)));
		assertTrue(copy.takeChangedAreas().isEmpty(), "a loaded column isn't a change to save");

		copy.replaceColumn(-1, -1, List.of());
		assertNull(copy.column(-1, -1));
	}

	@Test
	void rejectsADamagedSection() {
		PackedSection good = sectionOf(stone);
		long[] shortBlocks = java.util.Arrays.copyOf(good.blocks(), good.blocks().length - 1);
		assertThrows(IllegalArgumentException.class,
			() -> LayerSection.unpack(new PackedSection(0, good.palette(), shortBlocks)));
		// Index 1 with a palette of air alone.
		assertThrows(IllegalArgumentException.class,
			() -> LayerSection.unpack(new PackedSection(0, List.of(air), good.blocks())));
		assertThrows(IllegalArgumentException.class,
			() -> LayerSection.unpack(new PackedSection(0, List.of(), good.blocks())));
		assertThrows(IllegalArgumentException.class, () -> LayerColumn.unpack(List.of(good, good)));

		Layer layer = new Layer();
		layer.set(new BlockPos(0, 0, 0), glass);
		assertThrows(IllegalArgumentException.class, () -> layer.replaceColumn(0, 0, List.of(good, good)));
		assertEquals(glass, layer.get(new BlockPos(0, 0, 0)), "a damaged column changes nothing");
	}

	private static PackedSection sectionOf(BlockState state) {
		LayerSection section = new LayerSection();
		section.set(LayerSection.index(1, 2, 3), state);
		return section.pack(0);
	}
}
