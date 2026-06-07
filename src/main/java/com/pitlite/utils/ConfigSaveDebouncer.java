package com.pitlite.utils;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ConfigSaveDebouncer {

    private static boolean dirty;
    private static int ticksSinceDirty;

    public static void markDirty() {
        dirty = true;
        ticksSinceDirty = 0;
    }

    public static void flushNow() {
        if (!dirty) {
            return;
        }
        dirty = false;
        ConfigManager.persistAll();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !dirty) {
            return;
        }
        ticksSinceDirty++;
        if (ticksSinceDirty >= 40) {
            flushNow();
        }
    }
}
