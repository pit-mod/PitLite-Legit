package com.pitlite.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

public class NotificationOverlay {

    private static final int MARGIN_X = 8;
    private static final int MARGIN_Y = 32;
    private static final int GAP = 5;
    private static final int PAD_X = 8;
    private static final int PAD_Y = 6;
    private static final int ACCENT_W = 3;
    private static final int MIN_WIDTH = 72;
    private static final float MIN_TEXT_SCALE = 0.5f;
    private static final int FADE_IN_MS = 180;
    private static final int FADE_OUT_MS = 300;

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }

        List<NotificationManager.Toast> toasts = NotificationManager.getActiveToasts();
        if (toasts.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        NotificationManager.pruneExpired(now);

        ScaledResolution sr = new ScaledResolution(mc);
        FontRenderer fr = mc.fontRendererObj;
        int screenW = sr.getScaledWidth();
        int screenH = sr.getScaledHeight();
        int maxTextWidth = Math.max(48, screenW - MARGIN_X * 2 - ACCENT_W - PAD_X * 2);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        int stackY = screenH - MARGIN_Y;
        for (NotificationManager.Toast toast : toasts) {
            float alpha = toast.getAlpha(now, FADE_IN_MS, FADE_OUT_MS);
            if (alpha <= 0.02f) {
                continue;
            }

            float enter = toast.getEnterProgress(now, FADE_IN_MS);
            float exit = toast.getExitProgress(now, FADE_OUT_MS);

            Layout layout = measureLayout(fr, toast.message, maxTextWidth);
            int boxW = layout.boxW;
            int boxH = layout.boxH;

            stackY -= boxH;
            int drawX = screenW - MARGIN_X - boxW + (int) ((1f - enter) * 20f + exit * 16f);
            int drawY = stackY;
            stackY -= GAP;

            int bg = applyAlpha(0xE0141418, alpha);
            int accent = applyAlpha(toast.accentColor, alpha);
            int textColor = applyAlpha(0xFFF2F4F7, alpha);

            RenderUtils.drawRect(drawX, drawY, drawX + boxW, drawY + boxH, bg);
            RenderUtils.drawRect(drawX, drawY, drawX + ACCENT_W, drawY + boxH, accent);

            float textScale = layout.textScale;
            int textH = (int) Math.ceil(fr.FONT_HEIGHT * textScale);
            int textX = drawX + ACCENT_W + PAD_X;
            int textY = drawY + (boxH - textH) / 2;

            GlStateManager.pushMatrix();
            GlStateManager.translate(textX, textY, 0);
            GlStateManager.scale(textScale, textScale, 1f);
            fr.drawString(toast.message, 0, 0, textColor);
            GlStateManager.popMatrix();
        }

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static Layout measureLayout(FontRenderer fr, String message, int maxTextWidth) {
        int rawW = fr.getStringWidth(message);

        float textScale = 1f;
        if (rawW > maxTextWidth) {
            textScale = maxTextWidth / (float) rawW;
            textScale = Math.max(MIN_TEXT_SCALE, textScale);
        }

        int scaledTextW = (int) Math.ceil(rawW * textScale);
        int textH = (int) Math.ceil(fr.FONT_HEIGHT * textScale);
        int boxW = Math.max(MIN_WIDTH, ACCENT_W + PAD_X + scaledTextW + PAD_X);
        int boxH = textH + PAD_Y * 2;

        return new Layout(boxW, boxH, textScale, scaledTextW);
    }

    private static int applyAlpha(int argb, float alpha) {
        int a = (int) (((argb >> 24) & 0xFF) * alpha);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private static final class Layout {
        final int boxW;
        final int boxH;
        final float textScale;
        final int textW;

        Layout(int boxW, int boxH, float textScale, int textW) {
            this.boxW = boxW;
            this.boxH = boxH;
            this.textScale = textScale;
            this.textW = textW;
        }
    }
}
