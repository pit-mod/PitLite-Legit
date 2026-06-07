package com.pitlite.utils;

import com.pitlite.PitLite;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ConfigLifecycleHandler {

    private static boolean appliedLoadedStates;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (appliedLoadedStates || event.phase != TickEvent.Phase.END) {
            return;
        }
        if (net.minecraft.client.Minecraft.getMinecraft().thePlayer == null) {
            return;
        }
        appliedLoadedStates = true;
        PitLite.moduleManager.applyLoadedModuleStates();
    }
}
