package dev.buildingplanner.server;

import static dev.buildingplanner.server.LayerNetwork.NETHER;
import static dev.buildingplanner.server.LayerNetwork.OVERWORLD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.server.LayerNetwork.Game;
import dev.buildingplanner.sync.LayerChange;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The server's layer saved to its files and read back at the next start. */
class LayerStoreTest {
	private static final BlockPos A = new BlockPos(1, 64, 1);
	private static final BlockPos B = new BlockPos(-20, -64, 7);
	private static BlockState stone;
	private static BlockState glass;

	@TempDir
	Path world;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		stone = Blocks.STONE.defaultBlockState();
		glass = Blocks.GLASS.defaultBlockState();
	}

	private Path root() {
		return world.resolve("building-planner/layer");
	}

	@Test
	void aSavedLayerReadsBackTheSameInEveryDimension() throws IOException {
		LayerStore store = new LayerStore(root(), 0);
		LayerNetwork network = new LayerNetwork(store.load());
		Game alex = network.join("Alex");
		alex.watchAround(2);
		network.settle();
		alex.edit(new LayerChange(A, stone), new LayerChange(B, glass));
		network.settle();
		alex.moveTo(NETHER);
		alex.watchAround(0);
		network.settle();
		alex.edit(new LayerChange(A, glass));
		network.settle();

		store.save(network.server);
		store.close();

		LayerNetwork restarted = new LayerNetwork(new LayerStore(root(), 0).load());
		Game blair = restarted.join("Blair");
		blair.watchAround(2);
		restarted.settle();
		assertEquals(stone, blair.copy.get(A));
		assertEquals(glass, blair.copy.get(B));
		restarted.assertSameAsServer(blair);
		assertEquals(glass, restarted.server.get(NETHER, A));
		assertEquals(stone, restarted.server.get(OVERWORLD, A));
	}

	@Test
	void theStoreWritesAtMostEveryThirtySecondsAndOnlyWhatChanged() throws IOException {
		LayerStore store = new LayerStore(root(), 1_000);
		LayerNetwork network = new LayerNetwork(store.load());
		Game alex = network.join("Alex");
		alex.watchAround(0);
		network.settle();
		alex.edit(new LayerChange(A, stone));
		network.settle();
		Path file = root().resolve("minecraft/overworld/0.0.dat");

		store.tick(network.server, 1_000 + LayerStore.SAVE_INTERVAL_MILLIS - 1);
		store.close();
		assertFalse(Files.exists(file), "nothing is written before the interval");

		store = new LayerStore(root(), 1_000);
		store.tick(network.server, 1_000 + LayerStore.SAVE_INTERVAL_MILLIS);
		store.close();
		assertTrue(Files.exists(file));

		// Erasing the area's last ghost block deletes its file at the next save.
		alex.edit(new LayerChange(A, Blocks.AIR.defaultBlockState()));
		network.settle();
		store = new LayerStore(root(), 0);
		store.save(network.server);
		store.close();
		assertFalse(Files.exists(file));
	}
}
