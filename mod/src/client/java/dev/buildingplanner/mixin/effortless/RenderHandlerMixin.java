package dev.buildingplanner.mixin.effortless;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.buildingplanner.client.BuildingPlannerClient;
import dev.buildingplanner.client.EffortlessLine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import nl.requios.effortlessbuilding.render.RenderHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hook E31: during a session, Effortless Building's own line during a shape ("Left-click to cancel, Right-click to
 * place") is drawn in Building Planner's rectangles, where Effortless Building draws it ({@link EffortlessLine}). The
 * line only looks different, so the hook requires no match: an Effortless Building that draws it another way keeps
 * its own line, and Building Planner loads and works as before.
 *
 * <p>Hook E44: during a session, Effortless Building's icons beside the crosshair, which count the items a shape's
 * place click uses, don't show while the click's panel shows the same counts ({@link BuildingPlannerClient#stacksShow}).
 * The icons only show or not, so this hook requires no match either.
 */
@Mixin(value = RenderHandler.class, remap = false)
abstract class RenderHandlerMixin {
	@WrapOperation(method = "renderSubtitle", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"),
		require = 0)
	private static void buildingplanner$inRectangles(GuiGraphicsExtractor graphics, Font font, Component text, int x,
		int y, int colour, boolean shadow, Operation<Void> original) {
		if (!BuildingPlannerClient.active() || !EffortlessLine.draw(graphics, font, text)) {
			original.call(graphics, font, text, x, y, colour, shadow);
		}
	}

	@WrapWithCondition(method = "drawStacks", at = @At(value = "INVOKE",
		target = "Lnl/requios/effortlessbuilding/render/RenderHandler;drawPlacingStacks(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/Minecraft;II)V"),
		require = 0)
	private static boolean buildingplanner$stacksShow(GuiGraphicsExtractor graphics, Minecraft mc, int x, int y) {
		return !BuildingPlannerClient.active() || BuildingPlannerClient.stacksShow();
	}
}
