package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.ClientConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepeatSpeedTest {
	@Test
	void eachStartsAtTheGamesOwnPaceForAHeldButton(@TempDir Path dir) {
		ClientConfig config = ClientConfig.load(dir.resolve("config/buildingplanner-client.properties"));
		assertEquals(4, RepeatSpeed.PLACING.ticks(config));
		assertEquals(6, RepeatSpeed.ERASING.ticks(config));
	}

	@Test
	void aSettingIsKeptAndReadBackAfterARestart(@TempDir Path dir) throws IOException {
		Path file = dir.resolve("config/buildingplanner-client.properties");
		ClientConfig config = ClientConfig.load(file);
		RepeatSpeed.PLACING.set(config, 2);
		RepeatSpeed.ERASING.set(config, 10);
		ClientConfig reloaded = ClientConfig.load(file);
		assertEquals(2, RepeatSpeed.PLACING.ticks(reloaded));
		assertEquals(10, RepeatSpeed.ERASING.ticks(reloaded));
		assertTrue(Files.readString(file).contains("place-repeat-ticks=2"));
		assertTrue(Files.readString(file).contains("erase-repeat-ticks=10"));
	}

	@Test
	void aSettingOutsideOneToTwentyOrNotANumberGivesTheFirstSetting(@TempDir Path dir) throws IOException {
		Path file = dir.resolve("client.properties");
		Files.writeString(file, "place-repeat-ticks=0\nerase-repeat-ticks=fast\n");
		ClientConfig config = ClientConfig.load(file);
		assertEquals(4, RepeatSpeed.PLACING.ticks(config));
		assertEquals(6, RepeatSpeed.ERASING.ticks(config));
		Files.writeString(file, "place-repeat-ticks=21\nerase-repeat-ticks= 20 \n");
		config = ClientConfig.load(file);
		assertEquals(4, RepeatSpeed.PLACING.ticks(config));
		assertEquals(20, RepeatSpeed.ERASING.ticks(config));
	}

	@Test
	void theSpeedReadsInBlocksASecond() {
		assertEquals("20", RepeatSpeed.blocksPerSecond(1));
		assertEquals("10", RepeatSpeed.blocksPerSecond(2));
		assertEquals("6.67", RepeatSpeed.blocksPerSecond(3));
		assertEquals("5", RepeatSpeed.blocksPerSecond(4));
		assertEquals("3.33", RepeatSpeed.blocksPerSecond(6));
		assertEquals("2.5", RepeatSpeed.blocksPerSecond(8));
		assertEquals("1", RepeatSpeed.blocksPerSecond(20));
	}

	@Test
	void everyStepShowsADifferentSpeed() {
		Set<String> speeds = new HashSet<>();
		for (int ticks = RepeatSpeed.FASTEST; ticks <= RepeatSpeed.SLOWEST; ticks++) {
			speeds.add(RepeatSpeed.blocksPerSecond(ticks));
		}
		assertEquals(20, speeds.size());
	}

	@Test
	void theSliderRunsFromSlowestAtTheLeftToFastestAtTheRight() {
		assertEquals(0, RepeatSpeed.position(20));
		assertEquals(1, RepeatSpeed.position(1));
		assertEquals(20, RepeatSpeed.ticksAt(0));
		assertEquals(1, RepeatSpeed.ticksAt(1));
		assertEquals(20, RepeatSpeed.ticksAt(-0.5));
		assertEquals(1, RepeatSpeed.ticksAt(1.5));
	}

	@Test
	void eachStepsPositionGivesItsTicksBack() {
		for (int ticks = RepeatSpeed.FASTEST; ticks <= RepeatSpeed.SLOWEST; ticks++) {
			assertEquals(ticks, RepeatSpeed.ticksAt(RepeatSpeed.position(ticks)));
			// Anywhere within half a step of it, too.
			assertEquals(ticks, RepeatSpeed.ticksAt(RepeatSpeed.position(ticks) + 0.49 / 19));
			assertEquals(ticks, RepeatSpeed.ticksAt(RepeatSpeed.position(ticks) - 0.49 / 19));
		}
	}
}
