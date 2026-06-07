package com.pitlite.settings;

import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting {
    public int index;
    public List<String> modes;
    
    public ModeSetting(String name, String defaultMode, String... modes) {
        super(name);
        this.modes = Arrays.asList(modes);
        this.index = this.modes.indexOf(defaultMode);
        if (this.index == -1) this.index = 0;
    }
    
    public String getMode() {
        return modes.get(index);
    }
    
    public void cycle() {
        index = (index + 1) % modes.size();
    }
    
    public void setMode(String mode) {
        int i = modes.indexOf(mode);
        if (i != -1) {
            index = i;
        }
    }
}
