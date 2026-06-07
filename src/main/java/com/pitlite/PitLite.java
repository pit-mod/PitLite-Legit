package com.pitlite;

import com.pitlite.module.ModuleManager;
import com.pitlite.utils.LobbyPlayerIndex;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = PitLite.MODID, version = PitLite.VERSION, name = PitLite.NAME)
public class PitLite {
    public static final String MODID = "pitlegit";
    public static final String VERSION = "1.0";
    public static final String NAME = "PitLite Legit";

    public static ModuleManager moduleManager;

    @EventHandler
    public void init(FMLInitializationEvent event) {
        moduleManager = new ModuleManager();
        com.pitlite.utils.ConfigManager.loadConfig();
        MinecraftForge.EVENT_BUS.register(new com.pitlite.utils.HudPositionClampHandler());
        registerClientCommands();
        MinecraftForge.EVENT_BUS.register(new KeyHandler());
        MinecraftForge.EVENT_BUS.register(new com.pitlite.commands.CommandHandler());
        MinecraftForge.EVENT_BUS.register(new RenderHandler());
        MinecraftForge.EVENT_BUS.register(new com.pitlite.utils.NotificationOverlay());
        MinecraftForge.EVENT_BUS.register(new com.pitlite.utils.HudStackManager());
        MinecraftForge.EVENT_BUS.register(new com.pitlite.utils.HudDragHandler());
        MinecraftForge.EVENT_BUS.register(new com.pitlite.utils.ConfigLifecycleHandler());
        MinecraftForge.EVENT_BUS.register(new com.pitlite.utils.ConfigSaveDebouncer());
        MinecraftForge.EVENT_BUS.register(new com.pitlite.utils.HudPositionSaveDebouncer());
        MinecraftForge.EVENT_BUS.register(new com.pitlite.utils.TabJoinNotifier());
        MinecraftForge.EVENT_BUS.register(LobbyPlayerIndex.INSTANCE);
        com.pitlite.utils.KeybindRegistry.markStale();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            com.pitlite.utils.ConfigSaveDebouncer.flushNow();
            com.pitlite.utils.HudPositionSaveDebouncer.flushNow();
        }));
    }

    private static void registerClientCommands() {
        ClientCommandHandler.instance.registerCommand(new com.pitlite.commands.PitLiteAcceptCommand());
        ClientCommandHandler.instance.registerCommand(new com.pitlite.commands.PitLiteConfirmCommand());
    }
}
