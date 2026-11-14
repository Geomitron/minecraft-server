package dev.buildingplanner;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BuildingPlanner implements ModInitializer {
	public static final String MOD_ID = "buildingplanner";
	public static final Logger LOGGER = LoggerFactory.getLogger("Building Planner");

	@Override
	public void onInitialize() {
		LOGGER.info("Building Planner loaded");
	}
}
