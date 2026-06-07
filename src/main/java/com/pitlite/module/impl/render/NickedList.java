package com.pitlite.module.impl.render;

import com.pitlite.module.Category;
import com.pitlite.module.DraggableHud;
import com.pitlite.module.Module;
import com.pitlite.settings.BooleanSetting;
import com.pitlite.utils.DenickManager;
import com.pitlite.utils.DenickService;
import com.pitlite.utils.DenickUtils;
import com.pitlite.utils.HudPositionManager;
import com.pitlite.utils.LobbyPlayerIndex;
import com.pitlite.utils.MojangCache;
import com.pitlite.utils.MysticDisplayUtils;
import com.pitlite.utils.PitMapManager;
import com.pitlite.utils.Utils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NickedList extends Module implements DraggableHud {
    private static final double DEFAULT_X = 5.0;
    private static final double DEFAULT_Y = 70.0;
    private int refreshTickSkip;

    private final BooleanSetting showRegion = new BooleanSetting("Show Region", true);
    private final BooleanSetting showMystics = new BooleanSetting("Show Mystics", true);

    private static NickedList instance;

    private static class NickEntry {
        String nickName;
        String realName;
        String displayName;
        String realDisplayName;
        long joinTime;
        boolean mojangChecked;
        boolean isNick;
        String lastMysticFingerprint = "";
        boolean denickInFlight;
        boolean viewVerifyInFlight;
        String pendingRealName;
        int distance = -1;
        boolean inSpawn;
        String zone;
        String arrow;
        EntityPlayer entity;
        List<MysticDisplayUtils.Label> mystics = new ArrayList<>();
    }

    private final Map<String, NickEntry> nickedPlayers = new ConcurrentHashMap<>();
    private final Set<String> checkedCache = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private final Set<String> confirmedNicks = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private final Set<String> attemptedViewKeys = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private static final long VIEW_VERIFY_NO_GUI_MS = 4000L;

    private int viewGuiCloseTicks = -1;
    private boolean viewGuiSeenDuringVerify;

    private static class ViewVerifyRequest {
        final NickEntry entry;
        String candidateRealName;
        long sentAt;

        ViewVerifyRequest(NickEntry entry, String candidateRealName) {
            this.entry = entry;
            this.candidateRealName = candidateRealName;
        }
    }

    private final Deque<ViewVerifyRequest> viewVerifyQueue = new ArrayDeque<>();
    private ViewVerifyRequest activeViewVerify;

    private String lastLobby = "";
    private long lobbyJoinTime = 0;
    private static final long LOBBY_JOIN_DELAY_MS = 400L;
    private int cachedTotalHeight = 0;
    private int cachedMaxWidth = 80;

    public NickedList() {
        super("Nicked List", "Detects and displays nicked players in the lobby.", Category.RENDER);
        markDangerous();
        instance = this;
        addSetting(showRegion);
        addSetting(showMystics);
    }

    public static String getRealName(String name) {
        if (name == null) {
            return null;
        }
        String cached = DenickManager.get(name);
        if (cached != null) {
            return cached;
        }
        if (instance == null) {
            return null;
        }
        NickEntry entry = instance.nickedPlayers.get(name);
        if (entry != null && entry.isNick && entry.realName != null) {
            return entry.realName;
        }
        return null;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        nickedPlayers.clear();
        viewVerifyQueue.clear();
        activeViewVerify = null;
        attemptedViewKeys.clear();
        DenickManager.clear();
        lobbyJoinTime = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        nickedPlayers.clear();
        viewVerifyQueue.clear();
        activeViewVerify = null;
        attemptedViewKeys.clear();
        DenickManager.clear();
    }

    @SubscribeEvent
    public void onWorldLoad(net.minecraftforge.event.world.WorldEvent.Load event) {
        if (!isToggled()) {
            return;
        }
        nickedPlayers.clear();
        viewVerifyQueue.clear();
        activeViewVerify = null;
        attemptedViewKeys.clear();
        DenickManager.clear();
        lobbyJoinTime = System.currentTimeMillis();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGuiOpen(GuiOpenEvent event) {
        if (!isToggled()) {
            return;
        }
        if (event.gui == null) {
            processNextViewVerify();
            return;
        }
        if (activeViewVerify != null) {
            scheduleCloseViewGuiAfterTicks();
        }
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!isToggled() || activeViewVerify == null || event.type == 2 || mc.thePlayer == null) {
            return;
        }
        String plain = StringUtils.stripControlCodes(event.message.getUnformattedText()).toLowerCase();
        if (!plain.contains("not a valid username")) {
            return;
        }
        String tried = sanitizeViewUsername(activeViewVerify.candidateRealName);
        mc.thePlayer.addChatMessage(new ChatComponentText(
                "\u00a78[\u00a7bNicked List\u00a78] \u00a7c/view failed \u00a77(tried: \u00a7f/view "
                        + (tried != null ? tried : activeViewVerify.candidateRealName) + "\u00a77)"));
        failActiveViewVerify(false);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!isToggled() || event.phase != TickEvent.Phase.END || activeViewVerify == null) {
            return;
        }
        tickScheduledViewGuiClose();

        if (activeViewVerify == null) {
            return;
        }

        if (mc.currentScreen != null && viewGuiCloseTicks < 0) {
            scheduleCloseViewGuiAfterTicks();
            return;
        }

        if (!viewGuiSeenDuringVerify
                && System.currentTimeMillis() - activeViewVerify.sentAt > VIEW_VERIFY_NO_GUI_MS) {
            completeActiveViewVerify();
        }
    }

    private void resetViewGuiCloseState() {
        viewGuiCloseTicks = -1;
        viewGuiSeenDuringVerify = false;
    }

    private void scheduleCloseViewGuiAfterTicks() {
        viewGuiSeenDuringVerify = true;
        if (viewGuiCloseTicks < 0) {
            viewGuiCloseTicks = 1;
        }
    }

    private void tickScheduledViewGuiClose() {
        if (viewGuiCloseTicks < 0) {
            return;
        }
        viewGuiCloseTicks--;
        if (viewGuiCloseTicks != 0) {
            return;
        }
        viewGuiCloseTicks = -1;
        closeOpenGui();
        if (activeViewVerify != null) {
            failActiveViewVerify(false);
        } else {
            viewGuiSeenDuringVerify = false;
        }
    }

    private void closeOpenGui() {
        GuiScreen screen = mc.currentScreen;
        if (screen == null) {
            return;
        }
        screen.onGuiClosed();
        mc.displayGuiScreen(null);
        mc.setIngameFocus();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isToggled()) {
            return;
        }
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (mc.theWorld == null || mc.thePlayer == null) {
            if (!nickedPlayers.isEmpty()) {
                nickedPlayers.clear();
            }
            viewVerifyQueue.clear();
            activeViewVerify = null;
            return;
        }

        String currentLobby = Utils.getScoreboardTitle();
        if (!currentLobby.equals(lastLobby)) {
            lastLobby = currentLobby;
            lobbyJoinTime = System.currentTimeMillis();
            nickedPlayers.clear();
            viewVerifyQueue.clear();
            activeViewVerify = null;
            attemptedViewKeys.clear();
            DenickManager.clear();
        }

        if (!currentLobby.equalsIgnoreCase("THE HYPIXEL PIT")) {
            return;
        }

        if (System.currentTimeMillis() - lobbyJoinTime < LOBBY_JOIN_DELAY_MS) {
            return;
        }

        if (activeViewVerify == null && !viewVerifyQueue.isEmpty() && mc.currentScreen == null) {
            processNextViewVerify();
        }

        if (++refreshTickSkip % 5 != 0) {
            return;
        }
        refreshList();
    }

    private boolean hasPitTabLevelPrefix(String formatted) {
        if (formatted == null || formatted.isEmpty()) {
            return false;
        }
        String plain = StringUtils.stripControlCodes(formatted).trim();
        int open = plain.indexOf('[');
        if (open < 0 || open > 15) {
            return false;
        }
        int close = plain.indexOf(']', open + 1);
        if (close < 0) {
            return false;
        }
        String inside = plain.substring(open + 1, close);
        for (int i = 0; i < inside.length(); i++) {
            if (Character.isDigit(inside.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private EntityPlayer findPlayerEntity(String nickName) {
        return LobbyPlayerIndex.findByName(StringUtils.stripControlCodes(nickName));
    }

    private void refreshList() {
        if (mc.getNetHandler() == null) {
            return;
        }

        Set<String> tabNames = new HashSet<>();
        Map<String, String> tabInfo = new HashMap<>();

        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info.getGameProfile() == null || info.getGameProfile().getName() == null) {
                continue;
            }

            String name = info.getGameProfile().getName();

            String formatted = info.getDisplayName() != null ? info.getDisplayName().getFormattedText() : null;
            if (formatted == null && mc.theWorld != null) {
                net.minecraft.scoreboard.ScorePlayerTeam team = mc.theWorld.getScoreboard().getPlayersTeam(name);
                if (team != null) {
                    formatted = team.formatString(name);
                }
            }

            if (!hasPitTabLevelPrefix(formatted)) {
                continue;
            }

            tabNames.add(name.toLowerCase());
            tabInfo.put(name.toLowerCase(), formatted);

            if (name.equals(mc.thePlayer.getName())) {
                continue;
            }

            String knownReal = DenickManager.get(name);
            if (knownReal != null && (confirmedNicks.contains(name) || looksNicked(formatted, name))) {
                ensureNickEntry(name, knownReal);
                continue;
            }

            if (confirmedNicks.contains(name) && !nickedPlayers.containsKey(name)) {
                NickEntry entry = new NickEntry();
                entry.nickName = name;
                entry.isNick = true;
                entry.mojangChecked = true;
                entry.joinTime = System.currentTimeMillis();
                nickedPlayers.put(name, entry);
                continue;
            }

            if (formatted != null && looksNicked(formatted, name)) {
                if (!nickedPlayers.containsKey(name) && !checkedCache.contains(name)) {
                    NickEntry entry = new NickEntry();
                    entry.nickName = name;
                    entry.displayName = formatted;
                    entry.joinTime = System.currentTimeMillis();
                    nickedPlayers.put(name, entry);
                    checkMojangAsync(entry);
                }
            }
        }

        nickedPlayers.entrySet().removeIf(entry -> !tabNames.contains(entry.getValue().nickName.toLowerCase()));

        for (NickEntry e : nickedPlayers.values()) {
            if (!e.isNick) {
                continue;
            }

            String currentNickFormatted = tabInfo.get(e.nickName.toLowerCase());
            if (currentNickFormatted != null) {
                e.displayName = currentNickFormatted;
            }

            if (e.realName == null) {
                e.realName = DenickManager.get(e.nickName);
                if (e.realName != null) {
                    e.denickInFlight = false;
                }
            }

            if (e.realName != null) {
                String currentRealFormatted = tabInfo.get(e.realName.toLowerCase());
                if (currentRealFormatted != null) {
                    e.realDisplayName = currentRealFormatted;
                }
            }

            EntityPlayer found = findPlayerEntity(e.nickName);
            e.entity = found;

            if (e.entity != null) {
                e.distance = (int) mc.thePlayer.getDistanceToEntity(e.entity);
                e.inSpawn = PitMapManager.isInSpawn(e.entity.posX, e.entity.posY, e.entity.posZ);
                e.zone = PitMapManager.getZone(e.entity.posX, e.entity.posY, e.entity.posZ);
                e.arrow = getArrow(e.entity);

                if (e.realName == null && !e.viewVerifyInFlight) {
                    String fingerprint = DenickUtils.getMysticEquipmentFingerprint(e.entity);
                    if (!fingerprint.isEmpty() && !fingerprint.equals(e.lastMysticFingerprint)) {
                        e.lastMysticFingerprint = fingerprint;
                        attemptDenick(e);
                    }
                }
                e.mystics = MysticDisplayUtils.collect(e.entity);
            } else {
                e.distance = -1;
                e.zone = null;
                e.arrow = null;
                e.mystics.clear();
            }
        }

        int lh = mc.fontRendererObj.FONT_HEIGHT + 2;
        int visibleCount = 0;
        FontRenderer fr = mc.fontRendererObj;
        int maxW = fr.getStringWidth("Nicked List");
        for (NickEntry e : nickedPlayers.values()) {
            if (!e.isNick) {
                continue;
            }
            visibleCount++;
            maxW = Math.max(maxW, measureEntryWidth(fr, e));
        }
        cachedTotalHeight = visibleCount == 0 ? 0 : lh * (1 + visibleCount);
        cachedMaxWidth = Math.max(80, maxW);
    }

    private void ensureNickEntry(String nick, String realName) {
        NickEntry entry = nickedPlayers.get(nick);
        if (entry == null) {
            entry = new NickEntry();
            entry.nickName = nick;
            entry.joinTime = System.currentTimeMillis();
            nickedPlayers.put(nick, entry);
        }
        entry.isNick = true;
        entry.mojangChecked = true;
        entry.realName = realName;
        confirmedNicks.add(nick);
    }

    private boolean looksNicked(String formatted, String name) {
        if (formatted != null && (formatted.contains("\u00a77[") || formatted.contains("\u00a79["))) {
            return true;
        }
        return confirmedNicks.contains(name);
    }

    private void checkMojangAsync(NickEntry entry) {
        if (checkedCache.contains(entry.nickName)) {
            return;
        }
        checkedCache.add(entry.nickName);

        MojangCache.checkNickStatusAsync(entry.nickName, status -> {
            entry.mojangChecked = true;
            if (status == MojangCache.NickStatus.NICK) {
                entry.isNick = true;
                confirmedNicks.add(entry.nickName);
            } else if (status == MojangCache.NickStatus.REAL) {
                entry.isNick = false;
            } else {
                checkedCache.remove(entry.nickName);
            }
        });
    }

    private int measureEntryWidth(FontRenderer fr, NickEntry e) {
        int width = measureNameWidth(fr, e);
        if (showMystics.enabled && !e.mystics.isEmpty()) {
            width += fr.getStringWidth(" - ");
            for (int i = 0; i < e.mystics.size(); i++) {
                if (i > 0) {
                    width += fr.getStringWidth("/");
                }
                width += fr.getStringWidth(e.mystics.get(i).text);
            }
        }
        if (e.distance >= 0) {
            width += 80;
        }
        return width;
    }

    private int measureNameWidth(FontRenderer fr, NickEntry e) {
        String renderName;
        if (e.realName != null) {
            String realTag = e.realDisplayName != null ? e.realDisplayName : "\u00a7a" + e.realName;
            renderName = realTag + " \u00a77(" + e.nickName + ")";
        } else {
            renderName = e.displayName != null ? e.displayName : "\u00a7c" + e.nickName;
        }
        return fr.getStringWidth(renderName);
    }

    private void attemptDenick(NickEntry entry) {
        if (entry.entity == null || entry.denickInFlight || entry.viewVerifyInFlight
                || entry.realName != null) {
            return;
        }

        entry.denickInFlight = true;
        DenickService.denickPlayerAsync(entry.entity, entry.nickName, (nick, result) -> {
            if (result == null || result.isEmpty()) {
                entry.denickInFlight = false;
                return;
            }
            queueViewVerify(entry, result);
        });
    }

    private static String viewAttemptKey(String nickName, String candidateRealName) {
        return nickName.toLowerCase() + "|" + candidateRealName.toLowerCase();
    }

    private boolean hasAttemptedView(String nickName, String candidateRealName) {
        return attemptedViewKeys.contains(viewAttemptKey(nickName, candidateRealName));
    }

    private void markAttemptedView(String nickName, String candidateRealName) {
        attemptedViewKeys.add(viewAttemptKey(nickName, candidateRealName));
    }

    private void queueViewVerify(NickEntry entry, String candidateRealName) {
        String sanitized = sanitizeViewUsername(candidateRealName);
        if (sanitized == null) {
            entry.denickInFlight = false;
            return;
        }
        if (hasAttemptedView(entry.nickName, sanitized)) {
            entry.denickInFlight = false;
            return;
        }
        for (ViewVerifyRequest pending : viewVerifyQueue) {
            if (pending.entry == entry && pending.candidateRealName.equalsIgnoreCase(sanitized)) {
                return;
            }
        }
        if (activeViewVerify != null && activeViewVerify.entry == entry
                && activeViewVerify.candidateRealName.equalsIgnoreCase(sanitized)) {
            return;
        }
        viewVerifyQueue.offer(new ViewVerifyRequest(entry, sanitized));
        processNextViewVerify();
    }

    private void processNextViewVerify() {
        if (activeViewVerify != null || mc.thePlayer == null) {
            return;
        }
        if (mc.currentScreen != null) {
            return;
        }
        ViewVerifyRequest request = viewVerifyQueue.poll();
        if (request == null) {
            return;
        }
        if (!request.entry.isNick || request.entry.realName != null) {
            request.entry.denickInFlight = false;
            processNextViewVerify();
            return;
        }

        String viewName = sanitizeViewUsername(request.candidateRealName);
        if (viewName == null || viewName.length() < 3) {
            request.entry.denickInFlight = false;
            request.entry.viewVerifyInFlight = false;
            processNextViewVerify();
            return;
        }

        request.candidateRealName = viewName;
        resetViewGuiCloseState();
        activeViewVerify = request;
        request.sentAt = System.currentTimeMillis();
        request.entry.viewVerifyInFlight = true;
        request.entry.pendingRealName = viewName;
        markAttemptedView(request.entry.nickName, viewName);
        mc.thePlayer.sendChatMessage("/view " + viewName);
    }

    private static String sanitizeViewUsername(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String clean = StringUtils.stripControlCodes(name).trim().replace("\"", "").replace("'", "");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clean.length() && sb.length() < 16; i++) {
            char c = clean.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_') {
                sb.append(c);
            }
        }
        return sb.length() >= 3 ? sb.toString() : null;
    }

    private void completeActiveViewVerify() {
        if (activeViewVerify == null) {
            return;
        }
        NickEntry entry = activeViewVerify.entry;
        String realName = activeViewVerify.candidateRealName;
        entry.realName = realName;
        entry.pendingRealName = null;
        entry.viewVerifyInFlight = false;
        entry.denickInFlight = false;
        confirmedNicks.add(entry.nickName);
        DenickManager.put(entry.nickName, realName);
        activeViewVerify = null;
        resetViewGuiCloseState();
        processNextViewVerify();
    }

    private void failActiveViewVerify(boolean timedOut) {
        if (activeViewVerify == null) {
            return;
        }
        NickEntry entry = activeViewVerify.entry;
        entry.pendingRealName = null;
        entry.viewVerifyInFlight = false;
        entry.denickInFlight = false;
        closeOpenGui();
        activeViewVerify = null;
        resetViewGuiCloseState();
        processNextViewVerify();
    }

    private String getArrow(EntityPlayer target) {
        double dx = target.posX - mc.thePlayer.posX;
        double dz = target.posZ - mc.thePlayer.posZ;
        double angleToTarget = Math.toDegrees(Math.atan2(-dx, dz));
        double playerYaw = mc.thePlayer.rotationYaw % 360;
        double rel = ((angleToTarget - playerYaw) % 360 + 360) % 360;
        if (rel >= 337.5 || rel < 22.5) return "\u2191";
        if (rel < 67.5) return "\u2197";
        if (rel < 112.5) return "\u2192";
        if (rel < 157.5) return "\u2198";
        if (rel < 202.5) return "\u2193";
        if (rel < 247.5) return "\u2199";
        if (rel < 292.5) return "\u2190";
        return "\u2196";
    }

    public void renderStacked() {
        if (mc.thePlayer == null || nickedPlayers.isEmpty()) {
            return;
        }

        FontRenderer fr = mc.fontRendererObj;
        int lh = fr.FONT_HEIGHT + 2;
        int posX = getRenderX();
        int posY = getRenderY();

        List<NickEntry> sorted = new ArrayList<>();
        for (NickEntry entry : nickedPlayers.values()) {
            if (entry.isNick) {
                sorted.add(entry);
            }
        }
        if (sorted.isEmpty()) {
            return;
        }

        sorted.sort((a, b) -> Integer.compare(
                a.distance < 0 ? 1000 : a.distance,
                b.distance < 0 ? 1000 : b.distance));

        fr.drawStringWithShadow("Nicked List", posX, posY, 0xFF55FF);
        float y = posY + lh;

        for (NickEntry e : sorted) {
            int currentX = posX;
            String renderName;
            if (e.realName != null) {
                String realTag = e.realDisplayName != null ? e.realDisplayName : "\u00a7a" + e.realName;
                renderName = realTag + " \u00a77(" + e.nickName + ")";
            } else {
                renderName = e.displayName != null ? e.displayName : "\u00a7c" + e.nickName;
            }
            fr.drawStringWithShadow(renderName, currentX, (int) y, 0xFFFFFF);
            currentX += fr.getStringWidth(renderName);

            if (showMystics.enabled && !e.mystics.isEmpty()) {
                fr.drawStringWithShadow(" - ", currentX, (int) y, 0xFFFFFF);
                currentX += fr.getStringWidth(" - ");
                for (int i = 0; i < e.mystics.size(); i++) {
                    if (i > 0) {
                        fr.drawStringWithShadow("/", currentX, (int) y, 0xFFFFFF);
                        currentX += fr.getStringWidth("/");
                    }
                    MysticDisplayUtils.Label mystic = e.mystics.get(i);
                    fr.drawStringWithShadow(mystic.text, currentX, (int) y, mystic.color);
                    currentX += fr.getStringWidth(mystic.text);
                }
            }

            if (e.distance >= 0) {
                fr.drawStringWithShadow(" [", currentX, (int) y, 0xAAAAAA);
                currentX += fr.getStringWidth(" [");
                String distColor = e.distance < 15 ? "\u00a7a" : (e.distance < 50 ? "\u00a76" : "\u00a7c");
                String distText = distColor + e.distance;
                fr.drawStringWithShadow(distText, currentX, (int) y, 0xFFFFFF);
                currentX += fr.getStringWidth(distText);

                if (e.arrow != null && !e.arrow.isEmpty()) {
                    String arrowText = distColor + e.arrow;
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(currentX + 1, y - 1, 0);
                    GlStateManager.scale(1.25f, 1.25f, 1.0f);
                    fr.drawStringWithShadow(arrowText, 0, 0, 0xFFFFFF);
                    GlStateManager.popMatrix();
                    currentX += (int) (fr.getStringWidth(arrowText) * 1.25f) + 2;
                }

                if (showRegion.enabled && e.zone != null && !e.zone.equals("Unknown")) {
                    fr.drawStringWithShadow(" \u00a77- ", currentX, (int) y, 0xFFFFFF);
                    currentX += fr.getStringWidth(" \u00a77- ");
                    String zoneColor = getZoneColor(e.zone);
                    int hexColor = e.zone.equalsIgnoreCase("spring") ? 0xFFC0CB : 0xFFFFFF;
                    String zoneText = zoneColor + "\u00a7l" + e.zone.toUpperCase();
                    fr.drawStringWithShadow(zoneText, currentX, (int) y, hexColor);
                    currentX += fr.getStringWidth(zoneText);
                }
                fr.drawStringWithShadow(" \u00a77]", currentX, (int) y, 0xFFFFFF);
            }
            y += lh;
        }
    }

    private String getZoneColor(String zone) {
        String zLower = zone.toLowerCase();
        if (zLower.equals("spawn") || zLower.equals("garden") || zLower.equals("sewer") || zLower.equals("seaweed")) {
            return "\u00a7a";
        }
        if (zLower.equals("pit") || zLower.equals("demon") || zLower.equals("lava") || zLower.equals("port") || zLower.equals("geyser")) {
            return "\u00a7c";
        }
        if (zLower.equals("palace") || zLower.equals("winter") || zLower.equals("city")) {
            return "\u00a7b";
        }
        if (zLower.equals("badlands") || zLower.equals("autumn") || zLower.equals("mountains")
                || zLower.equals("farm") || zLower.equals("shipwreck")) {
            return "\u00a76";
        }
        if (zLower.equals("fortress")) {
            return "\u00a74";
        }
        if (zLower.equals("angel") || zLower.equals("summer") || zLower.equals("temple")) {
            return "\u00a7e";
        }
        if (zLower.equals("spring")) {
            return "";
        }
        if (zLower.equals("overspawn")) {
            return "\u00a75";
        }
        if (zLower.equals("water")) {
            return "\u00a79";
        }
        if (zLower.equals("sky")) {
            return "\u00a7f";
        }
        if (zLower.equals("forest")) {
            return "\u00a72";
        }
        return "\u00a7e";
    }

    public int getConfigY() {
        return (int) HudPositionManager.getY(getHudKey(), DEFAULT_Y);
    }

    @Override
    public String getHudKey() {
        return getName();
    }

    @Override
    public boolean isHudVisible() {
        for (NickEntry entry : nickedPlayers.values()) {
            if (entry.isNick) {
                return true;
            }
        }
        return false;
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
        return cachedMaxWidth;
    }

    @Override
    public int getHudHeight() {
        return Math.max(mc.fontRendererObj.FONT_HEIGHT + 2, cachedTotalHeight);
    }
}
