package com.pitlite.gui;

import com.pitlite.gui.physics.GuiMath;
import com.pitlite.utils.RenderUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;

public final class IoSToggleRenderer {

    private static final int TRACK_OFF = 0xFF3A3A3C;
    private static final int TRACK_ON = 0xFF007AFF;
    private static final int KNOB = 0xFFFFFFFF;

    private IoSToggleRenderer() {
    }

    public static void draw(
            FontRenderer fr,
            String label,
            boolean enabled,
            float knobPos,
            float pressScale,
            int x,
            int y,
            int width,
            int height,
            int labelColor) {

        int trackH = 12;
        int trackW = 22;
        int trackX = x + width - trackW - 4;
        int trackY = y + (height - trackH) / 2;

        drawSmartText(fr, label, x + GuiTheme.PADDING_X, y + GuiTheme.PADDING_Y, labelColor, width, trackW + 10);

        int trackColor = GuiMath.lerpColor(TRACK_OFF, TRACK_ON, GuiMath.clamp01(knobPos));
        RenderUtils.drawRect(trackX, trackY, trackX + trackW, trackY + trackH, trackColor);

        int knobSize = (int) (10 * pressScale);
        int knobTravel = trackW - knobSize - 2;
        int knobX = trackX + 1 + (int) (knobTravel * GuiMath.clamp01(knobPos));
        int knobY = trackY + (trackH - knobSize) / 2;
        RenderUtils.drawRect(knobX, knobY, knobX + knobSize, knobY + knobSize, KNOB);
    }

    private static void drawSmartText(FontRenderer fr, String text, int drawX, int drawY, int color, int rowWidth, int reserveRight) {
        int available = Math.max(1, rowWidth - reserveRight - GuiTheme.PADDING_X);
        GuiText.drawFit(text, drawX, drawY, available, color);
    }
}
