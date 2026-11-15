package dev.buildingplanner.sync;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** The rules for copying part of the plans and pasting it into a plan. */
public final class CopyRules {
	private CopyRules() {
	}

	/**
	 * Why a copy of {@code blocks} plan blocks is refused, or empty when it isn't: an area with no plan blocks, or more
	 * than {@code maxEditBlocks}, the server's cap for an edit, which its paste would exceed.
	 */
	public static Optional<PlayerMessage> refusal(int blocks, int maxEditBlocks) {
		if (blocks == 0) {
			return Optional.of(PlayerMessage.nothingToCopy());
		}
		if (blocks > maxEditBlocks) {
			return Optional.of(PlayerMessage.copyTooLarge(blocks, maxEditBlocks));
		}
		return Optional.empty();
	}

	/** The plan a copy comes from: the one most of its blocks came from, the first of them on a tie. */
	public static UUID sourcePlan(Map<UUID, Integer> blocksByPlan) {
		UUID source = null;
		int most = 0;
		for (Map.Entry<UUID, Integer> entry : blocksByPlan.entrySet()) {
			if (entry.getValue() > most) {
				source = entry.getKey();
				most = entry.getValue();
			}
		}
		return source;
	}

	/**
	 * The plan a paste goes into: the plan whose box holds the copy's origin, or else the copy's source plan when the
	 * player has it loaded; empty when neither.
	 */
	public static Optional<UUID> pasteTarget(UUID planAtOrigin, UUID sourcePlan, boolean sourceLoaded) {
		if (planAtOrigin != null) {
			return Optional.of(planAtOrigin);
		}
		return sourceLoaded ? Optional.ofNullable(sourcePlan) : Optional.empty();
	}
}
