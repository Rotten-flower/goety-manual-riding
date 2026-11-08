package dev.flower.manualriding;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.client.DeltaTracker;

import java.util.*;

/** 左下角技能栏；图标和键位资源仅预载，不逐帧解码。 / Cached lower-left mount HUD. */
public final class MountHud {
    private static final InputIcons INPUTS = new InputIcons();
    private static final Component AUTO = Component.translatable("hud.manualriding.auto"),
            MANUAL = Component.translatable("hud.manualriding.manual"),
            SWITCH = Component.translatable("hud.manualriding.switch");
    private static int profile = -1;
    private static Component[] names = {};

    public static void render(GuiGraphics g, DeltaTracker delta) {
        var mc = Minecraft.getInstance();
        int width = g.guiWidth(), height = g.guiHeight();
        if (mc.options.hideGui
                || mc.player == null
                || !Abilities.supports(mc.player.getVehicle())
                || mc.screen != null) {
            INPUTS.reset();
            return;
        }
        var mount = mc.player.getVehicle();
        var skills = Abilities.of(mount);
        if (profile != mount.getId()) {
            profile = mount.getId();
            names = new Component[skills.length];
            for (int i = 0; i < skills.length; i++)
                names[i] = Component.translatable(skills[i].translation());
        }
        long now = System.nanoTime();
        INPUTS.frame(now);
        boolean manual =
                ClientControls.status != null
                        && ClientControls.status.mountId() == mount.getId()
                        && ClientControls.status.manual();
        int rows = skills.length + 1;
        int top = height - 30 - rows * 22;
        g.pose().pushPose();
        g.pose().translate(10, top, 0);
        for (int i = 0; i < skills.length; i++) {
            int y = i * 22;
            int textX = (int) Math.ceil(INPUTS.draw(g, ClientControls.binding(i), 0, y, 18)) + 5;
            int color = manual ? 0xFFECEFE8 : 0xFFB6BDB6;
            if (ClientControls.usedSlot == i && now - ClientControls.usedAt < 200_000_000L)
                color = 0xFFF5C94D;
            g.drawString(mc.font, names[i], textX, y + 5, color, true);
            int cooldown = 0;
            if (ClientControls.status != null
                    && ClientControls.status.mountId() == mount.getId()
                    && i < ClientControls.status.remaining().length)
                cooldown =
                        Math.max(
                                0,
                                ClientControls.status.remaining()[i]
                                        - (int) ((now - ClientControls.receivedAt) / 50_000_000L));
            if (cooldown > 0) {
                String seconds = String.format(Locale.ROOT, "%.1fs", cooldown / 20f);
                g.drawString(
                        mc.font,
                        seconds,
                        textX + 6 + mc.font.width(names[i]),
                        y + 5,
                        0xFFB6BDB6,
                        true);
            }
        }
        int switchX =
                (int) Math.ceil(INPUTS.draw(g, ClientControls.MODE, 0, skills.length * 22, 18)) + 5;
        g.drawString(mc.font, SWITCH, switchX, skills.length * 22 + 5, 0xFFB6BDB6, true);
        // 模式提示仅在切换时出现，黑框跟随文字实际宽度。
        // Show a content-sized mode toast only after a mode switch.
        if (ClientControls.modeChangedAt != 0) {
            float elapsed = (now - ClientControls.modeChangedAt) / 1_000_000_000f;
            if (elapsed < 1.65f) {
                float enter = Math.min(1, elapsed / .18f);
                float leave = Math.min(1, (1.65f - elapsed) / .25f);
                float opacity = Math.max(0, Math.min(enter, leave));
                float smooth = enter * enter * (3 - 2 * enter);
                Component mode = manual ? MANUAL : AUTO;
                int w = mc.font.width(mode) + 16;
                int y = -26 + (int) ((1 - smooth) * 5);
                panel(g, 0, y, w, 20, ((int) (220 * opacity) << 24) | 0x101318);
                drawMode(g, mode, 8, y + 6, opacity, manual ? 0xF5C94D : 0xECEFE8);
            }
        }
        g.pose().popPose();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private static void panel(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x + 2, y, x + w - 2, y + h, color);
        g.fill(x, y + 2, x + w, y + h - 2, color);
    }

    private static void drawMode(
            GuiGraphics g, Component text, int x, int y, float opacity, int rgb) {
        int alpha = (int) (255 * opacity);
        if (alpha < 5) return;
        g.drawString(Minecraft.getInstance().font, text, x, y, (alpha << 24) | rgb, false);
    }
}
