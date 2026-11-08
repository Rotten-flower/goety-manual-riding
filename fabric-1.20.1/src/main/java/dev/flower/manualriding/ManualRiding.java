package dev.flower.manualriding;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
/** 骑乘战斗附属入口。 / Mount combat addon entry point. */
public final class ManualRiding implements ModInitializer {
    public static final String ID = "manualriding";
    @Override public void onInitialize() {
        RiderNetwork.init();
        ServerTickEvents.END_SERVER_TICK.register(MountCombat::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> MountCombat.stopped());
    }
}
