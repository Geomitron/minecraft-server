package dev.buildingplanner.sync;

import java.util.Optional;

/** The rules for copying ghost blocks. */
public final class CopyRules {
	private CopyRules() {
	}

	/**
	 * Why a copy of {@code blocks} blocks, ghost blocks or {@code real} ones, is refused, or empty when it isn't: a box
	 * with none, or more than {@code maxEditBlocks}, the server's cap for an edit, which its paste would exceed.
	 */
	public static Optional<PlayerMessage> refusal(int blocks, int maxEditBlocks, boolean real) {
		if (blocks == 0) {
			return Optional.of(real ? PlayerMessage.nothingRealToCopy() : PlayerMessage.nothingToCopy());
		}
		if (blocks > maxEditBlocks) {
			return Optional.of(PlayerMessage.copyTooLarge(blocks, maxEditBlocks));
		}
		return Optional.empty();
	}
}
