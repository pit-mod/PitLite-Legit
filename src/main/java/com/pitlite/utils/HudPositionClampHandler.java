package com.pitlite.utils;

import com.pitlite.PitLite;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class HudPositionClampHandler {
    private boolean clamped;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (clamped || event.phase != TickEvent.Phase.END) {
            return;
        }
        if (Minecraft.getMinecraft().thePlayer == null || PitLite.moduleManager == null) {
            return;
        }
        HudPositionManager.clampSavedPositions(PitLite.moduleManager);
        clamped = true;
    }
}
