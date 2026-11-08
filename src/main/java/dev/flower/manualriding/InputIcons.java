package dev.flower.manualriding;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import org.lwjgl.glfw.GLFW;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** 从共享姿态图集绘制物理按键及回弹。 / Draw physical input icons with rebound animation. */
final class InputIcons {
    private static final ResourceLocation POSES =
            new ResourceLocation("manualriding", "textures/gui/input/poses.png");
    private static final List<Icon> ICONS = new ArrayList<>();
    private static final int ATLAS_WIDTH;
    private static final int ATLAS_HEIGHT;

    static {
        try (var stream =
                InputIcons.class.getResourceAsStream(
                        "/assets/manualriding/textures/gui/input/layout.json")) {
            if (stream == null) throw new IllegalStateException("Missing input icon layout");
            var data =
                    JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                            .getAsJsonObject();
            ATLAS_WIDTH = data.get("width").getAsInt();
            ATLAS_HEIGHT = data.get("height").getAsInt();
            for (var value : data.getAsJsonArray("entries")) {
                var entry = value.getAsJsonObject();
                ICONS.add(
                        new Icon(
                                entry.get("index").getAsInt(),
                                entry.get("width").getAsInt(),
                                entry.get("key").getAsInt(),
                                entry.get("button").getAsInt()));
            }
        } catch (Exception error) {
            throw new IllegalStateException("Cannot load input icon layout", error);
        }
    }

    private final float[] amount = new float[ICONS.size()], origin = new float[ICONS.size()];
    private final long[] changed = new long[ICONS.size()];
    private final boolean[] down = new boolean[ICONS.size()];

    void frame(long now) {
        var mc = Minecraft.getInstance();
        boolean focused = mc.isWindowActive() && mc.player != null;
        long window = mc.getWindow().getWindow();
        for (int i = 0; i < ICONS.size(); i++) {
            Icon icon = ICONS.get(i);
            boolean pressed =
                    focused
                            && (icon.button >= 0
                                    ? GLFW.glfwGetMouseButton(window, icon.button)
                                            == GLFW.GLFW_PRESS
                                    : GLFW.glfwGetKey(window, icon.key) == GLFW.GLFW_PRESS
                                            || icon.key >= 340
                                                    && icon.key <= 342
                                                    && GLFW.glfwGetKey(window, icon.key + 4)
                                                            == GLFW.GLFW_PRESS);
            if (!focused) {
                amount[i] = origin[i] = 0;
                down[i] = false;
                changed[i] = now;
                continue;
            }
            if (pressed != down[i]) {
                origin[i] = amount[i];
                changed[i] = now;
                down[i] = pressed;
            }
            float t = Math.min(1, (now - changed[i]) / (pressed ? 120_000_000F : 200_000_000F));
            float eased = t * t * (3 - 2 * t);
            amount[i] = origin[i] + ((pressed ? 1 : 0) - origin[i]) * eased;
        }
    }

    void reset() {
        java.util.Arrays.fill(amount, 0);
        java.util.Arrays.fill(origin, 0);
        java.util.Arrays.fill(down, false);
    }

    float draw(GuiGraphics g, KeyMapping binding, float x, float y, int height) {
        InputConstants.Key key = binding.getKey();
        int index = -1;
        if (key.getType() == InputConstants.Type.MOUSE) {
            for (int i = 0; i < ICONS.size(); i++)
                if (ICONS.get(i).button == key.getValue()) {
                    index = i;
                    break;
                }
        } else if (key.getType() == InputConstants.Type.KEYSYM) {
            for (int i = 0; i < ICONS.size(); i++)
                if (ICONS.get(i).key == key.getValue()) {
                    index = i;
                    break;
                }
        }
        if (index < 0) {
            String label = binding.getTranslatedKeyMessage().getString();
            Minecraft mc = Minecraft.getInstance();
            g.drawString(
                    mc.font,
                    label,
                    (int) x,
                    (int) (y + (height - mc.font.lineHeight) / 2f),
                    0xFFECEFE8,
                    true);
            return mc.font.width(label);
        }
        Icon icon = ICONS.get(index);
        int pose =
                icon.index
                        + (icon.button >= 0 || down[index] ? 9 : 0)
                        + Math.round(amount[index] * 8);
        int sw = icon.width * 2, sh = 48;
        float w = height * (sw / (float) sh);
        Minecraft.getInstance().getTextureManager().getTexture(POSES).setFilter(true, false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(w / sw, height / (float) sh, 1);
        g.blit(POSES, 0, 0, pose % 16 * 128, pose / 16 * 48, sw, sh, ATLAS_WIDTH, ATLAS_HEIGHT);
        g.pose().popPose();
        return w;
    }

    private record Icon(int index, int width, int key, int button) {}
}
