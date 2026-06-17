# PitLite Legit

Open-source Hypixel Pit client mod for **Minecraft 1.8.9 (Forge)**. Visual and QoL features only.

## Requirements

- Java 8 JDK
- Minecraft 1.8.9 with [Forge](https://files.minecraftforge.net/) (recommended: `11.15.1.2318-1.8.9`)

## Install

1. Build the mod (see below) or use a release jar.
2. Copy `PitLite-Legit-1.2.jar` into your `.minecraft/mods` folder.
3. Launch Forge 1.8.9.

## Build from source

```bat
gradlew.bat build
```

Output: `build/libs/PitLite-Legit-1.2.jar`

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

## Modules (31)

### Render — Lobby & lists
| Module | Description |
|--------|-------------|
| **Players List** | KOS, friends and truce in lobby with distance, direction, and which region they are in |
| **Dark List** | List of players wearing Dark / Venom pants |
| **Rage List** | List of players wearing Rage pants |
| **Nicked List** | Detects and displays nicked players (auto denick) |

### Render — HUD & overlays
| Module | Description |
|--------|-------------|
| **TargetHUD** | Info about your current target |
| **Armor HUD** | Renders your armor on the screen |
| **HealthDisplay** | HP near the crosshair |
| **HUD** | Enabled modules list on screen |
| **Player** | Lobby player count |
| **Fps** | Current FPS |
| **FullBright** | Client-side max brightness |

### Render — Timers & events
| Module | Description |
|--------|-------------|
| **VenomTimer** | Venom cooldown timer |
| **DrainTimer** | Sprint drain countdown on players |
| **PinTimer** | Pin duration above pinned players |
| **Events** | Shows upcoming Pit events |

### Player — Utilities
| Module | Description |
|--------|-------------|
| **Mystic Rename** | Client-side mystic renamer (enchants) |
| **Low Lives Warning** | Highlights low-life mystics; alerts on pickup |
| **Click to View** | Click chat names to run `/view` |
| **Owner History** | Middle-click mystics for owner history |
| **AutoMath** | Solves Quick Maths and prints the answer (never auto-submits) |
| **AutoReconnect** | Reconnect on disconnect |
| **AutoSpawn** | Automatically runs `/spawn` |

### Swapping
| Module | Description |
|--------|-------------|
| **PantSwapper** <span style="color:red">**[RISKY]**</span> | Automatically swaps pants and optionally boots in inventory |
| **AutoPod** <span style="color:red">**[RISKY]**</span> | Auto swaps Escape Pod pants |
| **PhoenixSwap** <span style="color:red">**[RISKY]**</span> | Manual Phoenix swap with auto-restore when healthy |
| **BulletTimeSwap** <span style="color:red">**[RISKY]**</span> | Swaps to Bullet Time sword to block incoming arrows |
| **DarkSwap** <span style="color:red">**[RISKY]**</span> | Advanced swapping with inventory support and intelligent slot selection |
| **AntiVenom** <span style="color:red">**[RISKY]**</span> | Auto swaps to Dark Pants when hit by Venom |
| **MLBSwap** <span style="color:red">**[RISKY]**</span> | Swaps to Mega Longbow, shoots, and swaps back |

### Misc
| Module | Description |
|--------|-------------|
| **Discord Rich Presence** | Shows "Playing Hypixel Pit" on Discord |
| **Stop Your Addiction** | Permanent Pit intervention. Requires acceptance phrase. |

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

This mod is provided for educational purposes. The authors are not affiliated with Hypixel or Mojang.

## License

[MIT](LICENSE) — free to use, modify, and share with attribution.
