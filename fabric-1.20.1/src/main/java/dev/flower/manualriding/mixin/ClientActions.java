package dev.flower.manualriding.mixin;

import dev.flower.manualriding.ClientControls;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 重绑后仍通过原版动作入口接管。 / Capture rebound actions at their native entry points. */
@Mixin(Minecraft.class)
public abstract class ClientActions {
    @Inject(method="startAttack",at=@At("HEAD"),cancellable=true)
    private void manualriding$attack(CallbackInfoReturnable<Boolean> callback) {
        if(ClientControls.interaction(0))callback.setReturnValue(false);
    }
    @Inject(method="startUseItem",at=@At("HEAD"),cancellable=true)
    private void manualriding$use(CallbackInfo callback) {
        if(ClientControls.interaction(1))callback.cancel();
    }
    @Inject(method="continueAttack",at=@At("HEAD"),cancellable=true)
    private void manualriding$hold(boolean held,CallbackInfo callback) {
        if(ClientControls.manual())callback.cancel();
    }
}
