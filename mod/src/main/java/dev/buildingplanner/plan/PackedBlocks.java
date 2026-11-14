package dev.buildingplanner.plan;

/**
 * Litematica's block packing: each block's palette index takes {@code bits} bits, entries run on across long
 * boundaries, and the first entry sits in the lowest bits of the first long.
 */
final class PackedBlocks {
	private PackedBlocks() {
	}

	/** Bits per block for a palette of this size, as Litematica works them out when it reads a file. */
	static int bitsFor(int paletteSize) {
		return Math.max(2, Integer.SIZE - Integer.numberOfLeadingZeros(paletteSize - 1));
	}

	static long[] pack(int[] values, int bits) {
		long[] longs = new long[(int) ((values.length * (long) bits + 63) / 64)];
		long mask = (1L << bits) - 1;
		for (int i = 0; i < values.length; i++) {
			long start = i * (long) bits;
			int first = (int) (start >>> 6);
			int offset = (int) (start & 63);
			long value = values[i] & mask;
			longs[first] |= value << offset;
			if (offset + bits > 64) {
				longs[first + 1] |= value >>> (64 - offset);
			}
		}
		return longs;
	}

	static int[] unpack(long[] longs, int bits, int count) {
		long needed = (count * (long) bits + 63) / 64;
		if (longs.length < needed) {
			throw new IllegalArgumentException("Block array holds " + longs.length + " longs; " + count
				+ " blocks at " + bits + " bits need " + needed);
		}
		int[] values = new int[count];
		long mask = (1L << bits) - 1;
		for (int i = 0; i < count; i++) {
			long start = i * (long) bits;
			int first = (int) (start >>> 6);
			int offset = (int) (start & 63);
			long value = longs[first] >>> offset;
			if (offset + bits > 64) {
				value |= longs[first + 1] << (64 - offset);
			}
			values[i] = (int) (value & mask);
		}
		return values;
	}
}
