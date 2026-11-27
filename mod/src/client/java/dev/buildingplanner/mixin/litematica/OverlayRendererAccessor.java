package dev.buildingplanner.mixin.litematica;

import fi.dy.masa.litematica.render.OverlayRenderer;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Hook L13: Litematica's lines about the schematic block at the crosshair ("block info lines") and when it last wrote
 * them, which it does at most every 50 ms while it draws them, so the click's panel keeps out of them.
 */
@Mixin(value = OverlayRenderer.class, remap = false)
public interface OverlayRendererAccessor {
	@Accessor("blockInfoLines")
	List<String> buildingplanner$blockInfoLines();

	@Accessor("infoUpdateTime")
	long buildingplanner$infoUpdateTime();
}
