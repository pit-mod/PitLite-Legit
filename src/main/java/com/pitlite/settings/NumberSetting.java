package com.pitlite.settings;

public class NumberSetting extends Setting {
    public double value, min, max;
    public int decimalPlaces;
    
    public NumberSetting(String name, double defaultValue, double min, double max, int decimalPlaces) {
        super(name);
        this.value = defaultValue;
        this.min = min;
        this.max = max;
        this.decimalPlaces = decimalPlaces;
    }
    
    public void setValue(double value) {
        double clamped = Math.max(min, Math.min(max, value));
        double factor = Math.pow(10, decimalPlaces);
        this.value = Math.round(clamped * factor) / factor;
    }
}
