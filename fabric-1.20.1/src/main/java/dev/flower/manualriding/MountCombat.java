package dev.flower.manualriding;

import static dev.flower.manualriding.Abilities.Skill;

import com.Polarice3.Goety.api.entities.IAutoRideable;
import com.Polarice3.Goety.api.entities.ally.IServant;
import com.Polarice3.Goety.common.entities.ally.golem.RedstoneMonstrosity;
import com.Polarice3.Goety.common.entities.ally.illager.raider.*;
import com.Polarice3.Goety.common.entities.neutral.AbstractBroodMother;
import com.Polarice3.Goety.utils.MobUtil;

import dev.flower.manualriding.mixin.RedstoneCooldown;
import dev.flower.manualriding.mixin.TramplerDash;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.phys.*;

import java.util.*;

/** 服务端验证和原生招式驱动。 / Server-authoritative native attack control. */
public final class MountCombat {
    private static final Map<UUID, Control> RIDERS = new HashMap<>();

    public static Mob mount(ServerPlayer player) {
        Entity e = player.getVehicle();
        return player.isAlive()
                        && Abilities.supports(e)
                        && e.isAlive()
                        && e.getFirstPassenger() == player
                        && e instanceof IServant servant
                        && servant.getTrueOwner() == player
                ? (Mob) e
                : null;
    }

    public static void tick(net.minecraft.server.MinecraftServer server) {
        if (server == null) return;
        var iterator = RIDERS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || mount(player) != entry.getValue().mob) {
                entry.getValue().close();
                iterator.remove();
            }
        }
        for (var p : server.getPlayerList().getPlayers()) {
            Mob m = mount(p);
            if (m == null) continue;
            Control c = RIDERS.computeIfAbsent(p.getUUID(), id -> new Control(m));
            c.tick();
            if (p.tickCount % 4 == 0) RiderNetwork.sync(p, c);
        }
    }

    public static void stopped() {
        RIDERS.values().forEach(Control::close);
        RIDERS.clear();
    }

    public static void input(ServerPlayer player, int slot) {
        Mob m = mount(player);
        if (m == null) return;
        Control c = RIDERS.computeIfAbsent(player.getUUID(), id -> new Control(m));
        if (c.mob != m) {
            c.close();
            c = new Control(m);
            RIDERS.put(player.getUUID(), c);
        }
        if (slot == -1) c.toggle();
        else c.cast(player, slot);
        RiderNetwork.sync(player, c);
    }

    public static Control control(ServerPlayer player) {
        return RIDERS.get(player.getUUID());
    }

    public static final class Control {
        public final Mob mob;
        public ManualSession manual;
        private boolean oldAutonomous;
        private boolean oldWandering, oldStaying;
        public int activeSlot = -1;
        private int lockTicks;
        public int accepted;

        Control(Mob mob) {
            this.mob = mob;
        }

        public boolean isManual() {
            return manual != null;
        }

        void toggle() {
            if (manual == null) {
                oldAutonomous = ((IAutoRideable) mob).isAutonomous();
                oldWandering = ((IServant) mob).isWandering();
                oldStaying = ((IServant) mob).isStaying();
                manual = new ManualSession(mob);
                resetAnimation();
                ((IAutoRideable) mob).setAutonomous(false);
                ((IServant) mob).setStaying(false);
            } else close();
        }

        void tick() {
            if (manual == null) return;
            // 原生 H 键不能在手动模式中重新接管移动。 / Manual steering stays under the rider.
            ((IAutoRideable) mob).setAutonomous(false);
            if (lockTicks > 0) --lockTicks;
            // 原生蛛网只在有目标时生成；空放补齐相同帧数、弹量与弹体参数。
            // Native web attacks skip null targets; fill only that empty-shot frame.
            if (mob instanceof AbstractBroodMother b
                    && activeSlot >= 0
                    && Abilities.of(mob)[activeSlot] == Skill.WEB
                    && b.getAttackTicks() == 7
                    && b.getTarget() == null) {
                Vec3 aim = b.getLookAngle().scale(16);
                double horizontal = Math.sqrt(aim.x * aim.x + aim.z * aim.z);
                for (int i = 0; i < 8; i++) {
                    var web =
                            new com.Polarice3.Goety.common.entities.projectiles.WebShot(
                                    b, b.level());
                    web.setPos(web.position().add(0, 1, 0));
                    web.setXRot(web.getXRot() + 20);
                    web.shoot(aim.x, aim.y + horizontal * .2, aim.z, .75f, 30);
                    b.level().addFreshEntity(web);
                }
                b.playSound(
                        com.Polarice3.Goety.init.ModSounds.SPIDER_SPIT,
                        1,
                        .4f / (b.getRandom().nextFloat() * .4f + .8f));
            }
            manual.tick();

            if (!manual.busy() && lockTicks == 0) {
                activeSlot = -1;
                mob.setTarget(null);
            }
        }

        public boolean busy() {
            if (manual == null) return false;
            if (manual.busy() || lockTicks > 0) return true;
            if (mob instanceof ModRavager r)
                return r.getAttackTick() > 0 || r.getRoarTick() > 0 || r.getStunnedTick() > 0;
            if (mob instanceof RedstoneMonstrosity r)
                return r.isActivating()
                        || r.isSummoning()
                        || r.isMeleeAttacking()
                        || r.isBelching();
            if (mob instanceof AbstractBroodMother b) return b.getAttackType() != 0;
            return false;
        }

        public int remaining(int slot) {
            Skill[] skills = Abilities.of(mob);
            if (slot < 0 || slot >= skills.length) return 0;
            Skill s = skills[slot];
            int own =
                    (int)
                            Math.max(
                                    0,
                                    ((cn.sh1rocu.goety.api.extension.IEntityPersistentData) mob).goety$getPersistentData().getLong("manualriding." + s.key)
                                            - mob.level().getGameTime());
            int nativeCool = 0;
            if (mob instanceof ModRavager r && s == Skill.ROAR) nativeCool = r.getRoarCool();
            if (mob instanceof AllyTrampler t && s == Skill.DASH)
                nativeCool = ((TramplerDash) t).manualriding$getCooldown();
            if (mob instanceof AbstractBroodMother b)
                nativeCool =
                        switch (s) {
                            case WEB -> b.getWebCooldown();
                            case LEAP -> b.getChargeCooldown();
                            case RETREAT -> b.getBackOffCooldown();
                            case EGGS -> b.getSummonCooldown();
                            default -> 0;
                        };
            if (mob instanceof RedstoneMonstrosity r)
                nativeCool =
                        switch (s) {
                            case BELCH -> r.belchCool;
                            case RED_SUMMON ->
                                    ((RedstoneCooldown) r).manualriding$getSummonCooldown();
                            default -> 0;
                        };

            return Math.max(own, nativeCool);
        }

        public boolean cast(ServerPlayer player, int slot) {
            Skill[] skills = Abilities.of(mob);
            if (manual == null || slot < 0 || slot >= skills.length) return false;
            if (busy() || remaining(slot) > 0) {
                if (mob instanceof RedstoneMonstrosity)
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    busy()
                                            ? "message.manualriding.busy"
                                            : "message.manualriding.cooldown"),
                            true);
                return false;
            }
            Skill skill = skills[slot];
            LivingEntity target =
                    target(player, skill == Skill.WEB ? 16 : skill == Skill.PROJECTILE ? 15 : 40);
            mob.setTarget(target);
            mob.setYRot(player.getYRot());
            mob.setYHeadRot(player.getYRot());
            mob.yBodyRot = player.getYRot();
            Goal selected = null;
            boolean ok = false;
            if (slot == 0
                    && !(mob instanceof RedstoneMonstrosity)
) {
                mob.swing(InteractionHand.MAIN_HAND);
                LivingEntity melee = meleeTarget(player);
                if (melee != null) mob.doHurtTarget(melee);
                cooldown(skill, 20);
                lockTicks = 10;
                ok = true;
            } else if (skill == Skill.ROAR && mob instanceof ModRavager r) {
                r.forceRoar();
                lockTicks = 20;
                ok = true;
            } else if (skill == Skill.DASH && mob instanceof AllyTrampler t && mob.onGround()) {
                ((TramplerDash) t).manualriding$jump(1.0f, Vec3.ZERO);
                t.hasImpulse = true;
                player.connection.send(new ClientboundSetEntityMotionPacket(t));
                cooldown(skill, 200);
                lockTicks = 20;
                ok = true;
            } else if (mob instanceof AbstractBroodMother b) {
                String name =
                        switch (skill) {
                            case WEB -> "WebGoal";
                            case LEAP -> "ChargeGoal";
                            case RETREAT -> "BackOffGoal";
                            case EGGS -> "SummonSpiders";
                            default -> "";
                        };
                if ((skill == Skill.LEAP || skill == Skill.RETREAT) && !mob.onGround()) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    "message.manualriding.charge_ground"),
                            true);
                    return false;
                }
                if (skill == Skill.EGGS
                        && mob.level()
                                        .getEntitiesOfClass(
                                                LivingEntity.class,
                                                mob.getBoundingBox().inflate(32, 16, 32),
                                                e ->
                                                        e.isAlive()
                                                                && e
                                                                        instanceof
                                                                        com.Polarice3.Goety.common
                                                                                                .entities
                                                                                                .ally
                                                                                                .spider
                                                                                                .SpiderServant
                                                                                        s
                                                                && s.getTrueOwner()
                                                                        instanceof
                                                                        AbstractBroodMother)
                                        .size()
                                >= 4) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    "message.manualriding.spider_cap"),
                            true);
                    return false;
                }
                selected = find(name);
            } else if (mob instanceof RedstoneMonstrosity r) {
                if (skill == Skill.SMASH) selected = find("MeleeGoal");
                if (skill == Skill.BELCH) {
                    selected = find("BelchGoal");
                    // 手动喷吐按朝向施放，不要求原版 AI 锁敌。 / Manual belch can fire without an AI target.
                }
                if (skill == Skill.RED_SUMMON) {
                    selected = find("SummonGoal");
                    // 原版保留的数量和落地条件。 / Keep the original summon cap and ground requirement.
                    if (!mob.onGround()) {
                        player.displayClientMessage(
                                net.minecraft.network.chat.Component.translatable(
                                        "message.manualriding.ground"),
                                true);
                        return false;
                    }
                    long cubes =
                            mob.level()
                                    .getEntitiesOfClass(
                                            com.Polarice3.Goety.common
                                                    .entities
                                                    .ally
                                                    .golem
                                                    .RedstoneCube
                                                    .class,
                                            mob.getBoundingBox().inflate(32))
                                    .size();
                    if (cubes >= 3) {
                        player.displayClientMessage(
                                net.minecraft.network.chat.Component.translatable(
                                        "message.manualriding.redstone_cap"),
                                true);
                        return false;
                    }
                }
            }
            if (selected != null) {
                if (mob instanceof RedstoneMonstrosity r && skill == Skill.RED_SUMMON)
                    manual.run(selected, r::isSummoning);
                else if (mob instanceof RedstoneMonstrosity r && skill == Skill.BELCH)
                    // 原版继续条件首帧也要求目标，改用原生动作与冷却判断。
                    // Keep the native attack ticking from its first frame, including empty shots.
                    manual.run(
                            selected,
                            () -> r.belchCool <= 0 && !r.isMeleeAttacking() && !r.isSummoning());
                else manual.run(selected, null);
                // 普通重击也受动作时间限制，其他技能沿用原生冷却。 / Native goals own their skill cooldowns.
                if (slot == 0) cooldown(skill, 20);
                ok = true;
            }
            if (ok) {
                activeSlot = slot;
                ++accepted;
            } else mob.setTarget(null);
            return ok;
        }

        private void cooldown(Skill skill, int ticks) {
            ((cn.sh1rocu.goety.api.extension.IEntityPersistentData) mob).goety$getPersistentData()
                    .putLong("manualriding." + skill.key, mob.level().getGameTime() + ticks);
        }

        private Goal find(String simpleName) {
            return manual.goals().stream()
                    .map(WrappedGoal::getGoal)
                    .filter(g -> g.getClass().getSimpleName().equals(simpleName))
                    .findFirst()
                    .orElse(null);
        }

        private boolean enemy(ServerPlayer p, LivingEntity e) {
            return e != mob
                    && e != p
                    && e.isAlive()
                    && !e.isSpectator()
                    && !mob.hasPassenger(e)
                    && !MobUtil.areAllies(mob, e)
                    && mob.hasLineOfSight(e)
                    && (!(e instanceof net.minecraft.world.entity.player.Player pl)
                            || p.canHarmPlayer(pl));
        }

        private LivingEntity target(ServerPlayer p, double range) {
            Vec3 eye = p.getEyePosition(), end = eye.add(p.getLookAngle().scale(range));
            return mob
                    .level()
                    .getEntitiesOfClass(
                            LivingEntity.class, new AABB(eye, end).inflate(2), e -> enemy(p, e))
                    .stream()
                    .filter(e -> e.getBoundingBox().inflate(.45).clip(eye, end).isPresent())
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(eye)))
                    .orElse(null);
        }

        private LivingEntity meleeTarget(ServerPlayer p) {
            Vec3 forward = Vec3.directionFromRotation(0, p.getYRot());
            double reach = Math.max(2.5, mob.getBbWidth() + 1.5);
            return mob
                    .level()
                    .getEntitiesOfClass(
                            LivingEntity.class,
                            mob.getBoundingBox().inflate(reach, 2, reach),
                            e -> enemy(p, e))
                    .stream()
                    .filter(
                            e -> {
                                Vec3 delta = e.position().subtract(mob.position());
                                return delta.horizontalDistanceSqr() < reach * reach
                                        && delta.normalize().dot(forward) > .2;
                            })
                    .min(Comparator.comparingDouble(mob::distanceToSqr))
                    .orElse(null);
        }

        private void resetAnimation() {
            if (mob instanceof AbstractBroodMother b) {
                b.setAttackType(0);
                b.setAttackTicks(0);
                b.setAnimationState(0);
            }
            if (mob instanceof RedstoneMonstrosity r) {
                if (r.isSummoning())
                    ((RedstoneCooldown) r)
                            .manualriding$setSummonCooldown(
                                    Math.max(
                                            ((RedstoneCooldown) r).manualriding$getSummonCooldown(),
                                            260));
                r.summonTick = 0;
                r.setMeleeAttacking(false);
                r.setBelching(false);
                r.setAnimationState(RedstoneMonstrosity.IDLE);
            }

        }

        public void close() {
            if (manual == null) return;
            manual.close();
            manual = null;
            resetAnimation();
            ((IAutoRideable) mob).setAutonomous(oldAutonomous);
            ((IServant) mob).setWandering(oldWandering);
            ((IServant) mob).setStaying(oldStaying);
            activeSlot = -1;
            lockTicks = 0;
        }
    }
}
