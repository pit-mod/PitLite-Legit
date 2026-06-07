package com.pitlite.utils;

import net.minecraft.entity.player.EntityPlayer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerListStore {

    public enum ListType {
        KOS,
        FRIEND,
        TRUCE
    }

    private static final Map<ListType, Map<String, String>> LISTS = new ConcurrentHashMap<>();
    private static final Map<ListType, Set<String>> NAME_INDEX = new ConcurrentHashMap<>();

    static {
        for (ListType type : ListType.values()) {
            LISTS.put(type, new ConcurrentHashMap<>());
            NAME_INDEX.put(type, ConcurrentHashMap.newKeySet());
        }
    }

    private PlayerListStore() {
    }

    public static void add(ListType type, String name, String uuid) {
        if (uuid == null) {
            return;
        }
        LISTS.get(type).put(ProfileLookup.normalizeUuid(uuid), name);
        if (name != null) {
            NAME_INDEX.get(type).add(ProfileLookup.getCleanName(name).toLowerCase());
        }
    }

    public static void removeByName(ListType type, String name) {
        if (name == null) {
            return;
        }
        String clean = ProfileLookup.getCleanName(name).toLowerCase();
        LISTS.get(type).entrySet().removeIf(e ->
                ProfileLookup.getCleanName(e.getValue()).equalsIgnoreCase(clean));
        NAME_INDEX.get(type).remove(clean);
    }

    public static Map<String, String> get(ListType type) {
        return LISTS.get(type);
    }

    public static void clear(ListType type) {
        LISTS.get(type).clear();
        NAME_INDEX.get(type).clear();
    }

    public static void loadEntry(ListType type, String uuidKey, String displayName) {
        if (uuidKey == null || displayName == null) {
            return;
        }
        LISTS.get(type).put(ProfileLookup.normalizeUuid(uuidKey), displayName);
        NAME_INDEX.get(type).add(ProfileLookup.getCleanName(displayName).toLowerCase());
    }

    public static boolean containsPlayer(ListType type, EntityPlayer player) {
        if (player == null) {
            return false;
        }
        String uuid = ProfileLookup.normalizeUuid(player.getUniqueID().toString());
        Map<String, String> map = LISTS.get(type);
        if (map.containsKey(uuid) || map.containsKey(uuid.replace("-", ""))) {
            return true;
        }
        return containsByName(type, player.getName());
    }

    public static boolean containsByName(ListType type, String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return NAME_INDEX.get(type).contains(ProfileLookup.getCleanName(name).toLowerCase());
    }

    public static boolean containsByUuid(ListType type, String uuid) {
        if (uuid == null) {
            return false;
        }
        return getStoredName(type, uuid) != null;
    }

    public static String getStoredName(ListType type, String uuid) {
        if (uuid == null) {
            return null;
        }
        String normalized = ProfileLookup.normalizeUuid(uuid);
        Map<String, String> map = LISTS.get(type);
        String stored = map.get(normalized);
        if (stored == null) {
            stored = map.get(normalized.replace("-", ""));
        }
        return stored;
    }

    public static ListType getListType(String uuid, String name) {
        if (uuid != null) {
            for (ListType type : ListType.values()) {
                if (containsByUuid(type, uuid)) {
                    return type;
                }
            }
        }
        if (name != null && !name.isEmpty()) {
            for (ListType type : ListType.values()) {
                if (containsByName(type, name)) {
                    return type;
                }
            }
        }
        return null;
    }

    public static String syncDisplayName(ListType type, String uuid, String tabName) {
        if (uuid == null || tabName == null || tabName.isEmpty()) {
            return null;
        }
        String key = ProfileLookup.normalizeUuid(uuid);
        Map<String, String> map = LISTS.get(type);
        String stored = map.get(key);
        if (stored == null) {
            String compact = key.replace("-", "");
            stored = map.get(compact);
            if (stored != null) {
                key = compact;
            }
        }
        if (stored == null || stored.equalsIgnoreCase(tabName)) {
            return null;
        }
        map.put(key, tabName);
        NAME_INDEX.get(type).remove(ProfileLookup.getCleanName(stored).toLowerCase());
        NAME_INDEX.get(type).add(ProfileLookup.getCleanName(tabName).toLowerCase());
        return stored;
    }
}
