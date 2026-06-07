package com.pitlite.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

public class RenderUtils {

    public static void drawBox(double x, double y, double z, double x1, double y1, double z1, float red, float green, float blue, float alpha) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1.5F);
        GL11.glColor4f(red, green, blue, alpha);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(x, y, z);
        GL11.glVertex3d(x, y1, z);
        GL11.glVertex3d(x1, y, z);
        GL11.glVertex3d(x1, y1, z);
        GL11.glVertex3d(x, y, z1);
        GL11.glVertex3d(x, y1, z1);
        GL11.glVertex3d(x1, y, z1);
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x, y, z);
        GL11.glVertex3d(x1, y, z);
        GL11.glVertex3d(x, y1, z);
        GL11.glVertex3d(x1, y1, z);
        GL11.glVertex3d(x, y, z1);
        GL11.glVertex3d(x1, y, z1);
        GL11.glVertex3d(x, y1, z1);
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x, y, z);
        GL11.glVertex3d(x, y, z1);
        GL11.glVertex3d(x1, y, z);
        GL11.glVertex3d(x1, y, z1);
        GL11.glVertex3d(x, y1, z);
        GL11.glVertex3d(x, y1, z1);
        GL11.glVertex3d(x1, y1, z);
        GL11.glVertex3d(x1, y1, z1);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    public static void drawFilledBox(double x, double y, double z, double x1, double y1, double z1, float red, float green, float blue, float alpha) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glColor4f(red, green, blue, alpha);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(x, y, z);
        GL11.glVertex3d(x, y1, z);
        GL11.glVertex3d(x1, y1, z);
        GL11.glVertex3d(x1, y, z);
        GL11.glVertex3d(x, y, z1);
        GL11.glVertex3d(x, y1, z1);
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x1, y, z1);
        GL11.glVertex3d(x, y, z);
        GL11.glVertex3d(x, y1, z);
        GL11.glVertex3d(x, y1, z1);
        GL11.glVertex3d(x, y, z1);
        GL11.glVertex3d(x1, y, z);
        GL11.glVertex3d(x1, y1, z);
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x1, y, z1);
        GL11.glVertex3d(x, y, z);
        GL11.glVertex3d(x1, y, z);
        GL11.glVertex3d(x1, y, z1);
        GL11.glVertex3d(x, y, z1);
        GL11.glVertex3d(x, y1, z);
        GL11.glVertex3d(x1, y1, z);
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x, y1, z1);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    public static void drawLine(Vec3 start, Vec3 end, int color, float lineWidth) {
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float alpha = (color >> 24 & 0xFF) / 255.0F;

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(lineWidth);
        GL11.glColor4f(red, green, blue, alpha);

        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(start.xCoord, start.yCoord, start.zCoord);
        GL11.glVertex3d(end.xCoord, end.yCoord, end.zCoord);
        GL11.glEnd();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    public static void drawStringWithShadow(String text, int x, int y, int color) {
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(text, x, y, color);
    }

    public static void drawRect(int left, int top, int right, int bottom, int color) {
        int j;
        if (left < right) {
            j = left;
            left = right;
            right = j;
        }

        if (top < bottom) {
            j = top;
            top = bottom;
            bottom = j;
        }

        float f3 = (float)(color >> 24 & 255) / 255.0F;
        float f = (float)(color >> 16 & 255) / 255.0F;
        float f1 = (float)(color >> 8 & 255) / 255.0F;
        float f2 = (float)(color & 255) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(f, f1, f2, f3);
        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos((double)left, (double)bottom, 0.0D).endVertex();
        worldrenderer.pos((double)right, (double)bottom, 0.0D).endVertex();
        worldrenderer.pos((double)right, (double)top, 0.0D).endVertex();
        worldrenderer.pos((double)left, (double)top, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawHLine(int x1, int x2, int y, int color) {
        drawRect(x1, y, x2, y + 1, color);
    }

    public static void drawVLine(int x, int y1, int y2, int color) {
        drawRect(x, y1, x + 1, y2, color);
    }

    public static void drawOutline(int x1, int y1, int x2, int y2, int color) {
        drawHLine(x1, x2, y1, color);
        drawHLine(x1, x2, y2 - 1, color);
        drawVLine(x1, y1, y2, color);
        drawVLine(x2 - 1, y1, y2, color);
    }

    public static void drawGradientRect(int x1, int y1, int x2, int y2, int topColor, int bottomColor) {
        float a1 = ((topColor >> 24) & 0xFF) / 255.0f;
        float r1 = ((topColor >> 16) & 0xFF) / 255.0f;
        float g1 = ((topColor >> 8) & 0xFF) / 255.0f;
        float b1 = (topColor & 0xFF) / 255.0f;

        float a2 = ((bottomColor >> 24) & 0xFF) / 255.0f;
        float r2 = ((bottomColor >> 16) & 0xFF) / 255.0f;
        float g2 = ((bottomColor >> 8) & 0xFF) / 255.0f;
        float b2 = (bottomColor & 0xFF) / 255.0f;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(x2, y1, 0.0D).color(r1, g1, b1, a1).endVertex();
        worldrenderer.pos(x1, y1, 0.0D).color(r1, g1, b1, a1).endVertex();
        worldrenderer.pos(x1, y2, 0.0D).color(r2, g2, b2, a2).endVertex();
        worldrenderer.pos(x2, y2, 0.0D).color(r2, g2, b2, a2).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public static void drawRoundedRect(int x1, int y1, int x2, int y2, int radius, int color) {
        int r = Math.max(0, radius);
        if (r <= 0) {
            drawRect(x1, y1, x2, y2, color);
            return;
        }

        int w = Math.abs(x2 - x1);
        int h = Math.abs(y2 - y1);
        r = Math.min(r, Math.min(w / 2, h / 2));

        drawRect(x1 + r, y1, x2 - r, y2, color);
        drawRect(x1, y1 + r, x1 + r, y2 - r, color);
        drawRect(x2 - r, y1 + r, x2, y2 - r, color);

        drawQuarterCircle(x1 + r, y1 + r, r, 180, 270, color);
        drawQuarterCircle(x2 - r, y1 + r, r, 270, 360, color);
        drawQuarterCircle(x2 - r, y2 - r, r, 0, 90, color);
        drawQuarterCircle(x1 + r, y2 - r, r, 90, 180, color);
    }

    private static void drawQuarterCircle(int cx, int cy, int r, int startDeg, int endDeg, int color) {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float rr = ((color >> 16) & 0xFF) / 255.0f;
        float gg = ((color >> 8) & 0xFF) / 255.0f;
        float bb = (color & 0xFF) / 255.0f;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(rr, gg, bb, a);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        worldrenderer.pos(cx, cy, 0.0D).endVertex();

        int steps = Math.max(8, r * 2);
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / (float) steps;
            double ang = Math.toRadians(startDeg + (endDeg - startDeg) * t);
            double x = cx + Math.cos(ang) * r;
            double y = cy + Math.sin(ang) * r;
            worldrenderer.pos(x, y, 0.0D).endVertex();
        }

        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawShadowRounded(int x1, int y1, int x2, int y2, int radius, int layers, int baseAlpha) {
        int lay = Math.max(1, layers);
        int a0 = Math.max(0, Math.min(255, baseAlpha));
        for (int i = lay; i >= 1; i--) {
            int a = (int) (a0 * (i / (double) lay) * 0.35);
            int col = (a << 24);
            drawRoundedRect(x1 - i, y1 - i, x2 + i, y2 + i, radius + i, col);
        }
    }
}
