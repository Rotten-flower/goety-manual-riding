package dev.flower.manualriding;

import com.Polarice3.Goety.common.entities.ally.*;
import com.Polarice3.Goety.common.entities.ally.golem.RedstoneMonstrosity;
import com.Polarice3.Goety.common.entities.ally.illager.raider.*;
import com.Polarice3.Goety.common.entities.ally.spider.BroodMotherServant;
import com.Polarice3.Goety.common.entities.ally.undead.GraveMonstrosity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/** 槽位只列出该坐骑已有的主动招式。 / Slots reflect the mount's native attacks. */
public final class Abilities {
    public enum Skill {
        BITE("bite", 20),
        CLAW("claw", 20),
        TUSKS("tusks", 20),
        RAM("ram", 20),
        ROAR("roar", 100),
        DASH("dash", 200),
        WEB("web", 220),
        LEAP("leap", 130),
        RETREAT("retreat", 130),
        EGGS("eggs", 160),
        SMASH("smash", 76),
        BELCH("belch", 200),
        RED_SUMMON("red_summon", 260),
        COMBO("combo", 120),
        GRAVE_CHARGE("grave_charge", 120),
        PROJECTILE("projectile", 100),
        GRAVE_SUMMON("grave_summon", 400),
        CALL("call", 340);
        public final String key;
        public final int cooldown;

        Skill(String key, int cooldown) {
            this.key = key;
            this.cooldown = cooldown;
        }

        public String translation() {
            return "skill.manualriding." + key;
        }
    }

    private static final Skill[] BEAR = {Skill.CLAW},
            HOG = {Skill.TUSKS},
            SHARK = {Skill.BITE},
            RAVAGER = {Skill.RAM, Skill.ROAR},
            TRAMPLER = {Skill.RAM, Skill.DASH},
            SPIDER = {Skill.BITE, Skill.WEB, Skill.LEAP, Skill.EGGS, Skill.RETREAT},
            RED = {Skill.SMASH, Skill.BELCH, Skill.RED_SUMMON},
            GRAVE =
                    {
                        Skill.COMBO,
                        Skill.PROJECTILE,
                        Skill.GRAVE_CHARGE,
                        Skill.GRAVE_SUMMON,
                        Skill.CALL
                    },
            NONE = {};

    public static Skill[] of(Entity entity) {
        if (entity instanceof BearServant) return BEAR;
        if (entity instanceof HoglinServant) return HOG;
        if (entity instanceof Gnasher) return SHARK;
        if (entity instanceof ModRavager) return RAVAGER;
        if (entity instanceof AllyTrampler) return TRAMPLER;
        if (entity instanceof BroodMotherServant) return SPIDER;
        if (entity instanceof RedstoneMonstrosity) return RED;
        if (entity instanceof GraveMonstrosity) return GRAVE;
        return NONE;
    }

    public static boolean supports(Entity entity) {
        return entity instanceof Mob && of(entity).length > 0;
    }
}
