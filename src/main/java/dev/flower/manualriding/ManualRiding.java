package dev.flower.manualriding;

import net.minecraftforge.fml.common.Mod;

/** 骑乘战斗附属入口。 / Mount combat addon entry point. */
@Mod(ManualRiding.ID)
public final class ManualRiding {
    public static final String ID = "manualriding";

    public ManualRiding() {
        RiderNetwork.init();
    }
}
