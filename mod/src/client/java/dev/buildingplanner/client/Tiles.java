package dev.buildingplanner.client;

import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.layer.LayerColumn;
import dev.buildingplanner.layer.LayerSection;
import dev.buildingplanner.sync.LayerReplica;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Hands the player's copy of the layer to Litematica, which draws ghost blocks only through placements: one placement,
 * a tile, for each chunk column with ghost blocks, as tall as that column's sections from the lowest to the highest
 * with ghost blocks. Its schematic holds structure void wherever the layer holds no block, which Litematica doesn't
 * draw, so a tile's empty space never hides the copy's blocks. Tiles aren't saved and have no file.
 *
 * <p>Litematica rebuilds what it draws one chunk column at a time, so an edit writes its blocks into the tile and
 * rebuilds only that column. An edit that needs another height, or empties the column, replaces or removes the tile.
 *
 * <p>While the player hides ghost blocks, the tiles stay up to date but out of Litematica, so showing them needs no
 * reload.
 */
final class Tiles implements LayerReplica.Listener {
	/** The name of each tile's placement, schematic and region in Litematica. */
	static final String NAME = "Building Planner tile";
	private static final BlockState EMPTY = Blocks.STRUCTURE_VOID.defaultBlockState();

	/** A column's placement, its blocks, and its lowest and highest sections. */
	private record Tile(SchematicPlacement placement, LitematicaBlockStateContainer container, int lowest, int highest) {
	}

	private final Long2ObjectOpenHashMap<Tile> tiles = new Long2ObjectOpenHashMap<>();
	/** Every tile's placement and container, for the hooks that keep tiles out of Litematica's screens and edits. */
	private final ReferenceOpenHashSet<Object> parts = new ReferenceOpenHashSet<>();
	/** Whether a tile was added to Litematica since {@link #takeAdded}. */
	private boolean added;
	/** Whether the tiles are in Litematica, which draws them. */
	private boolean shown;

	Tiles(boolean shown) {
		this.shown = shown;
	}

	@Override
	public void columnChanged(int chunkX, int chunkZ, LayerColumn column, List<BlockPos> changed) {
		long key = Layer.key(chunkX, chunkZ);
		Tile tile = tiles.get(key);
		if (column == null || column.isEmpty()) {
			if (tile != null) {
				remove(key);
			}
			return;
		}
		int lowest = column.sections().firstIntKey();
		int highest = column.sections().lastIntKey();
		if (tile != null && changed != null && tile.lowest() == lowest && tile.highest() == highest) {
			int bottom = lowest << LayerSection.SHIFT;
			for (BlockPos pos : changed) {
				BlockState state = column.get(pos.getX(), pos.getY(), pos.getZ());
				tile.container().set(pos.getX() & LayerSection.SIDE - 1, pos.getY() - bottom,
					pos.getZ() & LayerSection.SIDE - 1, state.isAir() ? EMPTY : state);
			}
			if (shown) {
				manager().markChunkForRebuild(chunkX, chunkZ);
			}
			return;
		}
		if (tile != null) {
			remove(key);
		}
		tile = create(chunkX, chunkZ, column, lowest, highest);
		if (tile == null) {
			return;
		}
		// Known as a tile before Litematica sees it, which selects a new placement when none is selected (hook L4).
		tiles.put(key, tile);
		parts.add(tile.placement());
		parts.add(tile.container());
		if (shown) {
			manager().addSchematicPlacement(tile.placement(), false);
			added = true;
		} else {
			// Setting the placement up registers its chunks with Litematica (SchematicPlacement.onModified calls
			// onPostPlacementChange), which draws it though it was never added; removing it releases them.
			manager().removeSchematicPlacement(tile.placement());
		}
	}

	/** Whether the placement or block container is a tile's. */
	boolean holds(SchematicPlacement placement) {
		return parts.contains(placement);
	}

	boolean holds(LitematicaBlockStateContainer container) {
		return parts.contains(container);
	}

	private void remove(long key) {
		Tile tile = tiles.remove(key);
		if (shown) {
			manager().removeSchematicPlacement(tile.placement());
		}
		parts.remove(tile.placement());
		parts.remove(tile.container());
	}

	/** Whether ghost blocks show: whether the tiles are in Litematica. */
	boolean shown() {
		return shown;
	}

	/** Shows or hides ghost blocks: puts every tile into Litematica, or takes every tile out. */
	void setShown(boolean shown) {
		if (shown == this.shown) {
			return;
		}
		this.shown = shown;
		for (Tile tile : tiles.values()) {
			if (shown) {
				manager().addSchematicPlacement(tile.placement(), false);
				added = true;
			} else {
				manager().removeSchematicPlacement(tile.placement());
			}
		}
	}

	/** Whether a tile was added to Litematica since the last call, which would draw over the player's copy. */
	boolean takeAdded() {
		boolean was = added;
		added = false;
		return was;
	}

	/** How many tiles there are. */
	int count() {
		return tiles.size();
	}

	/** A column's tile as {@code lowest..highest} block y, or null without one. */
	String describe(int chunkX, int chunkZ) {
		Tile tile = tiles.get(Layer.key(chunkX, chunkZ));
		return tile == null ? null : (tile.lowest() << LayerSection.SHIFT) + ".."
			+ ((tile.highest() + 1 << LayerSection.SHIFT) - 1);
	}

	/** Removes every tile from Litematica, as the player leaves the server. */
	void clear() {
		if (shown) {
			for (Tile tile : tiles.values()) {
				manager().removeSchematicPlacement(tile.placement());
			}
		}
		tiles.clear();
		parts.clear();
	}

	private static Tile create(int chunkX, int chunkZ, LayerColumn column, int lowest, int highest) {
		BlockPos min = new BlockPos(chunkX << LayerSection.SHIFT, lowest << LayerSection.SHIFT, chunkZ << LayerSection.SHIFT);
		BlockPos max = min.offset(LayerSection.SIDE - 1, ((highest - lowest + 1) << LayerSection.SHIFT) - 1,
			LayerSection.SIDE - 1);
		AreaSelection area = new AreaSelection();
		area.addSubRegionBox(new Box(min, max, NAME), false);
		LitematicaSchematic schematic = LitematicaSchematic.createEmptySchematic(area, "");
		if (schematic == null) {
			return null;
		}
		schematic.getMetadata().setName(NAME);
		LitematicaBlockStateContainer container = schematic.getSubRegionContainer(NAME);
		int height = (highest - lowest + 1) << LayerSection.SHIFT;
		for (int y = 0; y < height; y++) {
			for (int z = 0; z < LayerSection.SIDE; z++) {
				for (int x = 0; x < LayerSection.SIDE; x++) {
					container.set(x, y, z, EMPTY);
				}
			}
		}
		for (Int2ObjectMap.Entry<LayerSection> entry : column.sections().int2ObjectEntrySet()) {
			LayerSection section = entry.getValue();
			int bottom = (entry.getIntKey() - lowest) << LayerSection.SHIFT;
			for (int index = 0; index < LayerSection.VOLUME; index++) {
				BlockState state = section.get(index);
				if (!state.isAir()) {
					container.set(index & LayerSection.SIDE - 1, bottom + (index >> 2 * LayerSection.SHIFT),
						index >> LayerSection.SHIFT & LayerSection.SIDE - 1, state);
				}
			}
		}
		SchematicPlacement placement = SchematicPlacement.createFor(schematic, min, NAME, true, true);
		placement.setShouldBeSaved(false);
		return new Tile(placement, container, lowest, highest);
	}

	private static SchematicPlacementManager manager() {
		return DataManager.getSchematicPlacementManager();
	}
}
