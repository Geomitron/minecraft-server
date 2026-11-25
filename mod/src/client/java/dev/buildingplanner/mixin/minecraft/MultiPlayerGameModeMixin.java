package dev.buildingplanner.mixin.minecraft;

import dev.buildingplanner.client.BuildingPlannerClient;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hook M6: a right click on a block calls {@code useItemOn}, which guesses the block ({@code BlockItem.place}, hook S2)
 * and sends {@code ServerboundUseItemOnPacket}; sending the click's turn first (see "Turning real blocks in Disable",
 * and "Settings" for the ghost block's turn the click's hit finds) puts it before the click on the one connection, so
 * both sides place with it.
 */
@Mixin(MultiPlayerGameMode.class)
abstract class MultiPlayerGameModeMixin {
	@Inject(method = "useItemOn", at = @At("HEAD"))
	private void buildingplanner$sendTurn(LocalPlayer player, InteractionHand hand, BlockHitResult hit,
		CallbackInfoReturnable<InteractionResult> cir) {
		BuildingPlannerClient.beforeUseItemOn(hit);
	}
}
