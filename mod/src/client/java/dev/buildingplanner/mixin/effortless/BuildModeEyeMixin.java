package dev.buildingplanner.mixin.effortless;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.buildingplanner.client.Aim;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import nl.requios.effortlessbuilding.buildmode.ThreeClicksBuildMode;
import nl.requios.effortlessbuilding.buildmode.buildmodes.Floor;
import nl.requios.effortlessbuilding.buildmode.buildmodes.Line;
import nl.requios.effortlessbuilding.buildmode.buildmodes.Wall;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hook E26: the lines of sight of the modes' later clicks start at the camera while Freecam's is on ({@link Aim}).
 * Each method builds the eye position from the player's position and eye height, its only {@code new Vec3}.
 */
@Mixin(value = {Floor.class, Line.class, Wall.class, ThreeClicksBuildMode.class}, remap = false)
abstract class BuildModeEyeMixin {
	@ModifyExpressionValue(method = {"findFloor", "findLine", "findWall", "findHeight"},
		at = @At(value = "NEW", target = "net/minecraft/world/phys/Vec3"))
	private static Vec3 buildingplanner$eyeAtAim(Vec3 eye, @Local(argsOnly = true) Player player) {
		Entity aim = Aim.forEffortless(player);
		return aim == player ? eye : aim.getEyePosition();
	}
}
