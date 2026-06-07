package com.pitlite.utils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class MojangCache {

    public enum NickStatus {
        UNKNOWN,
        REAL,
        NICK,
        ERROR
    }

    private static final String PROFILE_API = "https://api.mojang.com/users/profiles/minecraft/";
    private static final Map<String, String> UUID_BY_NAME = new ConcurrentHashMap<>();
    private static final Map<String, NickStatus> NICK_BY_NAME = new ConcurrentHashMap<>();

    private MojangCache() {
    }

    public static String getCachedUuid(String name) {
        if (name == null) {
            return null;
        }
        return UUID_BY_NAME.get(ProfileLookup.getCleanName(name));
    }

    public static NickStatus getCachedNickStatus(String name) {
        if (name == null) {
            return NickStatus.UNKNOWN;
        }
        return NICK_BY_NAME.getOrDefault(name, NickStatus.UNKNOWN);
    }

    public static String fetchUuid(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String clean = ProfileLookup.getCleanName(name);
        String cached = UUID_BY_NAME.get(clean);
        if (cached != null) {
            return cached;
        }
        String resolved = requestUuid(name);
        if (resolved != null) {
            UUID_BY_NAME.put(clean, resolved);
        }
        return resolved;
    }

    public static void fetchUuidAsync(String name, java.util.function.Consumer<String> callback) {
        if (name == null || name.isEmpty()) {
            if (callback != null) {
                callback.accept(null);
            }
            return;
        }
        String clean = ProfileLookup.getCleanName(name);
        String cached = UUID_BY_NAME.get(clean);
        if (cached != null) {
            if (callback != null) {
                callback.accept(cached);
            }
            return;
        }
        CompletableFuture.runAsync(() -> {
            String resolved = requestUuid(name);
            if (resolved != null) {
                UUID_BY_NAME.put(clean, resolved);
            }
            if (callback != null) {
                net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> callback.accept(resolved));
            }
        });
    }

    public static void checkNickStatusAsync(String name, Consumer<NickStatus> callback) {
        if (name == null || name.isEmpty()) {
            if (callback != null) {
                callback.accept(NickStatus.ERROR);
            }
            return;
        }

        NickStatus cached = NICK_BY_NAME.get(name);
        if (cached != null && cached != NickStatus.UNKNOWN) {
            if (callback != null) {
                callback.accept(cached);
            }
            return;
        }

        CompletableFuture.runAsync(() -> {
            NickStatus status = requestNickStatus(name);
            NICK_BY_NAME.put(name, status);
            if (callback != null) {
                net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> callback.accept(status));
            }
        });
    }

    private static NickStatus requestNickStatus(String name) {
        try {
            HttpURLConnection conn = openGet(PROFILE_API + name);
            int code = conn.getResponseCode();
            if (code == 200) {
                return NickStatus.REAL;
            }
            if (code == 204 || code == 404) {
                return NickStatus.NICK;
            }
            return NickStatus.ERROR;
        } catch (Exception e) {
            return NickStatus.ERROR;
        }
    }

    private static String requestUuid(String name) {
        try {
            HttpURLConnection conn = openGet(PROFILE_API + name);
            if (conn.getResponseCode() != 200) {
                return null;
            }
            try (InputStream in = conn.getInputStream(); java.util.Scanner sc = new java.util.Scanner(in)) {
                String response = sc.useDelimiter("\\A").hasNext() ? sc.next() : "";
                int idIndex = response.indexOf("\"id\"");
                if (idIndex == -1) {
                    return null;
                }
                int start = response.indexOf("\"", idIndex + 4) + 1;
                int end = response.indexOf("\"", start);
                if (start <= 0 || end <= start) {
                    return null;
                }
                String raw = response.substring(start, end);
                if (raw.length() != 32) {
                    return ProfileLookup.normalizeUuid(raw);
                }
                return raw.substring(0, 8) + "-"
                        + raw.substring(8, 12) + "-"
                        + raw.substring(12, 16) + "-"
                        + raw.substring(16, 20) + "-"
                        + raw.substring(20);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static HttpURLConnection openGet(String urlString) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        return conn;
    }
}
