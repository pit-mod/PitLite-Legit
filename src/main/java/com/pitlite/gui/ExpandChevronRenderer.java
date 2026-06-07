package com.pitlite.gui;

import com.pitlite.gui.physics.GuiMath;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;

public final class ExpandChevronRenderer {

    private ExpandChevronRenderer() {
    }

    public static void draw(FontRenderer fontRenderer, int drawX, int drawY, float expandT, int color) {
        float t = GuiMath.clamp01(expandT);
        float pivotX = drawX + 3f;
        float pivotY = drawY + fontRenderer.FONT_HEIGHT / 2f;

        GlStateManager.pushMatrix();
        GlStateManager.translate(pivotX, pivotY, 0f);
        GlStateManager.rotate(t * 90f, 0f, 0f, 1f);
        GlStateManager.translate(-pivotX, -pivotY, 0f);
        fontRenderer.drawStringWithShadow(">", drawX, drawY, color);
        GlStateManager.popMatrix();
    }
}
