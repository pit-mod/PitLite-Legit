package com.pitlite.module;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleManager {
    private List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        addModule(new com.pitlite.module.impl.render.KOSList());
        addModule(new com.pitlite.module.impl.render.DarkList());
        addModule(new com.pitlite.module.impl.render.RageList());
        addModule(new com.pitlite.module.impl.render.NickedList());

        addModule(new com.pitlite.module.impl.render.TargetHUD());
        addModule(new com.pitlite.module.impl.render.ArmorHUD());
        addModule(new com.pitlite.module.impl.render.HealthDisplay());
        addModule(new com.pitlite.module.impl.render.HUD());
        addModule(new com.pitlite.module.impl.render.PlayerCounter());
        addModule(new com.pitlite.module.impl.render.FpsDisplay());
        addModule(new com.pitlite.module.impl.render.FullBright());

        addModule(new com.pitlite.module.impl.render.VenomTimer());
        addModule(new com.pitlite.module.impl.render.DrainTimer());
        addModule(new com.pitlite.module.impl.render.PinTimer());
        addModule(new com.pitlite.module.impl.render.Events());

        addModule(new com.pitlite.module.impl.player.MysticRename());
        addModule(new com.pitlite.module.impl.player.LowLivesWarning());
        addModule(new com.pitlite.module.impl.player.ClickToView());
        addModule(new com.pitlite.module.impl.player.OwnerHistory());
        addModule(new com.pitlite.module.impl.player.AutoMath());

        addModule(new com.pitlite.module.impl.player.AutoReconnect());
        addModule(new com.pitlite.module.impl.player.AutoSpawn());
        addModule(new com.pitlite.module.impl.misc.DiscordRichPresence());
        addModule(new com.pitlite.module.impl.player.StopYourAddiction());
    }

    public void addModule(Module module) {
        applyShortDescription(module);
        modules.add(module);
    }

    private void applyShortDescription(Module module) {
        String d;
        switch (module.getName()) {
            case "Players List": d = "Shows KOS and friends in lobby."; break;
            case "Dark List": d = "Shows Dark/Venom players nearby."; break;
            case "Rage List": d = "Shows rage players in lobby."; break;
            case "Nicked List": d = "Auto denick via Mojang + PitPanda + /view. Ban risk on Hypixel."; break;
            case "TargetHUD": d = "Shows info about your target."; break;
            case "Armor HUD": d = "Shows your armor on screen."; break;
            case "HealthDisplay": d = "Shows your health near crosshair."; break;
            case "HUD": d = "Shows enabled module list on screen."; break;
            case "Player": d = "Shows how many players are in the lobby."; break;
            case "Fps": d = "Displays the current FPS."; break;
            case "FullBright": d = "Forces max world brightness."; break;
            case "VenomTimer": d = "Shows venom cooldown timer."; break;
            case "DrainTimer": d = "Shows sprint drain timers."; break;
            case "PinTimer": d = "Shows pin duration above players."; break;
            case "Events": d = "Shows upcoming Pit event timers."; break;
            case "Mystic Rename": d = "Client-side mystic item renamer."; break;
            case "Low Lives Warning": d = "Highlights low-life mystics and alerts on pickup."; break;
            case "Click to View": d = "Click chat usernames to /view their Pit profile."; break;
            case "Owner History": d = "Middle-click mystics in GUIs to show owner history."; break;
            case "AutoMath": d = "Solves Quick Maths; prints answer in chat. Never auto-submits."; break;
            case "AutoReconnect": d = "Reconnects automatically on disconnect."; break;
            case "AutoSpawn": d = "Auto runs /spawn command."; break;
            case "Discord Rich Presence": d = "Shows Playing Hypixel Pit on Discord."; break;
            case "Stop Your Addiction": d = "Permanent Pit intervention. Requires acceptance phrase."; break;
            default: return;
        }
        module.setDescription(d);
    }

    public void applyLoadedModuleStates() {
        for (Module module : modules) {
            module.applyLoadedState();
        }
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> getModulesByCategory(Category category) {
        return modules.stream().filter(m -> m.getCategory() == category).collect(Collectors.toList());
    }
}
