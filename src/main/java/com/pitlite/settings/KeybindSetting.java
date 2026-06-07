package com.pitlite.settings;

public class KeybindSetting extends Setting {
    public int code;
    
    public KeybindSetting(int code) {
        super("Keybind");
        this.code = code;
    }

    public KeybindSetting(String name, int code) {
        super(name);
        this.code = code;
    }
}
