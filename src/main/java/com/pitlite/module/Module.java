package com.pitlite.module;

import com.pitlite.settings.BooleanSetting;
import com.pitlite.settings.KeybindSetting;
import com.pitlite.settings.Setting;
import com.pitlite.utils.ConfigSaveDebouncer;
import com.pitlite.utils.HudStackManager;
import com.pitlite.utils.KeybindRegistry;
import com.pitlite.utils.NotificationManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

public class Module {
    protected Minecraft mc = Minecraft.getMinecraft();
    private String name;
    private String description;
    private Category category;
    private boolean toggled;
    private boolean dangerous;
    private boolean onEventBus;
    
    public KeybindSetting keybind = new KeybindSetting(Keyboard.KEY_NONE);
    private final BooleanSetting hideInHud = new BooleanSetting("Hid in HUD", false);
    public List<Setting> settings = new ArrayList<>();

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.settings.add(keybind);
        this.settings.add(hideInHud);
    }

    public void addSetting(Setting setting) {
        this.settings.add(setting);
    }

    public void addSettings(Setting... settings) {
        for (Setting s : settings) {
            this.settings.add(s);
        }
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Category getCategory() { return category; }
    public boolean isDangerous() { return dangerous; }
    protected void markDangerous() { this.dangerous = true; }
    public boolean isHiddenInHud() { return hideInHud.enabled; }
    
    public boolean isToggled() { return toggled; }

    public void setToggledFromConfig(boolean toggled) {
        this.toggled = toggled;
    }

    public void applyLoadedState() {
        if (toggled) {
            registerOnEventBus();
            onEnable();
        }
    }

    protected void registerOnEventBus() {
        if (!onEventBus) {
            MinecraftForge.EVENT_BUS.register(this);
            onEventBus = true;
        }
    }

    protected void unregisterFromEventBus() {
        if (onEventBus) {
            MinecraftForge.EVENT_BUS.unregister(this);
            onEventBus = false;
        }
    }

    public void setToggled(boolean toggled) {
        if (this.toggled == toggled) {
            return;
        }
        this.toggled = toggled;
        if (toggled) {
            registerOnEventBus();
            onEnable();
        } else {
            onDisable();
            unregisterFromEventBus();
        }
        if (this instanceof DraggableHud) {
            HudStackManager.markDirty();
        }
        ConfigSaveDebouncer.markDirty();
        KeybindRegistry.markStale();
        NotificationManager.showModuleToggle(name, toggled);
    }

    public void toggle() {
        setToggled(!toggled);
    }
    
    protected void onEnable() {}
    protected void onDisable() {}

    public void onKey(int key) {}
}
