package dev.buildingplanner.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One region of a plan: a box of blocks stored as a palette and one palette index per block, as Litematica stores a
 * sub-region.
 *
 * <p>Positions are plan coordinates: relative to the schematic's origin, before the placement's rotation and mirror.
 * The region keeps Litematica's {@code Position} and {@code Size} as the file gave them (a size may be negative, so
 * the region runs from its position toward lower coordinates); the box is {@link #minCorner()} to
 * {@link #maxCorner()}. Blocks are stored from the minimum corner, x fastest, then z, then y.
 */
public final class PlanRegion {
	static final BlockState AIR = Blocks.AIR.defaultBlockState();

	private final String name;
	private BlockPos position;
	private Vec3i size;
	private BlockPos min;
	private Vec3i dimensions;
	private final List<BlockState> palette;
	private final Map<BlockState, Integer> paletteIds = new HashMap<>();
	private int[] blocks;
	/** The region's other entries (block entities, entities, scheduled ticks), kept as the file had them. */
	private final CompoundTag extras;

	PlanRegion(String name, BlockPos position, Vec3i size, List<BlockState> palette, int[] blocks, CompoundTag extras) {
		if (size.getX() == 0 || size.getY() == 0 || size.getZ() == 0) {
			throw new IllegalArgumentException("Region " + name + " has an empty size " + size);
		}
		this.name = name;
		this.palette = new ArrayList<>(palette);
		for (int id = 0; id < this.palette.size(); id++) {
			paletteIds.putIfAbsent(this.palette.get(id), id);
		}
		this.extras = extras;
		setBox(position, size);
		if (blocks.length != volume()) {
			throw new IllegalArgumentException("Region " + name + " has " + blocks.length + " blocks for a volume of "
				+ volume());
		}
		this.blocks = blocks;
	}

	/** An empty region (all air) whose box runs from {@code min} over {@code dimensions}. */
	public static PlanRegion empty(String name, BlockPos min, Vec3i dimensions) {
		long volume = checkedVolume(dimensions);
		return new PlanRegion(name, min, dimensions, List.of(AIR), new int[(int) volume], new CompoundTag());
	}

	public String name() {
		return name;
	}

	/** Litematica's {@code Position}: the corner the region was defined from. */
	public BlockPos position() {
		return position;
	}

	/** Litematica's {@code Size}, negative on an axis where the region runs from its position toward lower values. */
	public Vec3i size() {
		return size;
	}

	public BlockPos minCorner() {
		return min;
	}

	public BlockPos maxCorner() {
		return min.offset(dimensions.getX() - 1, dimensions.getY() - 1, dimensions.getZ() - 1);
	}

	/** The box's size, positive on every axis. */
	public Vec3i dimensions() {
		return dimensions;
	}

	public long volume() {
		return (long) dimensions.getX() * dimensions.getY() * dimensions.getZ();
	}

	public boolean contains(BlockPos pos) {
		BlockPos max = maxCorner();
		return pos.getX() >= min.getX() && pos.getY() >= min.getY() && pos.getZ() >= min.getZ()
			&& pos.getX() <= max.getX() && pos.getY() <= max.getY() && pos.getZ() <= max.getZ();
	}

	public BlockState get(BlockPos pos) {
		return palette.get(blocks[indexOf(pos)]);
	}

	/** Sets a block inside the box and returns the block it replaced. */
	public BlockState set(BlockPos pos, BlockState state) {
		int index = indexOf(pos);
		BlockState previous = palette.get(blocks[index]);
		blocks[index] = paletteIds.computeIfAbsent(state, s -> {
			palette.add(s);
			return palette.size() - 1;
		});
		return previous;
	}

	/** Blocks other than air. */
	public int nonAirBlocks() {
		int count = 0;
		for (int id : blocks) {
			if (!palette.get(id).isAir()) {
				count++;
			}
		}
		return count;
	}

	/** The palette, in index order. Entries stay once added, as in Litematica. */
	public List<BlockState> palette() {
		return Collections.unmodifiableList(palette);
	}

	/**
	 * Grows the box to take in {@code from} to {@code to} (any two opposite corners), keeping every block at its plan
	 * coordinates. A grown region's position becomes its new minimum corner and its size turns positive. Block
	 * entities, entities and scheduled ticks move with their blocks. Returns false when the box already holds both.
	 */
	public boolean growToInclude(BlockPos from, BlockPos to) {
		BlockPos max = maxCorner();
		BlockPos newMin = new BlockPos(
			Math.min(min.getX(), Math.min(from.getX(), to.getX())),
			Math.min(min.getY(), Math.min(from.getY(), to.getY())),
			Math.min(min.getZ(), Math.min(from.getZ(), to.getZ())));
		BlockPos newMax = new BlockPos(
			Math.max(max.getX(), Math.max(from.getX(), to.getX())),
			Math.max(max.getY(), Math.max(from.getY(), to.getY())),
			Math.max(max.getZ(), Math.max(from.getZ(), to.getZ())));
		if (newMin.equals(min) && newMax.equals(max)) {
			return false;
		}
		Vec3i newDimensions = new Vec3i(newMax.getX() - newMin.getX() + 1, newMax.getY() - newMin.getY() + 1,
			newMax.getZ() - newMin.getZ() + 1);
		int[] grown = new int[(int) checkedVolume(newDimensions)];
		int shiftX = min.getX() - newMin.getX();
		int shiftY = min.getY() - newMin.getY();
		int shiftZ = min.getZ() - newMin.getZ();
		for (int y = 0; y < dimensions.getY(); y++) {
			for (int z = 0; z < dimensions.getZ(); z++) {
				int from0 = (y * dimensions.getZ() + z) * dimensions.getX();
				int to0 = ((y + shiftY) * newDimensions.getZ() + z + shiftZ) * newDimensions.getX() + shiftX;
				System.arraycopy(blocks, from0, grown, to0, dimensions.getX());
			}
		}
		BlockPos oldPosition = position;
		blocks = grown;
		setBox(newMin, newDimensions);
		shiftBlockPositions("TileEntities", shiftX, shiftY, shiftZ);
		shiftBlockPositions("PendingBlockTicks", shiftX, shiftY, shiftZ);
		shiftBlockPositions("PendingFluidTicks", shiftX, shiftY, shiftZ);
		shiftEntities(oldPosition.getX() - position.getX(), oldPosition.getY() - position.getY(),
			oldPosition.getZ() - position.getZ());
		return true;
	}

	int[] paletteIndices() {
		return blocks;
	}

	CompoundTag extras() {
		return extras;
	}

	private void setBox(BlockPos position, Vec3i size) {
		this.position = position;
		this.size = size;
		// Litematica's end corner: the position plus the size, less one step toward the position.
		BlockPos end = position.offset(size.getX() - Integer.signum(size.getX()), size.getY() - Integer.signum(size.getY()),
			size.getZ() - Integer.signum(size.getZ()));
		this.min = new BlockPos(Math.min(position.getX(), end.getX()), Math.min(position.getY(), end.getY()),
			Math.min(position.getZ(), end.getZ()));
		this.dimensions = new Vec3i(Math.abs(size.getX()), Math.abs(size.getY()), Math.abs(size.getZ()));
	}

	private int indexOf(BlockPos pos) {
		if (!contains(pos)) {
			throw new IllegalArgumentException(pos + " is outside region " + name + " (" + min + " to " + maxCorner() + ")");
		}
		int x = pos.getX() - min.getX();
		int y = pos.getY() - min.getY();
		int z = pos.getZ() - min.getZ();
		return (y * dimensions.getZ() + z) * dimensions.getX() + x;
	}

	private static long checkedVolume(Vec3i dimensions) {
		long volume = (long) dimensions.getX() * dimensions.getY() * dimensions.getZ();
		if (dimensions.getX() <= 0 || dimensions.getY() <= 0 || dimensions.getZ() <= 0 || volume > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Region dimensions out of range: " + dimensions);
		}
		return volume;
	}

	/** Block entities and scheduled ticks store x, y and z from the region's minimum corner. */
	private void shiftBlockPositions(String key, int dx, int dy, int dz) {
		for (Tag entry : extras.getListOrEmpty(key)) {
			if (entry instanceof CompoundTag tag && tag.contains("x") && tag.contains("y") && tag.contains("z")) {
				tag.putInt("x", tag.getIntOr("x", 0) + dx);
				tag.putInt("y", tag.getIntOr("y", 0) + dy);
				tag.putInt("z", tag.getIntOr("z", 0) + dz);
			}
		}
	}

	/** Entities store {@code Pos} from the region's position. */
	private void shiftEntities(int dx, int dy, int dz) {
		if (dx == 0 && dy == 0 && dz == 0) {
			return;
		}
		for (Tag entry : extras.getListOrEmpty("Entities")) {
			if (entry instanceof CompoundTag tag && tag.get("Pos") instanceof ListTag pos && pos.size() == 3) {
				pos.set(0, DoubleTag.valueOf(pos.getDoubleOr(0, 0) + dx));
				pos.set(1, DoubleTag.valueOf(pos.getDoubleOr(1, 0) + dy));
				pos.set(2, DoubleTag.valueOf(pos.getDoubleOr(2, 0) + dz));
			}
		}
	}
}
