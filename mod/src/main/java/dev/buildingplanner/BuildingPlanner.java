package dev.buildingplanner;

import dev.buildingplanner.server.ServerSync;
import dev.buildingplanner.sync.Payloads;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BuildingPlanner implements ModInitializer {
	public static final String MOD_ID = "buildingplanner";
	public static final Logger LOGGER = LoggerFactory.getLogger("Building Planner");

	@Override
	public void onInitialize() {
		Payloads.register();
		// On a dedicated server, and in the game for the player's own worlds.
		ServerSync.register();
		LOGGER.info("Building Planner loaded");
	}
}
