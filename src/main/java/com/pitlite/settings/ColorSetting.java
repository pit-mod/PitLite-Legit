package com.pitlite.settings;

import java.awt.Color;

public class ColorSetting extends Setting {
    public Color color;
    private final Color defaultColor;

    public ColorSetting(String name, Color defaultColor) {
        super(name);
        this.color = defaultColor;
        this.defaultColor = defaultColor;
    }

    public Color getColor() {
        return color;
    }

    public Color getDefaultColor() {
        return defaultColor;
    }

    public void setColor(Color color) {
        this.color = color == null ? defaultColor : color;
    }
}
