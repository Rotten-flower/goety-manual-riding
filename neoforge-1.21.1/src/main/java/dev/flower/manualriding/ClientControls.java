package dev.flower.manualriding;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import org.lwjgl.glfw.GLFW;

/** 仅手动骑乘时接管输入，菜单内不施法。 / Capture combat input only during manual riding. */
@EventBusSubscriber(modid = ManualRiding.ID, value = Dist.CLIENT)
public final class ClientControls {
    public static final KeyMapping MODE =
            new KeyMapping(
                    "key.manualriding.mode",
                    KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_LEFT_ALT,
                    "key.categories.manualriding");
    public static final KeyMapping[] EXTRA = {
        new KeyMapping(
                "key.manualriding.skill2",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "key.categories.manualriding"),
        new KeyMapping(
                "key.manualriding.skill3",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F,
                "key.categories.manualriding"),
        new KeyMapping(
                "key.manualriding.skill4",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "key.categories.manualriding")
    };
    public static RiderNetwork.Status status;
    public static long receivedAt, modeChangedAt, usedAt;
    private static int mount = -1, accepted;
    public static int usedSlot = -1;

    public static void receive(RiderNetwork.Status value) {
        var mc = Minecraft.getInstance();
        if (mc.player == null
                || mc.player.getVehicle() == null
                || mc.player.getVehicle().getId() != value.mountId()) return;
        if (status != null && status.manual() != value.manual()) modeChangedAt = System.nanoTime();
        if (value.accepted() != accepted) {
            usedAt = System.nanoTime();
            usedSlot = value.activeSlot();
            accepted = value.accepted();
        }
        status = value;
        receivedAt = System.nanoTime();
    }

    public static boolean mounted() {
        var mc = Minecraft.getInstance();
        return mc.player != null && mc.screen == null && Abilities.supports(mc.player.getVehicle());
    }

    public static boolean manual() {
        var mc = Minecraft.getInstance();
        return mounted()
                && status != null
                && status.manual()
                && status.mountId() == mc.player.getVehicle().getId();
    }

    public static KeyMapping binding(int slot) {
        var mc = Minecraft.getInstance();
        return slot == 0 ? mc.options.keyAttack : slot == 1 ? mc.options.keyUse : EXTRA[slot - 2];
    }

    private static void press(KeyMapping binding, int slot) {
        RiderNetwork.send(slot);
        // 同一物理按键只执行本次骑乘操作。 / Suppress conflicting native actions for this press.
        for (KeyMapping other : Minecraft.getInstance().options.keyMappings) {
            if (other != binding && other.getKey().equals(binding.getKey())) {
                other.setDown(false);
                while (other.consumeClick()) {}
            }
        }
        while (binding.consumeClick()) {}
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void key(InputEvent.Key event) {
        if (!mounted() || event.getAction() != GLFW.GLFW_PRESS) return;
        if (MODE.matches(event.getKey(), event.getScanCode())) {
            press(MODE, -1);
            return;
        }
        if (!manual()) return;
        for (int i = 0; i < EXTRA.length; i++)
            if (i + 2 < Abilities.of(Minecraft.getInstance().player.getVehicle()).length
                    && EXTRA[i].matches(event.getKey(), event.getScanCode())) {
                press(EXTRA[i], i + 2);
                return;
            }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void mouse(InputEvent.MouseButton.Pre event) {
        if (!mounted()) return;
        if (MODE.matchesMouse(event.getButton())) {
            event.setCanceled(true);
            if (event.getAction() == GLFW.GLFW_PRESS) press(MODE, -1);
            return;
        }
        if (!manual()) return;
        for (int i = 0; i < EXTRA.length; i++)
            if (i + 2 < Abilities.of(Minecraft.getInstance().player.getVehicle()).length
                    && EXTRA[i].matchesMouse(event.getButton())) {
                event.setCanceled(true);
                if (event.getAction() == GLFW.GLFW_PRESS) press(EXTRA[i], i + 2);
                return;
            }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!manual() || (!event.isAttack() && !event.isUseItem())) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        int slot = event.isAttack() ? 0 : 1;
        if (slot < Abilities.of(Minecraft.getInstance().player.getVehicle()).length)
            RiderNetwork.send(slot);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        int now =
                mc.player != null && Abilities.supports(mc.player.getVehicle())
                        ? mc.player.getVehicle().getId()
                        : -1;
        if (mount != now) {
            mount = now;
            status = null;
            accepted = 0;
            usedSlot = -1;
            modeChangedAt = 0;
        }
        if (manual()) {
            // 清理原生魔杖/咆哮等同键操作，避免在客户端 tick 再次消费。
            // Prevent native Goety bindings from consuming the same manual input later.
            for (var own : EXTRA)
                if (own.isDown())
                    for (var other : mc.options.keyMappings)
                        if (other != own && other.getKey().equals(own.getKey())) {
                            other.setDown(false);
                            while (other.consumeClick()) {}
                        }
        }
    }

    @EventBusSubscriber(
            modid = ManualRiding.ID,
            bus = EventBusSubscriber.Bus.MOD,
            value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent event) {
            RiderNetwork.clientReceiver = ClientControls::receive;
            event.register(MODE);
            for (var key : EXTRA) event.register(key);
        }

        @SubscribeEvent
        public static void overlay(RegisterGuiLayersEvent event) {
            event.registerAboveAll(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ManualRiding.ID, "mount_skills"), MountHud::render);
        }
    }
}
