package dev.buildingplanner.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildingplanner.plan.Plan;
import dev.buildingplanner.plan.PlanRegion;
import dev.buildingplanner.sync.BlockChange;
import dev.buildingplanner.sync.EditKind;
import dev.buildingplanner.sync.Payloads;
import dev.buildingplanner.sync.PlanPayloads;
import dev.buildingplanner.sync.PlanReplicas;
import dev.buildingplanner.sync.PlayerMessage;
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
		if (payload instanceof PlanPayloads.Applied applied) {
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

	/**
	 * One player's game with the add-on: its {@link PlanReplicas}, as the game runs it, with the messages each way
	 * queued so tests can order them.
	 */
	final class Client {
		final UUID id = UUID.randomUUID();
		final String name;
		final List<PlanPayloads.Rejected> rejections = new ArrayList<>();
		final List<PlayerMessage> notices = new ArrayList<>();
		final Deque<CustomPacketPayload> inbox = new ArrayDeque<>();
		private final Deque<CustomPacketPayload> outbox = new ArrayDeque<>();
		private final PlanReplicas replicas = new PlanReplicas(outbox::add, new PlanReplicas.View() {
		});

		private Client(String name) {
			this.name = name;
		}

		/** The client's copy of each subscribed plan that has had its snapshot. */
		Map<UUID, Plan> plans() {
			return replicas.plans();
		}

		int revision(UUID plan) {
			return replicas.revision(plan);
		}

		/** Revisions this client saw out of order (a gap makes it ask for a fresh snapshot). */
		int gaps() {
			return replicas.gaps();
		}

		void subscribe(UUID plan) {
			replicas.attach(plan);
		}

		void unsubscribe(UUID plan) {
			replicas.detach(plan);
		}

		int edit(UUID plan, EditKind kind, List<BlockChange> changes) {
			return replicas.edit(plan, kind, changes);
		}

		void undo() {
			replicas.undo();
		}

		void redo() {
			replicas.redo();
		}

		/** The server reads this client's next message; false when there was none. */
		boolean serverReadsOne() {
			CustomPacketPayload payload = outbox.poll();
			if (payload == null) {
				return false;
			}
			switch (payload) {
				case PlanPayloads.Subscribe p -> server.subscribe(id, p.plan());
				case PlanPayloads.Unsubscribe p -> server.unsubscribe(id, p.plan());
				case PlanPayloads.Edit p -> server.edit(id, p.plan(), p.clientSeq(), p.kind(), p.changes());
				case PlanPayloads.Undo p -> server.undo(id);
				case PlanPayloads.Redo p -> server.redo(id);
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
				case PlanPayloads.Rejected p -> rejections.add(p);
				case Payloads.Notice p -> notices.add(p.message());
				default -> {
				}
			}
			replicas.receive(payload);
			return true;
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
