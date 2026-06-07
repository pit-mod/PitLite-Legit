package com.pitlite.module.impl.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.pitlite.module.Category;
import com.pitlite.module.DraggableHud;
import com.pitlite.module.Module;
import com.pitlite.settings.BooleanSetting;
import com.pitlite.settings.ButtonSetting;
import com.pitlite.settings.NumberSetting;
import com.pitlite.utils.EventUtils;
import com.pitlite.utils.HudPositionManager;
import com.pitlite.utils.HudStackManager;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Events extends Module implements DraggableHud {
    private static final double DEFAULT_X = 5.0;
    private static final double DEFAULT_Y = 50.0;
    private ExecutorService fetchExecutor;
    private final List<String> eventList = new ArrayList<>();
    private int timePassIndex = 0;
    private String eventResponse;
    private long lastFetchTime = 0;
    private long lastAutoRefreshTime = 0;

    private final NumberSetting numEvents = new NumberSetting("Number of Events", 5, 1, 10, 0);
    private final ButtonSetting refreshButton = new ButtonSetting("Refresh", this::refreshData);

    private final BooleanSetting showBlockhead = new BooleanSetting("Blockhead", true);
    private final BooleanSetting showPizza = new BooleanSetting("Pizza", true);
    private final BooleanSetting showBeast = new BooleanSetting("Beast", true);
    private final BooleanSetting showRobbery = new BooleanSetting("Robbery", true);
    private final BooleanSetting showSpire = new BooleanSetting("Spire", true);
    private final BooleanSetting showSquads = new BooleanSetting("Squads", true);
    private final BooleanSetting showTDM = new BooleanSetting("Team Deathmatch", true);
    private final BooleanSetting showRaffle = new BooleanSetting("Raffle", true);
    private final BooleanSetting showRagePit = new BooleanSetting("Rage Pit", true);
    private final BooleanSetting show2xRewards = new BooleanSetting("2x Rewards", true);
    private final BooleanSetting showGiantCake = new BooleanSetting("Giant Cake", true);
    private final BooleanSetting showKOTL = new BooleanSetting("KOTL", true);
    private final BooleanSetting showDragonEgg = new BooleanSetting("Dragon Egg", true);
    private final BooleanSetting showAuction = new BooleanSetting("Auction", true);
    private final BooleanSetting showQuickMaths = new BooleanSetting("Quick Maths", true);
    private final BooleanSetting showKOTH = new BooleanSetting("KOTH", true);
    private final BooleanSetting showCarePackage = new BooleanSetting("Care Package", true);
    private final BooleanSetting showAllBounty = new BooleanSetting("All Bounty", true);

    public Events() {
        super("Events", "Lists the next events.", Category.RENDER);
        addSettings(numEvents, refreshButton,
                showBlockhead, showPizza, showBeast, showRobbery, showSpire, showSquads,
                showTDM, showRaffle, showRagePit, show2xRewards, showGiantCake, showKOTL,
                showDragonEgg, showAuction, showQuickMaths, showKOTH, showCarePackage, showAllBounty);
    }

    @Override
    protected void onEnable() {
        if (fetchExecutor == null || fetchExecutor.isShutdown()) {
            fetchExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "pitlite-event-fetch");
                t.setDaemon(true);
                return t;
            });
        }
    }

    @Override
    protected void onDisable() {
        if (fetchExecutor != null) {
            fetchExecutor.shutdownNow();
            fetchExecutor = null;
        }
    }

    private void refreshData() {
        timePassIndex = 0;
        eventList.clear();
        eventResponse = null;
        lastFetchTime = 0;
        lastParsedResponse = "";
        fetchEventData();
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!isToggled()) return;
        if (event.phase != TickEvent.Phase.END || event.player != mc.thePlayer) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAutoRefreshTime > 30000) {
            refreshData();
            lastAutoRefreshTime = currentTime;
        }

        if (currentTime - lastFetchTime > 5000) {
            fetchEventData();
            lastFetchTime = currentTime;
        }

        if (eventResponse != null) {
            try {
                if (!eventResponse.equals(lastParsedResponse)) {
                    JsonArray jsonArray = new JsonParser().parse(eventResponse).getAsJsonArray();
                    updateEventStrings(jsonArray);
                    lastParsedResponse = eventResponse;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            eventResponse = null;
        }
    }

    private String lastParsedResponse = "";

    private boolean isEventEnabled(String eventName) {
        if (eventName.contains("Blockhead")) return showBlockhead.enabled;
        if (eventName.contains("Pizza")) return showPizza.enabled;
        if (eventName.contains("Beast")) return showBeast.enabled;
        if (eventName.contains("Robbery")) return showRobbery.enabled;
        if (eventName.contains("Spire")) return showSpire.enabled;
        if (eventName.contains("Squads")) return showSquads.enabled;
        if (eventName.contains("Team Deathmatch")) return showTDM.enabled;
        if (eventName.contains("Raffle")) return showRaffle.enabled;
        if (eventName.contains("Rage Pit")) return showRagePit.enabled;
        if (eventName.contains("2x Rewards")) return show2xRewards.enabled;
        if (eventName.contains("Giant Cake")) return showGiantCake.enabled;
        if (eventName.contains("KOTL")) return showKOTL.enabled;
        if (eventName.contains("Dragon Egg")) return showDragonEgg.enabled;
        if (eventName.contains("Auction")) return showAuction.enabled;
        if (eventName.contains("Quick Maths")) return showQuickMaths.enabled;
        if (eventName.contains("KOTH")) return showKOTH.enabled;
        if (eventName.contains("Care Package")) return showCarePackage.enabled;
        if (eventName.contains("All bounty") || eventName.contains("Bounty")) return showAllBounty.enabled;
        return true;
    }

    private void updateEventStrings(JsonArray jsonArray) {
        if (jsonArray == null) return;

        try {
            List<String> newEventList = new ArrayList<>();
            int numEventsToShow = (int) numEvents.value;
            int count = 0;
            for (int i = timePassIndex; i < jsonArray.size(); i++) {
                try {
                    String eventName = jsonArray.get(i).getAsJsonObject().get("event").getAsString();
                    long eventTimestamp = jsonArray.get(i).getAsJsonObject().get("timestamp").getAsLong();
                    long timeUntilEventMillis = eventTimestamp - Instant.now().toEpochMilli();

                    if (timeUntilEventMillis < 0) {
                        timePassIndex++;
                        continue;
                    }

                    if (!isEventEnabled(eventName)) {
                        continue;
                    }

                    if (count < numEventsToShow) {
                        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeUntilEventMillis);
                        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeUntilEventMillis) % 60;
                        newEventList.add(eventName + " [" + String.format("%02d:%02d", minutes, seconds) + "]");
                        count++;
                    } else {
                        break;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            synchronized (eventList) {
                eventList.clear();
                eventList.addAll(newEventList);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void fetchEventData() {
        if (fetchExecutor == null || fetchExecutor.isShutdown()) {
            return;
        }
        fetchExecutor.submit(() -> eventResponse = EventUtils.fetchEvents());
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (!isToggled() || mc.thePlayer == null || mc.theWorld == null
                || event.type != RenderGameOverlayEvent.ElementType.CHAT) {
            return;
        }

        FontRenderer fr = mc.fontRendererObj;
        float posX = getRenderX();
        float posY = getRenderY();
        int maxEventNameWidth = 0;

        List<String> currentEvents;
        synchronized (eventList) {
            currentEvents = new ArrayList<>(eventList);
        }

        for (String eventInfo : currentEvents) {
            maxEventNameWidth = Math.max(maxEventNameWidth, fr.getStringWidth(eventInfo.split(" \\[")[0]));
        }

        for (int idx = 0; idx < currentEvents.size(); idx++) {
            String eventInfo = currentEvents.get(idx);
            String[] splitEvent = eventInfo.split(" \\[");
            String eventName = splitEvent[0];
            String timeRemaining = splitEvent[1].replace("]", "");

            int color = getColorForEvent(eventName);

            fr.drawStringWithShadow(eventName, posX, posY, color);

            int timeXPosition = (int) (posX + maxEventNameWidth + 2);

            fr.drawStringWithShadow("[", timeXPosition, posY, 0xAAAAAA);
            timeXPosition += fr.getStringWidth("[");

            int timeColor;
            if (idx == 0) {
                timeColor = 0xFF5555;
            } else if (idx == 1) {
                timeColor = 0xFFAA00;
            } else {
                timeColor = 0x00FF00;
            }

            fr.drawStringWithShadow(timeRemaining, timeXPosition, posY, timeColor);
            timeXPosition += fr.getStringWidth(timeRemaining);

            fr.drawStringWithShadow("]", timeXPosition, posY, 0xAAAAAA);

            posY += fr.FONT_HEIGHT + 2;
        }
    }

    @Override
    public String getHudKey() {
        return getName();
    }

    @Override
    public boolean isHudVisible() {
        synchronized (eventList) {
            return !eventList.isEmpty();
        }
    }

    @Override
    public int getHudX() {
        return (int) HudPositionManager.getX(getHudKey(), DEFAULT_X);
    }

    @Override
    public int getHudY() {
        return (int) HudPositionManager.getY(getHudKey(), DEFAULT_Y);
    }

    @Override
    public int getHudWidth() {
        synchronized (eventList) {
            if (eventList.isEmpty()) {
                return 1;
            }
            FontRenderer fr = mc.fontRendererObj;
            int max = 0;
            for (String eventInfo : eventList) {
                max = Math.max(max, fr.getStringWidth(eventInfo));
            }
            return Math.max(1, max);
        }
    }

    @Override
    public int getHudHeight() {
        synchronized (eventList) {
            int lines = Math.max(1, eventList.size());
            return lines * (mc.fontRendererObj.FONT_HEIGHT + 2);
        }
    }

    private int getColorForEvent(String eventName) {
        if (eventName.contains("Blockhead")) {
            return 0xFFAA00;
        }
        if (eventName.contains("Pizza")) {
            return 0xFF5555;
        }
        if (eventName.contains("Beast")) {
            return 0x55FF55;
        }
        if (eventName.contains("Robbery")) {
            return 0xFFAA00;
        }
        if (eventName.contains("Spire")) {
            return 0xAA00AA;
        }
        if (eventName.contains("Squads")) {
            return 0x55FFFF;
        }
        if (eventName.contains("Team Deathmatch")) {
            return 0xAA00AA;
        }
        if (eventName.contains("Raffle")) {
            return 0xFFAA00;
        }
        if (eventName.contains("Rage Pit")) {
            return 0xFF5555;
        }
        if (eventName.contains("2x Rewards")) {
            return 43520;
        }
        if (eventName.contains("Giant Cake")) {
            return 0xFF55FF;
        }
        if (eventName.contains("KOTL")) {
            return 0x55FF55;
        }
        if (eventName.contains("Dragon Egg")) {
            return 0xAA00AA;
        }
        if (eventName.contains("Auction")) {
            return 0xFFFF55;
        }
        if (eventName.contains("Quick Maths")) {
            return 0xAA00AA;
        }
        if (eventName.contains("KOTH")) {
            return 0x55FFFF;
        }
        if (eventName.contains("Care Package")) {
            return 0xFFAA00;
        }
        if (eventName.contains("All bounty")) {
            return 0xFFAA00;
        }
        return 0xFFFFFF;
    }
}
