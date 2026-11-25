package dev.buildingplanner.input;

import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;

/**
 * "Match ghost blocks" (see "Settings"), as pure functions: the setting's saved value, and the ghost block's turn a
 * right click in Disable takes with the setting on, which hook M6 sends in place of Left Ctrl's.
 */
public final class GhostMatch {
	/** The setting's key in {@code config/buildingplanner-client.properties}. */
	public static final String SETTING = "match-ghost-turn";

	private GhostMatch() {
	}

	/** Whether the saved value is on: {@code true}; a missing or other value reads as off, as the setting starts. */
	public static boolean on(String saved) {
		return "true".equals(saved);
	}

	/** The value the screen's "Save" button saves for the setting. */
	public static String saved(boolean on) {
		return String.valueOf(on);
	}

	/**
	 * The ghost block's turn a right click takes with the setting {@code on}: for {@code item}, the ghost block's values
	 * of the orientation properties {@link BlockTurn} sets, where the block Minecraft would place there
	 * ({@code placed}) is the ghost block's block, so a torch placed as a wall torch matches a ghost wall torch and not a
	 * ghost torch. None with the setting off, no ghost block there, nothing placed, another block, or a ghost block with
	 * none of those properties.
	 */
	public static BlockTurn.Held turn(boolean on, BlockState ghost, BlockState placed, Item item) {
		if (!on || ghost == null || ghost.isAir() || placed == null || placed.getBlock() != ghost.getBlock()) {
			return BlockTurn.Held.NONE;
		}
		Map<String, String> values = BlockTurn.values(ghost);
		return values.isEmpty() ? BlockTurn.Held.NONE : new BlockTurn.Held(item, new BlockTurn.Turn(values, null));
	}

	/**
	 * The turn the click takes: the ghost block's turn ({@link #turn}) where it has one and the block turned so can stay
	 * ({@code stays}); otherwise {@code otherwise}, the turn it takes with the setting off ({@link DisableTurn#clickTakes}).
	 */
	public static BlockTurn.Held clickTakes(BlockTurn.Held ghost, boolean stays, BlockTurn.Held otherwise) {
		return ghost.turn() != null && stays ? ghost : otherwise;
	}
}
