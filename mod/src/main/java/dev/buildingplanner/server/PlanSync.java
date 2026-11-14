package dev.buildingplanner.server;

import dev.buildingplanner.plan.Plan;
import dev.buildingplanner.plan.PlanRegion;
import dev.buildingplanner.sync.BlockChange;
import dev.buildingplanner.sync.EditKind;
import dev.buildingplanner.sync.Payloads;
import dev.buildingplanner.sync.PlayerMessage;
import dev.buildingplanner.sync.RegionBox;
import dev.buildingplanner.sync.SyncLimits;
import dev.buildingplanner.server.UndoHistory.Step;
import dev.buildingplanner.server.UndoHistory.StepBlock;
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
 *
 * <p>Each player's edits also go on their own undo list (see {@link #undo}).
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
	private final UndoHistory history;

	public PlanSync(SyncLimits limits, Outbox outbox) {
		this.limits = limits;
		this.outbox = outbox;
		this.history = new UndoHistory();
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

	/**
	 * Stops syncing a plan and forgets its subscribers and every player's undo steps in it. Their clients drop the
	 * plan when Syncmatica removes it.
	 */
	public void removePlan(UUID planId) {
		plans.remove(planId);
		history.dropPlan(planId);
	}

	public Optional<Plan> plan(UUID planId) {
		return Optional.ofNullable(plans.get(planId)).map(live -> live.plan);
	}

	/** The plans whose blocks or size changed since the last call, which clears their changed flags. */
	public List<UUID> takeChanged() {
		List<UUID> changed = new ArrayList<>();
		plans.forEach((planId, live) -> {
			if (live.changed) {
				live.changed = false;
				changed.add(planId);
			}
		});
		return changed;
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
		// Each block's state before the edit (its first write) and after it (its last).
		Map<RegionPos, StepBlock> step = new LinkedHashMap<>();
		for (BlockChange change : inBox) {
			BlockState before = plan.region(change.region()).orElseThrow().set(change.pos(), change.state());
			if (!before.equals(change.state())) {
				applied.add(change);
				step.merge(new RegionPos(change.region(), change.pos()),
					new StepBlock(change.region(), change.pos(), before, change.state()),
					(first, last) -> new StepBlock(first.region(), first.pos(), first.before(), last.after()));
			}
		}
		List<StepBlock> stepBlocks = step.values().stream().filter(block -> !block.before().equals(block.after())).toList();
		if (!stepBlocks.isEmpty()) {
			history.record(player, new Step(planId, stepBlocks));
		}
		live.revision++;
		live.changed |= !grown.isEmpty() || !applied.isEmpty();
		broadcast(live, new Payloads.Applied(planId, live.revision, player, clientSeq, grown, applied));
		return Optional.empty();
	}

	/**
	 * Takes back the player's newest step, in whichever plan it was made: each block goes back to its state before
	 * the step only if the plan still holds the step's state there. Blocks someone has changed since stay as they
	 * are, and a notice says how many. The undo goes to subscribers as an ordinary applied edit. Plans never shrink.
	 */
	public void undo(UUID player) {
		Optional<Step> step = history.takeUndo(player);
		if (step.isEmpty()) {
			outbox.send(player, new Payloads.Notice(PlayerMessage.nothingToUndo()));
			return;
		}
		Step undone = applyStep(player, step.get().reversed());
		history.undone(player, undone.reversed());
	}

	/** Takes the player's newest undone step forward again, as {@link #undo} takes it back. */
	public void redo(UUID player) {
		Optional<Step> step = history.takeRedo(player);
		if (step.isEmpty()) {
			outbox.send(player, new Payloads.Notice(PlayerMessage.nothingToRedo()));
			return;
		}
		history.redone(player, applyStep(player, step.get()));
	}

	/** Sets each block from its before-state to its after-state where the plan still holds the before-state. */
	private Step applyStep(UUID player, Step step) {
		LivePlan live = plans.get(step.plan());
		List<StepBlock> done = new ArrayList<>(step.blocks().size());
		for (StepBlock block : step.blocks()) {
			PlanRegion region = live.plan.region(block.region()).orElseThrow();
			if (region.get(block.pos()).equals(block.before())) {
				region.set(block.pos(), block.after());
				done.add(block);
			}
		}
		if (!done.isEmpty()) {
			live.revision++;
			live.changed = true;
			broadcast(live, new Payloads.Applied(step.plan(), live.revision, player, Payloads.NO_CLIENT_SEQ, List.of(),
				done.stream().map(block -> new BlockChange(block.region(), block.pos(), block.after())).toList()));
		}
		int left = step.blocks().size() - done.size();
		if (left > 0) {
			outbox.send(player, new Payloads.Notice(PlayerMessage.blocksLeftAsTheyAre(left)));
		}
		return new Step(step.plan(), done);
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

	private record RegionPos(String region, BlockPos pos) {
	}

	private static final class LivePlan {
		final Plan plan;
		final Set<UUID> subscribers = new LinkedHashSet<>();
		int revision;
		/** Set when the plan's blocks or size change; {@link #takeChanged} clears it. */
		boolean changed;

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
