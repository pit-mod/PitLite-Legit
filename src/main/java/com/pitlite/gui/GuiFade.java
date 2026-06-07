package com.pitlite.gui;

import com.pitlite.gui.physics.GuiMath;

public final class GuiFade {

    private GuiFade() {
    }

    public static int tint(int argb, float alpha) {
        float t = GuiMath.clamp01(alpha);
        if (t >= 0.999f) {
            return argb;
        }
        if (t <= 0.001f) {
            return argb & 0x00FFFFFF;
        }
        int a = (argb >> 24) & 0xFF;
        int newA = (int) (a * t);
        return (newA << 24) | (argb & 0x00FFFFFF);
    }
}
