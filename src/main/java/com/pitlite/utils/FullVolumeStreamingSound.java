package com.pitlite.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.ResourceLocation;

public class FullVolumeStreamingSound extends MovingSound {

    public FullVolumeStreamingSound(ResourceLocation soundLocation) {
        this(soundLocation, false);
    }

    public FullVolumeStreamingSound(ResourceLocation soundLocation, boolean repeat) {
        super(soundLocation);
        this.volume = 1.0F;
        this.pitch = 1.0F;
        this.repeat = repeat;
        this.repeatDelay = 0;
        this.attenuationType = ISound.AttenuationType.NONE;
        syncToPlayer();
    }

    public void stop() {
        this.donePlaying = true;
        this.repeat = false;
    }

    @Override
    public void update() {
        if (Minecraft.getMinecraft().thePlayer == null) {
            this.donePlaying = true;
            return;
        }
        syncToPlayer();
    }

    private void syncToPlayer() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        this.xPosF = (float) mc.thePlayer.posX;
        this.yPosF = (float) mc.thePlayer.posY;
        this.zPosF = (float) mc.thePlayer.posZ;
    }
}
