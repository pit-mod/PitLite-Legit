package com.pitlite.utils;

public final class DiscordRpcManager {
    private static final int MAX_FIELD_LEN = 128;
    private static final int PRESENCE_REFRESH_TICKS = 40;

    private static final DiscordIpcClient client = new DiscordIpcClient();

    private static boolean initialized = false;
    private static long startTimestamp = 0L;
    private static String lastError = "";

    private static String pendingDetails = "In Menu";
    private static String pendingState = "with PitLite";
    private static boolean pendingShowElapsed = true;
    private static boolean presenceDirty = true;
    private static int callbackTicks = 0;

    private DiscordRpcManager() {
    }

    public static String getLastError() {
        if (lastError != null && !lastError.isEmpty()) {
            return lastError;
        }
        return client.getLastError();
    }

    public static synchronized boolean start(String applicationId) {
        lastError = "";
        callbackTicks = 0;
        presenceDirty = true;

        if (applicationId == null || applicationId.trim().isEmpty()) {
            lastError = "Application ID is empty";
            return false;
        }

        if (initialized && client.isConnected()) {
            return true;
        }

        shutdownQuietly();

        if (!client.connect(applicationId)) {
            lastError = client.getLastError();
            return false;
        }

        startTimestamp = System.currentTimeMillis() / 1000L;
        initialized = true;
        presenceDirty = true;
        flushPresence();
        return true;
    }

    public static synchronized void updatePresence(boolean showElapsed, String details, String state) {
        if (!initialized) {
            return;
        }

        pendingShowElapsed = showElapsed;
        pendingDetails = truncate(details);
        pendingState = truncate(state);
        presenceDirty = true;
        flushPresence();
    }

    public static String getLocationDetails() {
        String title = Utils.getScoreboardTitle();
        if (title.equalsIgnoreCase("THE HYPIXEL PIT")) {
            return "In the Pit";
        }
        if (title.equalsIgnoreCase("HYPIXEL")) {
            return "In Pit Lobby";
        }
        if (title.isEmpty()) {
            return "In Menu";
        }
        return truncate(title);
    }

    public static void runCallbacks() {
        if (!initialized || !client.isConnected()) {
            return;
        }

        callbackTicks++;
        if (callbackTicks % PRESENCE_REFRESH_TICKS == 0) {
            presenceDirty = true;
        }

        flushPresence();
    }

    public static synchronized void shutdown() {
        if (!initialized) {
            return;
        }
        shutdownQuietly();
    }

    private static void flushPresence() {
        if (!initialized || !client.isConnected() || !presenceDirty) {
            return;
        }

        client.setActivity(
                pendingDetails == null ? "" : pendingDetails,
                pendingState == null ? "" : pendingState,
                "thepit",
                "PitLite",
                pendingShowElapsed ? startTimestamp : 0L,
                pendingShowElapsed);
        presenceDirty = false;
    }

    private static void shutdownQuietly() {
        if (initialized) {
            client.clearActivity();
        }
        client.disconnect();
        initialized = false;
        presenceDirty = true;
        callbackTicks = 0;
        startTimestamp = 0L;
    }

    public static boolean isRunning() {
        return initialized && client.isConnected();
    }

    public static boolean isDiscordReady() {
        return initialized && client.isConnected();
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        if (value.length() <= MAX_FIELD_LEN) {
            return value;
        }
        return value.substring(0, MAX_FIELD_LEN);
    }
}
