package com.pitlite.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class VideoRenderer {

    private static boolean playing = false;
    private static long startTime = 0;
    private static int totalFrames = 0;
    private static int fps = 30;
    private static String currentVideoName = "";
    private static int videoWidth = 480;
    private static int videoHeight = 270;

    public static void play(String name, int frameCount, int targetFps, int width, int height, boolean playSound) {
        currentVideoName = name;
        totalFrames = frameCount;
        fps = targetFps;
        videoWidth = width;
        videoHeight = height;
        startTime = System.currentTimeMillis();
        playing = true;

        if (playSound) {
            try {
                if (Minecraft.getMinecraft().thePlayer != null) {
                    Minecraft.getMinecraft().thePlayer.playSound("pitlite:" + name, 1.0F, 1.0F);
                }
            } catch (Exception e) {
                System.out.println("[PitLite] Could not play sound for video: " + name);
            }
        }
    }

    public static void play(String name, int frameCount, int targetFps) {
        play(name, frameCount, targetFps, 480, 270, true);
    }

    public static void stop() {
        playing = false;
        currentVideoName = "";
    }

    public static boolean isPlaying() {
        return playing;
    }

    public static void render(ScaledResolution sr) {
        if (!playing || currentVideoName.isEmpty()) return;

        long elapsed = System.currentTimeMillis() - startTime;
        int frameIndex = (int) (elapsed * fps / 1000);

        if (frameIndex >= totalFrames) {
            stop();
            return;
        }

        int frameNumber = frameIndex + 1;

        ResourceLocation frameTex = new ResourceLocation(
                "pitlite", "textures/video/" + currentVideoName + "/frame_" + frameNumber + ".png"
        );

        Minecraft mc = Minecraft.getMinecraft();

        int screenW = sr.getScaledWidth();
        int screenH = sr.getScaledHeight();
        int x = (screenW - videoWidth) / 2;
        int y = (screenH - videoHeight) / 2;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        mc.getTextureManager().bindTexture(frameTex);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, videoWidth, videoHeight, videoWidth, videoHeight);

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}
