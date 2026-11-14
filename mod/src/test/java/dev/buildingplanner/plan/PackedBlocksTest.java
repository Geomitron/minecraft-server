package dev.buildingplanner.plan;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import fi.dy.masa.litematica.schematic.container.LitematicaBitArray;
import java.util.Random;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PackedBlocksTest {
	@ParameterizedTest
	@ValueSource(ints = {2, 3, 4, 5, 7, 12, 17})
	void packsAsLitematicaDoes(int bits) {
		Random random = new Random(bits);
		int count = 1000 + bits;
		int[] values = new int[count];
		LitematicaBitArray litematica = new LitematicaBitArray(bits, count);
		for (int i = 0; i < count; i++) {
			values[i] = random.nextInt(1 << bits);
			litematica.setAt(i, values[i]);
		}
		long[] packed = PackedBlocks.pack(values, bits);
		assertArrayEquals(litematica.getBackingLongArray(), packed);
		assertArrayEquals(values, PackedBlocks.unpack(packed, bits, count));
	}

}
