package com.pitlite.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

public final class HudBounds {
    private HudBounds() {
    }

    public static int clampX(int x, int width) {
        return clampX(x, width, false);
    }

    public static int clampX(int x, int width, boolean centerAnchored) {
        int w = Math.max(1, width);
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int screenW = sr.getScaledWidth();

        if (centerAnchored) {
            int half = w / 2;
            int min = half;
            int max = screenW - half;
            if (max < min) {
                return screenW / 2;
            }
            return Math.max(min, Math.min(x, max));
        }

        int max = screenW - w;
        if (max < 0) {
            return 0;
        }
        return Math.max(0, Math.min(x, max));
    }

    public static int clampY(int y, int height) {
        int h = Math.max(1, height);
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int max = sr.getScaledHeight() - h;
        if (max < 0) {
            return 0;
        }
        return Math.max(0, Math.min(y, max));
    }

    public static int[] clamp(int x, int y, int width, int height, boolean centerAnchored) {
        return new int[]{
                clampX(x, width, centerAnchored),
                clampY(y, height)
        };
    }
}
