package com.pitlite.gui.physics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

public final class GuiScissor {

    private GuiScissor() {
    }

    public static void enable(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();

        int scrollY = 0;
        GuiAnimationController anim = GuiAnimationController.get();
        if (anim != null) {
            scrollY = Math.round(anim.getScrollY());
        }

        int screenTop = y - scrollY;
        int screenBottom = screenTop + height;

        int screenHeight = sr.getScaledHeight();
        if (mc.currentScreen instanceof GuiScreen) {
            screenHeight = ((GuiScreen) mc.currentScreen).height;
        }

        int clipTop = Math.max(0, screenTop);
        int clipBottom = Math.min(screenHeight, screenBottom);
        int clipH = clipBottom - clipTop;
        if (clipH <= 0) {
            return;
        }

        int scissorX = Math.max(0, x) * scale;
        int scissorW = Math.max(0, width * scale);
        int scissorY = mc.displayHeight - clipBottom * scale;
        int scissorH = clipH * scale;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorW, scissorH);
    }

    public static void disable() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
