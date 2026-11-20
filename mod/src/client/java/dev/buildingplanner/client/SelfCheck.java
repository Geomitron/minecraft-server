package dev.buildingplanner.client;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.layer.BoxCount;
import dev.buildingplanner.layer.ColumnCheck;
import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.layer.LayerColumn;
import dev.buildingplanner.layer.LayerSection;
import dev.buildingplanner.sync.LayerReplica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.litematica.world.ChunkSchematic;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * The game's self-check that an older shape's ghost blocks stay drawn ("Overlapping shapes"). After the game's copy of
 * the layer takes an edit or a column's snapshot, and the tiles take it (an edit in place, or a new tile in place of
 * the old), it compares the column's ghost blocks outside the edit's own positions with what they were before; once
 * Litematica has rebuilt the column from its tile, it compares the blocks Litematica holds there with the copy. A
 * difference writes one warning to the game's log, at most once a minute for each column, with the dimension, the
 * column, what changed it and the counts. Only the columns a change touches are read.
 */
final class SelfCheck {
	/** How long to wait for Litematica to rebuild a changed column before comparing what it holds anyway. */
	static final long REBUILD_WAIT_MILLIS = 30_000;

	/** A change under way: its column before it, how many ghost blocks the column held, and its tile. */
	private record Change(ColumnCheck.Before before, int blocks, String tile) {
	}

	/** A change whose column waits for Litematica's rebuild, with the number the rebuild must begin after. */
	private record Pending(Identifier dimension, String cause, int blocksBefore, long mark, long since) {
	}

	private final Tiles tiles;
	private final Long2ObjectOpenHashMap<Change> changing = new Long2ObjectOpenHashMap<>();
	private final Long2ObjectOpenHashMap<Pending> pending = new Long2ObjectOpenHashMap<>();
	private final ColumnCheck.Limit limit = new ColumnCheck.Limit();
	/** Set once a step of the self-check fails with an error, which turns it off until the player joins again. */
	private boolean failed;

	SelfCheck(Tiles tiles) {
		this.tiles = tiles;
	}

	/** Before an edit ({@code positions} its positions in the column) or a snapshot ({@code positions} null). */
	void changing(int chunkX, int chunkZ, LayerColumn column, List<BlockPos> positions) {
		guarded(() -> changing.put(Layer.key(chunkX, chunkZ), new Change(positions == null ? null
			: ColumnCheck.before(column, positions), blocks(column), tiles.describe(chunkX, chunkZ))));
	}

	/** Once the copy of the layer and the tiles took the change; a column dropped has no change under way. */
	void changed(Identifier dimension, int chunkX, int chunkZ, LayerColumn column, List<BlockPos> positions) {
		guarded(() -> compareLayer(dimension, chunkX, chunkZ, column, positions));
	}

	private void compareLayer(Identifier dimension, int chunkX, int chunkZ, LayerColumn column, List<BlockPos> positions) {
		long key = Layer.key(chunkX, chunkZ);
		Change change = changing.remove(key);
		if (change == null) {
			pending.remove(key);
			return;
		}
		String cause = cause(positions, change.tile(), tiles.describe(chunkX, chunkZ));
		if (positions != null) {
			ColumnCheck.Outside outside = ColumnCheck.outside(change.before(), column, positions);
			if (outside.changed() > 0) {
				warn(dimension, chunkX, chunkZ, cause, "outside the edit, the game's copy of the layer held "
					+ outside.before() + " ghost blocks before and " + outside.after() + " after, and "
					+ outside.changed() + " of those positions changed");
			}
		}
		if (column == null || column.isEmpty() || !tiles.shown()) {
			pending.remove(key);
			return;
		}
		pending.put(key, new Pending(dimension, cause, change.blocks(), RebuildOrder.mark(), System.currentTimeMillis()));
	}

	/**
	 * Once a client tick: compares what Litematica holds with the copy of the layer in each changed column that
	 * Litematica has rebuilt since and holds a chunk for, or that has waited {@link #REBUILD_WAIT_MILLIS}. Nothing is
	 * compared while ghost blocks are hidden or Litematica's rendering is off, when Litematica holds no tile, nor in a
	 * column Litematica holds no chunk for: a rebuild that can't handle the column yet leaves it out, and Litematica's
	 * own check around the camera (PlacementManagerTaskFixer) loads it once it comes into view.
	 */
	void tick(LayerReplica replica) {
		if (!pending.isEmpty()) {
			guarded(() -> compareLitematica(replica));
		}
	}

	private void compareLitematica(LayerReplica replica) {
		WorldSchematic world = SchematicWorldHandler.getSchematicWorld();
		if (!tiles.shown() || !Configs.Visuals.ENABLE_RENDERING.getBooleanValue() || world == null) {
			pending.clear();
			return;
		}
		long now = System.currentTimeMillis();
		var entries = pending.long2ObjectEntrySet().iterator();
		while (entries.hasNext()) {
			Long2ObjectMap.Entry<Pending> entry = entries.next();
			int x = Layer.keyX(entry.getLongKey());
			int z = Layer.keyZ(entry.getLongKey());
			Pending waiting = entry.getValue();
			boolean rebuilt = RebuildOrder.rebuiltSince(x, z, waiting.mark());
			ChunkSchematic chunk = world.getChunkSource().getChunkIfExists(x, z);
			if (!(rebuilt && chunk != null) && now - waiting.since() < REBUILD_WAIT_MILLIS) {
				continue;
			}
			entries.remove();
			LayerColumn column = replica.holds(x, z) ? replica.layer().column(x, z) : null;
			// Litematica holds only columns whose chunk the game has (SchematicPlacementManager.canHandleChunk).
			ClientLevel level = Minecraft.getInstance().level;
			if (column == null || !waiting.dimension().equals(replica.dimension()) || level == null
				|| !WorldUtils.isClientChunkLoaded(level, x, z) || chunk == null) {
				continue;
			}
			ColumnCheck.Held held = ColumnCheck.held(column, x, z, sections(chunk), otherPlacements(x, z));
			if (held.differing() > 0) {
				warn(waiting.dimension(), x, z, waiting.cause(), "the game's copy of the layer holds " + held.layer()
					+ " ghost blocks (the column held " + waiting.blocksBefore() + " before), Litematica holds "
					+ held.held() + ", and they differ at " + held.differing()
					+ (held.differing() == 1 ? " position" : " positions")
					+ (rebuilt ? "" : "; Litematica didn't rebuild the column within "
						+ REBUILD_WAIT_MILLIS / 1000 + " seconds"));
			}
		}
	}

	/** Forgets every change, as the player leaves the server or the level changes. */
	void clear() {
		changing.clear();
		pending.clear();
	}

	/** Runs a step of the self-check, which only reads, so that an error in it turns the check off, never the game. */
	private void guarded(Runnable step) {
		if (failed) {
			return;
		}
		try {
			step.run();
		} catch (RuntimeException e) {
			failed = true;
			clear();
			BuildingPlanner.LOGGER.warn("Self-check: turned off until the player joins again, after an error", e);
		}
	}

	private void warn(Identifier dimension, int chunkX, int chunkZ, String cause, String what) {
		if (limit.allows(Layer.key(chunkX, chunkZ), System.currentTimeMillis())) {
			int x = chunkX << LayerSection.SHIFT;
			int z = chunkZ << LayerSection.SHIFT;
			BuildingPlanner.LOGGER.warn("Self-check: chunk column {} {} (x {} to {}, z {} to {}) in {}, after {}: {}",
				chunkX, chunkZ, x, x + LayerSection.SIDE - 1, z, z + LayerSection.SIDE - 1, dimension, cause, what);
		}
	}

	private static String cause(List<BlockPos> positions, String tileBefore, String tileAfter) {
		String change = positions == null ? "a snapshot of the column"
			: "an edit of " + positions.size() + (positions.size() == 1 ? " block" : " blocks") + " in it";
		if (tileBefore == null || tileAfter == null) {
			return change;
		}
		return tileBefore.equals(tileAfter) ? change : change + ", which replaced its tile at y " + tileBefore
			+ " with one at y " + tileAfter;
	}

	private static int blocks(LayerColumn column) {
		int blocks = 0;
		if (column != null) {
			for (LayerSection section : column.sections().values()) {
				blocks += section.blocks();
			}
		}
		return blocks;
	}

	/** The sections Litematica holds in its chunk of the column. */
	private static BoxCount.Sections sections(ChunkSchematic chunk) {
		return (x, y, z) -> {
			int index = chunk.getSectionIndexFromSectionY(y);
			return index < 0 || index >= chunk.getSectionsCount() ? null : chunk.getSection(index);
		};
	}

	/** The boxes of the other placements in the column, the copy's and the player's own, which draw over the tile. */
	private static List<BoundingBox> otherPlacements(int chunkX, int chunkZ) {
		List<BoundingBox> boxes = new ArrayList<>();
		for (SchematicPlacementManager.PlacementPart part : DataManager.getSchematicPlacementManager()
			.getPlacementPartsInChunk(chunkX, chunkZ)) {
			if (!BuildingPlannerClient.isTile(part.getPlacement())) {
				boxes.add(part.getBox().toVanillaBox());
			}
		}
		return boxes;
	}
}
