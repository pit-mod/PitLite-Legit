package com.pitlite.utils;

import com.google.gson.*;
import com.pitlite.PitLite;
import com.pitlite.module.Module;
import com.pitlite.settings.BooleanSetting;
import com.pitlite.settings.ColorSetting;
import com.pitlite.settings.InputSetting;
import com.pitlite.settings.KeybindSetting;
import com.pitlite.settings.ModeSetting;
import com.pitlite.settings.NumberSetting;
import com.pitlite.settings.Setting;

import java.io.*;
import java.awt.Color;
import java.util.Map;

public class ConfigManager {
    private static final File CONFIG_DIR = new File("config/pitlegit");
    private static final File MODULES_FILE = new File(CONFIG_DIR, "modules.json");
    private static final File KOS_FILE = new File(CONFIG_DIR, "kos.json");
    private static final File FRIENDS_FILE = new File(CONFIG_DIR, "friends.json");
    private static final File TRUCE_FILE = new File(CONFIG_DIR, "truce.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private enum PlayerListType {
        KOS,
        FRIEND,
        TRUCE
    }

    public static void init() {
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.mkdirs();
        }
    }

    private static boolean playerListsMigrated;

    public static void loadConfig() {
        init();
        PitLiteApiConfig.load();
        playerListsMigrated = false;
        HudPositionManager.load();
        loadModules();
        loadPlayerMap(KOS_FILE, PlayerListType.KOS);
        loadPlayerMap(FRIENDS_FILE, PlayerListType.FRIEND);
        loadPlayerMap(TRUCE_FILE, PlayerListType.TRUCE);
        if (playerListsMigrated) {
            savePlayerMap(KOS_FILE, KOSManager.getKosPlayers());
            savePlayerMap(FRIENDS_FILE, FriendManager.getFriends());
            savePlayerMap(TRUCE_FILE, TruceManager.getTrucePlayers());
        }
    }

    public static void saveConfig() {
        ConfigSaveDebouncer.markDirty();
    }

    public static void persistAll() {
        init();
        saveModules();
        HudPositionManager.save();
        savePlayerMap(KOS_FILE, KOSManager.getKosPlayers());
        savePlayerMap(FRIENDS_FILE, FriendManager.getFriends());
        savePlayerMap(TRUCE_FILE, TruceManager.getTrucePlayers());
    }

    private static void saveModules() {
        JsonObject root = new JsonObject();
        for (Module module : PitLite.moduleManager.getModules()) {
            JsonObject moduleJson = new JsonObject();
            moduleJson.addProperty("toggled", module.isToggled());
            moduleJson.addProperty("keybind", module.keybind.code);

            JsonObject settingsJson = new JsonObject();
            for (Setting setting : module.settings) {
                if (setting instanceof BooleanSetting) {
                    settingsJson.addProperty(setting.name, ((BooleanSetting) setting).enabled);
                } else if (setting instanceof NumberSetting) {
                    settingsJson.addProperty(setting.name, ((NumberSetting) setting).value);
                } else if (setting instanceof ModeSetting) {
                    settingsJson.addProperty(setting.name, ((ModeSetting) setting).getMode());
                } else if (setting instanceof KeybindSetting) {
                    settingsJson.addProperty(setting.name, ((KeybindSetting) setting).code);
                } else if (setting instanceof ColorSetting) {
                    settingsJson.addProperty(setting.name, ((ColorSetting) setting).getColor().getRGB());
                } else if (setting instanceof InputSetting) {
                    settingsJson.addProperty(setting.name, ((InputSetting) setting).getContent());
                }
            }
            if (module.settings.size() > 0) {
                moduleJson.add("settings", settingsJson);
            }
            root.add(module.getName(), moduleJson);
        }

        try (FileWriter writer = new FileWriter(MODULES_FILE)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadModules() {
        if (!MODULES_FILE.exists()) return;

        try (FileReader reader = new FileReader(MODULES_FILE)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();

            for (Module module : PitLite.moduleManager.getModules()) {
                if (root.has(module.getName())) {
                    JsonObject moduleJson = root.getAsJsonObject(module.getName());
                    
                    if (moduleJson.has("toggled")) {
                        module.setToggledFromConfig(moduleJson.get("toggled").getAsBoolean());
                    }
                    if (moduleJson.has("keybind")) {
                        module.keybind.code = moduleJson.get("keybind").getAsInt();
                    }

                    if (moduleJson.has("settings")) {
                        JsonObject settingsJson = moduleJson.getAsJsonObject("settings");
                        HudPositionManager.migrateFromSettings(module.getName(), settingsJson);
                        for (Setting setting : module.settings) {
                            if (settingsJson.has(setting.name)) {
                                if (setting instanceof BooleanSetting) {
                                    ((BooleanSetting) setting).enabled = settingsJson.get(setting.name).getAsBoolean();
                                } else if (setting instanceof NumberSetting) {
                                    ((NumberSetting) setting).setValue(settingsJson.get(setting.name).getAsDouble());
                                } else if (setting instanceof ModeSetting) {
                                    ModeSetting modeSetting = (ModeSetting) setting;
                                    String mode = settingsJson.get(setting.name).getAsString();
                                    int idx = modeSetting.modes.indexOf(mode);
                                    if (idx != -1) modeSetting.index = idx;
                                } else if (setting instanceof KeybindSetting) {
                                    ((KeybindSetting) setting).code = settingsJson.get(setting.name).getAsInt();
                                } else if (setting instanceof ColorSetting) {
                                    ((ColorSetting) setting).setColor(new Color(settingsJson.get(setting.name).getAsInt(), true));
                                } else if (setting instanceof InputSetting) {
                                    ((InputSetting) setting).setContent(settingsJson.get(setting.name).getAsString());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void savePlayerMap(File file, Map<String, String> players) {
        JsonObject root = new JsonObject();
        for (Map.Entry<String, String> entry : players.entrySet()) {
            root.addProperty(entry.getKey(), entry.getValue());
        }

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadPlayerMap(File file, PlayerListType type) {
        if (!file.exists()) return;

        try (FileReader reader = new FileReader(file)) {
            JsonElement parsed = new JsonParser().parse(reader);

            if (parsed.isJsonArray()) {
                loadLegacyNameArray(parsed.getAsJsonArray(), type);
                playerListsMigrated = true;
                return;
            }

            if (!parsed.isJsonObject()) return;

            JsonObject root = parsed.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                loadEntry(type, entry.getKey(), entry.getValue().getAsString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadEntry(PlayerListType type, String uuidKey, String displayName) {
        switch (type) {
            case FRIEND:
                FriendManager.loadEntry(uuidKey, displayName);
                break;
            case TRUCE:
                TruceManager.loadEntry(uuidKey, displayName);
                break;
            default:
                KOSManager.loadEntry(uuidKey, displayName);
                break;
        }
    }

    private static void clearList(PlayerListType type) {
        switch (type) {
            case FRIEND:
                FriendManager.clear();
                break;
            case TRUCE:
                TruceManager.clear();
                break;
            default:
                KOSManager.clear();
                break;
        }
    }

    private static void addToList(PlayerListType type, String name, String uuid) {
        switch (type) {
            case FRIEND:
                FriendManager.add(name, uuid);
                break;
            case TRUCE:
                TruceManager.add(name, uuid);
                break;
            default:
                KOSManager.add(name, uuid);
                break;
        }
    }

    private static void loadLegacyNameArray(JsonArray array, PlayerListType type) {
        clearList(type);

        for (JsonElement element : array) {
            String name = element.getAsString();
            String uuid = ProfileLookup.fetchUuid(name);
            if (uuid == null) {
                System.out.println("[PitLite] Could not resolve UUID for " + name + " during migration.");
                continue;
            }
            addToList(type, name, uuid);
        }
    }
}
