package dev.buildingplanner.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.sun.management.ThreadMXBean;

class PlanRegionTest {
	private static BlockState stone;
	private static BlockState dirt;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		stone = Blocks.STONE.defaultBlockState();
		dirt = Blocks.DIRT.defaultBlockState();
	}

	@Test
	void growsTowardNegativeCoordinatesKeepingPlanCoordinates() {
		PlanRegion region = PlanRegion.empty("Main", new BlockPos(0, 0, 0), new Vec3i(2, 2, 2));
		region.set(new BlockPos(0, 0, 0), stone);
		region.set(new BlockPos(1, 1, 1), dirt);

		assertTrue(region.growToInclude(new BlockPos(-3, -2, -1), new BlockPos(-3, -2, -1)));

		assertEquals(new BlockPos(-3, -2, -1), region.minCorner());
		assertEquals(new BlockPos(1, 1, 1), region.maxCorner());
		assertEquals(new BlockPos(-3, -2, -1), region.position());
		assertEquals(new Vec3i(5, 4, 3), region.size());
		assertEquals(stone, region.get(new BlockPos(0, 0, 0)));
		assertEquals(dirt, region.get(new BlockPos(1, 1, 1)));
		assertTrue(region.get(new BlockPos(-3, -2, -1)).isAir());
		assertEquals(2, region.nonAirBlocks());
	}

	@Test
	void growsARegionWithANegativeSizeOnEverySide() {
		// Litematica's Position (2, 2, 2) with Size (-3, -3, -3) covers 0..2 on each axis.
		PlanRegion region = new PlanRegion("Back", new BlockPos(2, 2, 2), new Vec3i(-3, -3, -3), List.of(PlanRegion.AIR),
			new CompoundTag());
		assertEquals(BlockPos.ZERO, region.minCorner());
		region.set(new BlockPos(2, 0, 1), stone);

		assertTrue(region.growToInclude(new BlockPos(-2, 4, -5), new BlockPos(5, -1, 0)));

		assertEquals(new BlockPos(-2, -1, -5), region.minCorner());
		assertEquals(new BlockPos(5, 4, 2), region.maxCorner());
		assertEquals(new Vec3i(8, 6, 8), region.size());
		assertEquals(stone, region.get(new BlockPos(2, 0, 1)));
		assertEquals(1, region.nonAirBlocks());
	}

	@Test
	void growthMovesBlockEntitiesTicksAndEntitiesWithTheirBlocks() {
		CompoundTag extras = new CompoundTag();
		CompoundTag chest = new CompoundTag();
		chest.putString("id", "minecraft:chest");
		chest.putInt("x", 1);
		chest.putInt("y", 0);
		chest.putInt("z", 2);
		ListTag blockEntities = new ListTag();
		blockEntities.add(chest);
		extras.put("TileEntities", blockEntities);
		CompoundTag tick = chest.copy();
		tick.remove("id");
		ListTag ticks = new ListTag();
		ticks.add(tick);
		extras.put("PendingBlockTicks", ticks);
		CompoundTag entity = new CompoundTag();
		ListTag pos = new ListTag();
		pos.add(DoubleTag.valueOf(0.5));
		pos.add(DoubleTag.valueOf(1.0));
		pos.add(DoubleTag.valueOf(2.5));
		entity.put("Pos", pos);
		ListTag entities = new ListTag();
		entities.add(entity);
		extras.put("Entities", entities);
		PlanRegion region = new PlanRegion("Main", new BlockPos(10, 0, 10), new Vec3i(3, 3, 3), List.of(PlanRegion.AIR),
			extras);

		region.growToInclude(new BlockPos(8, -1, 10), new BlockPos(8, -1, 10));

		CompoundTag movedChest = region.extras().getListOrEmpty("TileEntities").getCompoundOrEmpty(0);
		assertEquals(3, movedChest.getIntOr("x", 0));
		assertEquals(1, movedChest.getIntOr("y", 0));
		assertEquals(2, movedChest.getIntOr("z", 0));
		assertEquals(3, region.extras().getListOrEmpty("PendingBlockTicks").getCompoundOrEmpty(0).getIntOr("x", 0));
		ListTag movedPos = region.extras().getListOrEmpty("Entities").getCompoundOrEmpty(0).getListOrEmpty("Pos");
		assertEquals(2.5, movedPos.getDoubleOr(0, 0));
		assertEquals(2.0, movedPos.getDoubleOr(1, 0));
		assertEquals(2.5, movedPos.getDoubleOr(2, 0));
	}

	/**
	 * A region at the size cap on every side with only a floor of blocks: memory follows the floor's 1,024 sections
	 * (about 8 MiB), where one int per position of the box took 512 MiB. A snapshot carries every position, packed, and
	 * the copy built from it takes the same few sections.
	 */
	@Test
	void aLargeMostlyEmptyRegionTakesMemoryForItsBlocksNotItsBox() {
		ThreadMXBean threads = (ThreadMXBean) ManagementFactory.getThreadMXBean();
		long mib = 1024 * 1024;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		long before = threads.getCurrentThreadAllocatedBytes();
		PlanRegion region = PlanRegion.empty("Main", BlockPos.ZERO, new Vec3i(512, 512, 512));
		for (int x = 0; x < 512; x++) {
			for (int z = 0; z < 512; z++) {
				region.set(pos.set(x, 0, z), stone);
			}
		}
		long built = threads.getCurrentThreadAllocatedBytes() - before;

		assertEquals(32 * 32, region.allocatedSections());
		assertTrue(built < 10 * mib, "building the region allocated " + built / mib + " MiB");
		assertEquals(512 * 512, region.nonAirBlocks());

		RegionSnapshot snapshot = RegionSnapshot.of(region);
		// Two bits a block, for every position in the box.
		assertEquals(512L * 512 * 512 * 2 / 64, snapshot.packed().length);
		before = threads.getCurrentThreadAllocatedBytes();
		PlanRegion copy = snapshot.toRegion();
		long copied = threads.getCurrentThreadAllocatedBytes() - before;

		assertTrue(copied < 10 * mib, "reading the snapshot allocated " + copied / mib + " MiB");
		assertEquals(32 * 32, copy.allocatedSections());
		assertEquals(stone, copy.get(new BlockPos(511, 0, 511)));
		assertTrue(copy.get(new BlockPos(511, 1, 511)).isAir());
		assertEquals(512 * 512, copy.nonAirBlocks());
	}

	@Test
	void erasingASectionsLastBlockFreesIt() {
		PlanRegion region = PlanRegion.empty("Main", new BlockPos(-20, 0, 0), new Vec3i(40, 1, 1));
		region.set(new BlockPos(-17, 0, 0), stone);
		region.set(new BlockPos(5, 0, 0), dirt);
		assertEquals(2, region.allocatedSections());

		assertEquals(stone, region.set(new BlockPos(-17, 0, 0), PlanRegion.AIR));

		assertEquals(1, region.allocatedSections());
		assertTrue(region.get(new BlockPos(-17, 0, 0)).isAir());
		assertEquals(dirt, region.get(new BlockPos(5, 0, 0)));
	}

	@Test
	void doesNotGrowForBlocksInsideAndRefusesWritesOutside() {
		PlanRegion region = PlanRegion.empty("Main", new BlockPos(-1, -1, -1), new Vec3i(3, 3, 3));

		assertFalse(region.growToInclude(new BlockPos(-1, -1, -1), new BlockPos(1, 1, 1)));
		assertThrows(IllegalArgumentException.class, () -> region.set(new BlockPos(2, 0, 0), stone));
	}
}
