package dev.buildingplanner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class BuildingPlannerMixinPluginTest {
	private static final String EFFORTLESS_TARGET = "nl.requios.effortlessbuilding.buildpipeline.BuildPipelineClient";
	private static final String EFFORTLESS_MIXIN = "dev.buildingplanner.mixin.effortless.BuildPipelineClientMixin";
	private static final String LITEMATICA_TARGET = "fi.dy.masa.litematica.util.SchematicUtils";
	private static final String LITEMATICA_MIXIN = "dev.buildingplanner.mixin.litematica.SchematicUtilsMixin";

	private static BuildingPlannerMixinPlugin with(String... mods) {
		return new BuildingPlannerMixinPlugin(Set.of(mods)::contains);
	}

	@Test
	void skipsEffortlessBuildingHooksWhenItIsMissing() {
		BuildingPlannerMixinPlugin plugin = with("litematica", "malilib");

		assertFalse(plugin.shouldApplyMixin(EFFORTLESS_TARGET, EFFORTLESS_MIXIN));
		assertTrue(plugin.shouldApplyMixin(LITEMATICA_TARGET, LITEMATICA_MIXIN));
	}

	@Test
	void skipsLitematicaHooksWithoutLitematicaOrMaLiLib() {
		assertFalse(with("malilib", "effortlessbuilding").shouldApplyMixin(LITEMATICA_TARGET, LITEMATICA_MIXIN));
		assertFalse(with("litematica", "effortlessbuilding").shouldApplyMixin(LITEMATICA_TARGET, LITEMATICA_MIXIN));
		assertTrue(with("effortlessbuilding").shouldApplyMixin(EFFORTLESS_TARGET, EFFORTLESS_MIXIN));
	}

	@Test
	void appliesEveryHookWhenTheModsAreInstalled() {
		BuildingPlannerMixinPlugin plugin = with("litematica", "malilib", "effortlessbuilding");

		assertTrue(plugin.shouldApplyMixin(EFFORTLESS_TARGET, EFFORTLESS_MIXIN));
		assertTrue(plugin.shouldApplyMixin(LITEMATICA_TARGET, LITEMATICA_MIXIN));
	}
}
