package com.pitlite.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class NotificationManager {

    private static final int MAX_TOASTS = 7;
    private static final List<Toast> ACTIVE = new ArrayList<>();
    private static boolean suppressModuleToggle;

    private NotificationManager() {
    }

    public static void show(String message, int durationMs) {
        if (message == null || message.isEmpty()) {
            return;
        }
        int duration = Math.max(1200, durationMs);
        ACTIVE.add(0, new Toast(message, duration, accentFromMessage(message)));
        trim();
    }

    public static void showModuleToggle(String moduleName, boolean enabled) {
        if (suppressModuleToggle || moduleName == null) {
            return;
        }
        int accent = enabled ? 0xFF55CC55 : 0xFFFF5555;
        String state = enabled ? "Enabled" : "Disabled";
        String color = enabled ? "\u00a7a" : "\u00a7c";
        show(color + "\u00a7l" + moduleName + "\u00a7r\u00a77 " + state, 2200, accent);
    }

    public static void show(String message, int durationMs, int accentColor) {
        if (message == null || message.isEmpty()) {
            return;
        }
        ACTIVE.add(0, new Toast(message, Math.max(1200, durationMs), accentColor));
        trim();
    }

    public static void showInChat(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null && message != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }

    public static void setSuppressModuleToggle(boolean suppress) {
        suppressModuleToggle = suppress;
    }

    static List<Toast> getActiveToasts() {
        return Collections.unmodifiableList(ACTIVE);
    }

    static void pruneExpired(long now) {
        Iterator<Toast> it = ACTIVE.iterator();
        while (it.hasNext()) {
            if (it.next().isExpired(now)) {
                it.remove();
            }
        }
    }

    private static void trim() {
        while (ACTIVE.size() > MAX_TOASTS) {
            ACTIVE.remove(ACTIVE.size() - 1);
        }
    }

    private static int accentFromMessage(String message) {
        if (message.contains("\u00a7a")) {
            return 0xFF55CC55;
        }
        if (message.contains("\u00a7c")) {
            return 0xFFFF5555;
        }
        if (message.contains("\u00a7e")) {
            return 0xFFFFCC55;
        }
        if (message.contains("\u00a79")) {
            return 0xFF5599FF;
        }
        if (message.contains("\u00a7b")) {
            return 0xFF55CCFF;
        }
        return 0xFF2F89FF;
    }

    public static final class Toast {
        final String message;
        final long createdAt;
        final int durationMs;
        final int accentColor;

        Toast(String message, int durationMs, int accentColor) {
            this.message = message;
            this.durationMs = durationMs;
            this.accentColor = accentColor;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isExpired(long now) {
            return now - createdAt > durationMs + FADE_OUT_MS;
        }

        float getAlpha(long now, int fadeInMs, int fadeOutMs) {
            long age = now - createdAt;
            long remaining = durationMs - age;
            if (age < fadeInMs) {
                return easeOutCubic(age / (float) fadeInMs);
            }
            if (remaining < fadeOutMs) {
                return easeInCubic(Math.max(0f, remaining / (float) fadeOutMs));
            }
            return 1f;
        }

        float getEnterProgress(long now, int fadeInMs) {
            long age = now - createdAt;
            if (age >= fadeInMs) {
                return 1f;
            }
            return easeOutBack(age / (float) fadeInMs);
        }

        float getExitProgress(long now, int fadeOutMs) {
            long remaining = durationMs - (now - createdAt);
            if (remaining >= fadeOutMs) {
                return 0f;
            }
            return 1f - easeInCubic(Math.max(0f, remaining / (float) fadeOutMs));
        }

        private static float easeOutCubic(float t) {
            return 1f - (float) Math.pow(1f - clamp01(t), 3);
        }

        private static float easeInCubic(float t) {
            return (float) Math.pow(clamp01(t), 3);
        }

        private static float easeOutBack(float t) {
            float c1 = 1.70158f;
            float c3 = c1 + 1f;
            t = clamp01(t);
            return 1f + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
        }

        private static float clamp01(float t) {
            return t < 0f ? 0f : (t > 1f ? 1f : t);
        }
    }

    private static final int FADE_OUT_MS = 350;
}
