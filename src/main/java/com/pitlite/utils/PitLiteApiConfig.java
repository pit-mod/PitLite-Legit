package com.pitlite.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public final class PitLiteApiConfig {

    private static final File API_FILE = new File("config/pitlegit/api.json");
    private static final String DEFAULT_PITPANDA_KEY = "617c9c76-e5a5-4213-a6b9-aded74ae64c8";

    private static String pitPandaKey = DEFAULT_PITPANDA_KEY;

    private PitLiteApiConfig() {
    }

    public static void load() {
        ConfigManager.init();
        if (!API_FILE.exists()) {
            pitPandaKey = DEFAULT_PITPANDA_KEY;
            save();
            return;
        }
        try (FileReader reader = new FileReader(API_FILE)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            if (root.has("pitPandaKey")) {
                String key = root.get("pitPandaKey").getAsString();
                if (key != null && !key.trim().isEmpty()) {
                    pitPandaKey = key.trim();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getPitPandaKey() {
        return pitPandaKey;
    }

    public static void setPitPandaKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            pitPandaKey = DEFAULT_PITPANDA_KEY;
        } else {
            pitPandaKey = key.trim();
        }
        save();
    }

    public static void save() {
        ConfigManager.init();
        JsonObject root = new JsonObject();
        root.addProperty("pitPandaKey", pitPandaKey);
        try (FileWriter writer = new FileWriter(API_FILE)) {
            new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
