package dev.flower.manualriding.mixin;

import com.Polarice3.Goety.common.entities.ally.golem.RedstoneMonstrosity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 读取并保留原生召唤冷却。 / Preserve native summon cooldown. */
@Mixin(value = RedstoneMonstrosity.class, remap = false)
public interface RedstoneCooldown {
    @Accessor("summonCool")
    int manualriding$getSummonCooldown();

    @Accessor("summonCool")
    void manualriding$setSummonCooldown(int ticks);
}
