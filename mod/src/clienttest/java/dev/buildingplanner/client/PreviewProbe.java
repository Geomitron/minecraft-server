package dev.buildingplanner.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What Effortless Building's preview drew in the last whole frame: the positions it drew as blocks to place or break,
 * as its {@code renderBoundingBoxFaces} gets them, for the driver's {@code preview}, and the state it drew each block
 * in, after the entry's mirror and turn, for its {@code previewstate}.
 */
public final class PreviewProbe {
	private static List<BlockPos> drawing = new ArrayList<>();
	private static List<BlockPos> drawn = List.of();
	private static Map<BlockPos, BlockState> statesDrawing = new HashMap<>();
	private static Map<BlockPos, BlockState> statesDrawn = Map.of();

	private PreviewProbe() {
	}

	/** A frame begins: what the one before drew is the last whole frame's. */
	public static void beforeFrame() {
		drawn = drawing;
		drawing = new ArrayList<>();
		statesDrawn = statesDrawing;
		statesDrawing = new HashMap<>();
	}

	/** The preview draws these positions in this frame. */
	public static void drawing(List<BlockPos> positions) {
		positions.forEach(pos -> drawing.add(pos.immutable()));
	}

	/** The preview draws the block at {@code pos} in this state in this frame. */
	public static void drawingState(BlockPos pos, BlockState state) {
		statesDrawing.put(pos.immutable(), state);
	}

	/** The positions the last whole frame's preview drew, as x,y,z in the order drawn, or none. */
	static String positions() {
		return drawn.isEmpty() ? "none" : String.join(" ",
			drawn.stream().map(pos -> pos.getX() + "," + pos.getY() + "," + pos.getZ()).toList());
	}

	/**
	 * The state the last whole frame's preview drew at {@code pos}, with its properties, such as
	 * minecraft:oak_stairs[facing=east,half=bottom,...], or none.
	 */
	static String state(BlockPos pos) {
		BlockState state = statesDrawn.get(pos);
		if (state == null) {
			return "none";
		}
		String properties = state.getProperties().stream()
			.map(property -> property.getName() + "=" + state.getValue(property)).sorted()
			.collect(Collectors.joining(","));
		return BuiltInRegistries.BLOCK.getKey(state.getBlock()) + "[" + properties + "]";
	}
}
