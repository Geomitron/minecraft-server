package dev.buildingplanner;

import dev.buildingplanner.server.ServerSync;
import dev.buildingplanner.sync.Payloads;
import dev.buildingplanner.sync.PlanPayloads;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BuildingPlanner implements ModInitializer {
	public static final String MOD_ID = "buildingplanner";
	public static final Logger LOGGER = LoggerFactory.getLogger("Building Planner");

	@Override
	public void onInitialize() {
		Payloads.register();
		PlanPayloads.register();
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
			ServerSync.register();
		}
		LOGGER.info("Building Planner loaded");
	}
}
