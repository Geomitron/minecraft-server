package dev.buildingplanner.plan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Reads and writes plans in Litematica's {@code .litematic} format, version 7: gzip-compressed NBT with metadata and,
 * per region, a block-state palette (by name and properties) and the packed palette indices.
 */
public final class LitematicFormat {
	/** The format version Litematica 0.29 writes. */
	public static final int VERSION = 7;
	public static final int SUB_VERSION = 1;
	/** Versions 5 and up name block states as 1.13 and later do; older files use numeric ids. */
	private static final int OLDEST_READABLE_VERSION = 5;

	private static final String POSITION = "Position";
	private static final String SIZE = "Size";
	private static final String PALETTE = "BlockStatePalette";
	private static final String BLOCKS = "BlockStates";

	private LitematicFormat() {
	}

	public static Plan readFile(Path file) throws IOException {
		return read(NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()));
	}

	/** Writes through a temporary file beside the target and a rename, so a crash never leaves half a file. */
	public static void writeFile(Plan plan, Path file) throws IOException {
		writeFile(write(plan), file);
	}

	/** Writes a plan's tag from {@link #write}, as {@link #writeFile(Plan, Path)} does. */
	public static void writeFile(CompoundTag root, Path file) throws IOException {
		Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
		NbtIo.writeCompressed(root, temporary);
		Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}

	public static Plan read(CompoundTag root) {
		int version = root.getIntOr("Version", -1);
		if (version < OLDEST_READABLE_VERSION || version > VERSION) {
			throw new IllegalArgumentException("Unsupported .litematic version " + version);
		}
		int dataVersion = root.getIntOr("MinecraftDataVersion", currentDataVersion());
		CompoundTag regionsTag = root.getCompoundOrEmpty("Regions");
		Map<String, PlanRegion> regions = new LinkedHashMap<>();
		for (String name : regionsTag.keySet()) {
			regionsTag.getCompound(name).ifPresent(tag -> regions.put(name, readRegion(name, tag.copy(), dataVersion)));
		}
		return new Plan(root.getCompoundOrEmpty("Metadata").copy(), regions);
	}

	public static CompoundTag write(Plan plan) {
		CompoundTag root = new CompoundTag();
		root.putInt("MinecraftDataVersion", currentDataVersion());
		root.putInt("Version", VERSION);
		root.putInt("SubVersion", SUB_VERSION);
		root.put("Metadata", plan.metadata().copy());
		CompoundTag regions = new CompoundTag();
		for (PlanRegion region : plan.regions()) {
			regions.put(region.name(), writeRegion(region));
		}
		root.put("Regions", regions);
		return root;
	}

	private static PlanRegion readRegion(String name, CompoundTag tag, int dataVersion) {
		BlockPos position = readVector(tag.getCompoundOrEmpty(POSITION));
		Vec3i size = readVector(tag.getCompoundOrEmpty(SIZE));
		List<BlockState> palette = new ArrayList<>();
		for (Tag entry : tag.getListOrEmpty(PALETTE)) {
			CompoundTag state = entry instanceof CompoundTag compound ? compound : new CompoundTag();
			if (dataVersion < currentDataVersion()) {
				state = (CompoundTag) DataFixers.getDataFixer().update(References.BLOCK_STATE,
					new Dynamic<Tag>(NbtOps.INSTANCE, state), dataVersion, currentDataVersion()).getValue();
			}
			palette.add(LitematicBlockStates.read(state));
		}
		if (palette.isEmpty()) {
			palette.add(PlanRegion.AIR);
		}
		long[] packed = tag.getLongArray(BLOCKS).orElseGet(() -> new long[0]);
		for (String key : List.of(POSITION, SIZE, PALETTE, BLOCKS)) {
			tag.remove(key);
		}
		// Litematica shows an index past the palette as air.
		return PlanRegion.unpack(name, position, size, palette, packed, tag, false);
	}

	private static CompoundTag writeRegion(PlanRegion region) {
		CompoundTag tag = region.extras().copy();
		tag.put(POSITION, writeVector(region.position()));
		tag.put(SIZE, writeVector(region.size()));
		ListTag palette = new ListTag();
		for (BlockState state : region.palette()) {
			palette.add(LitematicBlockStates.write(state));
		}
		tag.put(PALETTE, palette);
		tag.put(BLOCKS, new LongArrayTag(region.packedBlocks()));
		return tag;
	}

	private static BlockPos readVector(CompoundTag tag) {
		return new BlockPos(tag.getIntOr("x", 0), tag.getIntOr("y", 0), tag.getIntOr("z", 0));
	}

	private static CompoundTag writeVector(Vec3i vector) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("x", vector.getX());
		tag.putInt("y", vector.getY());
		tag.putInt("z", vector.getZ());
		return tag;
	}

	private static int currentDataVersion() {
		return SharedConstants.getCurrentVersion().dataVersion().version();
	}
}
