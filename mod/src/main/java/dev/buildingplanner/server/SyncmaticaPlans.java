package dev.buildingplanner.server;

import ch.endte.syncmatica.Context;
import ch.endte.syncmatica.Syncmatica;
import ch.endte.syncmatica.data.ServerPlacement;
import ch.endte.syncmatica.data.SyncmaticManager;
import com.mojang.authlib.GameProfile;
import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.OpenChoices;
import dev.buildingplanner.plan.Plan;
import dev.buildingplanner.sync.Payloads;
import dev.buildingplanner.sync.PlayerMessage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;

/**
 * Keeps the live sync's plans in step with Syncmatica's placements on the server: every placement is a plan. At start
 * each placement's plan comes from its file; a newly shared placement's plan comes from the file Syncmatica stored; a
 * removed placement's plan stops syncing and goes to {@code deleted/}. Also holds the delete check (hook S1).
 *
 * <p>Syncmatica starts its server side after Fabric's server-starting event, so this attaches on the first server tick
 * that finds it started. Everything here runs on the server thread.
 */
final class SyncmaticaPlans {
	private final MinecraftServer server;
	private final PlanSync sync;
	private final PlanStore store;
	private final PlanSync.Outbox outbox;
	private final LongSupplier clock;
	private SyncmaticManager placements;

	SyncmaticaPlans(MinecraftServer server, PlanSync sync, PlanStore store, PlanSync.Outbox outbox, LongSupplier clock) {
		this.server = server;
		this.sync = sync;
		this.store = store;
		this.outbox = outbox;
		this.clock = clock;
	}

	void tick() {
		if (placements == null) {
			attach();
		}
		store.tick(sync, clock.getAsLong());
	}

	/** Saves every changed plan and waits for the files. */
	void stop() {
		store.saveChanged(sync, clock.getAsLong());
		store.close();
	}

	/**
	 * Hook S1: whether {@code sender} may delete the placement. Only the player who shared it may, and operators too
	 * while {@link OpenChoices#OPERATORS_DELETE_ANY_PLAN} is on. A refused sender gets a notice. A placement Syncmatica
	 * doesn't have passes, as Syncmatica ignores it.
	 */
	boolean mayDelete(GameProfile sender, UUID placementId) {
		ServerPlacement placement = placements == null ? null : placements.getPlacement(placementId);
		if (placement == null) {
			return true;
		}
		boolean operator = OpenChoices.OPERATORS_DELETE_ANY_PLAN
			&& server.getPlayerList().isOp(new NameAndId(sender));
		if (allowsDelete(placement.getOwner().uuid, sender.id(), operator, OpenChoices.OPERATORS_DELETE_ANY_PLAN)) {
			return true;
		}
		outbox.send(sender.id(), new Payloads.Notice(PlayerMessage.notYourPlan(placement.getOwner().getName())));
		return false;
	}

	static boolean allowsDelete(UUID owner, UUID sender, boolean senderIsOperator, boolean operatorsMayDelete) {
		return owner.equals(sender) || operatorsMayDelete && senderIsOperator;
	}

	private void attach() {
		Context context = Syncmatica.getContext(Syncmatica.SERVER_CONTEXT);
		if (context == null || !context.isStarted()) {
			return;
		}
		placements = context.getSyncmaticManager();
		List<ServerPlacement> existing = List.copyOf(placements.getAll());
		for (ServerPlacement placement : existing) {
			add(context, placement);
		}
		placements.addServerPlacementConsumer(placement -> changed(context, placement));
		BuildingPlanner.LOGGER.info("Building Planner is syncing {} of {} shared plans", existing.stream()
			.filter(placement -> sync.plan(placement.getId()).isPresent()).count(), existing.size());
	}

	/** Syncmatica calls this when a placement is added, moved or removed. */
	private void changed(Context context, ServerPlacement placement) {
		UUID id = placement.getId();
		boolean shared = placements.getPlacement(id) != null;
		if (shared && sync.plan(id).isEmpty()) {
			add(context, placement);
		} else if (!shared) {
			Optional<Plan> plan = sync.plan(id);
			sync.removePlan(id);
			store.archive(id, plan, clock.getAsLong());
		}
	}

	private void add(Context context, ServerPlacement placement) {
		store.load(placement.getId(), context.getFileStorage().getLocalLitematic(placement))
			.ifPresent(plan -> sync.addPlan(placement.getId(), plan));
	}
}
