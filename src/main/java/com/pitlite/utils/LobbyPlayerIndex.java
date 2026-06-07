package com.pitlite.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class LobbyPlayerIndex {

    public static final LobbyPlayerIndex INSTANCE = new LobbyPlayerIndex();

    private static Map<String, EntityPlayer> byCleanName = Collections.emptyMap();
    private static Map<String, EntityPlayer> byDisplayName = Collections.emptyMap();

    private LobbyPlayerIndex() {
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            byCleanName = Collections.emptyMap();
            byDisplayName = Collections.emptyMap();
            return;
        }

        Map<String, EntityPlayer> clean = new HashMap<>();
        Map<String, EntityPlayer> display = new HashMap<>();
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == null) {
                continue;
            }
            display.put(player.getName().toLowerCase(), player);
            clean.put(ProfileLookup.getCleanName(player.getName()).toLowerCase(), player);
        }
        byCleanName = clean;
        byDisplayName = display;
    }

    public static EntityPlayer findByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        EntityPlayer p = byDisplayName.get(name.toLowerCase());
        if (p != null) {
            return p;
        }
        return byCleanName.get(ProfileLookup.getCleanName(name).toLowerCase());
    }

    public static Map<String, EntityPlayer> snapshotByDisplayName() {
        return byDisplayName;
    }
}
