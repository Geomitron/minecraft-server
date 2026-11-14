package dev.buildingplanner.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.plan.Plan;
import dev.buildingplanner.plan.PlanRegion;
import dev.buildingplanner.sync.BlockChange;
import dev.buildingplanner.sync.EditKind;
import dev.buildingplanner.sync.Payloads;
import dev.buildingplanner.sync.PlayerMessage;
import dev.buildingplanner.sync.RegionBox;
import dev.buildingplanner.sync.SyncLimits;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * A server's {@link PlanSync} and simulated clients joined by in-order queues, one each way per client, as one
 * connection keeps a player's packets in order. The test decides when the server reads each client's next message and
 * when each client reads its next one, so edits interleave as they would over real connections.
 */
final class SimulatedNetwork {
	final PlanSync server;
	/** Every revision the server sent in an {@code applied}, per plan, in send order. */
	final Map<UUID, List<Integer>> sentRevisions = new HashMap<>();
	private final Map<UUID, Client> clients = new HashMap<>();

	SimulatedNetwork(SyncLimits limits) {
		server = new PlanSync(limits, this::deliver);
	}

	Client client(String name) {
		Client client = new Client(name);
		clients.put(client.id, client);
		return client;
	}

	private void deliver(UUID player, CustomPacketPayload payload) {
		if (payload instanceof Payloads.Applied applied) {
			List<Integer> revisions = sentRevisions.computeIfAbsent(applied.plan(), p -> new ArrayList<>());
			if (revisions.isEmpty() || revisions.getLast() != applied.rev()) {
				revisions.add(applied.rev());
			}
		}
		clients.get(player).inbox.add(payload);
	}

	/** Runs every message in every queue until all are empty. */
	void settle() {
		boolean moved = true;
		while (moved) {
			moved = false;
			for (Client client : clients.values()) {
				while (client.serverReadsOne()) {
					moved = true;
				}
				while (client.readsOne()) {
					moved = true;
				}
			}
		}
	}

	/** One player's game with the add-on: its copy of each subscribed plan, kept from snapshots and applied edits. */
	final class Client {
		final UUID id = UUID.randomUUID();
		final String name;
		final Map<UUID, Plan> plans = new HashMap<>();
		final Map<UUID, Integer> revisions = new HashMap<>();
		final List<Payloads.Rejected> rejections = new ArrayList<>();
		final List<PlayerMessage> notices = new ArrayList<>();
		/** Revisions this client saw out of order (a gap makes it ask for a fresh snapshot). */
		int gaps;
		final Deque<CustomPacketPayload> inbox = new ArrayDeque<>();
		private final Deque<CustomPacketPayload> outbox = new ArrayDeque<>();
		private int nextSeq = 1;

		private Client(String name) {
			this.name = name;
		}

		void subscribe(UUID plan) {
			outbox.add(new Payloads.Subscribe(plan));
		}

		void unsubscribe(UUID plan) {
			plans.remove(plan);
			revisions.remove(plan);
			outbox.add(new Payloads.Unsubscribe(plan));
		}

		/**
		 * Sends an edit. A Litematica edit shows on this client at once, as Litematica writes it before the add-on sends
		 * it; shapes show when the server's {@code applied} comes back.
		 */
		int edit(UUID plan, EditKind kind, List<BlockChange> changes) {
			if (kind == EditKind.LITEMATICA && plans.containsKey(plan)) {
				for (BlockChange change : changes) {
					plans.get(plan).region(change.region()).filter(r -> r.contains(change.pos()))
						.ifPresent(region -> region.set(change.pos(), change.state()));
				}
			}
			int seq = nextSeq++;
			outbox.add(new Payloads.Edit(plan, seq, kind, changes));
			return seq;
		}

		void undo() {
			outbox.add(new Payloads.Undo());
		}

		void redo() {
			outbox.add(new Payloads.Redo());
		}

		/** The server reads this client's next message; false when there was none. */
		boolean serverReadsOne() {
			CustomPacketPayload payload = outbox.poll();
			if (payload == null) {
				return false;
			}
			switch (payload) {
				case Payloads.Subscribe p -> server.subscribe(id, p.plan());
				case Payloads.Unsubscribe p -> server.unsubscribe(id, p.plan());
				case Payloads.Edit p -> server.edit(id, p.plan(), p.clientSeq(), p.kind(), p.changes());
				case Payloads.Undo p -> server.undo(id);
				case Payloads.Redo p -> server.redo(id);
				default -> throw new IllegalStateException("Not a client payload: " + payload);
			}
			return true;
		}

		/** This client reads its next message from the server; false when there was none. */
		boolean readsOne() {
			CustomPacketPayload payload = inbox.poll();
			if (payload == null) {
				return false;
			}
			switch (payload) {
				case Payloads.Snapshot p -> {
					plans.put(p.plan(), Plan.fromSnapshot(p.regions()));
					revisions.put(p.plan(), p.rev());
				}
				case Payloads.Applied p -> apply(p);
				case Payloads.Rejected p -> {
					rejections.add(p);
					if (p.clientSeq() == Payloads.NO_CLIENT_SEQ) {
						// The server doesn't have the plan.
						plans.remove(p.plan());
						revisions.remove(p.plan());
					} else if (plans.containsKey(p.plan())) {
						// Drop any early copy of the edit: take a fresh snapshot.
						outbox.add(new Payloads.Subscribe(p.plan()));
					}
				}
				case Payloads.Notice p -> notices.add(p.message());
				default -> throw new IllegalStateException("Not a server payload: " + payload);
			}
			return true;
		}

		private void apply(Payloads.Applied applied) {
			Plan plan = plans.get(applied.plan());
			if (plan == null) {
				return;
			}
			int expected = revisions.get(applied.plan()) + 1;
			if (applied.rev() != expected) {
				gaps++;
				outbox.add(new Payloads.Subscribe(applied.plan()));
				return;
			}
			for (RegionBox box : applied.growth()) {
				plan.region(box.region()).orElseThrow().growToInclude(box.min(), box.max());
			}
			for (BlockChange change : applied.changes()) {
				plan.region(change.region()).orElseThrow().set(change.pos(), change.state());
			}
			revisions.put(applied.plan(), applied.rev());
		}

		/** Messages still waiting on either side of this client's connection. */
		boolean idle() {
			return inbox.isEmpty() && outbox.isEmpty();
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/** Asserts the two plans have the same regions, boxes and blocks. */
	static void assertSamePlan(Plan expected, Plan actual, String label) {
		assertNotNull(actual, label + ": no plan");
		List<String> expectedNames = expected.regions().stream().map(PlanRegion::name).toList();
		assertEquals(expectedNames, actual.regions().stream().map(PlanRegion::name).toList(), label + ": regions");
		for (PlanRegion region : expected.regions()) {
			PlanRegion other = actual.region(region.name()).orElseThrow();
			assertEquals(region.minCorner(), other.minCorner(), label + ": " + region.name() + " corner");
			assertEquals(region.dimensions(), other.dimensions(), label + ": " + region.name() + " size");
			for (BlockPos pos : BlockPos.betweenClosed(region.minCorner(), region.maxCorner())) {
				assertEquals(region.get(pos), other.get(pos), label + ": " + region.name() + " block at " + pos);
			}
		}
	}

	/** Asserts each plan's revisions went out as 1, 2, 3 and so on. */
	void assertRevisionsInOrder() {
		sentRevisions.forEach((plan, revisions) -> {
			for (int i = 0; i < revisions.size(); i++) {
				assertEquals(i + 1, revisions.get(i), "revision " + (i + 1) + " of " + plan);
			}
		});
		for (Client client : clients.values()) {
			assertTrue(client.idle(), client + " still has messages");
		}
	}
}
