package com.pitlite.utils;

import net.minecraft.entity.player.EntityPlayer;

import java.util.Map;

public class KOSManager {
    private static boolean enabled = true;

    public static void add(String name, String uuid) {
        PlayerListStore.add(PlayerListStore.ListType.KOS, name, uuid);
    }

    public static void removeByName(String name) {
        PlayerListStore.removeByName(PlayerListStore.ListType.KOS, name);
    }

    public static Map<String, String> getKosPlayers() {
        return PlayerListStore.get(PlayerListStore.ListType.KOS);
    }

    public static void clear() {
        PlayerListStore.clear(PlayerListStore.ListType.KOS);
    }

    public static void loadEntry(String uuidKey, String displayName) {
        PlayerListStore.loadEntry(PlayerListStore.ListType.KOS, uuidKey, displayName);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean state) {
        enabled = state;
    }

    public static boolean isKOS(EntityPlayer player) {
        return enabled && PlayerListStore.containsPlayer(PlayerListStore.ListType.KOS, player);
    }

    public static boolean isKOSByName(String name) {
        return enabled && PlayerListStore.containsByName(PlayerListStore.ListType.KOS, name);
    }

    public static boolean isKOSByUuid(String uuid) {
        return enabled && PlayerListStore.containsByUuid(PlayerListStore.ListType.KOS, uuid);
    }
}
