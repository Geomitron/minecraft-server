package dev.buildingplanner.mixin.syncmatica;

import ch.endte.syncmatica.litematica.LitematicManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.FileType;
import java.nio.file.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hook S2: each plan Syncmatica loads gets its own schematic. Litematica caches schematics by file, and Syncmatica
 * names files by content, so two plans from one file would otherwise share their blocks.
 */
@Mixin(value = LitematicManager.class, remap = false)
abstract class LitematicManagerMixin {
	@WrapOperation(
		method = "renderSyncmatic(Lch/endte/syncmatica/data/ServerPlacement;)V",
		at = @At(value = "INVOKE",
			target = "Lfi/dy/masa/litematica/data/SchematicHolder;getOrLoad(Ljava/nio/file/Path;)Lfi/dy/masa/litematica/schematic/LitematicaSchematic;"))
	private LitematicaSchematic buildingplanner$ownSchematic(SchematicHolder holder, Path file,
		Operation<LitematicaSchematic> original) {
		LitematicaSchematic cached = original.call(holder, file);
		if (cached == null || DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().stream()
			.noneMatch(placement -> placement.getSchematic() == cached)) {
			return cached;
		}
		LitematicaSchematic own = LitematicaSchematic.createFromFile(file.getParent(), file.getFileName().toString(),
			FileType.fromFile(file));
		if (own == null) {
			return cached;
		}
		holder.addSchematic(own, true);
		return own;
	}
}
