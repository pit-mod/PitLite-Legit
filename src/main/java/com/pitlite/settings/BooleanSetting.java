package com.pitlite.settings;

public class BooleanSetting extends Setting {
    public boolean enabled;
    
    public BooleanSetting(String name, boolean defaultValue) {
        super(name);
        this.enabled = defaultValue;
    }
    
    public void toggle() {
        this.enabled = !this.enabled;
    }
}
