package dev.buildingplanner.layer;

import java.util.List;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A section as files and messages carry it: its height ({@code y}, the block's y divided by 16, rounded down), its
 * palette, and its packed blocks ({@link LayerSection#pack}).
 */
public record PackedSection(int y, List<BlockState> palette, long[] blocks) {
}
