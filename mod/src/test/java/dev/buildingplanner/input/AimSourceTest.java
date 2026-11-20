package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class AimSourceTest {
	private final Object player = new Object();
	private final Object freecamCamera = new Object();

	@Test
	void whileFreecamsCameraIsOnTheAimIsTheCameras() {
		assertSame(freecamCamera, AimSource.of(freecamCamera, player, false));
	}

	@Test
	void withPlayerControlOnTheAimIsThePlayers() {
		assertSame(player, AimSource.of(freecamCamera, player, true));
	}

	@Test
	void withFreecamOffTheCameraIsThePlayerAndSoIsTheAim() {
		assertSame(player, AimSource.of(player, player, false));
	}

	@Test
	void withNoCameraEntityTheAimIsThePlayers() {
		assertSame(player, AimSource.of(null, player, false));
	}
}
