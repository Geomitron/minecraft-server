package dev.buildingplanner.plan;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One region of a plan: a box of blocks, each a palette index, as Litematica stores a sub-region.
 *
 * <p>Positions are plan coordinates: relative to the schematic's origin, before the placement's rotation and mirror.
 * The region keeps Litematica's {@code Position} and {@code Size} as the file gave them (a size may be negative, so
 * the region runs from its position toward lower coordinates); the box is {@link #minCorner()} to
 * {@link #maxCorner()}. Packed blocks run from the minimum corner, x fastest, then z, then y.
 *
 * <p>In memory the blocks sit in 16-block cubes (sections) at two bytes a block. Palette index 0 (air in plans the
 * add-on makes, and in Litematica's files) fills the box wherever no section is allocated, and a section is freed
 * when it holds only index 0 again, so memory follows the blocks a region holds, not its box.
 */
public final class PlanRegion {
	static final BlockState AIR = Blocks.AIR.defaultBlockState();
	/** Sections index the palette with chars. Vanilla has far fewer block states than this. */
	static final int MAX_PALETTE_SIZE = 1 << Character.SIZE;
	/**
	 * The longest side a region can have. Section keys hold 21 signed bits an axis, counted from where the region
	 * started; this keeps every section of a region within them.
	 */
	static final int MAX_SIDE = 1 << 23;
	private static final int SECTION_SHIFT = 4;
	private static final int SECTION_SIDE = 1 << SECTION_SHIFT;
	private static final int SECTION_MASK = SECTION_SIDE - 1;
	private static final int SECTION_VOLUME = SECTION_SIDE * SECTION_SIDE * SECTION_SIDE;
	private static final int KEY_BITS = 21;
	private static final long KEY_MASK = (1L << KEY_BITS) - 1;

	private final String name;
	private BlockPos position;
	private Vec3i size;
	private BlockPos min;
	private Vec3i dimensions;
	private final List<BlockState> palette;
	private final Map<BlockState, Integer> paletteIds = new HashMap<>();
	/** The section holding the region's first minimum corner; section keys count from it, so growth moves none. */
	private final int anchorX;
	private final int anchorY;
	private final int anchorZ;
	private final Long2ObjectOpenHashMap<Section> sections = new Long2ObjectOpenHashMap<>();
	/** The region's other entries (block entities, entities, scheduled ticks), kept as the file had them. */
	private final CompoundTag extras;

	/** A region holding palette index 0 throughout. */
	PlanRegion(String name, BlockPos position, Vec3i size, List<BlockState> palette, CompoundTag extras) {
		if (size.getX() == 0 || size.getY() == 0 || size.getZ() == 0) {
			throw new IllegalArgumentException("Region " + name + " has an empty size " + size);
		}
		if (palette.isEmpty() || palette.size() > MAX_PALETTE_SIZE) {
			throw new IllegalArgumentException("Region " + name + " has a palette of " + palette.size() + " entries");
		}
		this.name = name;
		this.palette = new ArrayList<>(palette);
		for (int id = 0; id < this.palette.size(); id++) {
			paletteIds.putIfAbsent(this.palette.get(id), id);
		}
		this.extras = extras;
		setBox(position, size);
		checkDimensions(dimensions);
		anchorX = min.getX() >> SECTION_SHIFT;
		anchorY = min.getY() >> SECTION_SHIFT;
		anchorZ = min.getZ() >> SECTION_SHIFT;
	}

	/** An empty region (all air) whose box runs from {@code min} over {@code dimensions}. */
	public static PlanRegion empty(String name, BlockPos min, Vec3i dimensions) {
		return new PlanRegion(name, min, dimensions, List.of(AIR), new CompoundTag());
	}

	/**
	 * A region holding packed blocks, in Litematica's order. An index past the palette is an error when
	 * {@code strict}; otherwise it reads as index 0, as Litematica shows it.
	 */
	static PlanRegion unpack(String name, BlockPos position, Vec3i size, List<BlockState> palette, long[] packed,
		CompoundTag extras, boolean strict) {
		PlanRegion region = new PlanRegion(name, position, size, palette, extras);
		PackedBlocks.Reader reader = new PackedBlocks.Reader(packed, PackedBlocks.bitsFor(palette.size()), region.volume());
		region.forEachRun((x, y, z, length) -> {
			Section section = null;
			for (int i = 0; i < length; i++) {
				int id = reader.read();
				if (id >= palette.size()) {
					if (strict) {
						throw new IllegalArgumentException("Region " + name + " has a block past its palette");
					}
					id = 0;
				}
				if (id != 0) {
					if (section == null) {
						section = region.sections.computeIfAbsent(region.sectionKey(x, y, z), key -> new Section());
					}
					section.set(localIndex(x + i, y, z), id);
				}
			}
		});
		return region;
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
		return pos.getX() - (long) min.getX() >= 0 && pos.getX() - (long) min.getX() < dimensions.getX()
			&& pos.getY() - (long) min.getY() >= 0 && pos.getY() - (long) min.getY() < dimensions.getY()
			&& pos.getZ() - (long) min.getZ() >= 0 && pos.getZ() - (long) min.getZ() < dimensions.getZ();
	}

	public BlockState get(BlockPos pos) {
		checkInside(pos);
		Section section = sections.get(sectionKey(pos.getX(), pos.getY(), pos.getZ()));
		return palette.get(section == null ? 0 : section.get(localIndex(pos.getX(), pos.getY(), pos.getZ())));
	}

	/** Sets a block inside the box and returns the block it replaced. */
	public BlockState set(BlockPos pos, BlockState state) {
		checkInside(pos);
		Integer known = paletteIds.get(state);
		int id;
		if (known != null) {
			id = known;
		} else {
			if (palette.size() == MAX_PALETTE_SIZE) {
				throw new IllegalStateException("Region " + name + " has no room for another palette entry");
			}
			id = palette.size();
			palette.add(state);
			paletteIds.put(state, id);
		}
		long key = sectionKey(pos.getX(), pos.getY(), pos.getZ());
		Section section = sections.get(key);
		if (section == null) {
			if (id == 0) {
				return palette.getFirst();
			}
			section = new Section();
			sections.put(key, section);
		}
		int previous = section.set(localIndex(pos.getX(), pos.getY(), pos.getZ()), id);
		if (section.nonZero == 0) {
			sections.remove(key);
		}
		return palette.get(previous);
	}

	/** Blocks other than air. */
	public int nonAirBlocks() {
		long count = 0;
		long nonZero = 0;
		for (Section section : sections.values()) {
			nonZero += section.nonZero;
			for (char id : section.ids) {
				if (id != 0 && !palette.get(id).isAir()) {
					count++;
				}
			}
		}
		if (!palette.getFirst().isAir()) {
			count += volume() - nonZero;
		}
		return (int) count;
	}

	/** Sections holding a block other than palette index 0; each takes {@value #SECTION_VOLUME} chars. */
	int allocatedSections() {
		return sections.size();
	}

	/** The palette, in index order. Entries stay once added, as in Litematica. */
	public List<BlockState> palette() {
		return Collections.unmodifiableList(palette);
	}

	/** The blocks packed as Litematica packs them, at {@link PackedBlocks#bitsFor} the palette's size. */
	long[] packedBlocks() {
		PackedBlocks.Writer writer = new PackedBlocks.Writer(volume(), PackedBlocks.bitsFor(palette.size()));
		forEachRun((x, y, z, length) -> {
			Section section = sections.get(sectionKey(x, y, z));
			if (section == null) {
				writer.repeat(0, length);
				return;
			}
			int start = localIndex(x, y, z);
			for (int i = 0; i < length; i++) {
				writer.write(section.ids[start + i]);
			}
		});
		return writer.longs();
	}

	/** The sections holding something other than palette index 0, each packed for a snapshot. */
	List<RegionSnapshot.Section> snapshotSections() {
		int bits = PackedBlocks.bitsFor(palette.size());
		List<RegionSnapshot.Section> packed = new ArrayList<>(sections.size());
		for (Long2ObjectMap.Entry<Section> entry : sections.long2ObjectEntrySet()) {
			PackedBlocks.Writer writer = new PackedBlocks.Writer(SECTION_VOLUME, bits);
			for (char id : entry.getValue().ids) {
				writer.write(id);
			}
			long key = entry.getLongKey();
			packed.add(new RegionSnapshot.Section(keyField(key, 2 * KEY_BITS) + anchorX, keyField(key, KEY_BITS) + anchorY,
				keyField(key, 0) + anchorZ, writer.longs()));
		}
		return packed;
	}

	/**
	 * A region holding a snapshot's sections. A section outside the box, or a block outside the box or past the
	 * palette, is an error.
	 */
	static PlanRegion fromSections(String name, BlockPos min, Vec3i dimensions, List<BlockState> palette,
		List<RegionSnapshot.Section> sections) {
		PlanRegion region = new PlanRegion(name, min, dimensions, palette, new CompoundTag());
		BlockPos max = region.maxCorner();
		int bits = PackedBlocks.bitsFor(palette.size());
		for (RegionSnapshot.Section packed : sections) {
			if (packed.x() < min.getX() >> SECTION_SHIFT || packed.x() > max.getX() >> SECTION_SHIFT
				|| packed.y() < min.getY() >> SECTION_SHIFT || packed.y() > max.getY() >> SECTION_SHIFT
				|| packed.z() < min.getZ() >> SECTION_SHIFT || packed.z() > max.getZ() >> SECTION_SHIFT) {
				throw new IllegalArgumentException("Region " + name + " has a section outside its box");
			}
			PackedBlocks.Reader reader = new PackedBlocks.Reader(packed.packed(), bits, SECTION_VOLUME);
			int baseX = packed.x() << SECTION_SHIFT;
			int baseY = packed.y() << SECTION_SHIFT;
			int baseZ = packed.z() << SECTION_SHIFT;
			Section section = null;
			for (int index = 0; index < SECTION_VOLUME; index++) {
				int id = reader.read();
				if (id == 0) {
					continue;
				}
				int x = baseX + (index & SECTION_MASK);
				int y = baseY + (index >> 2 * SECTION_SHIFT);
				int z = baseZ + (index >> SECTION_SHIFT & SECTION_MASK);
				if (id >= palette.size() || x < min.getX() || x > max.getX() || y < min.getY() || y > max.getY()
					|| z < min.getZ() || z > max.getZ()) {
					throw new IllegalArgumentException("Region " + name + " has a block outside its box or palette");
				}
				if (section == null) {
					section = region.sections.computeIfAbsent(region.sectionKey(x, y, z), key -> new Section());
				}
				section.set(index, id);
			}
		}
		return region;
	}

	/** Calls {@code action} for each block whose palette index isn't 0; index 0 fills every other position. */
	public void forEachSetBlock(BiConsumer<BlockPos, BlockState> action) {
		for (Long2ObjectMap.Entry<Section> entry : sections.long2ObjectEntrySet()) {
			long key = entry.getLongKey();
			int baseX = keyField(key, 2 * KEY_BITS) + anchorX << SECTION_SHIFT;
			int baseY = keyField(key, KEY_BITS) + anchorY << SECTION_SHIFT;
			int baseZ = keyField(key, 0) + anchorZ << SECTION_SHIFT;
			char[] ids = entry.getValue().ids;
			for (int index = 0; index < SECTION_VOLUME; index++) {
				if (ids[index] != 0) {
					action.accept(new BlockPos(baseX + (index & SECTION_MASK), baseY + (index >> 2 * SECTION_SHIFT),
						baseZ + (index >> SECTION_SHIFT & SECTION_MASK)), palette.get(ids[index]));
				}
			}
		}
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
		checkDimensions(newDimensions);
		int shiftX = min.getX() - newMin.getX();
		int shiftY = min.getY() - newMin.getY();
		int shiftZ = min.getZ() - newMin.getZ();
		BlockPos oldPosition = position;
		setBox(newMin, newDimensions);
		shiftBlockPositions("TileEntities", shiftX, shiftY, shiftZ);
		shiftBlockPositions("PendingBlockTicks", shiftX, shiftY, shiftZ);
		shiftBlockPositions("PendingFluidTicks", shiftX, shiftY, shiftZ);
		shiftEntities(oldPosition.getX() - position.getX(), oldPosition.getY() - position.getY(),
			oldPosition.getZ() - position.getZ());
		return true;
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

	private void checkInside(BlockPos pos) {
		if (!contains(pos)) {
			throw new IllegalArgumentException(pos + " is outside region " + name + " (" + min + " to " + maxCorner() + ")");
		}
	}

	/**
	 * Calls {@code run} for each stretch of the box, in packed order, that lies along x within one section: its first
	 * position and its length.
	 */
	private void forEachRun(Run run) {
		int maxX = min.getX() + dimensions.getX() - 1;
		for (int y = min.getY(); y < min.getY() + dimensions.getY(); y++) {
			for (int z = min.getZ(); z < min.getZ() + dimensions.getZ(); z++) {
				for (int x = min.getX(); x <= maxX; ) {
					int length = Math.min(SECTION_SIDE - (x & SECTION_MASK), maxX - x + 1);
					run.accept(x, y, z, length);
					x += length;
				}
			}
		}
	}

	@FunctionalInterface
	private interface Run {
		void accept(int x, int y, int z, int length);
	}

	private long sectionKey(int x, int y, int z) {
		return ((long) ((x >> SECTION_SHIFT) - anchorX) & KEY_MASK) << (2 * KEY_BITS)
			| ((long) ((y >> SECTION_SHIFT) - anchorY) & KEY_MASK) << KEY_BITS
			| ((long) ((z >> SECTION_SHIFT) - anchorZ) & KEY_MASK);
	}

	/** One axis of a section key, sign-extended: the section's offset from the anchor along that axis. */
	private static int keyField(long key, int shift) {
		return (int) (key << (Long.SIZE - shift - KEY_BITS) >> (Long.SIZE - KEY_BITS));
	}

	private static int localIndex(int x, int y, int z) {
		return ((y & SECTION_MASK) * SECTION_SIDE + (z & SECTION_MASK)) * SECTION_SIDE + (x & SECTION_MASK);
	}

	/** Each side from 1 to {@link #MAX_SIDE}, and a volume an int can count. */
	private static void checkDimensions(Vec3i dimensions) {
		long volume = (long) dimensions.getX() * dimensions.getY() * dimensions.getZ();
		if (dimensions.getX() <= 0 || dimensions.getY() <= 0 || dimensions.getZ() <= 0 || dimensions.getX() > MAX_SIDE
			|| dimensions.getY() > MAX_SIDE || dimensions.getZ() > MAX_SIDE || volume > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Region dimensions out of range: " + dimensions);
		}
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

	/** 16×16×16 palette indices, y slowest, then z, then x, and how many aren't 0. */
	private static final class Section {
		final char[] ids = new char[SECTION_VOLUME];
		int nonZero;

		int get(int index) {
			return ids[index];
		}

		/** Sets one index and returns the one it replaced. */
		int set(int index, int id) {
			int previous = ids[index];
			ids[index] = (char) id;
			nonZero += (previous == 0 ? 0 : -1) + (id == 0 ? 0 : 1);
			return previous;
		}
	}
}
