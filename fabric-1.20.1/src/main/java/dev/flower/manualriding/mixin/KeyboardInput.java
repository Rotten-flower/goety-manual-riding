package dev.flower.manualriding.mixin;
import dev.flower.manualriding.ClientControls;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** 原版更新绑定后清理同键操作。 / Clear conflicting bindings after native input handling. */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardInput {
    @Inject(method="keyPress",at=@At("TAIL"))
    private void manualriding$key(long window,int key,int scan,int action,int mods,CallbackInfo callback) {
        ClientControls.key(key,scan,action);
    }
}
