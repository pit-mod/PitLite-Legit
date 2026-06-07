package com.pitlite.utils;

import com.pitlite.PitLite;
import com.pitlite.module.Module;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class KeybindRegistry {

    private static Map<Integer, List<Module>> byKey = new HashMap<>();
    private static boolean stale = true;

    private KeybindRegistry() {
    }

    public static void markStale() {
        stale = true;
    }

    public static void rebuildIfNeeded() {
        if (!stale || PitLite.moduleManager == null) {
            return;
        }
        Map<Integer, List<Module>> map = new HashMap<>();
        for (Module module : PitLite.moduleManager.getModules()) {
            int code = module.keybind.code;
            if (code == Keyboard.KEY_NONE) {
                continue;
            }
            map.computeIfAbsent(code, k -> new ArrayList<>()).add(module);
        }
        byKey = map;
        stale = false;
    }

    public static List<Module> getModulesForKey(int key) {
        rebuildIfNeeded();
        List<Module> list = byKey.get(key);
        return list != null ? list : java.util.Collections.emptyList();
    }
}
