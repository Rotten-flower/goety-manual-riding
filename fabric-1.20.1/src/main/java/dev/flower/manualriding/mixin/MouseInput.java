package dev.flower.manualriding.mixin;
import dev.flower.manualriding.ClientControls;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** 鼠标重绑与键盘使用同一槽位。 / Mouse rebinding uses the same skill slots. */
@Mixin(MouseHandler.class)
public abstract class MouseInput {
    @Inject(method="onPress",at=@At("HEAD"),cancellable=true)
    private void manualriding$mouse(long window,int button,int action,int mods,CallbackInfo callback) {
        if(ClientControls.mouse(button,action))callback.cancel();
    }
}
