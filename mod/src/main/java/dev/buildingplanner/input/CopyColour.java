package dev.buildingplanner.input;

import dev.buildingplanner.input.CopyPlacement.Spot;
import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.layer.LayerSection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The colour of the copy at the aim, in Satisfactory's hologram colours: red where the server would refuse its paste,
 * yellow where some of its blocks would replace ghost blocks already there, and blue where the paste would go in as it
 * shows. While "Place real blocks" acts, the rules for real blocks ({@link #ofReal}). With a set fixed, the whole set has
 * one colour, since it pastes as one edit.
 */
public final class CopyColour {
	private static final Vec3i ONE = new Vec3i(1, 1, 1);

	private CopyColour() {
	}

	public enum Colour {
		BLUE, YELLOW, RED
	}

	/**
	 * Why the server would refuse the paste, checked in the server's order: the block cap, then each block's place; and
	 * for real blocks the cap, then the inventory ({@code NOT_ENOUGH_BLOCKS}).
	 */
	public enum Refusal {
		TOO_MANY_BLOCKS, BUILD_HEIGHT, WORLD_BORDER, NOT_ENOUGH_BLOCKS
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
	 * The colour of a set of copies in the boxes {@code set} (the copy's own box alone without a set), each copy holding
	 * {@code blocksPerCopy} blocks: red while {@link #refusal} finds a reason, otherwise yellow while {@code overGhosts}
	 * says so (asked only then, since it reads every block), otherwise blue.
	 */
	public static Shown of(List<Spot> set, int blocksPerCopy, World world, int maxEditBlocks,
		BooleanSupplier overGhosts) {
		Optional<Refusal> refusal = refusal(set, blocksPerCopy, world, maxEditBlocks);
		if (refusal.isPresent()) {
			return Shown.red(refusal.get());
		}
		return overGhosts.getAsBoolean() ? Shown.YELLOW : Shown.BLUE;
	}

	/**
	 * Why the server would refuse the set's paste: more blocks than its cap, or a block outside the build height or the
	 * world border. The copy's box is the smallest box around its blocks, so its blocks reach each side of every copy's
	 * box, and of the set's; the build height bounds y, and the border bounds x and z, each on its own, so the set's
	 * lowest and highest corners stand for its blocks.
	 */
	public static Optional<Refusal> refusal(List<Spot> set, int blocksPerCopy, World world, int maxEditBlocks) {
		if ((long) set.size() * blocksPerCopy > maxEditBlocks) {
			return Optional.of(Refusal.TOO_MANY_BLOCKS);
		}
		Spot box = CopyGrid.box(set);
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
	 * Whether a block of any copy in the set would replace a ghost block in {@code layer}: a ghost block other than the
	 * copy's own block there, which the paste would leave as it is. {@code blocks} are the copy's blocks, as it's turned
	 * and mirrored, by their position from the copy's lowest corner. A set over no column of the layer reads no block.
	 */
	public static boolean overGhosts(List<Spot> set, Map<BlockPos, BlockState> blocks, Layer layer) {
		Spot box = CopyGrid.box(set);
		if (!anyColumn(layer, box)) {
			return false;
		}
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (Spot spot : set) {
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

	/**
	 * The colour of the locked positions (see "Locking and the first click": "Colours"), as copies of one block:
	 * {@code own} the positions, and {@code placed} the blocks a right click would place at them, by their positions in
	 * the world, which may differ between positions, as the block mix's do. Red while {@link #refusal} finds a reason,
	 * otherwise yellow where a block would replace a different ghost block, otherwise blue.
	 */
	public static Shown ofBlocks(List<BlockPos> own, Map<BlockPos, BlockState> placed, World world, int maxEditBlocks,
		Layer layer) {
		List<Spot> spots = own.stream().map(pos -> new Spot(pos, ONE)).toList();
		return of(spots, 1, world, maxEditBlocks, () -> placed.entrySet().stream().anyMatch(block -> {
			BlockState ghost = layer.get(block.getKey());
			return !ghost.isAir() && ghost != block.getValue();
		}));
	}


	/**
	 * The colour of a set of copies while "Place real blocks" acts (see "Place real blocks": "A paste"), the copies in
	 * the boxes {@code set} (the copy's own box alone without a set), each holding {@code blocks}, by their position
	 * from the copy's lowest corner. Red while the set carries more than {@link RealPlace#MAX_BLOCKS} blocks, checked
	 * first, as on the server, or while {@code inventory} lacks an item the blocks that go in need, counted as for a
	 * shape ({@link RealPlace#needs}); otherwise yellow while any block the server places itself (all but the second
	 * half of a two-part block) wouldn't go in as far as the player's game can tell ({@link RealPlace#goesIn}: a block
	 * no item places, such as water, past the reach from {@code eyes}, outside the world, in a space that doesn't take
	 * it, or with an entity in its way), and blue where every one would. Ghost blocks don't count, since real blocks
	 * don't replace them, and neither does whether a block can stay, since the copy's own blocks may hold it up.
	 */
	public static Shown ofReal(List<Spot> set, Map<BlockPos, BlockState> blocks, RealPlace.World world, Vec3 eyes,
		RealPlace.Inventory inventory) {
		if (RealPlace.tooMany((long) set.size() * blocks.size())) {
			return Shown.red(Refusal.TOO_MANY_BLOCKS);
		}
		List<RealPlace.Entry> placed = new ArrayList<>(set.size() * blocks.size());
		for (Spot spot : set) {
			BlockPos corner = spot.min();
			blocks.forEach((offset, state) -> placed.add(new RealPlace.Entry(corner.offset(offset), state)));
		}
		List<RealPlace.Entry> own = RealPlace.withoutSecondHalves(placed);
		List<RealPlace.Entry> going = RealPlace.goingIn(own, world, eyes);
		if (!inventory.covers(RealPlace.needs(going))) {
			return Shown.red(Refusal.NOT_ENOUGH_BLOCKS);
		}
		return going.size() == own.size() ? Shown.BLUE : Shown.YELLOW;
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
