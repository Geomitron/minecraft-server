package dev.buildingplanner.client;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.ClientConfig;
import dev.buildingplanner.look.LookDefaults;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import java.io.IOException;

/**
 * Sets how Litematica draws ghost blocks, and turns its tool item off, once on a player's computer, whether or not
 * they join a server with Building Planner: translucent blocks at 0.8 opacity and no tool item, for each option the
 * player hasn't changed from Litematica's default. It saves Litematica's config, then records that in the player's
 * {@link ClientConfig}, so a later change of the player's own stays.
 */
final class LitematicaLook {
	private LitematicaLook() {
	}

	/** Call once Litematica has loaded its config, once a game run. */
	static void setOnce(ClientConfig config) {
		if (LookDefaults.applied(config)) {
			return;
		}
		try {
			ConfigBoolean translucent = Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT;
			boolean translucentNow = LookDefaults.choose(translucent.getBooleanValue(), translucent.getDefaultBooleanValue(),
				LookDefaults.TRANSLUCENT);
			if (translucentNow != translucent.getBooleanValue()) {
				translucent.setBooleanValue(translucentNow);
			}
			ConfigDouble alpha = Configs.Visuals.GHOST_BLOCK_ALPHA;
			double alphaNow = LookDefaults.choose(alpha.getDoubleValue(), alpha.getDefaultDoubleValue(),
				LookDefaults.GHOST_ALPHA);
			if (Double.compare(alphaNow, alpha.getDoubleValue()) != 0) {
				alpha.setDoubleValue(alphaNow);
			}
			ConfigBoolean toolItem = Configs.Generic.TOOL_ITEM_ENABLED;
			boolean toolItemNow = LookDefaults.choose(toolItem.getBooleanValue(), toolItem.getDefaultBooleanValue(),
				LookDefaults.TOOL_ITEM);
			if (toolItemNow != toolItem.getBooleanValue()) {
				toolItem.setBooleanValue(toolItemNow);
			}
			Configs.saveToFile();
			LookDefaults.markApplied(config);
		} catch (IOException | RuntimeException e) {
			BuildingPlanner.LOGGER.warn("Couldn't set Litematica's ghost block look and tool item", e);
		}
	}
}
