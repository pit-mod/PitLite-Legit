package com.pitlite.module.impl.render;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import com.pitlite.module.Category;
import com.pitlite.module.Module;
import com.pitlite.module.ModuleManager;
import com.pitlite.settings.BooleanSetting;
import com.pitlite.settings.NumberSetting;
import com.pitlite.settings.ModeSetting;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class HUD extends Module {

    private final ModeSetting colorMode = new ModeSetting("Color Mode", "Rainbow",
            new String[]{"Rainbow", "Chroma", "Astolfo"});
    private final NumberSetting colorSpeed = new NumberSetting("Color Speed", 1.0, 0.5, 1.5, 1);
    private final NumberSetting colorSaturation = new NumberSetting("Saturation", 75.0, 0.0, 100.0, 0);
    private final NumberSetting colorBrightness = new NumberSetting("Brightness", 75.0, 0.0, 100.0, 0);

    private final ModeSetting positionX = new ModeSetting("Position X", "Right",
            new String[]{"Left", "Right"});
    private final ModeSetting positionY = new ModeSetting("Position Y", "Top",
            new String[]{"Top", "Bottom"});
    private final NumberSetting offsetX = new NumberSetting("Offset X", 2.0, 0.0, 255.0, 0);
    private final NumberSetting offsetY = new NumberSetting("Offset Y", 2.0, 0.0, 255.0, 0);
    

    private final NumberSetting scale = new NumberSetting("Scale", 1.0, 0.5, 1.5, 1);
    private final NumberSetting backgroundOpacity = new NumberSetting("Background", 25.0, 0.0, 100.0, 0);
    private final BooleanSetting showBar = new BooleanSetting("Bar", true);
    private final BooleanSetting shadow = new BooleanSetting("Shadow", true);
    private final BooleanSetting lowercase = new BooleanSetting("Lowercase", false);

    private List<Module> activeModules = new ArrayList<>();

    public HUD() {
        super("HUD", "Displays enabled modules on screen", Category.RENDER);
        addSettings(colorMode, colorSpeed, colorSaturation, colorBrightness,
                positionX, positionY, offsetX, offsetY,
                scale, backgroundOpacity, showBar, shadow, lowercase);
    }

    public Color getColor(long time, long offset) {
        Color color = Color.WHITE;
        String mode = colorMode.getMode();

        switch (mode) {
            case "Rainbow":
                color = fromHSB(getColorCycle(time, offset), 1.0f, 1.0f);
                break;
            case "Chroma":
                color = fromHSB(getColorCycle(time / 3L, 0L), 1.0f, 1.0f);
                break;
            case "Astolfo":
                float cycle = getColorCycle(time, offset);
                if (cycle % 1.0f < 0.5f) {
                    cycle = 1.0f - cycle % 1.0f;
                }
                color = fromHSB(cycle, 1.0f, 1.0f);
                break;
        }

        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return Color.getHSBColor(
                hsb[0],
                hsb[1] * ((float) colorSaturation.value / 100.0f),
                hsb[2] * ((float) colorBrightness.value / 100.0f)
        );
    }

    public Color getColor(long time) {
        return getColor(time, 0L);
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (!isToggled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (mc.gameSettings.showDebugInfo) return;

        refreshActiveModules();

        if (activeModules.isEmpty()) return;

        float scaleFactor = (float) scale.value;
        float fontHeight = mc.fontRendererObj.FONT_HEIGHT - 1.0f;
        float barExtra = showBar.enabled ? (shadow.enabled ? 2.0f : 1.0f) : 0.0f;
        float x = (float) offsetX.value;
        float y = (float) offsetY.value + 1.0f * scaleFactor;

        ScaledResolution sr = new ScaledResolution(mc);
        boolean isRight = positionX.getMode().equals("Right");
        boolean isBottom = positionY.getMode().equals("Bottom");

        if (isRight) {
            if (showBar.enabled) {
                x += (1.0f + barExtra) * scaleFactor;
            }
            x = sr.getScaledWidth() - x;
        } else {
            x += (1.0f + barExtra) * scaleFactor;
        }
        if (isBottom) {
            y = sr.getScaledHeight() - y - fontHeight * scaleFactor;
        }

        GlStateManager.pushMatrix();
        GlStateManager.scale(scaleFactor, scaleFactor, 0.0f);

        long time = System.currentTimeMillis();
        long offset = 0;

        for (Module module : activeModules) {
            String name = lowercase.enabled ? module.getName().toLowerCase(Locale.ROOT) : module.getName();
            float textWidth = mc.fontRendererObj.getStringWidth(name) - (shadow.enabled ? 0 : 1);
            int color = getColor(time, offset).getRGB();

            if (backgroundOpacity.value > 0) {
                float bgAlpha = (float) backgroundOpacity.value / 100.0f;
                int bgColor = new Color(0.0f, 0.0f, 0.0f, bgAlpha).getRGB();

                drawRect(
                        x / scaleFactor - 1.0f - (isRight ? textWidth : 0.0f),
                        y / scaleFactor - (isBottom ? (shadow.enabled ? 1.0f : 0.0f) : (offset == 0 ? 1.0f : 0.0f)),
                        x / scaleFactor + 1.0f + (isRight ? 0.0f : textWidth),
                        y / scaleFactor + fontHeight + (isBottom ? (offset == 0 ? 1.0f : 0.0f) : (shadow.enabled ? 1.0f : 0.0f)),
                        bgColor
                );
            }

            if (showBar.enabled) {
                if (shadow.enabled) {
                    drawRect(
                            x / scaleFactor + (isRight ? 1.0f : -3.0f),
                            y / scaleFactor - (isBottom ? 1.0f : (offset == 0 ? 1.0f : 0.0f)),
                            x / scaleFactor + (isRight ? 2.0f : -2.0f),
                            y / scaleFactor + fontHeight + (isBottom ? (offset == 0 ? 1.0f : 0.0f) : 1.0f),
                            color
                    );
                    int darkerColor = (color & 0x00FCFCFC) >> 2 | (color & 0xFF000000);
                    drawRect(
                            x / scaleFactor + (isRight ? 2.0f : -2.0f),
                            y / scaleFactor - (isBottom ? 1.0f : (offset == 0 ? 1.0f : 0.0f)),
                            x / scaleFactor + (isRight ? 3.0f : -1.0f),
                            y / scaleFactor + fontHeight + (isBottom ? (offset == 0 ? 1.0f : 0.0f) : 1.0f),
                            darkerColor
                    );
                } else {
                    drawRect(
                            x / scaleFactor + (isRight ? 1.0f : -2.0f),
                            y / scaleFactor - (isBottom ? 0.0f : (offset == 0 ? 1.0f : 0.0f)),
                            x / scaleFactor + (isRight ? 2.0f : -1.0f),
                            y / scaleFactor + fontHeight + (isBottom ? (offset == 0 ? 1.0f : 0.0f) : 0.0f),
                            color
                    );
                }
            }

            GlStateManager.disableDepth();
            float textX = x / scaleFactor - (isRight ? textWidth : 0.0f);
            float textY = y / scaleFactor + (isBottom ? 1.0f : 0.0f);

            if (shadow.enabled) {
                mc.fontRendererObj.drawStringWithShadow(name, textX, y / scaleFactor, color);
            } else {
                mc.fontRendererObj.drawString(name, textX, textY, color, false);
            }

            y += (fontHeight + (shadow.enabled ? 1.0f : 0.0f)) * scaleFactor * (isBottom ? -1.0f : 1.0f);
            offset++;
        }

        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    private void refreshActiveModules() {
        ModuleManager mgr = com.pitlite.PitLite.moduleManager;
        if (mgr == null) return;

        activeModules = new ArrayList<>();
        for (Module m : mgr.getModules()) {
            if (m.isToggled() && m != this && !m.isHiddenInHud()) {
                activeModules.add(m);
            }
        }
        activeModules.sort(Comparator.comparingInt(
                (Module m) -> mc.fontRendererObj.getStringWidth(m.getName())
        ).reversed());
    }

    private float getColorCycle(long time, long offset) {
        long speed = (long) (3000.0 / Math.pow(
                Math.min(Math.max(0.5f, (float) colorSpeed.value), 1.5f), 3.0));
        return 1.0f - (float) (Math.abs(time - offset * 300L) % speed) / (float) speed;
    }

    private static Color fromHSB(float hue, float saturation, float brightness) {
        return Color.getHSBColor(hue, saturation, brightness);
    }

    private void drawRect(float x1, float y1, float x2, float y2, int color) {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        GL11.glColor4f(r, g, b, a == 0.0f ? 1.0f : a);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x1, y2);
        GL11.glVertex2f(x2, y2);
        GL11.glEnd();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}
