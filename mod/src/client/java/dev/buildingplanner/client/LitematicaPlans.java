package dev.buildingplanner.client;

import ch.endte.syncmatica.data.ServerPlacement;
import ch.endte.syncmatica.litematica.LitematicManager;
import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.mixin.litematica.LitematicaSchematicAccessor;
import dev.buildingplanner.mixin.litematica.SchematicUtilsInvoker;
import dev.buildingplanner.plan.Plan;
import dev.buildingplanner.plan.PlanRegion;
import dev.buildingplanner.sync.BlockChange;
import dev.buildingplanner.sync.PlanReplicas;
import dev.buildingplanner.sync.PlayerMessage;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic.EntityInfo;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.SchematicUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Litematica's side of the player's plans: which placements are plans, and their blocks kept equal to the server's.
 * A plan is a placement Syncmatica draws; its id is the placement's Syncmatica id.
 *
 * <p>A snapshot replaces each region's container (the grid Litematica draws from) with one built from the plan, and
 * growth swaps in a bigger one, both through hook L3. Edits from the server write into the containers.
 */
final class LitematicaPlans implements PlanReplicas.View {
	/** A plan's placement, and Syncmatica's record of it. */
	private record Attached(ServerPlacement server, SchematicPlacement placement) {
	}

	/** Which plan region a container holds, and that region's minimum corner in plan coordinates. */
	record Target(UUID plan, String region, BlockPos min) {
	}

	private final EditGlow glow;
	private final Map<UUID, Attached> attached = new HashMap<>();
	private final Map<LitematicaBlockStateContainer, Target> containers = new IdentityHashMap<>();

	LitematicaPlans(EditGlow glow) {
		this.glow = glow;
	}

	/**
	 * Matches the plans to the placements Syncmatica draws now: {@code attach} for each new one (or one whose placement
	 * was replaced), {@code detach} for each that's gone.
	 */
	void reconcile(Consumer<UUID> attach, Consumer<UUID> detach) {
		Map<UUID, Attached> now = new HashMap<>();
		for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements()) {
			ServerPlacement server = LitematicManager.getInstance().getRenderingSyncmatic(placement);
			if (server != null) {
				now.put(server.getId(), new Attached(server, placement));
			}
		}
		for (UUID planId : List.copyOf(attached.keySet())) {
			Attached current = now.get(planId);
			if (current == null || current.placement() != attached.get(planId).placement()) {
				forget(planId);
				detach.accept(planId);
			}
		}
		now.forEach((planId, current) -> {
			if (!attached.containsKey(planId)) {
				attached.put(planId, current);
				attach.accept(planId);
			}
		});
	}

	/** Forgets every plan, as the player leaves the server. */
	void clear() {
		attached.clear();
		containers.clear();
	}

	/** The plan region {@code container} holds, or null when it holds none. */
	Target target(LitematicaBlockStateContainer container) {
		return containers.get(container);
	}

	/** A world position in a plan: the region it belongs to and its plan position. */
	record Located(UUID plan, String region, BlockPos pos) {
	}

	/** The plan and region whose box holds a world position, or null when no plan's box does. */
	Located locate(BlockPos world) {
		for (UUID planId : attached.keySet()) {
			Located located = locateIn(planId, world, null);
			if (located != null) {
				return located;
			}
		}
		return null;
	}

	/**
	 * A world position in a plan: in the region whose box holds it, or else in {@code fallbackRegion}, outside that
	 * region's box (hook L2). Null when the plan isn't kept live here, or the position is in none of its boxes and
	 * there's no fallback.
	 */
	Located locateIn(UUID planId, BlockPos world, String fallbackRegion) {
		Attached current = attached.get(planId);
		if (current == null) {
			return null;
		}
		SchematicPlacement placement = current.placement();
		Located fallback = null;
		for (Map.Entry<LitematicaBlockStateContainer, Target> entry : containers.entrySet()) {
			Target target = entry.getValue();
			if (!target.plan().equals(planId)) {
				continue;
			}
			BlockPos rel = SchematicUtilsInvoker.buildingplanner$toContainer(world, placement.getSchematic(),
				target.region(), placement, placement.getRelativeSubRegionPlacement(target.region()));
			if (rel == null) {
				continue;
			}
			Located located = new Located(planId, target.region(), target.min().offset(rel));
			Vec3i size = entry.getKey().getSize();
			if (rel.getX() >= 0 && rel.getY() >= 0 && rel.getZ() >= 0
				&& rel.getX() < size.getX() && rel.getY() < size.getY() && rel.getZ() < size.getZ()) {
				return located;
			}
			if (target.region().equals(fallbackRegion)) {
				fallback = located;
			}
		}
		return fallback;
	}

	/** The plan a placement shows, or null when it isn't a plan kept live here. */
	UUID planOf(SchematicPlacement placement) {
		for (Map.Entry<UUID, Attached> entry : attached.entrySet()) {
			if (entry.getValue().placement() == placement && ownsContainers(entry.getKey())) {
				return entry.getKey();
			}
		}
		return null;
	}

	/** A block state as a plan stores it, before its placement's rotation and mirror. */
	BlockState untransformed(UUID planId, String region, BlockState state) {
		return SchematicUtils.getUntransformedBlockState(state, attached.get(planId).placement(), region);
	}

	@Override
	public void loaded(UUID planId, Plan plan) {
		Attached current = attached.get(planId);
		if (current == null) {
			return;
		}
		LitematicaSchematic schematic = current.placement().getSchematic();
		removeContainers(planId);
		if (containers.values().stream().anyMatch(t -> attached.get(t.plan()).placement().getSchematic() == schematic)) {
			BuildingPlanner.LOGGER.warn("Plan {} shares its schematic with another plan, so it isn't kept live", planId);
			return;
		}
		boolean boxesChanged = false;
		for (PlanRegion region : plan.regions()) {
			boxesChanged |= install(planId, schematic, region);
		}
		long volume = 0;
		BlockPos min = null;
		BlockPos max = null;
		for (PlanRegion region : plan.regions()) {
			volume += (long) region.dimensions().getX() * region.dimensions().getY() * region.dimensions().getZ();
			min = min == null ? region.minCorner() : PositionUtils.getMinCorner(min, region.minCorner());
			max = max == null ? region.maxCorner() : PositionUtils.getMaxCorner(max, region.maxCorner());
		}
		if (min != null) {
			schematic.getMetadata().setTotalVolume((int) Math.min(volume, Integer.MAX_VALUE));
			schematic.getMetadata().setEnclosingSize(max.subtract(min).offset(1, 1, 1));
		}
		redraw(current, boxesChanged);
	}

	@Override
	public void grew(UUID planId, PlanRegion region) {
		Attached current = attached.get(planId);
		if (current == null || !ownsContainers(planId)) {
			return;
		}
		LitematicaSchematic schematic = current.placement().getSchematic();
		containers.remove(schematic.getSubRegionContainer(region.name()));
		redraw(current, install(planId, schematic, region));
	}

	@Override
	public void changed(UUID planId, UUID author, List<BlockChange> changes) {
		Attached current = attached.get(planId);
		if (current == null || !ownsContainers(planId)) {
			return;
		}
		SchematicPlacement placement = current.placement();
		LitematicaSchematic schematic = placement.getSchematic();
		Map<String, BlockPos> mins = new HashMap<>();
		containers.forEach((container, target) -> {
			if (target.plan().equals(planId)) {
				mins.put(target.region(), target.min());
			}
		});
		Set<ChunkPos> chunks = new HashSet<>();
		List<BlockPos> glowing = new ArrayList<>();
		for (BlockChange change : changes) {
			LitematicaBlockStateContainer container = schematic.getSubRegionContainer(change.region());
			BlockPos min = mins.get(change.region());
			if (container == null || min == null) {
				continue;
			}
			BlockPos rel = change.pos().subtract(min);
			if (container.get(rel.getX(), rel.getY(), rel.getZ()) == change.state()) {
				continue;
			}
			container.set(rel.getX(), rel.getY(), rel.getZ(), change.state());
			BlockPos world = worldPosition(placement, change.region(), rel);
			if (world != null) {
				chunks.add(ChunkPos.containing(world));
				glowing.add(world);
			}
		}
		chunks.forEach(DataManager.getSchematicPlacementManager()::markChunkForRebuild);
		Minecraft minecraft = Minecraft.getInstance();
		if (!glowing.isEmpty() && minecraft.player != null && !author.equals(minecraft.player.getUUID())) {
			glow.add(glowing, author);
		}
	}

	@Override
	public void message(PlayerMessage message) {
		showMessage(message);
	}

	static void showMessage(PlayerMessage message) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null) {
			minecraft.player.sendSystemMessage(Component.translatable(message.key(), message.args().toArray()));
		}
	}

	/** Swaps in a container built from {@code region}; returns whether the region's box changed. */
	private boolean install(UUID planId, LitematicaSchematic schematic, PlanRegion region) {
		LitematicaSchematicAccessor maps = (LitematicaSchematicAccessor) schematic;
		BlockPos position = maps.buildingplanner$subRegionPositions().get(region.name());
		BlockPos size = maps.buildingplanner$subRegionSizes().get(region.name());
		if (position == null || size == null) {
			BuildingPlanner.LOGGER.warn("Plan {} has region {}, which its schematic lacks", planId, region.name());
			return false;
		}
		BlockPos oldMin = PositionUtils.getMinCorner(position,
			position.offset(PositionUtils.getRelativeEndPositionFromAreaSize(size)));
		Vec3i oldDimensions = new Vec3i(Math.abs(size.getX()), Math.abs(size.getY()), Math.abs(size.getZ()));
		BlockPos min = region.minCorner();
		Vec3i dimensions = region.dimensions();

		LitematicaBlockStateContainer container = new LitematicaBlockStateContainer(dimensions.getX(),
			dimensions.getY(), dimensions.getZ());
		BlockState fill = region.palette().getFirst();
		if (!fill.isAir()) {
			for (int y = 0; y < dimensions.getY(); y++) {
				for (int z = 0; z < dimensions.getZ(); z++) {
					for (int x = 0; x < dimensions.getX(); x++) {
						container.set(x, y, z, fill);
					}
				}
			}
		}
		region.forEachSetBlock((pos, state) ->
			container.set(pos.getX() - min.getX(), pos.getY() - min.getY(), pos.getZ() - min.getZ(), state));
		maps.buildingplanner$blockContainers().put(region.name(), container);
		containers.put(container, new Target(planId, region.name(), min));

		if (oldMin.equals(min) && oldDimensions.equals(dimensions)) {
			return false;
		}
		// Block entities are kept from the region's minimum corner, entities from its position.
		BlockPos shift = oldMin.subtract(min);
		Map<BlockPos, CompoundData> blockEntities = schematic.getBlockEntityMapForRegion(region.name());
		if (blockEntities != null && !shift.equals(BlockPos.ZERO)) {
			Map<BlockPos, CompoundData> moved = new HashMap<>();
			blockEntities.forEach((pos, data) -> moved.put(pos.offset(shift), data));
			blockEntities.clear();
			blockEntities.putAll(moved);
		}
		List<EntityInfo> entities = schematic.getEntityListForRegion(region.name());
		BlockPos entityShift = position.subtract(min);
		if (entities != null && !entityShift.equals(BlockPos.ZERO)) {
			entities.replaceAll(info -> new EntityInfo(info.posVec().add(Vec3.atLowerCornerOf(entityShift)), info.nbt()));
		}
		maps.buildingplanner$subRegionPositions().put(region.name(), min);
		maps.buildingplanner$subRegionSizes().put(region.name(), new BlockPos(dimensions.getX(), dimensions.getY(), dimensions.getZ()));
		return true;
	}

	/**
	 * Redraws a plan. After a box changed, the placement's sub-regions are reset to the schematic's values, as
	 * Syncmatica does when it updates a placement, then Syncmatica puts back its own sub-region data.
	 */
	private static void redraw(Attached current, boolean boxesChanged) {
		SchematicPlacement placement = current.placement();
		if (boxesChanged) {
			boolean locked = placement.isLocked();
			if (locked) {
				placement.toggleLocked();
			}
			placement.resetAllSubRegionsToSchematicValues(message -> {
			});
			if (locked) {
				placement.toggleLocked();
			}
			LitematicManager.getInstance().updateRendered(current.server());
		}
		DataManager.getSchematicPlacementManager().markAllPlacementsOfSchematicForRebuild(placement.getSchematic());
	}

	/** The world position of a container position, as Litematica places a region's blocks. */
	static BlockPos worldPosition(SchematicPlacement placement, String regionName, BlockPos containerPos) {
		SubRegionPlacement region = placement.getRelativeSubRegionPlacement(regionName);
		BlockPos size = placement.getSchematic().getAreaSize(regionName);
		if (region == null || size == null) {
			return null;
		}
		BlockPos regionPos = region.getPos();
		BlockPos minRel = PositionUtils.getMinCorner(regionPos,
			regionPos.offset(PositionUtils.getRelativeEndPositionFromAreaSize(size)));
		BlockPos withinRegion = minRel.subtract(regionPos).offset(containerPos);
		return PositionUtils.getTransformedPlacementPosition(withinRegion, placement, region)
			.offset(PositionUtils.getTransformedBlockPos(regionPos, placement.getMirror(), placement.getRotation()))
			.offset(placement.getOrigin());
	}

	private boolean ownsContainers(UUID planId) {
		return containers.values().stream().anyMatch(target -> target.plan().equals(planId));
	}

	private void removeContainers(UUID planId) {
		containers.values().removeIf(target -> target.plan().equals(planId));
	}

	private void forget(UUID planId) {
		removeContainers(planId);
		attached.remove(planId);
	}
}
