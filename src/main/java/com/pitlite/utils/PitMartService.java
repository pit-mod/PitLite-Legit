package com.pitlite.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PitMartService {

    private static final String PITPANDA_ITEM = "https://pitpanda.rocks/api/item/";
    private static final String PITPANDA_ITEM_SEARCH = "https://pitpanda.rocks/api/itemsearch/nonce";
    private static final String MOJANG_API = "https://sessionserver.mojang.com/session/minecraft/profile/";

    private static final long CACHE_TTL_MS = 5L * 60L * 1000L;
    private static final int MAX_DISPLAY_OWNERS = 20;
    private static final int MOJANG_CONNECT_MS = 2500;
    private static final int MOJANG_READ_MS = 2500;
    private static final Map<String, CachedHistory> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> USERNAME_BY_UUID = new ConcurrentHashMap<>();
    private static final ExecutorService USERNAME_EXECUTOR = Executors.newFixedThreadPool(8);

    public enum FetchStatus {
        OK,
        NOT_INDEXED,
        BLOCKED,
        ERROR
    }

    private PitMartService() {
    }

    public static final class OwnerRecord {
        public final String uuid;
        public final String username;
        public final long seenAtMs;

        public OwnerRecord(String uuid, String username, long seenAtMs) {
            this.uuid = uuid;
            this.username = username;
            this.seenAtMs = seenAtMs;
        }
    }

    public static final class OwnerHistoryResult {
        public final String itemId;
        public final int nonce;
        public final String itemName;
        public final List<OwnerRecord> owners;
        public final int totalOwners;
        public final FetchStatus status;

        public OwnerHistoryResult(String itemId, int nonce, String itemName, List<OwnerRecord> owners,
                                  FetchStatus status) {
            this(itemId, nonce, itemName, owners, owners != null ? owners.size() : 0, status);
        }

        public OwnerHistoryResult(String itemId, int nonce, String itemName, List<OwnerRecord> owners,
                                  int totalOwners, FetchStatus status) {
            this.itemId = itemId;
            this.nonce = nonce;
            this.itemName = itemName;
            this.owners = owners;
            this.totalOwners = totalOwners;
            this.status = status;
        }
    }

    private static final class CachedHistory {
        final OwnerHistoryResult result;
        final long fetchedAt;

        CachedHistory(OwnerHistoryResult result, long fetchedAt) {
            this.result = result;
            this.fetchedAt = fetchedAt;
        }
    }

    public static OwnerHistoryResult peekCachedByNonce(int nonce) {
        if (nonce == 0) {
            return null;
        }
        return getCached("nonce:" + nonce);
    }

    public static OwnerHistoryResult fetchByNonce(int nonce) {
        if (nonce == 0) {
            return null;
        }
        String cacheKey = "nonce:" + nonce;
        OwnerHistoryResult cached = getCached(cacheKey);
        if (cached != null) {
            return cached;
        }

        OwnerHistoryResult fast = fetchFromPitPandaNonceSearch(nonce, false);
        if (fast == null || fast.status != FetchStatus.OK) {
            return fast;
        }

        OwnerHistoryResult resolved = withResolvedUsernames(fast);
        putCache(cacheKey, resolved);
        if (resolved.itemId != null && !resolved.itemId.isEmpty()) {
            putCache("id:" + resolved.itemId, resolved);
        }
        return resolved;
    }

    public static OwnerHistoryResult fetchByNonceFast(int nonce) {
        if (nonce == 0) {
            return null;
        }
        OwnerHistoryResult cached = getCached("nonce:" + nonce);
        if (cached != null) {
            return cached;
        }
        return fetchFromPitPandaNonceSearch(nonce, false);
    }

    public static OwnerHistoryResult withResolvedUsernames(OwnerHistoryResult result) {
        if (result == null || result.owners == null || result.owners.isEmpty()) {
            return result;
        }
        List<OwnerRecord> resolved = resolveUsernamesParallel(result.owners);
        return new OwnerHistoryResult(result.itemId, result.nonce, result.itemName, resolved,
                result.totalOwners, result.status);
    }

    public static OwnerHistoryResult fetchByMongoId(String mongoId) {
        if (mongoId == null || mongoId.isEmpty()) {
            return null;
        }
        String cacheKey = "id:" + mongoId;
        OwnerHistoryResult cached = getCached(cacheKey);
        if (cached != null) {
            return cached;
        }
        OwnerHistoryResult result = fetchFromPitPandaItemId(mongoId, -1, false);
        if (result != null && result.status == FetchStatus.OK) {
            result = withResolvedUsernames(result);
            putCache(cacheKey, result);
            if (result.nonce > 0) {
                putCache("nonce:" + result.nonce, result);
            }
        }
        return result;
    }

    private static OwnerHistoryResult fetchFromPitPandaNonceSearch(int nonce, boolean resolveNames) {
        String url = PITPANDA_ITEM_SEARCH + nonce + "?key=" + PitLiteApiConfig.getPitPandaKey() + "&raw=true";
        JsonElement root = PitHttp.getJson(url);
        if (root == null) {
            return new OwnerHistoryResult(null, nonce, null, Collections.emptyList(), FetchStatus.ERROR);
        }
        if (!root.isJsonObject()) {
            return null;
        }

        JsonObject response = root.getAsJsonObject();
        if (response.has("success") && !response.get("success").getAsBoolean()) {
            return new OwnerHistoryResult(null, nonce, null, Collections.emptyList(), FetchStatus.NOT_INDEXED);
        }
        if (!response.has("items") || !response.get("items").isJsonArray()) {
            return new OwnerHistoryResult(null, nonce, null, Collections.emptyList(), FetchStatus.NOT_INDEXED);
        }

        JsonArray items = response.getAsJsonArray("items");
        if (items.size() == 0) {
            return new OwnerHistoryResult(null, nonce, null, Collections.emptyList(), FetchStatus.NOT_INDEXED);
        }

        return parsePitPandaItem(items.get(0).getAsJsonObject(), nonce, resolveNames);
    }

    private static OwnerHistoryResult fetchFromPitPandaItemId(String mongoId, int knownNonce, boolean resolveNames) {
        String url = PITPANDA_ITEM + mongoId + "?key=" + PitLiteApiConfig.getPitPandaKey() + "&raw=true";
        JsonElement root = PitHttp.getJson(url);
        if (root == null) {
            return new OwnerHistoryResult(mongoId, knownNonce, null, Collections.emptyList(), FetchStatus.ERROR);
        }
        if (!root.isJsonObject()) {
            return null;
        }

        JsonObject response = root.getAsJsonObject();
        if (response.has("success") && !response.get("success").getAsBoolean()) {
            return new OwnerHistoryResult(mongoId, knownNonce, null, Collections.emptyList(), FetchStatus.NOT_INDEXED);
        }

        JsonObject itemDoc = null;
        if (response.has("item") && response.get("item").isJsonObject()) {
            itemDoc = response.getAsJsonObject("item");
        }
        if (itemDoc == null) {
            return new OwnerHistoryResult(mongoId, knownNonce, null, Collections.emptyList(), FetchStatus.NOT_INDEXED);
        }

        return parsePitPandaItem(itemDoc, knownNonce, resolveNames);
    }

    private static OwnerHistoryResult parsePitPandaItem(JsonObject itemDoc, int knownNonce, boolean resolveNames) {
        String itemId = extractMongoId(itemDoc);
        int nonce = knownNonce;
        if (nonce <= 0 && itemDoc.has("nonce") && !itemDoc.get("nonce").isJsonNull()) {
            nonce = itemDoc.get("nonce").getAsInt();
        }

        String itemName = extractItemName(itemDoc);
        List<OwnerRecord> allOwners = parseOwners(itemDoc);
        if (allOwners.isEmpty()) {
            return new OwnerHistoryResult(itemId, nonce, itemName, Collections.emptyList(), FetchStatus.NOT_INDEXED);
        }

        int totalOwners = allOwners.size();
        List<OwnerRecord> recentOwners = selectMostRecentOwners(allOwners, MAX_DISPLAY_OWNERS);
        if (resolveNames) {
            recentOwners = resolveUsernamesParallel(recentOwners);
        }
        return new OwnerHistoryResult(itemId, nonce, itemName, recentOwners, totalOwners, FetchStatus.OK);
    }

    private static String extractMongoId(JsonObject itemDoc) {
        if (itemDoc.has("_id") && !itemDoc.get("_id").isJsonNull()) {
            JsonElement id = itemDoc.get("_id");
            if (id.isJsonPrimitive()) {
                return id.getAsString();
            }
            if (id.isJsonObject() && id.getAsJsonObject().has("$oid")) {
                return id.getAsJsonObject().get("$oid").getAsString();
            }
        }
        if (itemDoc.has("id") && !itemDoc.get("id").isJsonNull() && itemDoc.get("id").isJsonPrimitive()) {
            String value = itemDoc.get("id").getAsString();
            if (value.length() == 24 && value.matches("[0-9a-fA-F]+")) {
                return value;
            }
        }
        return null;
    }

    private static List<OwnerRecord> parseOwners(JsonObject itemDoc) {
        List<OwnerRecord> records = new ArrayList<>();

        if (itemDoc.has("owners") && itemDoc.get("owners").isJsonArray()) {
            for (JsonElement element : itemDoc.getAsJsonArray("owners")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                OwnerRecord record = parseOwnerEntry(element.getAsJsonObject());
                if (record != null) {
                    records.add(record);
                }
            }
        }

        if (records.isEmpty() && itemDoc.has("owner") && !itemDoc.get("owner").isJsonNull()) {
            String ownerUuid = normalizeUuid(itemDoc.get("owner").getAsString());
            if (ownerUuid != null) {
                records.add(new OwnerRecord(ownerUuid, ownerUuid, 0L));
            }
        }

        return records;
    }

    private static List<OwnerRecord> selectMostRecentOwners(List<OwnerRecord> owners, int max) {
        if (owners.isEmpty() || max <= 0) {
            return Collections.emptyList();
        }
        List<OwnerRecord> chronological = reversedOldestFirst(owners);
        if (chronological.size() <= max) {
            return new ArrayList<>(chronological);
        }
        return new ArrayList<>(chronological.subList(chronological.size() - max, chronological.size()));
    }

    public static List<OwnerRecord> resolveUsernamesParallel(List<OwnerRecord> records) {
        if (records == null || records.isEmpty()) {
            return records;
        }

        OwnerRecord[] resolved = new OwnerRecord[records.size()];
        CompletableFuture<?>[] tasks = new CompletableFuture[records.size()];

        for (int i = 0; i < records.size(); i++) {
            final int index = i;
            final OwnerRecord record = records.get(i);
            tasks[i] = CompletableFuture.runAsync(() -> resolved[index] = resolveOwnerRecord(record), USERNAME_EXECUTOR);
        }

        CompletableFuture.allOf(tasks).join();

        List<OwnerRecord> output = new ArrayList<>(records.size());
        Collections.addAll(output, resolved);
        return output;
    }

    private static OwnerRecord resolveOwnerRecord(OwnerRecord record) {
        String username = record.username;
        if (username == null || username.isEmpty() || username.equals(record.uuid)) {
            username = lookupUsername(record.uuid);
        }
        if (username == null || username.isEmpty()) {
            username = record.uuid;
        }
        return new OwnerRecord(record.uuid, username, record.seenAtMs);
    }

    private static List<OwnerRecord> resolveUsernames(List<OwnerRecord> records) {
        return resolveUsernamesParallel(records);
    }

    private static String lookupUsername(String uuidNoDashes) {
        if (uuidNoDashes == null || uuidNoDashes.isEmpty()) {
            return null;
        }
        String cached = USERNAME_BY_UUID.get(uuidNoDashes);
        if (cached != null) {
            return cached;
        }
        String resolved = resolveUsername(uuidNoDashes);
        if (resolved != null) {
            USERNAME_BY_UUID.put(uuidNoDashes, resolved);
        }
        return resolved;
    }

    private static OwnerRecord parseOwnerEntry(JsonObject entry) {
        String uuid = null;
        if (entry.has("uuid") && !entry.get("uuid").isJsonNull()) {
            uuid = normalizeUuid(entry.get("uuid").getAsString());
        } else if (entry.has("owner") && !entry.get("owner").isJsonNull()) {
            uuid = normalizeUuid(entry.get("owner").getAsString());
        }
        if (uuid == null) {
            return null;
        }

        String username = null;
        if (entry.has("username") && !entry.get("username").isJsonNull()) {
            username = entry.get("username").getAsString();
        } else if (entry.has("name") && !entry.get("name").isJsonNull()) {
            username = entry.get("name").getAsString();
        }
        if (username == null || username.isEmpty()) {
            username = uuid;
        }

        long seenAt = parseTime(entry);
        return new OwnerRecord(uuid, username, seenAt);
    }

    private static long parseTime(JsonObject entry) {
        if (entry.has("time") && !entry.get("time").isJsonNull()) {
            JsonElement time = entry.get("time");
            if (time.isJsonPrimitive()) {
                if (time.getAsJsonPrimitive().isNumber()) {
                    long value = time.getAsLong();
                    return value < 1_000_000_000_000L ? value * 1000L : value;
                }
                try {
                    SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                    iso.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Date parsed = iso.parse(time.getAsString());
                    return parsed != null ? parsed.getTime() : 0L;
                } catch (Exception ignored) {
                }
            }
        }
        if (entry.has("timestamp") && entry.get("timestamp").isJsonPrimitive()) {
            long value = entry.get("timestamp").getAsLong();
            return value < 1_000_000_000_000L ? value * 1000L : value;
        }
        return 0L;
    }

    private static String extractItemName(JsonObject itemDoc) {
        if (itemDoc.has("item") && itemDoc.get("item").isJsonObject()) {
            JsonObject item = itemDoc.getAsJsonObject("item");
            if (item.has("name") && !item.get("name").isJsonNull()) {
                return item.get("name").getAsString();
            }
        }
        return "Mystic Item";
    }

    private static String normalizeUuid(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        return raw.replace("-", "").toLowerCase(Locale.ROOT);
    }

    public static String resolveUsername(String uuidNoDashes) {
        if (uuidNoDashes == null || uuidNoDashes.isEmpty()) {
            return null;
        }
        JsonElement root = PitHttp.getJson(MOJANG_API + uuidNoDashes, MOJANG_CONNECT_MS, MOJANG_READ_MS);
        if (root == null || !root.isJsonObject()) {
            return null;
        }
        JsonObject response = root.getAsJsonObject();
        if (response.has("name")) {
            return response.get("name").getAsString();
        }
        return null;
    }

    public static String formatOwnerLine(OwnerRecord record, int index) {
        String name = formatDisplayName(record);
        if (record.seenAtMs > 0L) {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            return "\u00a77" + (index + 1) + ". \u00a7f" + name + " \u00a78(" + fmt.format(new Date(record.seenAtMs)) + ")";
        }
        return "\u00a77" + (index + 1) + ". \u00a7f" + name;
    }

    private static String formatDisplayName(OwnerRecord record) {
        String name = record.username != null ? record.username : record.uuid;
        if (name != null && record.uuid != null && name.equals(record.uuid) && name.length() >= 8) {
            return "\u00a78" + name.substring(0, 8) + "...";
        }
        return name;
    }

    public static void appendOwnerHistoryTooltip(List<String> tooltip, OwnerHistoryResult result, boolean loading) {
        tooltip.add("");
        tooltip.add("\u00a78\u00a7m--------------------");
        tooltip.add("\u00a7bOwner History");
        if (loading && (result == null || result.status == null)) {
            tooltip.add("\u00a77Loading...");
            return;
        }
        if (result == null || result.owners.isEmpty()) {
            if (result != null && result.status == FetchStatus.ERROR) {
                tooltip.add("\u00a7cCould not reach item database");
            } else {
                tooltip.add("\u00a7cItem not indexed yet");
                tooltip.add("\u00a77Trade it once or search on pitpanda.rocks");
            }
            return;
        }
        List<OwnerRecord> owners = result.owners;
        int startIndex = getDisplayStartIndex(result);
        for (int i = 0; i < owners.size(); i++) {
            tooltip.add(formatOwnerLine(owners.get(i), startIndex + i));
        }
        if (result.totalOwners > owners.size()) {
            tooltip.add("\u00a77... " + (result.totalOwners - owners.size()) + " older owners hidden");
        }
        if (hasUnresolvedNames(owners)) {
            tooltip.add("\u00a77Resolving names...");
        }
    }

    private static boolean hasUnresolvedNames(List<OwnerRecord> owners) {
        for (OwnerRecord owner : owners) {
            if (owner.username != null && owner.uuid != null && owner.username.equals(owner.uuid)) {
                return true;
            }
        }
        return false;
    }

    public static int getDisplayStartIndex(OwnerHistoryResult result) {
        if (result == null || result.owners == null || result.owners.isEmpty()) {
            return 0;
        }
        if (result.totalOwners > result.owners.size()) {
            return result.totalOwners - result.owners.size();
        }
        return 0;
    }

    public static List<OwnerRecord> reversedOldestFirst(List<OwnerRecord> owners) {
        List<OwnerRecord> copy = new ArrayList<>(owners);
        Collections.reverse(copy);
        return copy;
    }

    private static OwnerHistoryResult getCached(String key) {
        CachedHistory cached = CACHE.get(key);
        if (cached == null) {
            return null;
        }
        if (System.currentTimeMillis() - cached.fetchedAt > CACHE_TTL_MS) {
            CACHE.remove(key);
            return null;
        }
        return cached.result;
    }

    private static void putCache(String key, OwnerHistoryResult result) {
        if (result.status == FetchStatus.OK) {
            CACHE.put(key, new CachedHistory(result, System.currentTimeMillis()));
        }
    }
}
