package com.pitlite.gui.physics;

public final class GuiMath {

    private GuiMath() {
    }

    public static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * clamp01(t);
    }

    public static int lerpColor(int fromArgb, int toArgb, float t) {
        t = clamp01(t);
        int fa = (fromArgb >> 24) & 0xFF;
        int fr = (fromArgb >> 16) & 0xFF;
        int fg = (fromArgb >> 8) & 0xFF;
        int fb = fromArgb & 0xFF;

        int ta = (toArgb >> 24) & 0xFF;
        int tr = (toArgb >> 16) & 0xFF;
        int tg = (toArgb >> 8) & 0xFF;
        int tb = toArgb & 0xFF;

        int a = (int) (fa + (ta - fa) * t);
        int r = (int) (fr + (tr - fr) * t);
        int g = (int) (fg + (tg - fg) * t);
        int b = (int) (fb + (tb - fb) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int withAlpha(int argb, float alpha) {
        int a = (int) (255f * clamp01(alpha));
        return (a << 24) | (argb & 0x00FFFFFF);
    }
}
