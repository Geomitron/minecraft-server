package dev.buildingplanner.layer;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.Strategy;

/** Real blocks for the tests, in the game's own chunk sections. */
public final class TestSections {
	private TestSections() {
	}

	/** A world holding {@code blocks}, by position, and air elsewhere, with no section where it holds none. */
	public static BoxCount.Sections of(Map<BlockPos, BlockState> blocks) {
		Map<BlockPos, LevelChunkSection> sections = new HashMap<>();
		blocks.forEach((pos, state) -> sections.computeIfAbsent(
			new BlockPos(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4), key -> empty())
			.setBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, state));
		return (x, y, z) -> sections.get(new BlockPos(x, y, z));
	}

	/** A section of air, as the game makes for a chunk; the tests never read its biomes. */
	public static LevelChunkSection empty() {
		return new LevelChunkSection(new PalettedContainer<>(Blocks.AIR.defaultBlockState(),
			Strategy.createForBlockStates(Block.BLOCK_STATE_REGISTRY)), null);
	}
}
