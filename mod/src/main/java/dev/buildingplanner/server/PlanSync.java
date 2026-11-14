package dev.buildingplanner.server;

import dev.buildingplanner.plan.Plan;
import dev.buildingplanner.plan.PlanRegion;
import dev.buildingplanner.sync.BlockChange;
import dev.buildingplanner.sync.EditKind;
import dev.buildingplanner.sync.Payloads;
import dev.buildingplanner.sync.PlayerMessage;
import dev.buildingplanner.sync.RegionBox;
import dev.buildingplanner.sync.SyncLimits;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The server's live sync: the plans in memory, who has each loaded, and the order of their edits.
 *
 * <p>Every method runs on the server's main thread, one call at a time, so edits apply in the order they arrive. Each
 * applied edit raises its plan's revision by one and goes to every subscriber, its author included, through the
 * {@link Outbox}. Each block takes the value of the last edit, in this order, that wrote it; there are no locks.
 * Plans are keyed by Syncmatica's placement id; revisions start at 0 when a plan is added.
 */
public final class PlanSync {
	/** Sends a payload to one player; the Fabric adapter sends it on that player's connection, in call order. */
	@FunctionalInterface
	public interface Outbox {
		void send(UUID player, CustomPacketPayload payload);
	}

	private final SyncLimits limits;
	private final Outbox outbox;
	private final Map<UUID, LivePlan> plans = new HashMap<>();

	public PlanSync(SyncLimits limits, Outbox outbox) {
		this.limits = limits;
		this.outbox = outbox;
	}

	public SyncLimits limits() {
		return limits;
	}

	/** Starts syncing a plan. A plan id can be added once until it's removed. */
	public void addPlan(UUID planId, Plan plan) {
		if (plans.putIfAbsent(planId, new LivePlan(plan)) != null) {
			throw new IllegalStateException("Plan " + planId + " is already synced");
		}
	}

	/** Stops syncing a plan and forgets its subscribers. Their clients drop the plan when Syncmatica removes it. */
	public void removePlan(UUID planId) {
		plans.remove(planId);
	}

	public Optional<Plan> plan(UUID planId) {
		return Optional.ofNullable(plans.get(planId)).map(live -> live.plan);
	}

	public int revision(UUID planId) {
		LivePlan live = plans.get(planId);
		if (live == null) {
			throw new IllegalArgumentException("No plan " + planId);
		}
		return live.revision;
	}

	/**
	 * Sends the player a snapshot at the plan's current revision; every later {@code applied} follows on the same
	 * connection, so nothing falls between them. Subscribing again asks for a fresh snapshot.
	 */
	public void subscribe(UUID player, UUID planId) {
		LivePlan live = plans.get(planId);
		if (live == null) {
			outbox.send(player, new Payloads.Rejected(planId, Payloads.NO_CLIENT_SEQ, PlayerMessage.planGone()));
			return;
		}
		live.subscribers.add(player);
		outbox.send(player, new Payloads.Snapshot(planId, live.revision, live.plan.snapshot()));
	}

	public void unsubscribe(UUID player, UUID planId) {
		LivePlan live = plans.get(planId);
		if (live != null) {
			live.subscribers.remove(player);
		}
	}

	/** The player left: they no longer have any plan loaded. */
	public void disconnect(UUID player) {
		for (LivePlan live : plans.values()) {
			live.subscribers.remove(player);
		}
	}

	/**
	 * Checks and applies one edit. A shape grows its region to take in blocks outside it; an erase skips them; a
	 * Litematica edit must stay inside. A failed check changes nothing and answers the author with {@code rejected}.
	 */
	public void edit(UUID player, UUID planId, int clientSeq, EditKind kind, List<BlockChange> changes) {
		LivePlan live = plans.get(planId);
		Optional<PlayerMessage> problem = live == null ? Optional.of(PlayerMessage.planGone())
			: !live.subscribers.contains(player) ? Optional.of(PlayerMessage.planNotLoaded())
			: changes.size() > limits.maxEditBlocks()
				? Optional.of(PlayerMessage.tooManyBlocks(changes.size(), limits.maxEditBlocks()))
			: Optional.empty();
		if (problem.isEmpty()) {
			problem = applyEdit(player, planId, live, clientSeq, kind, changes);
		}
		problem.ifPresent(reason -> outbox.send(player, new Payloads.Rejected(planId, clientSeq, reason)));
	}

	private Optional<PlayerMessage> applyEdit(UUID player, UUID planId, LivePlan live, int clientSeq, EditKind kind,
		List<BlockChange> changes) {
		Plan plan = live.plan;
		List<BlockChange> inBox = new ArrayList<>(changes.size());
		Map<PlanRegion, Box> growth = new LinkedHashMap<>();
		for (BlockChange change : changes) {
			Optional<PlanRegion> found = plan.region(change.region());
			if (found.isEmpty()) {
				return Optional.of(PlayerMessage.editDoesNotFit());
			}
			PlanRegion region = found.get();
			if (region.contains(change.pos())) {
				inBox.add(change);
				continue;
			}
			switch (kind) {
				case SHAPE -> {
					growth.computeIfAbsent(region, r -> new Box(r.minCorner(), r.maxCorner())).include(change.pos());
					inBox.add(change);
				}
				case ERASE -> {
					// Outside the box is empty already.
				}
				case LITEMATICA -> {
					return Optional.of(PlayerMessage.editDoesNotFit());
				}
			}
		}
		Optional<PlayerMessage> tooLarge = checkSize(plan, growth);
		if (tooLarge.isPresent()) {
			return tooLarge;
		}

		List<RegionBox> grown = new ArrayList<>(growth.size());
		growth.forEach((region, box) -> {
			region.growToInclude(box.min, box.max);
			grown.add(new RegionBox(region.name(), region.minCorner(), region.dimensions()));
		});
		List<BlockChange> applied = new ArrayList<>(inBox.size());
		for (BlockChange change : inBox) {
			BlockState before = plan.region(change.region()).orElseThrow().set(change.pos(), change.state());
			if (!before.equals(change.state())) {
				applied.add(change);
			}
		}
		live.revision++;
		broadcast(live, new Payloads.Applied(planId, live.revision, player, clientSeq, grown, applied));
		return Optional.empty();
	}

	/** Growth must leave the plan's enclosing box within the size cap on every side that grows. */
	private Optional<PlayerMessage> checkSize(Plan plan, Map<PlanRegion, Box> growth) {
		if (growth.isEmpty()) {
			return Optional.empty();
		}
		Box before = null;
		for (PlanRegion region : plan.regions()) {
			before = Box.union(before, new Box(region.minCorner(), region.maxCorner()));
		}
		Box after = before.copy();
		growth.values().forEach(box -> after.include(box.min).include(box.max));
		int[] sidesBefore = before.sides();
		int[] sidesAfter = after.sides();
		for (int axis = 0; axis < 3; axis++) {
			if (sidesAfter[axis] > sidesBefore[axis] && sidesAfter[axis] > limits.maxPlanSide()) {
				return Optional.of(PlayerMessage.planTooLarge(sidesAfter[axis], limits.maxPlanSide()));
			}
		}
		return Optional.empty();
	}

	private void broadcast(LivePlan live, CustomPacketPayload payload) {
		for (UUID subscriber : live.subscribers) {
			outbox.send(subscriber, payload);
		}
	}

	private static final class LivePlan {
		final Plan plan;
		final Set<UUID> subscribers = new LinkedHashSet<>();
		int revision;

		LivePlan(Plan plan) {
			this.plan = plan;
		}
	}

	/** A box of block positions, inclusive; its sides are worked out in longs so a far-off position can't overflow. */
	private static final class Box {
		BlockPos min;
		BlockPos max;

		Box(BlockPos min, BlockPos max) {
			this.min = min;
			this.max = max;
		}

		static Box union(Box box, Box other) {
			return box == null ? other.copy() : box.include(other.min).include(other.max);
		}

		Box copy() {
			return new Box(min, max);
		}

		Box include(BlockPos pos) {
			min = BlockPos.min(min, pos);
			max = BlockPos.max(max, pos);
			return this;
		}

		int[] sides() {
			return new int[] {side(min.getX(), max.getX()), side(min.getY(), max.getY()), side(min.getZ(), max.getZ())};
		}

		private static int side(int min, int max) {
			return (int) Math.min((long) max - min + 1, Integer.MAX_VALUE);
		}
	}
}
