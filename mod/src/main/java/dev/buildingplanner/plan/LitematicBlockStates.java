package dev.buildingplanner.plan;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Block states as Litematica's palette stores them: {@code Name}, and {@code Properties} with every property's value
 * as a string when the block has properties. (Vanilla 26.3 spells these {@code id} and {@code properties}; the data
 * fixer's output is read in either spelling.)
 */
final class LitematicBlockStates {
	private LitematicBlockStates() {
	}

	static CompoundTag write(BlockState state) {
		CompoundTag tag = new CompoundTag();
		tag.putString("Name", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
		if (!state.getProperties().isEmpty()) {
			CompoundTag properties = new CompoundTag();
			for (Property<?> property : state.getProperties()) {
				properties.putString(property.getName(), valueName(state, property));
			}
			tag.put("Properties", properties);
		}
		return tag;
	}

	/** Reads a palette entry; an unknown block reads as air, and an unknown property or value keeps the default. */
	static BlockState read(CompoundTag tag) {
		String name = tag.getString("Name").or(() -> tag.getString("id")).orElse("");
		Identifier id = Identifier.tryParse(name);
		Block block = id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
		if (block == null) {
			return PlanRegion.AIR;
		}
		BlockState state = block.defaultBlockState();
		CompoundTag properties = tag.getCompound("Properties").or(() -> tag.getCompound("properties")).orElse(null);
		if (properties != null) {
			StateDefinition<Block, BlockState> definition = block.getStateDefinition();
			for (String key : properties.keySet()) {
				Property<?> property = definition.getProperty(key);
				if (property != null) {
					state = withValue(state, property, properties.getStringOr(key, ""));
				}
			}
		}
		return state;
	}

	private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
		return property.getName(state.getValue(property));
	}

	private static <T extends Comparable<T>> BlockState withValue(BlockState state, Property<T> property, String value) {
		return property.getValue(value).map(parsed -> state.setValue(property, parsed)).orElse(state);
	}
}
