package com.pitlite.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class DiscordIpcClient {

    private enum OpCode {
        HANDSHAKE, FRAME, CLOSE, PING, PONG
    }

    private static final int IPC_VERSION = 1;
    private static final int MAX_PIPE_INDEX = 9;

    private final Object writeLock = new Object();
    private RandomAccessFile pipe;
    private Thread readThread;
    private volatile boolean connected;
    private volatile boolean activityAcknowledged;
    private String lastError = "";
    private String lastActivityError = "";

    public String getLastError() {
        if (lastError != null && !lastError.isEmpty()) {
            return lastError;
        }
        if (lastActivityError != null && !lastActivityError.isEmpty()) {
            return lastActivityError;
        }
        return "Unknown error";
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isActivityAcknowledged() {
        return activityAcknowledged;
    }

    public boolean connect(String applicationId) {
        lastError = "";
        lastActivityError = "";
        activityAcknowledged = false;

        if (applicationId == null || applicationId.trim().isEmpty()) {
            lastError = "Application ID is empty";
            return false;
        }

        String clientId = applicationId.trim();
        disconnectQuietly();

        for (int i = 0; i <= MAX_PIPE_INDEX; i++) {
            RandomAccessFile candidate = null;
            try {
                candidate = new RandomAccessFile(pipePath(i), "rw");
                pipe = candidate;
                send(OpCode.HANDSHAKE, handshakePayload(clientId));
                JsonObject response = readFrameJson();
                if (!isHandshakeSuccess(response)) {
                    String message = ipcErrorMessage(response);
                    lastError = message != null ? message : "Discord rejected the Application ID";
                    disconnectQuietly();
                    continue;
                }
                connected = true;
                activityAcknowledged = false;
                startReader();
                return true;
            } catch (IOException ignored) {
                closeFile(candidate);
                pipe = null;
            } catch (Throwable t) {
                closeFile(candidate);
                pipe = null;
                lastError = formatThrowable(t);
            }
        }

        if (lastError == null || lastError.isEmpty()) {
            lastError = "Discord desktop is not running (no discord-ipc pipe found). Open the Discord app, not the browser.";
        }
        return false;
    }

    public void setActivity(String details, String state, String largeImageKey, String largeImageText,
                            long startTimestampEpochSec, boolean showElapsed) {
        if (!connected || pipe == null) {
            return;
        }

        JsonObject activity = new JsonObject();
        if (details != null) {
            activity.addProperty("details", details);
        }
        if (state != null) {
            activity.addProperty("state", state);
        }

        JsonObject assets = new JsonObject();
        if (largeImageKey != null && !largeImageKey.isEmpty()) {
            assets.addProperty("large_image", largeImageKey);
        }
        if (largeImageText != null && !largeImageText.isEmpty()) {
            assets.addProperty("large_text", largeImageText);
        }
        if (assets.entrySet().size() > 0) {
            activity.add("assets", assets);
        }

        if (showElapsed && startTimestampEpochSec > 0L) {
            JsonObject timestamps = new JsonObject();
            timestamps.addProperty("start", startTimestampEpochSec);
            activity.add("timestamps", timestamps);
        }

        JsonObject args = new JsonObject();
        args.addProperty("pid", currentPid());
        args.add("activity", activity);

        JsonObject frame = new JsonObject();
        frame.addProperty("cmd", "SET_ACTIVITY");
        frame.add("args", args);

        try {
            send(OpCode.FRAME, frame);
        } catch (IOException e) {
            lastActivityError = "Failed to send activity: " + e.getMessage();
            connected = false;
        }
    }

    public void clearActivity() {
        if (!connected || pipe == null) {
            return;
        }

        JsonObject args = new JsonObject();
        args.addProperty("pid", currentPid());
        args.add("activity", null);

        JsonObject frame = new JsonObject();
        frame.addProperty("cmd", "SET_ACTIVITY");
        frame.add("args", args);

        try {
            send(OpCode.FRAME, frame);
        } catch (IOException ignored) {
        }
    }

    public void disconnect() {
        disconnectQuietly();
    }

    private void disconnectQuietly() {
        connected = false;
        activityAcknowledged = false;
        Thread thread = readThread;
        readThread = null;
        if (thread != null) {
            thread.interrupt();
        }
        RandomAccessFile file = pipe;
        pipe = null;
        if (file != null) {
            try {
                sendClose(file);
            } catch (IOException ignored) {
            }
            closeFile(file);
        }
    }

    private void sendClose(RandomAccessFile file) throws IOException {
        synchronized (writeLock) {
            byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);
            writePacket(file, OpCode.CLOSE, payload);
        }
    }

    private void startReader() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                readerLoop();
            }
        }, "PitLite-Discord-IPC");
        thread.setDaemon(true);
        readThread = thread;
        thread.start();
    }

    private void readerLoop() {
        while (connected && pipe != null && !Thread.currentThread().isInterrupted()) {
            try {
                JsonObject json = readFrameJson();
                if (json == null) {
                    continue;
                }
                handleIncoming(json);
            } catch (IOException e) {
                if (connected) {
                    lastError = "Discord IPC disconnected";
                }
                connected = false;
                break;
            } catch (Throwable ignored) {
            }
        }
    }

    private void handleIncoming(JsonObject json) {
        if (json.has("evt") && "ERROR".equalsIgnoreCase(json.get("evt").getAsString())) {
            String message = ipcErrorMessage(json);
            if (message != null) {
                lastActivityError = message;
            }
            return;
        }

        if (json.has("evt") && "READY".equalsIgnoreCase(json.get("evt").getAsString())) {
            activityAcknowledged = true;
            return;
        }

        if (!json.has("evt") && json.has("nonce")) {
            activityAcknowledged = true;
            lastActivityError = "";
        }
    }

    private static boolean isHandshakeSuccess(JsonObject response) {
        if (response == null) {
            return false;
        }
        if (response.has("code") && response.get("code").getAsInt() != 0) {
            return false;
        }
        if (response.has("evt") && "ERROR".equalsIgnoreCase(response.get("evt").getAsString())) {
            return false;
        }
        return response.has("cmd") || response.has("data");
    }

    private static String ipcErrorMessage(JsonObject json) {
        if (json == null) {
            return null;
        }
        if (json.has("message")) {
            return json.get("message").getAsString();
        }
        if (json.has("data") && json.get("data").isJsonObject()) {
            JsonObject data = json.getAsJsonObject("data");
            if (data.has("message")) {
                return data.get("message").getAsString();
            }
        }
        return null;
    }

    private static JsonObject handshakePayload(String clientId) {
        JsonObject payload = new JsonObject();
        payload.addProperty("v", IPC_VERSION);
        payload.addProperty("client_id", clientId);
        return payload;
    }

    private void send(OpCode op, JsonObject data) throws IOException {
        RandomAccessFile file = pipe;
        if (file == null) {
            throw new IOException("Not connected");
        }
        data.addProperty("nonce", UUID.randomUUID().toString());
        byte[] bytes = data.toString().getBytes(StandardCharsets.UTF_8);
        synchronized (writeLock) {
            writePacket(file, op, bytes);
        }
    }

    private static void writePacket(RandomAccessFile file, OpCode op, byte[] jsonBytes) throws IOException {
        ByteBuffer header = ByteBuffer.allocate(8);
        header.putInt(Integer.reverseBytes(op.ordinal()));
        header.putInt(Integer.reverseBytes(jsonBytes.length));
        file.write(header.array());
        file.write(jsonBytes);
    }

    private JsonObject readFrameJson() throws IOException {
        RandomAccessFile file = pipe;
        if (file == null) {
            throw new IOException("Not connected");
        }

        waitForData(file);

        int opInt = Integer.reverseBytes(file.readInt());
        int length = Integer.reverseBytes(file.readInt());
        if (length < 0 || length > 1_000_000) {
            throw new IOException("Invalid IPC frame length");
        }

        byte[] data = new byte[length];
        file.readFully(data);

        if (opInt == OpCode.CLOSE.ordinal()) {
            connected = false;
            return null;
        }

        String text = new String(data, StandardCharsets.UTF_8).trim();
        if (text.isEmpty()) {
            return new JsonObject();
        }
        return new JsonParser().parse(text).getAsJsonObject();
    }

    private static void waitForData(RandomAccessFile file) throws IOException {
        int spins = 0;
        while (file.length() == 0 && spins < 200) {
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted");
            }
            spins++;
        }
        if (file.length() == 0) {
            throw new IOException("Discord IPC timed out");
        }
    }

    private static String pipePath(int index) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return "\\\\?\\pipe\\discord-ipc-" + index;
        }
        String base = System.getenv("XDG_RUNTIME_DIR");
        if (base == null || base.isEmpty()) {
            base = System.getenv("TMPDIR");
        }
        if (base == null || base.isEmpty()) {
            base = "/tmp";
        }
        return base + "/discord-ipc-" + index;
    }

    private static int currentPid() {
        String runtime = ManagementFactory.getRuntimeMXBean().getName();
        int at = runtime.indexOf('@');
        if (at <= 0) {
            return 0;
        }
        try {
            return Integer.parseInt(runtime.substring(0, at));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void closeFile(RandomAccessFile file) {
        if (file == null) {
            return;
        }
        try {
            file.close();
        } catch (IOException ignored) {
        }
    }

    private static String formatThrowable(Throwable t) {
        String name = t.getClass().getSimpleName();
        if (t.getMessage() != null && !t.getMessage().isEmpty()) {
            return name + ": " + t.getMessage();
        }
        return name;
    }
}
