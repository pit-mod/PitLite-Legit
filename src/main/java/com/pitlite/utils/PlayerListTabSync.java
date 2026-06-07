package com.pitlite.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

import java.util.ArrayList;
import java.util.List;

public final class PlayerListTabSync {

    public static final class TabPlayer {
        public final String uuid;
        public final String name;
        public final String formattedName;

        public TabPlayer(String uuid, String name, String formattedName) {
            this.uuid = uuid;
            this.name = name;
            this.formattedName = formattedName;
        }
    }

    private PlayerListTabSync() {
    }

    public static List<TabPlayer> collectTabPlayers(Minecraft mc) {
        List<TabPlayer> players = new ArrayList<>();
        if (mc.getNetHandler() == null) {
            return players;
        }

        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info.getGameProfile() == null || info.getGameProfile().getName() == null) {
                continue;
            }
            String name = info.getGameProfile().getName();
            String uuid = ProfileLookup.normalizeUuid(info.getGameProfile().getId().toString());
            String formatted = info.getDisplayName() != null ? info.getDisplayName().getFormattedText() : null;
            if (formatted == null && mc.theWorld != null) {
                net.minecraft.scoreboard.ScorePlayerTeam team =
                        mc.theWorld.getScoreboard().getPlayersTeam(name);
                if (team != null) {
                    formatted = team.formatString(name);
                }
            }
            players.add(new TabPlayer(uuid, name, formatted));
        }
        return players;
    }

    public static PlayerListStore.ListType getListType(String uuid, String name) {
        if (uuid != null && KOSManager.isEnabled() && KOSManager.isKOSByUuid(uuid)) {
            return PlayerListStore.ListType.KOS;
        }
        if (uuid != null && FriendManager.isFriendByUuid(uuid)) {
            return PlayerListStore.ListType.FRIEND;
        }
        if (uuid != null && TruceManager.isTruceByUuid(uuid)) {
            return PlayerListStore.ListType.TRUCE;
        }
        if (KOSManager.isEnabled() && KOSManager.isKOSByName(name)) {
            return PlayerListStore.ListType.KOS;
        }
        if (FriendManager.isFriendByName(name)) {
            return PlayerListStore.ListType.FRIEND;
        }
        if (TruceManager.isTruceByName(name)) {
            return PlayerListStore.ListType.TRUCE;
        }
        return null;
    }

    public static boolean isOnList(String uuid, String name) {
        return getListType(uuid, name) != null;
    }

    public static String syncRename(PlayerListStore.ListType type, String uuid, String tabName) {
        String previous = PlayerListStore.syncDisplayName(type, uuid, tabName);
        if (previous != null) {
            ConfigSaveDebouncer.markDirty();
        }
        return previous;
    }

    public static String formatJoinMessage(PlayerListStore.ListType type, String tabName, String previousName) {
        String suffix = previousName != null && !previousName.equalsIgnoreCase(tabName)
                ? " \u00a77(was \u00a7f" + previousName + "\u00a77)"
                : "";
        switch (type) {
            case KOS:
                return "\u00a7cKOS Joined: \u00a7f" + tabName + suffix;
            case FRIEND:
                return "\u00a7aFriend Joined: \u00a7f" + tabName + suffix;
            case TRUCE:
                return TruceManager.COLOR_CODE + "Truce Joined: \u00a7f" + tabName + suffix;
            default:
                return "\u00a7f" + tabName + suffix;
        }
    }

    public static String formatRenameMessage(PlayerListStore.ListType type, String tabName, String previousName) {
        switch (type) {
            case KOS:
                return "\u00a7cKOS renamed: \u00a7f" + tabName + " \u00a77(was \u00a7f" + previousName + "\u00a77)";
            case FRIEND:
                return "\u00a7aFriend renamed: \u00a7f" + tabName + " \u00a77(was \u00a7f" + previousName + "\u00a77)";
            case TRUCE:
                return TruceManager.COLOR_CODE + "Truce renamed: \u00a7f" + tabName
                        + " \u00a77(was \u00a7f" + previousName + "\u00a77)";
            default:
                return "\u00a7f" + tabName + " \u00a77(was \u00a7f" + previousName + "\u00a77)";
        }
    }

    public static String formatTabJoinMessage(PlayerListStore.ListType type, String tabName, String previousName) {
        String suffix = previousName != null && !previousName.equalsIgnoreCase(tabName)
                ? " \u00a77(was \u00a7f" + previousName + "\u00a77)"
                : "";
        switch (type) {
            case KOS:
                return "\u00a7c[KOS joined tab] \u00a7f" + tabName + suffix;
            case FRIEND:
                return "\u00a7a[Friend joined tab] \u00a7f" + tabName + suffix;
            default:
                return "\u00a7f" + tabName + suffix;
        }
    }
}
