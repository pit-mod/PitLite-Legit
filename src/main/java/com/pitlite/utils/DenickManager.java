package com.pitlite.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DenickManager {

    private DenickManager() {
    }

    private static final Map<String, String> denickedPlayers = new ConcurrentHashMap<>();

    public static void put(String nick, String realUsername) {
        if (nick == null || realUsername == null) {
            return;
        }
        removeByNick(nick);
        denickedPlayers.put(nick, realUsername);
    }

    public static String get(String nick) {
        if (nick == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : denickedPlayers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(nick)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static boolean contains(String nick) {
        return get(nick) != null;
    }

    public static void removeByNick(String nick) {
        if (nick == null) {
            return;
        }
        denickedPlayers.entrySet().removeIf(entry -> entry.getKey().equalsIgnoreCase(nick));
    }

    public static void clear() {
        denickedPlayers.clear();
    }

    public static boolean isEmpty() {
        return denickedPlayers.isEmpty();
    }

    public static Map<String, String> getAll() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(denickedPlayers));
    }
}
