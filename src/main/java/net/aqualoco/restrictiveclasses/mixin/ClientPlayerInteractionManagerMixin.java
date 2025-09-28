package net.aqualoco.restrictiveclasses.mixin;

import net.aqualoco.restrictiveclasses.client.ClientRules;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Shadow private MinecraftClient client;

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void rc$guardUseItem(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        var res = ClientRules.get().canUseItem(player.getStackInHand(hand));
        if (res == ClientRules.Result.ALLOW) return;
        if (res == ClientRules.Result.PENDING) { ClientRules.get().showSyncingMessage(); cir.setReturnValue(ActionResult.FAIL); return; }
        if (res == ClientRules.Result.REQUIRE) ClientRules.get().showDeniedMessageForUseItem(player.getStackInHand(hand));
        else ClientRules.get().showDisabledMessage();
        cir.setReturnValue(ActionResult.FAIL);
    }

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void rc$guardUseBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (client.world == null) return;
        BlockState state = client.world.getBlockState(hitResult.getBlockPos());
        var res = ClientRules.get().canUseBlock(state);
        if (res == ClientRules.Result.ALLOW) return;
        if (res == ClientRules.Result.PENDING) { ClientRules.get().showSyncingMessage(); cir.setReturnValue(ActionResult.FAIL); return; }
        if (res == ClientRules.Result.REQUIRE) ClientRules.get().showDeniedMessageForUseBlock(state);
        else ClientRules.get().showDisabledMessage();
        cir.setReturnValue(ActionResult.FAIL);
    }

    @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
    private void rc$guardAttack(BlockPos pos, Direction dir, CallbackInfoReturnable<Boolean> cir) {
        if (client.world == null) return;
        BlockState state = client.world.getBlockState(pos);
        var res = ClientRules.get().canBreak(state);
        if (res == ClientRules.Result.ALLOW) return;
        if (res == ClientRules.Result.PENDING) { ClientRules.get().showSyncingMessage(); cir.setReturnValue(false); return; }
        if (res == ClientRules.Result.REQUIRE) ClientRules.get().showDeniedMessageForBreak(state);
        else ClientRules.get().showDisabledMessage();
        cir.setReturnValue(false);
    }
}
