package dev.buildingplanner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BuildingPlannerMixinPluginTest {
	private static final String EFFORTLESS_MIXIN = "dev.buildingplanner.mixin.effortless.EffortlessBuildingClientMixin";
	private static final String OTHER_MIXIN = "dev.buildingplanner.mixin.litematica.SchematicUtilsMixin";

	@Test
	void skipsEffortlessBuildingHooksWhenItIsMissing() {
		BuildingPlannerMixinPlugin plugin = new BuildingPlannerMixinPlugin(id -> false);

		assertFalse(plugin.shouldApplyMixin("nl.requios.effortlessbuilding.EffortlessBuildingClient", EFFORTLESS_MIXIN));
		assertTrue(plugin.shouldApplyMixin("fi.dy.masa.litematica.util.SchematicUtils", OTHER_MIXIN));
	}

	@Test
	void appliesEffortlessBuildingHooksWhenItIsInstalled() {
		BuildingPlannerMixinPlugin plugin = new BuildingPlannerMixinPlugin("effortlessbuilding"::equals);

		assertTrue(plugin.shouldApplyMixin("nl.requios.effortlessbuilding.EffortlessBuildingClient", EFFORTLESS_MIXIN));
	}
}
