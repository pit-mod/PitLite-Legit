package com.pitlite.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class HudPositionManager {
    private static final File POSITIONS_FILE = new File("config/pitlegit/hud_positions.json");
    private static final Map<String, double[]> positions = new HashMap<>();

    public static double getX(String key, double defaultX) {
        double[] pos = positions.get(key);
        return pos != null ? pos[0] : defaultX;
    }

    public static double getY(String key, double defaultY) {
        double[] pos = positions.get(key);
        return pos != null ? pos[1] : defaultY;
    }

    public static void set(String key, double x, double y) {
        positions.put(key, new double[]{x, y});
    }

    public static void setBounded(String key, double x, double y, int width, int height, boolean centerAnchored) {
        int[] clamped = HudBounds.clamp((int) x, (int) y, width, height, centerAnchored);
        set(key, clamped[0], clamped[1]);
    }

    public static void clampSavedPositions(com.pitlite.module.ModuleManager moduleManager) {
        if (moduleManager == null) {
            return;
        }
        for (com.pitlite.module.Module module : moduleManager.getModules()) {
            if (!(module instanceof com.pitlite.module.DraggableHud)) {
                continue;
            }
            com.pitlite.module.DraggableHud hud = (com.pitlite.module.DraggableHud) module;
            String key = hud.getHudKey();
            double[] pos = positions.get(key);
            if (pos == null) {
                continue;
            }
            setBounded(
                    key,
                    pos[0],
                    pos[1],
                    Math.max(1, hud.getHudWidth()),
                    Math.max(1, hud.getHudHeight()),
                    hud.isHudCenterAnchored());
        }
    }

    public static void migrateFromSettings(String key, JsonObject settingsJson) {
        if (settingsJson == null || positions.containsKey(key)) {
            return;
        }
        if (settingsJson.has("X Pos") && settingsJson.has("Y Pos")) {
            set(key,
                    settingsJson.get("X Pos").getAsDouble(),
                    settingsJson.get("Y Pos").getAsDouble());
        }
    }

    public static void load() {
        positions.clear();
        ConfigManager.init();
        if (!POSITIONS_FILE.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(POSITIONS_FILE)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            for (Map.Entry<String, com.google.gson.JsonElement> entry : root.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject obj = entry.getValue().getAsJsonObject();
                if (obj.has("x") && obj.has("y")) {
                    set(entry.getKey(), obj.get("x").getAsDouble(), obj.get("y").getAsDouble());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        ConfigManager.init();
        JsonObject root = new JsonObject();
        for (Map.Entry<String, double[]> entry : positions.entrySet()) {
            JsonObject pos = new JsonObject();
            pos.add("x", new JsonPrimitive(entry.getValue()[0]));
            pos.add("y", new JsonPrimitive(entry.getValue()[1]));
            root.add(entry.getKey(), pos);
        }
        try (FileWriter writer = new FileWriter(POSITIONS_FILE)) {
            new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
