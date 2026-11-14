package dev.buildingplanner.plan;

/**
 * Litematica's block packing: each block's palette index takes {@code bits} bits, entries run on across long
 * boundaries, and the first entry sits in the lowest bits of the first long. {@link Writer} and {@link Reader} go
 * through the entries in order, so a region packs and unpacks without holding one int per block.
 */
final class PackedBlocks {
	private PackedBlocks() {
	}

	/** Bits per block for a palette of this size, as Litematica works them out when it reads a file. */
	static int bitsFor(int paletteSize) {
		return Math.max(2, Integer.SIZE - Integer.numberOfLeadingZeros(paletteSize - 1));
	}

	static int longsFor(long count, int bits) {
		long longs = (count * bits + 63) / 64;
		if (longs > Integer.MAX_VALUE - 8) {
			throw new IllegalArgumentException(count + " blocks at " + bits + " bits don't fit one array");
		}
		return (int) longs;
	}

	/** Writes entries in order into a new array of the right length. */
	static final class Writer {
		private final long[] longs;
		private final int bits;
		private final long mask;
		private long position;

		Writer(long count, int bits) {
			this.longs = new long[longsFor(count, bits)];
			this.bits = bits;
			this.mask = (1L << bits) - 1;
		}

		void write(int value) {
			int first = (int) (position >>> 6);
			int offset = (int) (position & 63);
			long masked = value & mask;
			longs[first] |= masked << offset;
			if (offset + bits > 64) {
				longs[first + 1] |= masked >>> (64 - offset);
			}
			position += bits;
		}

		/** Writes the same value {@code count} times; a run of zeros only moves on. */
		void repeat(int value, int count) {
			if (value == 0) {
				position += (long) count * bits;
				return;
			}
			for (int i = 0; i < count; i++) {
				write(value);
			}
		}

		long[] longs() {
			return longs;
		}
	}

	/** Reads entries in order from an array holding at least {@code count} of them. */
	static final class Reader {
		private final long[] longs;
		private final int bits;
		private final long mask;
		private long position;

		Reader(long[] longs, int bits, long count) {
			long needed = (count * bits + 63) / 64;
			if (longs.length < needed) {
				throw new IllegalArgumentException("Block array holds " + longs.length + " longs; " + count
					+ " blocks at " + bits + " bits need " + needed);
			}
			this.longs = longs;
			this.bits = bits;
			this.mask = (1L << bits) - 1;
		}

		int read() {
			int first = (int) (position >>> 6);
			int offset = (int) (position & 63);
			long value = longs[first] >>> offset;
			if (offset + bits > 64) {
				value |= longs[first + 1] << (64 - offset);
			}
			position += bits;
			return (int) (value & mask);
		}
	}
}
