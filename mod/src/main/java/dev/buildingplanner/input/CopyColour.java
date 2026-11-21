package dev.buildingplanner.input;

import dev.buildingplanner.input.CopyPlacement.Spot;
import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.layer.LayerSection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The colour of the copy at the aim, in Satisfactory's hologram colours: red where the server would refuse its paste,
 * yellow where some of its blocks would replace ghost blocks already there, and blue where the paste would go in as it
 * shows. With a zoop's row fixed, the whole row has one colour, since it pastes as one edit.
 */
public final class CopyColour {
	private CopyColour() {
	}

	public enum Colour {
		BLUE, YELLOW, RED
	}

	/** Why the server would refuse the paste, checked in the server's order: the block cap, then each block's place. */
	public enum Refusal {
		TOO_MANY_BLOCKS, BUILD_HEIGHT, WORLD_BORDER
	}

	/** The copy's colour, with the server's reason while it's red. */
	public record Shown(Colour colour, Refusal refusal) {
		public static final Shown BLUE = new Shown(Colour.BLUE, null);
		public static final Shown YELLOW = new Shown(Colour.YELLOW, null);

		public static Shown red(Refusal refusal) {
			return new Shown(Colour.RED, refusal);
		}

		public boolean red() {
			return colour == Colour.RED;
		}
	}

	/** The world's limits on where a block may go, as the server checks an edit's positions. */
	public interface World {
		boolean inBuildHeight(int y);

		boolean insideBorder(BlockPos pos);
	}

	/**
	 * The colour of a row of copies in the boxes {@code row} (the copy's own box alone without a row), each copy holding
	 * {@code blocksPerCopy} blocks: red while {@link #refusal} finds a reason, otherwise yellow while {@code overGhosts}
	 * says so (asked only then, since it reads every block), otherwise blue.
	 */
	public static Shown of(List<Spot> row, int blocksPerCopy, World world, int maxEditBlocks,
		BooleanSupplier overGhosts) {
		Optional<Refusal> refusal = refusal(row, blocksPerCopy, world, maxEditBlocks);
		if (refusal.isPresent()) {
			return Shown.red(refusal.get());
		}
		return overGhosts.getAsBoolean() ? Shown.YELLOW : Shown.BLUE;
	}

	/**
	 * Why the server would refuse the row's paste: more blocks than its cap, or a block outside the build height or the
	 * world border. The copy's box is the smallest box around its blocks, so its blocks reach each side of every copy's
	 * box, and of the row's; the build height bounds y, and the border bounds x and z, each on its own, so the row's
	 * lowest and highest corners stand for its blocks.
	 */
	public static Optional<Refusal> refusal(List<Spot> row, int blocksPerCopy, World world, int maxEditBlocks) {
		if ((long) row.size() * blocksPerCopy > maxEditBlocks) {
			return Optional.of(Refusal.TOO_MANY_BLOCKS);
		}
		Spot box = CopyRow.box(row);
		BlockPos min = box.min();
		BlockPos max = box.max();
		if (!world.inBuildHeight(min.getY()) || !world.inBuildHeight(max.getY())) {
			return Optional.of(Refusal.BUILD_HEIGHT);
		}
		if (!world.insideBorder(min) || !world.insideBorder(max)) {
			return Optional.of(Refusal.WORLD_BORDER);
		}
		return Optional.empty();
	}

	/**
	 * Whether a block of any copy in the row would replace a ghost block in {@code layer}: a ghost block other than the
	 * copy's own block there, which the paste would leave as it is. {@code blocks} are the copy's blocks, as it's turned
	 * and mirrored, by their position from the copy's lowest corner. A row over no column of the layer reads no block.
	 */
	public static boolean overGhosts(List<Spot> row, Map<BlockPos, BlockState> blocks, Layer layer) {
		Spot box = CopyRow.box(row);
		if (!anyColumn(layer, box)) {
			return false;
		}
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (Spot spot : row) {
			BlockPos corner = spot.min();
			for (Map.Entry<BlockPos, BlockState> block : blocks.entrySet()) {
				BlockPos offset = block.getKey();
				at.set(corner.getX() + offset.getX(), corner.getY() + offset.getY(), corner.getZ() + offset.getZ());
				BlockState ghost = layer.get(at);
				if (!ghost.isAir() && ghost != block.getValue()) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean anyColumn(Layer layer, Spot box) {
		BlockPos min = box.min();
		BlockPos max = box.max();
		for (int x = min.getX() >> LayerSection.SHIFT; x <= max.getX() >> LayerSection.SHIFT; x++) {
			for (int z = min.getZ() >> LayerSection.SHIFT; z <= max.getZ() >> LayerSection.SHIFT; z++) {
				if (layer.column(x, z) != null) {
					return true;
				}
			}
		}
		return false;
	}
}
