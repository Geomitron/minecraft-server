package dev.buildingplanner.layer;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMaps;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.state.BlockState;

/** One chunk column of the layer: its sections that hold ghost blocks, by height, lowest first. */
public final class LayerColumn {
	private final Int2ObjectRBTreeMap<LayerSection> sections = new Int2ObjectRBTreeMap<>();

	LayerColumn() {
	}

	public BlockState get(int x, int y, int z) {
		LayerSection section = sections.get(y >> LayerSection.SHIFT);
		return section == null ? LayerSection.AIR : section.get(LayerSection.index(x, y, z));
	}

	/** Sets a ghost block (air for none), freeing a section left empty, and returns the block it replaced. */
	BlockState set(int x, int y, int z, BlockState state) {
		int height = y >> LayerSection.SHIFT;
		LayerSection section = sections.get(height);
		if (section == null) {
			if (state.isAir()) {
				return LayerSection.AIR;
			}
			section = new LayerSection();
			sections.put(height, section);
		}
		BlockState replaced = section.set(LayerSection.index(x, y, z), state);
		if (section.isEmpty()) {
			sections.remove(height);
		}
		return replaced;
	}

	/** The sections holding ghost blocks, by height. */
	public Int2ObjectSortedMap<LayerSection> sections() {
		return Int2ObjectSortedMaps.unmodifiable(sections);
	}

	public boolean isEmpty() {
		return sections.isEmpty();
	}

	/** Every section packed, lowest first. */
	public List<PackedSection> pack() {
		List<PackedSection> packed = new ArrayList<>(sections.size());
		for (Int2ObjectMap.Entry<LayerSection> entry : sections.int2ObjectEntrySet()) {
			packed.add(entry.getValue().pack(entry.getIntKey()));
		}
		return packed;
	}

	/** A column from its packed sections; two sections at one height, or a damaged section, is an error. */
	static LayerColumn unpack(List<PackedSection> packed) {
		LayerColumn column = new LayerColumn();
		for (PackedSection section : packed) {
			if (column.sections.containsKey(section.y())) {
				throw new IllegalArgumentException("Two sections at height " + section.y());
			}
			LayerSection unpacked = LayerSection.unpack(section);
			if (!unpacked.isEmpty()) {
				column.sections.put(section.y(), unpacked);
			}
		}
		return column;
	}
}
