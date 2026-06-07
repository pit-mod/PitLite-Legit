package com.pitlite.module.impl.misc;

import com.pitlite.module.Category;
import com.pitlite.module.Module;
import com.pitlite.settings.BooleanSetting;
import com.pitlite.settings.InputSetting;
import com.pitlite.utils.DiscordRpcManager;
import com.pitlite.utils.NotificationManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class DiscordRichPresence extends Module {

    private static final int CONNECT_RETRY_TICKS = 100;

    private final InputSetting applicationId = new InputSetting("Application ID", "1311409587999051826");
    private final BooleanSetting showElapsed = new BooleanSetting("Show Elapsed", true);

    private String lastDetails = "";
    private boolean notifiedActive = false;
    private boolean notifiedConnectFailure = false;
    private int waitTicks = 0;
    private int connectRetryTicks = 0;
    private boolean awaitingConnection = false;

    public DiscordRichPresence() {
        super("Discord Rich Presence",
                "Shows a game activity on Discord (like Fortnite). Name your Discord app \"Hypixel Pit\" so it says Playing Hypixel Pit.",
                Category.MISC);
        addSettings(applicationId, showElapsed);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        notifiedActive = false;
        notifiedConnectFailure = false;
        waitTicks = 0;
        connectRetryTicks = 0;
        lastDetails = "";
        awaitingConnection = true;
        tryConnect();
    }

    @Override
    public void onDisable() {
        awaitingConnection = false;
        DiscordRpcManager.shutdown();
        super.onDisable();
    }

    private void tryConnect() {
        if (!isToggled()) {
            return;
        }

        if (DiscordRpcManager.start(applicationId.getContent())) {
            awaitingConnection = false;
            notifiedConnectFailure = false;
            if (!notifiedActive) {
                NotificationManager.show(
                        "Connecting to Discord... Turn on \"Display current activity\" in Discord Settings > Activity Privacy.",
                        8000);
            }
            pushPresence();
            return;
        }

        awaitingConnection = true;
        if (!notifiedConnectFailure) {
            notifiedConnectFailure = true;
            NotificationManager.show(
                    "Discord failed: " + DiscordRpcManager.getLastError()
                            + " (will retry while this module stays on)",
                    10000);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!isToggled() || event.phase != TickEvent.Phase.END) {
            return;
        }

        if (awaitingConnection || !DiscordRpcManager.isRunning()) {
            connectRetryTicks++;
            if (connectRetryTicks >= CONNECT_RETRY_TICKS) {
                connectRetryTicks = 0;
                tryConnect();
            }
            return;
        }

        DiscordRpcManager.runCallbacks();

        if (!notifiedActive && DiscordRpcManager.isDiscordReady()) {
            notifiedActive = true;
            NotificationManager.show("Discord Rich Presence is now active on your profile.", 4000);
            pushPresence();
        } else if (!notifiedActive) {
            waitTicks++;
            if (waitTicks == 200) {
                NotificationManager.show(
                        "Still waiting for Discord. Is the Discord desktop app running? Check Activity Privacy settings.",
                        8000);
            }
        }

        if (!DiscordRpcManager.isDiscordReady()) {
            return;
        }

        String presenceKey = buildPresenceKey();
        if (!presenceKey.equals(lastDetails)) {
            lastDetails = presenceKey;
            pushPresence();
        }
    }

    private String buildPresenceKey() {
        return buildPresenceDetails() + "|" + buildPresenceState();
    }

    private String buildPresenceDetails() {
        return getUsername() + " is playing Hypixel Pit";
    }

    private String buildPresenceState() {
        return DiscordRpcManager.getLocationDetails();
    }

    private String getUsername() {
        if (mc.thePlayer != null) {
            String name = mc.thePlayer.getName();
            if (name != null && !name.isEmpty()) {
                return name;
            }
        }
        if (mc.getSession() != null) {
            String name = mc.getSession().getUsername();
            if (name != null && !name.isEmpty()) {
                return name;
            }
        }
        return "Player";
    }

    private void pushPresence() {
        DiscordRpcManager.updatePresence(showElapsed.enabled, buildPresenceDetails(), buildPresenceState());
    }
}
