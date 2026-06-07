package com.pitlite.gui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import com.pitlite.utils.RenderUtils;
import com.pitlite.gui.physics.GuiMath;

public final class ModuleRowRenderer {

    public static final int DISABLED_BG = 0x96141414;       // rgba(20, 20, 20, 150)
    public static final int DISABLED_TEXT = 0xFF787880;

    public static final int ENABLED_BASE_BG = 0xC81E2024;   // rgba(30, 32, 36, 200)

    public static final int ACCENT_IOS_BLUE = 0xFF007AFF;
    public static final int ACCENT_IOS_MINT = 0xFF34C759;
    public static final int ACCENT_IOS_RED = 0xFFFF453A;

    public static final float ACCENT_BAR_WIDTH = 2.5f;
    public static final float ACCENT_TINT_STRENGTH = 0.15f;

    private static final int ENABLED_TEXT = 0xFFFFFFFF;
    private static final int HOVER_LIFT = 0x0CFFFFFF;

    private ModuleRowRenderer() {
    }

    public static void drawModuleRow(
            FontRenderer fontRenderer,
            String label,
            boolean enabled,
            boolean dangerous,
            boolean hovered,
            int rowX,
            int rowY,
            int rowW,
            int rowH,
            int maxLabelWidth,
            int accentColor) {
        drawModuleRow(fontRenderer, label, enabled, dangerous, hovered ? 1f : 0f, 1f,
                rowX, rowY, rowW, rowH, maxLabelWidth);
    }

    public static void drawModuleRow(
            FontRenderer fontRenderer,
            String label,
            boolean enabled,
            boolean dangerous,
            boolean hovered,
            int rowX,
            int rowY,
            int rowW,
            int rowH,
            int maxLabelWidth) {
        drawModuleRow(fontRenderer, label, enabled, dangerous, hovered ? 1f : 0f,
                rowX, rowY, rowW, rowH, maxLabelWidth);
    }

    public static void drawModuleRow(
            FontRenderer fontRenderer,
            String label,
            boolean enabled,
            boolean dangerous,
            float hoverStrength,
            int rowX,
            int rowY,
            int rowW,
            int rowH,
            int maxLabelWidth) {
        drawModuleRow(fontRenderer, label, enabled, dangerous, hoverStrength, 1f,
                rowX, rowY, rowW, rowH, maxLabelWidth);
    }

    public static void drawModuleRow(
            FontRenderer fontRenderer,
            String label,
            boolean enabled,
            boolean dangerous,
            float hoverStrength,
            float rowAlpha,
            int rowX,
            int rowY,
            int rowW,
            int rowH,
            int maxLabelWidth) {
        int accent = dangerous ? ACCENT_IOS_RED : ACCENT_IOS_BLUE;
        if (enabled) {
            drawEnabledRow(fontRenderer, label, dangerous, hoverStrength, rowAlpha, rowX, rowY, rowW, rowH, maxLabelWidth, accent);
        } else {
            drawDisabledRow(fontRenderer, label, dangerous, hoverStrength, rowAlpha, rowX, rowY, rowW, rowH, maxLabelWidth);
        }
    }

    public static void drawModuleRowBlended(
            FontRenderer fontRenderer,
            String label,
            float enableBlend,
            boolean dangerous,
            boolean hovered,
            float hoverScale,
            int rowX,
            int rowY,
            int rowW,
            int rowH,
            int maxLabelWidth) {

        float t = GuiMath.clamp01(enableBlend);
        int accent = dangerous ? ACCENT_IOS_RED : ACCENT_IOS_BLUE;

        if (t <= 0.001f) {
            drawDisabledRow(fontRenderer, label, dangerous, hovered ? 1f : 0f, 1f, rowX, rowY, rowW, rowH, maxLabelWidth);
            return;
        }
        if (t >= 0.999f) {
            drawEnabledRow(fontRenderer, label, dangerous, hovered ? 1f : 0f, 1f, rowX, rowY, rowW, rowH, maxLabelWidth, accent);
            return;
        }

        drawDisabledRow(fontRenderer, label, dangerous, hovered ? 1f : 0f, 1f, rowX, rowY, rowW, rowH, maxLabelWidth);
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1f, 1f, 1f, t);
        drawEnabledRow(fontRenderer, label, dangerous, hovered ? 1f : 0f, 1f, rowX, rowY, rowW, rowH, maxLabelWidth, accent);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public static int getTextInsetX(boolean enabled) {
        return getTextInsetX(enabled ? 1f : 0f);
    }

    public static int getTextInsetX(float enableBlend) {
        int barPx = accentBarPixelWidth();
        return (int) GuiMath.lerp(GuiTheme.PADDING_X, GuiTheme.PADDING_X + barPx, GuiMath.clamp01(enableBlend));
    }

    public static int accentBarPixelWidth() {
        return (int) Math.ceil(ACCENT_BAR_WIDTH);
    }

    private static void drawDisabledRow(
            FontRenderer fr,
            String label,
            boolean dangerous,
            float hoverStrength,
            float rowAlpha,
            int rowX,
            int rowY,
            int rowW,
            int rowH,
            int maxLabelWidth) {

        int bg = DISABLED_BG;
        if (hoverStrength > 0.001f) {
            bg = applyAlphaOverlay(DISABLED_BG, withAlpha(HOVER_LIFT, hoverStrength));
        }
        bg = GuiFade.tint(bg, rowAlpha);
        RenderUtils.drawRect(rowX, rowY, rowX + rowW, rowY + rowH, bg);

        int textX = rowX + getTextInsetX(false);
        int textY = rowY + verticalTextOffset(rowH, fr);
        int textColor = GuiFade.tint(dangerous ? ACCENT_IOS_RED : DISABLED_TEXT, rowAlpha);
        if (maxLabelWidth > 0) {
            GuiText.drawFit(label, textX, textY, maxLabelWidth, textColor);
        } else {
            fr.drawStringWithShadow(label, textX, textY, textColor);
        }
    }

    private static void drawEnabledRow(
            FontRenderer fr,
            String label,
            boolean dangerous,
            float hoverStrength,
            float rowAlpha,
            int rowX,
            int rowY,
            int rowW,
            int rowH,
            int maxLabelWidth,
            int accentColor) {

        int accent = dangerous ? ACCENT_IOS_RED : accentColor;

        int bg = ENABLED_BASE_BG;
        if (hoverStrength > 0.001f) {
            bg = applyAlphaOverlay(ENABLED_BASE_BG, withAlpha(HOVER_LIFT, hoverStrength));
        }
        bg = GuiFade.tint(bg, rowAlpha);
        RenderUtils.drawRect(rowX, rowY, rowX + rowW, rowY + rowH, bg);

        int tint = blendOver(bg, withAlpha(accent, ACCENT_TINT_STRENGTH));
        RenderUtils.drawRect(rowX, rowY, rowX + rowW, rowY + rowH, tint);

        drawAccentBar(rowX, rowY, rowH, GuiFade.tint(accent, rowAlpha));

        int textX = rowX + getTextInsetX(true);
        int textY = rowY + verticalTextOffset(rowH, fr);
        int textColor = GuiFade.tint(dangerous ? ACCENT_IOS_RED : ENABLED_TEXT, rowAlpha);
        if (maxLabelWidth > 0 && fr.getStringWidth(label) > maxLabelWidth) {
            GuiText.drawFit(label, textX, textY, maxLabelWidth, textColor);
        } else {
            fr.drawStringWithShadow(label, textX, textY, textColor);
        }
    }

    public static void drawAccentBar(int rowX, int rowY, int rowH, int accentColor) {
        int fullPx = 2;
        int halfPx = 1;

        int rowBottom = rowY + rowH;
        RenderUtils.drawRect(rowX, rowY, rowX + fullPx, rowBottom, accentColor);

        int halfAlpha = (accentColor & 0x00FFFFFF) | (0x80 << 24);
        RenderUtils.drawRect(rowX + fullPx, rowY, rowX + fullPx + halfPx, rowBottom, halfAlpha);
    }

    public static void drawEmissiveLabel(FontRenderer fr, String label, int textX, int textY, int accentColor) {
        int glow = withAlpha(accentColor, 0.28f);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        fr.drawString(label, textX, textY, glow);
        fr.drawString(label, textX + 1, textY, glow);
        fr.drawString(label, textX, textY + 1, withAlpha(accentColor, 0.14f));
        fr.drawString(label, textX, textY, ENABLED_TEXT);

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static int verticalTextOffset(int rowH, FontRenderer fr) {
        return Math.max(GuiTheme.PADDING_Y, (rowH - fr.FONT_HEIGHT) / 2);
    }

    private static int withAlpha(int argb, float alpha) {
        int a = (int) (255f * clamp01(alpha));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private static int applyAlphaOverlay(int baseArgb, int overlayArgb) {
        float baseA = ((baseArgb >> 24) & 0xFF) / 255f;
        float overA = ((overlayArgb >> 24) & 0xFF) / 255f;
        float outA = clamp01(baseA + overA * (1f - baseA));

        int br = (baseArgb >> 16) & 0xFF;
        int bg = (baseArgb >> 8) & 0xFF;
        int bb = baseArgb & 0xFF;

        int or = (overlayArgb >> 16) & 0xFF;
        int og = (overlayArgb >> 8) & 0xFF;
        int ob = overlayArgb & 0xFF;

        float t = overA * (1f - baseA);
        int r = (int) (br * baseA * (1f - t) + or * t + br * (1f - baseA) * 0.5f);
        int g = (int) (bg * baseA * (1f - t) + og * t + bg * (1f - baseA) * 0.5f);
        int b = (int) (bb * baseA * (1f - t) + ob * t + bb * (1f - baseA) * 0.5f);

        return ((int) (outA * 255) << 24) | (clamp255(r) << 16) | (clamp255(g) << 8) | clamp255(b);
    }

    private static int blendOver(int baseArgb, int tintArgb) {
        float baseA = ((baseArgb >> 24) & 0xFF) / 255f;
        float tintA = ((tintArgb >> 24) & 0xFF) / 255f;

        int br = (baseArgb >> 16) & 0xFF;
        int bg = (baseArgb >> 8) & 0xFF;
        int bb = baseArgb & 0xFF;

        int tr = (tintArgb >> 16) & 0xFF;
        int tg = (tintArgb >> 8) & 0xFF;
        int tb = tintArgb & 0xFF;

        float outA = tintA + baseA * (1f - tintA);
        if (outA <= 0f) {
            return 0;
        }

        int r = (int) ((tr * tintA + br * baseA * (1f - tintA)) / outA);
        int g = (int) ((tg * tintA + bg * baseA * (1f - tintA)) / outA);
        int b = (int) ((tb * tintA + bb * baseA * (1f - tintA)) / outA);

        return ((int) (outA * 255) << 24) | (clamp255(r) << 16) | (clamp255(g) << 8) | clamp255(b);
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    private static int clamp255(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
