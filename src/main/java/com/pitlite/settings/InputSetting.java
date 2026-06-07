package com.pitlite.settings;

public class InputSetting extends Setting {
    private String content;
    
    public InputSetting(String name, String defaultValue) {
        super(name);
        this.content = defaultValue;
    }

    public void setContent(String content) {
        this.content = content;
    }
    
    public String getContent() {
        return this.content;
    }
}
