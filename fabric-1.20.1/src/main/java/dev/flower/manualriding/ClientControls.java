package dev.flower.manualriding;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.*;

import org.lwjgl.glfw.GLFW;

/** 仅手动骑乘时接管输入，菜单内不施法。 / Capture combat input only during manual riding. */
public final class ClientControls {
    public static final KeyMapping MODE =
            new KeyMapping(
                    "key.manualriding.mode",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_LEFT_ALT,
                    "key.categories.manualriding");
    public static final KeyMapping[] EXTRA = {
        new KeyMapping(
                "key.manualriding.skill2",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "key.categories.manualriding"),
        new KeyMapping(
                "key.manualriding.skill3",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F,
                "key.categories.manualriding"),
        new KeyMapping(
                "key.manualriding.skill4",
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
            if (other != binding && other.same(binding)) {
                other.setDown(false);
                while (other.consumeClick()) {}
            }
        }
        while (binding.consumeClick()) {}
    }

    public static void key(int key, int scanCode, int action) {
        if (!mounted() || action != GLFW.GLFW_PRESS) return;
        if (MODE.matches(key, scanCode)) {
            press(MODE, -1);
            return;
        }
        if (!manual()) return;
        for (int i = 0; i < EXTRA.length; i++)
            if (i + 2 < Abilities.of(Minecraft.getInstance().player.getVehicle()).length
                    && EXTRA[i].matches(key, scanCode)) {
                press(EXTRA[i], i + 2);
                return;
            }
    }

    public static boolean mouse(int button, int action) {
        if (!mounted()) return false;
        if (MODE.matchesMouse(button)) {
            if (action == GLFW.GLFW_PRESS) press(MODE, -1);
            return true;
        }
        if (!manual()) return false;
        for (int i=0; i<EXTRA.length; i++)
            if (i+2<Abilities.of(Minecraft.getInstance().player.getVehicle()).length && EXTRA[i].matchesMouse(button)) {
                if (action==GLFW.GLFW_PRESS) press(EXTRA[i],i+2);
                return true;
            }
        return false;
    }

    /** 所有攻击/使用绑定均走原版入口。 / Intercept the action after native key rebinding. */
    public static boolean interaction(int slot) {
        if (!manual()) return false;
        if (slot < Abilities.of(Minecraft.getInstance().player.getVehicle()).length) RiderNetwork.send(slot);
        return true;
    }

    public static void tick(Minecraft mc) {
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
                        if (other != own && other.same(own)) {
                            other.setDown(false);
                            while (other.consumeClick()) {}
                        }
        }
    }

    public static void init() {
        net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(MODE);
        for (var key : EXTRA) net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(key);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(ClientControls::tick);
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(MountHud::render);
    }
}
