package dev.buildingplanner.sync;

import dev.buildingplanner.plan.Plan;
import dev.buildingplanner.plan.PlanRegion;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * One player's copies of the plans they have loaded, kept from the server's snapshots and applied edits, and the edits
 * they send. There is one for each connection to a server with Building Planner, and every call runs on the client's
 * main thread.
 *
 * <p>Attaching a plan subscribes to it. The server answers with a snapshot, then every applied edit in revision order.
 * A gap in revisions, a change the copy can't take, or a rejected edit asks for a fresh snapshot; until it arrives, the
 * plan's applied edits are skipped. A Litematica edit changes the copy at once, as Litematica has already written it
 * on the player's screen; other edits change it when the server's {@code applied} comes back.
 */
public final class PlanReplicas {
	/** Sends a payload to the server. */
	@FunctionalInterface
	public interface Connection {
		void send(CustomPacketPayload payload);
	}

	/** What the player's game shows of their plans: Litematica's placements in the game, nothing in tests. */
	public interface View {
		/** A snapshot replaced the plan's blocks, and its regions' boxes may have changed. */
		default void loaded(UUID planId, Plan plan) {
		}

		/** A region grew to its new box, keeping its blocks. */
		default void grew(UUID planId, PlanRegion region) {
		}

		/** Blocks changed in an edit {@code author} made. */
		default void changed(UUID planId, UUID author, List<BlockChange> changes) {
		}

		/** A message for the player. */
		default void message(PlayerMessage message) {
		}
	}

	private final Connection connection;
	private final View view;
	private final Map<UUID, Replica> replicas = new HashMap<>();
	private int nextSeq = 1;
	private int gaps;

	public PlanReplicas(Connection connection, View view) {
		this.connection = connection;
		this.view = view;
	}

	/** Subscribes to a plan; does nothing if it's attached already. */
	public void attach(UUID planId) {
		if (replicas.putIfAbsent(planId, new Replica()) == null) {
			connection.send(new Payloads.Subscribe(planId));
		}
	}

	/** Unsubscribes from a plan and forgets the copy. */
	public void detach(UUID planId) {
		if (replicas.remove(planId) != null) {
			connection.send(new Payloads.Unsubscribe(planId));
		}
	}

	public boolean isAttached(UUID planId) {
		return replicas.containsKey(planId);
	}

	/** The plan's copy, once its first snapshot has arrived. */
	public Optional<Plan> plan(UUID planId) {
		return Optional.ofNullable(replicas.get(planId)).map(replica -> replica.plan);
	}

	/** Every attached plan's copy that has had a snapshot. */
	public Map<UUID, Plan> plans() {
		Map<UUID, Plan> plans = new HashMap<>();
		replicas.forEach((planId, replica) -> {
			if (replica.plan != null) {
				plans.put(planId, replica.plan);
			}
		});
		return Collections.unmodifiableMap(plans);
	}

	/** The revision the copy is at, or -1 before its first snapshot. */
	public int revision(UUID planId) {
		Replica replica = replicas.get(planId);
		return replica == null || replica.plan == null ? -1 : replica.revision;
	}

	/** How many times a gap in revisions made this client ask for a fresh snapshot. */
	public int gaps() {
		return gaps;
	}

	/**
	 * Sends an edit and returns its client sequence number. A Litematica edit also changes the copy at once, inside its
	 * regions' boxes.
	 */
	public int edit(UUID planId, EditKind kind, List<BlockChange> changes) {
		Replica replica = replicas.get(planId);
		if (kind == EditKind.LITEMATICA && replica != null && replica.plan != null) {
			for (BlockChange change : changes) {
				replica.plan.region(change.region()).filter(region -> region.contains(change.pos()))
					.ifPresent(region -> region.set(change.pos(), change.state()));
			}
		}
		int seq = nextSeq++;
		connection.send(new Payloads.Edit(planId, seq, kind, changes));
		return seq;
	}

	/** Asks for a fresh snapshot of an attached plan, dropping the copy's early Litematica changes. */
	public void resync(UUID planId) {
		Replica replica = replicas.get(planId);
		if (replica != null) {
			resubscribe(planId, replica);
		}
	}

	public void undo() {
		connection.send(new Payloads.Undo());
	}

	public void redo() {
		connection.send(new Payloads.Redo());
	}

	/** Takes one payload from the server. */
	public void receive(CustomPacketPayload payload) {
		switch (payload) {
			case Payloads.Snapshot snapshot -> load(snapshot);
			case Payloads.Applied applied -> apply(applied);
			case Payloads.Rejected rejected -> reject(rejected);
			case Payloads.Notice notice -> view.message(notice.message());
			default -> throw new IllegalArgumentException("Not a sync payload: " + payload.type());
		}
	}

	private void load(Payloads.Snapshot snapshot) {
		Replica replica = replicas.get(snapshot.plan());
		if (replica == null) {
			return;
		}
		replica.plan = Plan.fromSnapshot(snapshot.regions());
		replica.revision = snapshot.rev();
		replica.awaitingSnapshot = false;
		view.loaded(snapshot.plan(), replica.plan);
	}

	private void apply(Payloads.Applied applied) {
		Replica replica = replicas.get(applied.plan());
		if (replica == null || replica.plan == null || replica.awaitingSnapshot) {
			return;
		}
		if (applied.rev() != replica.revision + 1) {
			gaps++;
			resubscribe(applied.plan(), replica);
			return;
		}
		Plan plan = replica.plan;
		for (RegionBox box : applied.growth()) {
			Optional<PlanRegion> region = plan.region(box.region());
			if (region.isEmpty()) {
				resubscribe(applied.plan(), replica);
				return;
			}
			region.get().growToInclude(box.min(), box.max());
			view.grew(applied.plan(), region.get());
		}
		for (BlockChange change : applied.changes()) {
			Optional<PlanRegion> region = plan.region(change.region()).filter(r -> r.contains(change.pos()));
			if (region.isEmpty()) {
				resubscribe(applied.plan(), replica);
				return;
			}
			region.get().set(change.pos(), change.state());
		}
		replica.revision = applied.rev();
		if (!applied.changes().isEmpty()) {
			view.changed(applied.plan(), applied.author(), applied.changes());
		}
	}

	private void reject(Payloads.Rejected rejected) {
		Replica replica = replicas.get(rejected.plan());
		if (rejected.clientSeq() == Payloads.NO_CLIENT_SEQ) {
			// The server doesn't have the plan.
			replicas.remove(rejected.plan());
			return;
		}
		view.message(rejected.reason());
		if (replica != null) {
			// Drop any early copy of the edit.
			resubscribe(rejected.plan(), replica);
		}
	}

	private void resubscribe(UUID planId, Replica replica) {
		replica.awaitingSnapshot = true;
		connection.send(new Payloads.Subscribe(planId));
	}

	private static final class Replica {
		/** Null until the first snapshot. */
		Plan plan;
		int revision;
		/** Set while a fresh snapshot is on its way; applied edits until then are already in it or after it. */
		boolean awaitingSnapshot;
	}
}
