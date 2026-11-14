package dev.buildingplanner.client;

import dev.buildingplanner.BuildingPlanner;
import net.fabricmc.api.ClientModInitializer;

public final class BuildingPlannerClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BuildingPlanner.LOGGER.info("Building Planner client loaded");
	}
}
