package dev.buildingplanner.client;

import dev.buildingplanner.BuildingPlanner;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The plans players shared before the shared layer: Litematica saved their placements and restores them on its own, and
 * they would show old plans over the layer. Their files stay.
 */
final class OldPlans {
	private OldPlans() {
	}

	/**
	 * Removes from Litematica every placement whose schematic file lies where Syncmatica's client kept shared plans
	 * ({@code schematics/sync/} in the game folder) or where "Start a plan" saved them ({@code Building Planner/} in
	 * Litematica's schematics folder).
	 */
	static void remove() {
		List<Path> folders = List.of(folder(FabricLoader.getInstance().getGameDir().resolve("schematics").resolve("sync")),
			folder(DataManager.getSchematicsBaseDirectory().resolve("Building Planner")));
		SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
		int removed = 0;
		for (SchematicPlacement placement : manager.getAllSchematicsPlacements()) {
			Path file = placement.getSchematicFile();
			if (file != null && folders.stream().anyMatch(folder(file)::startsWith)) {
				manager.removeSchematicPlacement(placement);
				removed++;
			}
		}
		if (removed > 0) {
			BuildingPlanner.LOGGER.info("Removed {} placements of old plans from Litematica", removed);
		}
	}

	private static Path folder(Path path) {
		return path.toAbsolutePath().normalize();
	}
}
