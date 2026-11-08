package dev.flower.manualriding.mixin;

import com.Polarice3.Goety.common.entities.ally.illager.raider.AllyTrampler;

import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 调用原版冲刺，保留其碰撞伤害和冷却。 / Invoke Goety's original dash. */
@Mixin(value = AllyTrampler.class, remap = false)
public interface TramplerDash {
    @Invoker("executeRidersJump")
    void manualriding$jump(float strength, Vec3 input);

    @Accessor("dashCooldown")
    int manualriding$getCooldown();
}
