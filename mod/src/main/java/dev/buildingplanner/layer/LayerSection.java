package dev.buildingplanner.layer;

import dev.buildingplanner.plan.PackedBlocks;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One 16-block cube of a column: each position a two-byte index into the section's own palette, index 0 for no ghost
 * block. A palette entry whose last block goes is freed and its index reused, so the palette holds at most the states
 * in use and never outgrows the two bytes. Positions run y slowest, then z, then x ({@link #index}).
 */
public final class LayerSection {
	public static final int SHIFT = 4;
	public static final int SIDE = 1 << SHIFT;
	public static final int VOLUME = SIDE * SIDE * SIDE;
	static final BlockState AIR = Blocks.AIR.defaultBlockState();
	private static final int MASK = SIDE - 1;

	private final char[] ids = new char[VOLUME];
	/** Index 0 is air; a freed entry holds null until it's reused. */
	private final List<BlockState> palette = new ArrayList<>(List.of(AIR));
	/** How many positions hold each palette entry; entry 0 isn't counted. */
	private final IntArrayList counts = new IntArrayList(new int[] {0});
	private final Object2IntOpenHashMap<BlockState> paletteIds = new Object2IntOpenHashMap<>();
	private final IntArrayList freeIds = new IntArrayList();
	private int blocks;

	LayerSection() {
		paletteIds.defaultReturnValue(-1);
	}

	/** The index of a position inside its section, from any world coordinates. */
	public static int index(int x, int y, int z) {
		return (y & MASK) << 2 * SHIFT | (z & MASK) << SHIFT | x & MASK;
	}

	/** The ghost block at an index, or air for none. */
	public BlockState get(int index) {
		return palette.get(ids[index]);
	}

	/** Sets the ghost block at an index (air, or any air state, for none) and returns the one it replaced. */
	BlockState set(int index, BlockState state) {
		int id = state.isAir() ? 0 : idFor(state);
		int previous = ids[index];
		if (previous == id) {
			return palette.get(previous);
		}
		BlockState replaced = palette.get(previous);
		ids[index] = (char) id;
		if (id != 0) {
			counts.set(id, counts.getInt(id) + 1);
			blocks++;
		}
		if (previous != 0) {
			blocks--;
			int left = counts.getInt(previous) - 1;
			counts.set(previous, left);
			if (left == 0) {
				paletteIds.removeInt(replaced);
				palette.set(previous, null);
				freeIds.add(previous);
			}
		}
		return replaced;
	}

	/** Positions holding a ghost block. */
	public int blocks() {
		return blocks;
	}

	public boolean isEmpty() {
		return blocks == 0;
	}

	/** Block states in use. */
	public int paletteSize() {
		return paletteIds.size();
	}

	/**
	 * The section packed for a file or a message: a palette of air then the states in use, and each position's index
	 * into it at {@link PackedBlocks#bitsFor} the palette's size, packed as Litematica packs a region.
	 */
	public PackedSection pack(int y) {
		List<BlockState> packedPalette = new ArrayList<>(paletteIds.size() + 1);
		packedPalette.add(AIR);
		int[] remap = new int[palette.size()];
		for (int id = 1; id < palette.size(); id++) {
			if (palette.get(id) != null) {
				remap[id] = packedPalette.size();
				packedPalette.add(palette.get(id));
			}
		}
		PackedBlocks.Writer writer = new PackedBlocks.Writer(VOLUME, PackedBlocks.bitsFor(packedPalette.size()));
		for (char id : ids) {
			writer.write(remap[id]);
		}
		return new PackedSection(y, packedPalette, writer.longs());
	}

	/**
	 * A section from its packed form. Any air state in the palette stands for no ghost block. A block array of another
	 * length, or an index past the palette, is an error.
	 */
	static LayerSection unpack(PackedSection packed) {
		List<BlockState> packedPalette = packed.palette();
		if (packedPalette.isEmpty()) {
			throw new IllegalArgumentException("Section " + packed.y() + " has an empty palette");
		}
		int bits = PackedBlocks.bitsFor(packedPalette.size());
		if (packed.blocks().length != PackedBlocks.longsFor(VOLUME, bits)) {
			throw new IllegalArgumentException("Section " + packed.y() + " holds " + packed.blocks().length
				+ " longs of blocks; a palette of " + packedPalette.size() + " needs " + PackedBlocks.longsFor(VOLUME, bits));
		}
		PackedBlocks.Reader reader = new PackedBlocks.Reader(packed.blocks(), bits, VOLUME);
		LayerSection section = new LayerSection();
		for (int index = 0; index < VOLUME; index++) {
			int id = reader.read();
			if (id >= packedPalette.size()) {
				throw new IllegalArgumentException("Section " + packed.y() + " has a block past its palette");
			}
			section.set(index, packedPalette.get(id));
		}
		return section;
	}

	private int idFor(BlockState state) {
		int id = paletteIds.getInt(state);
		if (id >= 0) {
			return id;
		}
		if (freeIds.isEmpty()) {
			id = palette.size();
			palette.add(state);
			counts.add(0);
		} else {
			id = freeIds.popInt();
			palette.set(id, state);
		}
		paletteIds.put(state, id);
		return id;
	}
}
