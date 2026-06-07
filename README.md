# PitLite Legit

Open-source Hypixel Pit client mod for **Minecraft 1.8.9 (Forge)**. Visual and QoL features only — no license key, no obfuscation, no auth or anti-debug checks.

## Requirements

- Java 8 JDK
- Minecraft 1.8.9 with [Forge](https://files.minecraftforge.net/) (recommended: `11.15.1.2318-1.8.9`)

## Install

1. Build the mod (see below) or use a release jar.
2. Copy `PitLite-Legit-1.0.jar` into your `.minecraft/mods` folder.
3. Launch Forge 1.8.9.

## Build from source

```bat
gradlew.bat build
```

Output: `build/libs/PitLite-Legit-1.0.jar`

## Controls

| Input | Action |
|-------|--------|
| **Right Shift** | Open ClickGUI |
| **`.pitlegit`** / **`.pitlite`** | Open ClickGUI |
| **`.pitlegit export <name>`** | Export settings preset |
| **`.pitlegit import <name>`** | Import settings preset |
| **`.kos` / `.friend` / `.truce`** | Manage player lists |
| **`.denick <player>`** | Denick lookup |
| **`.owners` / `.oh`** | Owner history (hold mystic or pass item id) |

Settings and lists are saved under `config/pitlegit/` (separate from full PitLite).

## Modules (24)

**Lobby & lists:** Players List, Dark List, Rage List, Nicked List

**HUD:** TargetHUD, Armor HUD, HealthDisplay, HUD, Player, Fps, FullBright

**Timers:** VenomTimer, DrainTimer, PinTimer, Events

**Utilities:** Mystic Rename, Low Lives Warning, Click to View, Owner History, AutoMath

**Misc:** AutoReconnect, AutoSpawn, Discord Rich Presence, Stop Your Addiction

### AutoMath

Solves Hypixel Quick Maths events and **never auto-submits**. Prints the answer in chat and can show it on-screen (with sound) — you type `/ac <answer>` yourself.

## Optional config

Copy `config-example/api.json` to `config/pitlegit/api.json` to use your own [PitPanda](https://pitpanda.net) API key for Owner History / denick. A default public key is bundled if you skip this.

Discord Rich Presence needs the Discord desktop app running. Set your Discord application name to **Hypixel Pit** (or customize the Application ID in module settings).

## Project layout

```
src/main/java/com/pitlite/   Mod source
src/main/resources/          Mixins, mcmod.info, jumpscare assets
config-example/              Sample runtime config
```

## Disclaimer

This mod is provided for educational purposes. Use on Hypixel at your own risk. The authors are not affiliated with Hypixel or Mojang.

## License

[MIT](LICENSE) — free to use, modify, and share with attribution.
