package dev.buildingplanner.client;

import ch.endte.syncmatica.Context;
import ch.endte.syncmatica.communication.ClientCommunicationManager;
import ch.endte.syncmatica.communication.ExchangeTarget;
import ch.endte.syncmatica.communication.exchange.ShareLitematicExchange;
import ch.endte.syncmatica.data.LocalLitematicState;
import ch.endte.syncmatica.data.ServerPlacement;
import ch.endte.syncmatica.litematica.LitematicManager;
import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.sync.BlockChange;
import dev.buildingplanner.sync.EditKind;
import dev.buildingplanner.sync.PlanReplicas;
import dev.buildingplanner.sync.PlayerMessage;
import dev.buildingplanner.sync.SyncLimits;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Building Planner on one connection to a server that has it: the player's plans, kept live in Litematica. Everything
 * runs on the client's main thread.
 */
final class ClientSession {
	private final SyncLimits limits;
	private final EditGlow glow;
	private final LitematicaPlans plans;
	private final PlanReplicas replicas;
	/** Litematica writes to plans since the last tick, by plan, the newest state at each position. */
	private final Map<UUID, Map<BlockPos, BlockChange>> captured = new LinkedHashMap<>();
	/** Plans this session has loaded for the player already; a plan the player unloads stays unloaded until they rejoin. */
	private final Set<UUID> autoLoaded = new HashSet<>();

	ClientSession(SyncLimits limits, EditGlow glow) {
		this.limits = limits;
		this.glow = glow;
		this.plans = new LitematicaPlans(glow);
		this.replicas = new PlanReplicas(ClientPlayNetworking::send, plans);
	}

	void receive(CustomPacketPayload payload) {
		replicas.receive(payload);
	}

	void tick() {
		Context context = LitematicManager.getInstance().getActiveContext();
		if (context == null || !context.isStarted()) {
			return;
		}
		sendCaptured();
		plans.reconcile(replicas::attach, replicas::detach);
		loadNewPlans(context);
	}

	void close() {
		plans.clear();
		captured.clear();
		glow.clear();
	}

	/** Records a Litematica write about to happen, if it changes a plan's block. */
	void captureWrite(LitematicaBlockStateContainer container, int x, int y, int z, BlockState state) {
		LitematicaPlans.Target target = plans.target(container);
		if (target == null || container.get(x, y, z) == state) {
			return;
		}
		BlockPos pos = target.min().offset(x, y, z);
		captured.computeIfAbsent(target.plan(), plan -> new LinkedHashMap<>())
			.put(pos, new BlockChange(target.region(), pos, state));
	}

	void undo() {
		sendCaptured();
		replicas.undo();
	}

	void redo() {
		sendCaptured();
		replicas.redo();
	}

	/**
	 * Shares a new plan's placement through Syncmatica, as its Share button does. Its file must be saved, since
	 * Syncmatica uploads the file.
	 */
	static void share(SchematicPlacement placement) {
		Context context = LitematicManager.getInstance().getActiveContext();
		ExchangeTarget server = ((ClientCommunicationManager) context.getCommunicationManager()).getServer();
		context.getCommunicationManager().startExchange(new ShareLitematicExchange(placement, server, context));
	}

	/** Sends each plan's captured writes as one edit; Litematica has already drawn them. */
	private void sendCaptured() {
		captured.forEach((planId, changes) -> {
			if (changes.size() > limits.maxEditBlocks()) {
				LitematicaPlans.showMessage(PlayerMessage.tooManyBlocks(changes.size(), limits.maxEditBlocks()));
				replicas.resync(planId);
			} else {
				replicas.edit(planId, EditKind.LITEMATICA, new ArrayList<>(changes.values()));
			}
		});
		captured.clear();
	}

	/**
	 * Loads each plan in the player's dimension once a session (Syncmatica draws a plan only in its own dimension):
	 * from the file Syncmatica has, or by starting Syncmatica's download, which draws the plan when it finishes.
	 */
	private void loadNewPlans(Context context) {
		LitematicManager litematic = LitematicManager.getInstance();
		String dimension = litematic.getPlayerDimension();
		for (ServerPlacement placement : context.getSyncmaticManager().getAll()) {
			if (!dimension.equals(placement.getDimension()) || !autoLoaded.add(placement.getId())
				|| litematic.isRendered(placement)) {
				continue;
			}
			LocalLitematicState state = context.getFileStorage().getLocalState(placement);
			try {
				if (state.isLocalFileReady()) {
					litematic.renderSyncmatic(placement);
				} else if (state.isReadyForDownload() && !context.getCommunicationManager().getDownloadState(placement)) {
					ExchangeTarget server = ((ClientCommunicationManager) context.getCommunicationManager()).getServer();
					context.getCommunicationManager().download(placement, server);
				}
			} catch (Exception e) {
				BuildingPlanner.LOGGER.warn("Couldn't load plan {}", placement.getId(), e);
			}
		}
	}
}
