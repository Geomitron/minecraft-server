package dev.buildingplanner.client;

import dev.buildingplanner.layer.MaterialCount;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.util.BlockUtils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Litematica's material list for the blocks in a box the player drew: each block's total, ghost and real blocks alike,
 * and for the ghost blocks how many are still missing in the world and how many have the wrong block there. It counts
 * the player's copy of the layer, which holds the columns the game has loaded, against the player's world, by the rule
 * Litematica's own list uses, so the copy, which Litematica also draws, never counts. While the player's ghost blocks
 * are hidden, it lists the real blocks only. Litematica's Refresh button counts again.
 */
final class GhostMaterialList extends MaterialListBase {
	private final Identifier dimension;
	private final BlockPos min;
	private final BlockPos max;

	/** The list for the box from {@code min} to {@code max} in {@code dimension}, counted at once. */
	GhostMaterialList(Identifier dimension, BlockPos min, BlockPos max) {
		this.dimension = dimension;
		this.min = min.immutable();
		this.max = max.immutable();
		reCreateMaterialList();
	}

	@Override
	public String getName() {
		return getTitle();
	}

	@Override
	public String getTitle() {
		return Component.translatable("buildingplanner.ui.material_list.title").getString();
	}

	/** Counts again, while the player is still in the box's dimension with a session; otherwise keeps the counts. */
	@Override
	public void reCreateMaterialList() {
		ClientSession session = BuildingPlannerClient.session();
		Minecraft mc = Minecraft.getInstance();
		if (session == null || mc.level == null || !dimension.equals(session.replica().dimension())) {
			return;
		}
		MaterialCount count = MaterialCount.of(session.replica().layer(), min, max, BoxList.realSections(mc.level),
			GhostMaterialList::built, session.ghostsShown());
		setMaterialListEntries(MaterialListUtils.getMaterialList(count.total(), count.missing(), count.wrong(),
			new Object2IntOpenHashMap<>(), new Object2IntOpenHashMap<>(), new Object2IntOpenHashMap<>(), mc.player));
	}

	/**
	 * Whether the real block is the ghost block built, as Litematica's list decides (its options "materialListIgnoreState"
	 * and "ignoreCropAge").
	 */
	private static boolean built(BlockState ghost, BlockState real) {
		return real == ghost
			|| Configs.Generic.MATERIAL_LIST_IGNORE_STATE.getBooleanValue() && real.getBlock() == ghost.getBlock()
			|| Configs.Visuals.IGNORE_CROP_AGE.getBooleanValue() && BlockUtils.areStatesEqualIgnoringAge(ghost, real);
	}
}
