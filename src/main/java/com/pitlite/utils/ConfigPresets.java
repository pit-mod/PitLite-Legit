package com.pitlite.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pitlite.PitLite;
import com.pitlite.module.Module;
import com.pitlite.settings.BooleanSetting;
import com.pitlite.settings.ColorSetting;
import com.pitlite.settings.InputSetting;
import com.pitlite.settings.KeybindSetting;
import com.pitlite.settings.ModeSetting;
import com.pitlite.settings.NumberSetting;
import com.pitlite.settings.Setting;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class ConfigPresets {

    private static final File PRESET_DIR = new File("config/pitlegit/presets");

    private ConfigPresets() {
    }

    public static boolean exportPreset(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        ConfigManager.init();
        if (!PRESET_DIR.exists()) {
            PRESET_DIR.mkdirs();
        }
        File out = new File(PRESET_DIR, sanitize(name) + ".json");
        ConfigManager.persistAll();
        try {
            Files.copy(new File("config/pitlegit/modules.json").toPath(), out.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            copyIfExists("config/pitlegit/kos.json", PRESET_DIR, sanitize(name) + "_kos.json");
            copyIfExists("config/pitlegit/friends.json", PRESET_DIR, sanitize(name) + "_friends.json");
            copyIfExists("config/pitlegit/truce.json", PRESET_DIR, sanitize(name) + "_truce.json");
            copyIfExists("config/pitlegit/hud_positions.json", PRESET_DIR, sanitize(name) + "_hud.json");
            NotificationManager.show("\u00a7aExported preset bundle: \u00a7f" + out.getName(), 4000);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.show("\u00a7cExport failed.", 3000);
            return false;
        }
    }

    public static boolean importPreset(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        File preset = new File(PRESET_DIR, sanitize(name) + ".json");
        if (!preset.exists()) {
            NotificationManager.show("\u00a7cPreset not found: \u00a7f" + preset.getName(), 3000);
            return false;
        }
        NotificationManager.setSuppressModuleToggle(true);
        try {
            applyModulesFromFile(preset);
        } finally {
            NotificationManager.setSuppressModuleToggle(false);
        }
        ConfigManager.persistAll();
        ConfigSaveDebouncer.markDirty();
        NotificationManager.show("\u00a7aImported preset: \u00a7f" + preset.getName(), 4000);
        return true;
    }

    private static void applyModulesFromFile(File file) {
        try (FileReader reader = new FileReader(file)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            for (Module module : PitLite.moduleManager.getModules()) {
                if (!root.has(module.getName())) {
                    continue;
                }
                JsonObject moduleJson = root.getAsJsonObject(module.getName());
                if (moduleJson.has("toggled")) {
                    boolean on = moduleJson.get("toggled").getAsBoolean();
                    if (module.isToggled() != on) {
                        module.setToggled(on);
                    } else {
                        module.setToggledFromConfig(on);
                    }
                }
                if (moduleJson.has("keybind")) {
                    module.keybind.code = moduleJson.get("keybind").getAsInt();
                }
                if (moduleJson.has("settings")) {
                    JsonObject settingsJson = moduleJson.getAsJsonObject("settings");
                    for (Setting setting : module.settings) {
                        if (!settingsJson.has(setting.name)) {
                            continue;
                        }
                        applySetting(setting, settingsJson);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager.show("\u00a7cImport failed.", 3000);
        }
    }

    private static void applySetting(Setting setting, JsonObject settingsJson) {
        if (setting instanceof BooleanSetting) {
            ((BooleanSetting) setting).enabled = settingsJson.get(setting.name).getAsBoolean();
        } else if (setting instanceof NumberSetting) {
            ((NumberSetting) setting).setValue(settingsJson.get(setting.name).getAsDouble());
        } else if (setting instanceof ModeSetting) {
            ModeSetting modeSetting = (ModeSetting) setting;
            String mode = settingsJson.get(setting.name).getAsString();
            int idx = modeSetting.modes.indexOf(mode);
            if (idx != -1) {
                modeSetting.index = idx;
            }
        } else if (setting instanceof KeybindSetting) {
            ((KeybindSetting) setting).code = settingsJson.get(setting.name).getAsInt();
        } else if (setting instanceof ColorSetting) {
            ((ColorSetting) setting).setColor(new Color(settingsJson.get(setting.name).getAsInt(), true));
        } else if (setting instanceof InputSetting) {
            ((InputSetting) setting).setContent(settingsJson.get(setting.name).getAsString());
        }
    }

    private static String sanitize(String name) {
        return name.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    private static void copyIfExists(String src, File dir, String destName) {
        File source = new File(src);
        if (!source.exists()) {
            return;
        }
        try {
            Files.copy(source.toPath(), new File(dir, destName).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {
        }
    }

}
