package dev.buildingplanner.mixin.effortless;

import dev.buildingplanner.client.BuildingPlannerClient;
import net.minecraft.client.KeyMapping;
import nl.requios.effortlessbuilding.utilities.KeyBindings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Hook E23: Effortless Building's four keys go under Building Planner's heading in Controls. Minecraft 26.3 names a
 * key's heading by the translation {@code key.category.<namespace>.<path>} of its category's id, which Effortless
 * Building's language file doesn't have (it names {@code key.categories.effortlessbuilding}), so its heading shows as
 * that raw name. Each of the four {@code KeyMapping} constructors in the class's static initializer is given
 * Building Planner's category instead.
 */
@Mixin(value = KeyBindings.class, remap = false)
abstract class KeyBindingsMixin {
	@ModifyArg(method = "<clinit>", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/KeyMapping;<init>(Ljava/lang/String;ILnet/minecraft/client/KeyMapping$Category;)V"),
		index = 2)
	private static KeyMapping.Category buildingplanner$category(KeyMapping.Category effortlessBuilding) {
		return BuildingPlannerClient.category();
	}
}
