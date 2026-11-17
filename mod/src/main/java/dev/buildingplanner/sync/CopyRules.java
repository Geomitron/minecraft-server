package dev.buildingplanner.sync;

import java.util.Optional;

/** The rules for copying ghost blocks. */
public final class CopyRules {
	private CopyRules() {
	}

	/**
	 * Why a copy of {@code blocks} ghost blocks is refused, or empty when it isn't: an area with no ghost blocks, or more
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

}
