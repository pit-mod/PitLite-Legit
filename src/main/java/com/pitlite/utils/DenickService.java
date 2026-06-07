package com.pitlite.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DenickService {
    private static final String PITPANDA_API = "https://pitpanda.rocks/api/itemsearch/nonce";
    private static final String MOJANG_API = "https://sessionserver.mojang.com/session/minecraft/profile/";

    private static final ConcurrentLinkedQueue<DenickRequest> DENICK_QUEUE = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean WORKER_RUNNING = new AtomicBoolean(false);

    private DenickService() {
    }

    public static int getQueueSize() {
        return DENICK_QUEUE.size() + (WORKER_RUNNING.get() ? 1 : 0);
    }

    public static void denickByName(String targetName) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            chat("\u00a7cYou must be in a world to denick players.");
            return;
        }

        EntityPlayer target = LobbyPlayerIndex.findByName(targetName);
        if (target == null) {
            chat("\u00a7cPlayer not found!");
            return;
        }

        String nick = target.getName();
        String cached = DenickManager.get(nick);
        if (cached != null) {
            chat("\u00a78[\u00a7bPitLite\u00a78] - \u00a7aAlready denicked: " + nick + " -> " + cached);
            return;
        }

        chat("\u00a78[\u00a7bPitLite\u00a78] - \u00a79Denicking " + nick + " \u00a79(queued).");
        enqueueDenick(nick, collectNonces(target), null);
    }

    public static void denickPlayerAsync(EntityPlayer target, String nickName) {
        denickPlayerAsync(target, nickName, null);
    }

    public interface DenickCallback {
        void onComplete(String nick, String realUsername);
    }

    public static void denickPlayerAsync(EntityPlayer target, String nickName, DenickCallback callback) {
        if (nickName == null) {
            return;
        }
        List<Integer> nonces = target != null ? collectNonces(target) : new ArrayList<>();
        enqueueDenick(nickName, nonces, callback);
    }

    private static void enqueueDenick(String nickName, List<Integer> nonces, DenickCallback callback) {
        DENICK_QUEUE.offer(new DenickRequest(nickName, new ArrayList<>(nonces), callback));
        drainQueue();
    }

    private static final class DenickRequest {
        final String nickName;
        final List<Integer> nonces;
        final DenickCallback callback;

        DenickRequest(String nickName, List<Integer> nonces, DenickCallback callback) {
            this.nickName = nickName;
            this.nonces = nonces;
            this.callback = callback;
        }
    }

    private static void drainQueue() {
        if (!WORKER_RUNNING.compareAndSet(false, true)) {
            return;
        }
        new Thread(() -> {
            try {
                DenickRequest next;
                while ((next = DENICK_QUEUE.poll()) != null) {
                    final String nick = next.nickName;
                    final DenickCallback callback = next.callback;
                    final String result = resolveRealUsernameFromNonces(next.nonces);
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        if (callback != null) {
                            callback.onComplete(nick, result);
                            return;
                        }
                        if (result == null || result.isEmpty()) {
                            chat("\u00a7cCould not denick " + nick + ".");
                            return;
                        }
                        DenickManager.put(nick, result);
                        chat("\u00a78[\u00a7bPitLite\u00a78] - \u00a7aDenicked: " + nick + " -> " + result);
                    });
                    Thread.sleep(800L);
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                WORKER_RUNNING.set(false);
                if (!DENICK_QUEUE.isEmpty()) {
                    drainQueue();
                }
            }
        }, "PitLite-Denick").start();
    }

    public static String resolveRealUsername(EntityPlayer player) {
        return resolveRealUsernameFromNonces(collectNonces(player));
    }

    private static String resolveRealUsernameFromNonces(List<Integer> nonces) {
        if (nonces == null || nonces.isEmpty()) {
            return null;
        }

        String ownerUUID = null;
        for (Integer nonce : nonces) {
            ownerUUID = getOwnerFromPitPanda(nonce);
            if (ownerUUID != null && !ownerUUID.isEmpty()) {
                break;
            }
        }

        if (ownerUUID == null || ownerUUID.isEmpty()) {
            return null;
        }

        return getUsernameFromUUID(ownerUUID.replace("-", ""));
    }

    public static List<Integer> collectNonces(EntityPlayer player) {
        return DenickUtils.collectDenickNonces(player);
    }

    private static String getOwnerFromPitPanda(int nonce) {
        try {
            URL url = new URL(PITPANDA_API + nonce + "?key=" + PitLiteApiConfig.getPitPandaKey());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder responseStr = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseStr.append(line);
            }
            reader.close();

            JsonObject response = new JsonParser().parse(responseStr.toString()).getAsJsonObject();
            if (response.has("success") && response.get("success").getAsBoolean()) {
                if (response.has("items") && response.getAsJsonArray("items").size() > 0) {
                    JsonElement ownerElement = response.getAsJsonArray("items").get(0).getAsJsonObject().get("owner");
                    if (ownerElement != null && !ownerElement.isJsonNull()) {
                        return ownerElement.getAsString();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String getUsernameFromUUID(String uuid) {
        try {
            URL url = new URL(MOJANG_API + uuid);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            JsonObject response = new JsonParser().parse(reader).getAsJsonObject();
            reader.close();
            return response.get("name").getAsString();
        } catch (Exception ignored) {
        }
        return null;
    }

    private static void chat(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
}
